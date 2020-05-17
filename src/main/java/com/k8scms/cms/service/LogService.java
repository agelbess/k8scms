package com.k8scms.cms.service;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.mongo.MongoService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Inject
    MongoService mongoService;

    @Inject
    CmsProperties cmsProperties;

    public void log(String database, String collection, String method, Document body, String userName, UriInfo uriInfo) {
        if ((database + ":" + collection + ":" + method).matches(cmsProperties.getLog())) {
            Document log = new Document();
            log.put("database", database);
            log.put("collection", collection);
            log.put("method", method);
            log.put("uri", URLDecoder.decode(uriInfo.getRequestUri().toString(), StandardCharsets.UTF_8));
            log.put("userName", userName);
            Optional.ofNullable(body).ifPresent(b -> log.put("body", body));
            log.put("date", new Date());
            logger.info(log.toJson());
            mongoService.post(
                    cmsProperties.getDatabase(),
                    cmsProperties.getCollectionLog(),
                    log)
                    .await()
                    .atMost(cmsProperties.getMongoTimeoutDuration());
        }
    }
}
