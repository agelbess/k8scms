/*
 * MIT License
 *
 * Copyright (c) 2020 Alexandros Gelbessis
 *
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
 *
 */

package com.k8scms.cms.service;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.CronExpression;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    @Inject
    MongoService mongoService;

    @Inject
    CmsProperties cmsProperties;

    @Inject
    Vertx vertx;

    @Scheduled(every = "1s")
    void everySecond() {
        List<Document> documents = new ArrayList<>();
        mongoService.get(cmsProperties.getDatabase(), cmsProperties.getCollectionScheduler(), new Document("on", true))
                .map(scheduler -> {
                    Document filter = new Document();
                    filter.put("on", true);
                    filter.put("running", false);
                    // filter.put("running", new Document("$in", Arrays.asList(false, null)));
                    filter.put("schedulerName", scheduler.get("name"));
                    filter.put("$expr", new Document("$lt", Arrays.asList("$successCount", "$maxSuccessCount")));

                    FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
                    // ascending order on priority
                    findOneAndUpdateOptions.sort(new BsonDocument("priority", new BsonInt32(-1)));

                    List<Document> schedulerTasks = new ArrayList();
                    for (int i = 0; i < scheduler.getInteger("batchSize"); i++) {
                        Document document = mongoService.findOneAndUpdate(cmsProperties.getDatabase(), cmsProperties.getCollectionSchedulerTask(),
                                filter,
                                new Document("$set", new Document("running", true)),
                                findOneAndUpdateOptions).await().atMost(cmsProperties.getMongoTimeoutDuration());
                        if (document != null) {
                            documents.add(document);
                        }

                    }
                    return schedulerTasks;
                }).collectItems().asList().await().atMost(cmsProperties.getMongoTimeoutDuration());
        if (!documents.isEmpty()) {
            Uni.combine().all().unis(documents.stream()
                    .filter(Objects::nonNull)
                    .filter(schedulerTask -> {
                        String cron = schedulerTask.getString("cron");
                        if (!CronExpression.isValidExpression(cron)) {
                            throw new VertxException("Invalid cron expression " + cron);
                        }
                        CronExpression cronExpression;
                        try {
                            cronExpression = new CronExpression(cron);
                        } catch (ParseException e) {
                            throw new VertxException("Invalid cronExpression");
                        }
                        Date now = new Date();
                        return cronExpression.isSatisfiedBy(now);
                    })
                    .map(this::sendHttp).collect(Collectors.toList())).combinedWith(objects -> {
                return objects.stream().map(o -> (UpdateResult) o).collect(Collectors.toList());
            }).await().atMost(cmsProperties.getMongoTimeoutDuration());
        }
    }

    private Uni<UpdateResult> sendHttp(Document schedulerTask) {
        Map<String, Object> http = (Map<String, Object>) schedulerTask.get("http");
        WebClient webClient = WebClient.create(vertx);
        MultiMap multimap = MultiMap.caseInsensitiveMultiMap();
        Map<String, String> headers = (Map<String, String>) http.get("headers");
        if (headers != null) {
            for (String key : headers.keySet()) {
                multimap.add(key, headers.get(key));
            }
        }
        return webClient
                .getAbs((String) http.get("url"))
                .method(HttpMethod.valueOf((String) http.get("method")))
                .putHeaders(multimap)
                .send()
                .flatMap(bufferHttpResponse -> {
                    if (bufferHttpResponse.statusCode() >= 400) {
                        throw new VertxException("Invalid status: " + bufferHttpResponse.statusCode());
                    }
                    int successCount = Optional.ofNullable(schedulerTask.getInteger("successCount")).orElse(0);
                    schedulerTask.put("successCount", successCount + 1);
                    schedulerTask.put("running", false);
                    schedulerTask.put("lastFireDate", new Date());
                    schedulerTask.put("lastSuccess", bufferHttpResponse.statusCode());
                    return mongoService.put(cmsProperties.getDatabase(), cmsProperties.getCollectionSchedulerTask(),
                            new Document("_id", schedulerTask.get("_id")), schedulerTask);
                })
                .onFailure().recoverWithUni(throwable -> {
                    int failCount = Optional.ofNullable(schedulerTask.getInteger("failCount")).orElse(0);
                    schedulerTask.put("failCount", failCount + 1);
                    schedulerTask.put("running", false);
                    schedulerTask.put("lastFireDate", new Date());
                    schedulerTask.put("lastFail", throwable.getMessage());
                    return mongoService.put(cmsProperties.getDatabase(), cmsProperties.getCollectionSchedulerTask(),
                            new Document("_id", schedulerTask.get("_id")), schedulerTask);
                });
    }
}
