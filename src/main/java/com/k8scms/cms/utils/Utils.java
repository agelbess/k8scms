/*
 * MIT License
 * Copyright (c) 2020 Alexandros Gelbessis
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.k8scms.cms.utils;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.model.GetOptions;
import com.k8scms.cms.model.Permissions;
import io.quarkus.security.UnauthorizedException;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jasypt.util.text.BasicTextEncryptor;
import org.mindrot.jbcrypt.BCrypt;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {
    }

    static Jsonb jsonb;

    static {
        JsonbConfig jsonbConfig = new JsonbConfig();
        jsonbConfig.setProperty(JsonbConfig.FORMATTING, true);
        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    public static ToString stringify(Object o) {
        return new ToString(() -> jsonb.toJson(o));
    }

    public static String toJson(Object o) {
        return jsonb.toJson(o);
    }

    public static <T> T fromJson(String data, Class<T> clazz) {
        return jsonb.fromJson(data, clazz);
    }

    public static String fromResourcePathToString(String path) {
        return convertToString(Utils.class.getResourceAsStream(path));
    }

    public static String convertToString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
    }

    public static Document documentFromUriInfo(UriInfo uriInfo) {
        Document filter = new Document();
        uriInfo.getQueryParameters().forEach(
                (key, values) -> {
                    filter.put(key, values.get(0));
                }
        );
        return filter;
    }

    public static Document getDocumentWithoutGetOptions(Document document) {
        Document filter = new Document();
        document.entrySet().forEach(entry -> {
            if (!Arrays.asList(Constants.GET_OPTIONS_QUERY_PARAMS).contains(entry.getKey())) {
                filter.put(entry.getKey(), entry.getValue());
            }
        });
        return filter;
    }

    // all values are passed as string
    public static GetOptions getGetOptionsFromDocument(Document document) {
        GetOptions getOptions = new GetOptions();
        document.forEach(
                (key, value) -> {
                    switch (key) {
                        case Constants.QUERY_PARAM_SORT:
                            getOptions.setSort((String) value);
                            break;
                        case Constants.QUERY_PARAM_SORT_DIRECTION:
                            getOptions.setSortDirection(((Decimal128) value).intValue());
                            break;
                        case Constants.QUERY_PARAM_SKIP:
                            getOptions.setSkip(((Decimal128) value).intValue());
                            break;
                        case Constants.QUERY_PARAM_LIMIT:
                            getOptions.setLimit(((Decimal128) value).intValue());
                            break;
                        case Constants.QUERY_PARAM_NO_LIMIT:
                            getOptions.setNoLimit(Boolean.parseBoolean(value.toString()));
                            break;
                    }
                }
        );
        return getOptions;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String encrypt1(String text, String key) {
        String salted = createSalt(text, key);
        return hashSaltedPassword(salted, false);
    }

    public static boolean checkEncrypt1(String text, String key, String encryptedHash) {
        String salted = createSalt(text, key);
        if (encryptedHash.length() < 3) {
            return false;
        }
        return BCrypt.checkpw(salted, encryptedHash);
    }

    // use the text, a key (from properties file?) and a constant
    private static String createSalt(String text, String key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            String hashedKey = toHexString(md.digest());

            md.update(String.format("%s.%s.%s", hashedKey, Constants.SALT_KEY, text).getBytes());
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }

    private static String hashSaltedPassword(String saltedPassword, boolean replaceAWithB) {
        String hashed = BCrypt.hashpw(saltedPassword, BCrypt.gensalt(12));
        if (replaceAWithB) {
            hashed = hashed.substring(0, 2) + "b" + hashed.substring(3);
        }
        return hashed;
    }

    public static String encrypt2(String text, String password) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(password + Constants.TWO_WAY_SALT_KEY);
        return encryptor.encrypt(text);
    }

    public static String decrypt2(String text, String password) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(password + Constants.TWO_WAY_SALT_KEY);
        return encryptor.decrypt(text);
    }

    public static void checkLdapUser(String name, String password, CmsProperties cmsProperties, SecretProperties secretProperties) {
        // connect to ldap with user credentials to validate them
        DirContext userContext = null;
        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            secretProperties.getLdapProtocol().ifPresent(protocol -> props.put(Context.SECURITY_PROTOCOL, protocol));
            props.put(Context.SECURITY_AUTHENTICATION, secretProperties.getLdapAuthentication());
            props.put(Context.PROVIDER_URL, secretProperties.getLdapUrl());
            props.put(Context.SECURITY_PRINCIPAL, toDN(name));
            props.put(Context.SECURITY_CREDENTIALS, password);
            userContext = new InitialDirContext(props);
        } catch (NamingException e) {
            throw generateUnauthorizedException("User not found in ldap: " + e.getMessage(), cmsProperties.getEnv());
        }
    }

    // user@domain.com -> cn=username,ou=domain users,dc=example,dc=com
    private static String toDN(String name) {
        String[] tokens = name.split("@");
        if (tokens.length < 2) {
            throw new IllegalArgumentException(String.format("Name '$s' not in user@domain.com format", name));
        }
        String[] domainTokens = tokens[1].split("\\.");
        return "cn=" + tokens[0] + ",dc=" + String.join(",dc=", Arrays.asList(domainTokens));
    }

    public static boolean hasPermission(String cluster, String database, String collection, String method, List<Permissions> permissionsList) {
        for (Permissions permissions : permissionsList) {
            if (cluster.matches(permissions.getCluster()) &&
                    database.matches(permissions.getDatabase()) &&
                    collection.matches(permissions.getCollection()) &&
                    method.matches(permissions.getMethod())) {
                return true;
            }
        }
        return false;
    }

    public static UnauthorizedException generateUnauthorizedException(String message, String env) {
        if (env.equals(Constants.ENV_PROD)) {
            return new UnauthorizedException();
        } else {
            return new UnauthorizedException(message);
        }
    }

    public static String replaceProperties(String text) {
        return replaceProperties(text, property -> ConfigProvider.getConfig().getValue(property, String.class));
    }

    static String replaceProperties(String text, Function<String, String> supplier) {
        Pattern pattern = Pattern.compile(Constants.REGEX_PROPERTIES_REPLACE);
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(matchResult -> {
            String property = text.substring(matcher.start() + 2, matcher.end() - 1);
            return supplier.apply(property);
        });
    }

    public static boolean isCharset(String text, String charset) throws UnsupportedEncodingException {
        return Charset.forName(charset).newEncoder().canEncode(text);
    }

    public static Document getUserFilter(String name) {
        return new Document("name",
                new Document("$regex", String.format("^%s$", name)).append("$options", "i"));
    }
}
