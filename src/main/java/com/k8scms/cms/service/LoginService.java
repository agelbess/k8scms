package com.k8scms.cms.service;

import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import io.quarkus.security.UnauthorizedException;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;

@ApplicationScoped
public class LoginService {

    @Inject
    MongoService mongoService;

    @Inject
    CmsProperties cmsProperties;

    @Inject
    SecretProperties secretProperties;

    public Document login(Document userNamePassword) {
        Document userFilter = new Document();
        String name = userNamePassword.getString("name");
        String password = userNamePassword.getString("password");
        userFilter.put("name", name);
        Document user = findUser(userFilter);
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
                cmsProperties.getDatabase(),
                cmsProperties.getCollectionUser(),
                filter
        )
                .toUni()
                .await()
                .atMost(cmsProperties.getMongoTimeoutDuration());
    }

    public Document createSu(Document userNamePassword) {
        Document suRole = new Document();
        suRole.put("name", "delete");
        suRole.put("permissions", ".*:.*:.*");
        mongoService.post(
                cmsProperties.getDatabase(),
                cmsProperties.getCollectionRole(),
                suRole)
                .await()
                .atMost(cmsProperties.getMongoTimeoutDuration());
        Document user = new Document();
        user.put("name", userNamePassword.getString("name"));
        user.put("securityRealm", Constants.SECURITY_REALM_LOCAL);
        user.put("password", Utils.encrypt1(
                userNamePassword.getString("password"),
                secretProperties.getSecretEncryptionKey()
        ));
        user.put("roles", Collections.singletonList("delete"));
        mongoService.post(
                cmsProperties.getDatabase(),
                cmsProperties.getCollectionUser(),
                user)
                .await()
                .atMost(cmsProperties.getMongoTimeoutDuration());
        return user;
    }
}
