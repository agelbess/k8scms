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

package com.k8scms.cms.filter;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.model.Filter;
import com.k8scms.cms.model.GetOptions;
import com.k8scms.cms.model.Permissions;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApiLocalAuthenticationFilter
public class ApiLocalAuthentication implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiLocalAuthentication.class);

    @Inject
    CmsProperties cmsProperties;

    @Inject
    SecretProperties secretProperties;

    @Inject
    MongoService mongoService;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        String[] userNamePass = getBasicAuthUserPass(containerRequestContext);
        if (userNamePass != null) {
            GetOptions getOptions = new GetOptions();
            getOptions.setLimit(1);
            Document user = mongoService.get(
                    cmsProperties.getCluster(),
                    cmsProperties.getDatabase(),
                    cmsProperties.getCollectionUser(),
                    Utils.getUserFilter(userNamePass[0]),
                    getOptions)
                    .toUni()
                    .await()
                    .indefinitely();
            String securityRealm = user.getString("securityRealm");
            switch (securityRealm) {
                case Constants.SECURITY_REALM_LOCAL:
                    if (!Utils.checkEncrypt1(userNamePass[1], secretProperties.getSecretEncryptionKey(), (String) user.get("password"))) {
                        throw Utils.generateUnauthorizedException("Invalid password", cmsProperties.getEnv());
                    }
                    break;
                case Constants.SECURITY_REALM_LDAP:
                    Utils.checkLdapUser(userNamePass[0], userNamePass[1], cmsProperties, secretProperties);
                default:
                    throw Utils.generateUnauthorizedException(String.format("Invalid security realm '%s'", securityRealm), cmsProperties.getEnv());
            }
            initContext(user, containerRequestContext);
        } else {
            Cookie uidCookie = containerRequestContext.getCookies().get(Constants.COOKIE_UID);
            String[] tokens = null;
            if (uidCookie == null) {
                throw Utils.generateUnauthorizedException("Cookie is missing or expired", cmsProperties.getEnv());
            } else {
                String uid = uidCookie.getValue();
                if (uid == null) {
                    throw Utils.generateUnauthorizedException("Cookie value is missing", cmsProperties.getEnv());
                } else {
                    uid = new String(Base64.getDecoder().decode(uid));
                    tokens = uid.split("\\.");
                    if (tokens.length != 3) {
                        throw Utils.generateUnauthorizedException("Invalid cookie, tokens are not 3", cmsProperties.getEnv());
                    } else {
                        String random = tokens[0];
                        String encryptedUser = tokens[1];
                        Date loginDate = new Date(Long.parseLong(tokens[2]));
                        // no need for this, cookie's age will do its job
                        if (new Date().getTime()
                                - loginDate.getTime() > secretProperties.getSessionTimeout() * 1000) {
                            throw Utils.generateUnauthorizedException("Cookie timed out", cmsProperties.getEnv());
                        }
                        Document decryptedUser = Document.parse(Utils.decrypt2(encryptedUser, secretProperties.getSessionEncryptionKey() + "." + random));
                        logger.trace("Decrypted user from cookie: {}", decryptedUser);
                        GetOptions getOptions = new GetOptions();
                        getOptions.setLimit(1);
                        Document user = mongoService.get(
                                cmsProperties.getCluster(),
                                cmsProperties.getDatabase(),
                                cmsProperties.getCollectionUser(),
                                Utils.getUserFilter(decryptedUser.getString("name")),
                                getOptions)
                                .toUni()
                                .await()
                                .indefinitely();
                        if (user == null) {
                            throw Utils.generateUnauthorizedException(String.format("User %s not found in %s.%s.%s", decryptedUser.get("name"), cmsProperties.getCluster(), cmsProperties.getDatabase(), cmsProperties.getCollectionUser()), cmsProperties.getEnv());
                        }
                        initContext(user, containerRequestContext);
                    }
                }
            }
        }
    }

    private void initContext(Document user, ContainerRequestContext containerRequestContext) {
        containerRequestContext.setProperty(Constants.CONTEXT_PROPERTY_USER, user);
        containerRequestContext.setProperty(Constants.CONTEXT_PROPERTY_USER_NAME, user.getString("name"));
        // find roles
        Document inFilter = new Document();
        inFilter.put("$in", user.get("roles") != null ? user.get("roles") : new ArrayList<>());
        Document rolesFilter = new Document();
        rolesFilter.put("name", inFilter);
        List<Document> roles = mongoService.get(cmsProperties.getCluster(), cmsProperties.getDatabase(), cmsProperties.getCollectionRole(), rolesFilter)
                .collectItems().asList()
                .await()
                .indefinitely();
        containerRequestContext.setProperty(Constants.CONTEXT_PROPERTY_USER_ROLES, roles);

        List<Permissions> permissions = roles.stream()
                .map(document -> document.getList("permissions", Document.class))
                .flatMap(List::stream)
                .map(Permissions::new)
                .collect(Collectors.toList());
        // add the user defined permissions
        Optional.ofNullable(user.getList("permissions", Document.class))
                .ifPresent(documents -> documents.forEach(document -> permissions.add(new Permissions(document))));
        containerRequestContext.setProperty(Constants.CONTEXT_PROPERTY_USER_PERMISSIONS, permissions);

        List<Filter> filters = roles.stream()
                .map(document -> document.getList("filters", Document.class))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(Filter::new)
                .collect(Collectors.toList());
        // add the user defined permissions
        Optional.ofNullable(user.getList("filters", Document.class))
                .ifPresent(documents -> documents.forEach(document -> filters.add(new Filter(document))));
        containerRequestContext.setProperty(Constants.CONTEXT_PROPERTY_USER_FILTERS, filters);
    }

    public static String[] getBasicAuthUserPass(ContainerRequestContext containerRequestContext) {
        String authorization = containerRequestContext.getHeaderString("Authorization");
        if (authorization == null) {
            return null;
        } else {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            String credentials =
                    new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            // credentials = username:password
            String[] values = credentials.split(":", 2);
            return values;
        }
    }

}
