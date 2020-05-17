package com.k8scms.cms;

import io.quarkus.arc.config.ConfigProperties;

import java.util.Optional;

@ConfigProperties()
public interface SecretProperties {

    public String getSessionEncryptionKey();

    public int getSessionTimeout();

    public String getSecretEncryptionKey();

    public String getLdapUrl();

    public Optional<String> getLdapProtocol();

    public String getLdapAuthentication();

    public String getLdapAdminUsername();

    public String getLdapAdminPassword();
}
