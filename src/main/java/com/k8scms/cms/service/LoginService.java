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

package com.k8scms.cms.service;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.ModelUtils;
import com.k8scms.cms.utils.Utils;
import io.quarkus.security.UnauthorizedException;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.Collections;

@ApplicationScoped
public class LoginService {

    @Inject
    ModelService modelService;

    @Inject
    MongoService mongoService;

    @Inject
    CmsProperties cmsProperties;

    @Inject
    SecretProperties secretProperties;

    public Document login(Document userNamePassword) {
        String name = userNamePassword.getString("name");
        String password = userNamePassword.getString("password");
        Document user = findUser(Utils.getUserFilter(name));
        if (user == null) {
            // find at least one
            if (findUser(new Document()) == null) {
                // No one exists, create one
                user = createSu(userNamePassword);
            } else {
                // User not found
                throw new UnauthorizedException("User not found");
            }
        } else {
            String securityRealm = user.getString("securityRealm");
            if (securityRealm == null) {
                throw new IllegalArgumentException("SecurityRealm not set for user");
            }
            switch (securityRealm) {
                case Constants.SECURITY_REALM_LOCAL:
                    if (!Utils.checkEncrypt1(
                            password,
                            secretProperties.getSecretEncryptionKey(),
                            (String) user.get("password"))) {
                        throw Utils.generateUnauthorizedException("Invalid password", cmsProperties.getEnv());
                    }
                    break;
                case Constants.SECURITY_REALM_LDAP:
                    Utils.checkLdapUser(name, password, cmsProperties, secretProperties);
                    break;
                default:
                    throw Utils.generateUnauthorizedException(String.format("Invalid security realm '%s'", securityRealm), cmsProperties.getEnv());
            }
        }
        // remove the password
        user.put("password", null);
        return user;
    }

    public Document findUser(Document filter) {
        return mongoService.get(
                cmsProperties.getCluster(),
                cmsProperties.getDatabase(),
                cmsProperties.getCollectionUser(),
                filter
        )
                .toUni()
                .await()
                .indefinitely();
    }

    public Document createSu(Document userNamePassword) {
        Document user = new Document();
        user.put("name", userNamePassword.getString("name"));
        user.put("permissions", Arrays.asList(new Document()
                .append("cluster", ".*")
                .append("database", ".*")
                .append("collection", ".*")
                .append("method", ".*")));
        user.put("securityRealm", Constants.SECURITY_REALM_LOCAL);
        user.put("password", Utils.encrypt1(
                userNamePassword.getString("password"),
                secretProperties.getSecretEncryptionKey()
        ));
        Model model = modelService.getModel(cmsProperties.getCluster(), cmsProperties.getDatabase(), cmsProperties.getCollectionUser());
        ModelUtils.applySystemFields(HttpMethod.POST, user, model);
        mongoService.post(
                cmsProperties.getCluster(),
                cmsProperties.getDatabase(),
                cmsProperties.getCollectionUser(),
                Collections.singletonList(user),
                true)
                .await()
                .indefinitely();
        return user;
    }
}
