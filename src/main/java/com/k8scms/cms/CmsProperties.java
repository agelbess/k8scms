package com.k8scms.cms;

import io.quarkus.arc.config.ConfigProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigProperties()
public interface CmsProperties {

    public String getEnv();

    public String getLog();

    public Integer getLimit();

    public Integer getPageSize();

    public Integer getMongoTimeout();

    public String getDatabase();

    public String getCollectionModel();

    public String getCollectionUser();

    public String getCollectionRole();

    public String getCollectionLog();

    public String getCollectionScheduler();

    public String getCollectionSchedulerTask();

    default Duration getMongoTimeoutDuration() {
        return Duration.of(getMongoTimeout(), ChronoUnit.SECONDS);
    }

    public String getErrorContact();
}
