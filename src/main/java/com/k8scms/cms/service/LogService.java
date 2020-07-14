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
    private static final Logger logger = LoggerFactory.getLogger(com.k8scms.cms.service.LogService.class);

    @Inject
    MongoService mongoService;

    @Inject
    ModelService modelService;

    @Inject
    CmsProperties cmsProperties;

    public void log(String cluster, String database, String collection, String method, Document body, String userName, UriInfo uriInfo) {
        // do not log secrets
        if (body != null) {
            modelService.getModel(cluster, database, collection).getFields().stream()
                    .filter(f -> f.getEncryption() != null)
                    .forEach(field -> body.put(field.getName(), "********"));
        }

        if ((String.format("%s:%s:%s:%s", cluster, database, collection, method)).matches(cmsProperties.getLog())) {
            Document log = new Document();
            log.put("cluster", cluster);
            log.put("database", database);
            log.put("collection", collection);
            log.put("method", method);
            log.put("uri", URLDecoder.decode(uriInfo.getRequestUri().toString(), StandardCharsets.UTF_8));
            log.put("userName", userName);
            Optional.ofNullable(body).ifPresent(b -> log.put("body", body));
            log.put("date", new Date());
            logger.info(log.toJson());
            mongoService.post(
                    cmsProperties.getCluster(),
                    cmsProperties.getDatabase(),
                    cmsProperties.getCollectionLog(),
                    log)
                    .await()
                    .indefinitely();
        }
    }
}
