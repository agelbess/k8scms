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

package com.k8scms.cms.mongo;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.model.CollectionMeta;
import com.k8scms.cms.model.GetOptions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClients;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class MongoService {

    private static final Logger logger = LoggerFactory.getLogger(MongoService.class);

    private Map<String, ReactiveMongoClient> mongoClients;

    @Inject
    CmsProperties cmsProperties;

    @PostConstruct
    void postConstruct() {
        mongoClients = new HashMap<>();
        int i = 0;
        while (true) {
            try {
                String clusterProperty = ConfigProvider.getConfig().getValue("cms.cluster-" + (i++), String.class);
                logger.debug("Adding cluster connection '{}'", clusterProperty);
                // 0 is the cluster name
                // 1 is the cluster connection
                String[] clusterTokens = clusterProperty.split(",", 2);
                MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(clusterTokens[1]))
                        .applyToConnectionPoolSettings(builder -> {
                            builder.maxWaitTime(cmsProperties.getMongoTimeout(), TimeUnit.SECONDS);
                        })
                        .build();
                mongoClients.put(clusterTokens[0], new ReactiveMongoClientImpl(MongoClients.create(mongoClientSettings)));
            } catch (NoSuchElementException e) {
                // expected
                break;
            }
            if (i == 100) {
                break;
            }
        }

    }

    private ReactiveMongoClient findMongoClient(String clusterName) {
        return mongoClients.get(Optional.ofNullable(clusterName).orElse(cmsProperties.getCluster()));
    }

    public Uni<String> createIndex(String cluster, String database, String collection, Document index, IndexOptions indexOptions) {
        index.forEach((k, o) -> {
            if (!o.equals("2dsphere")) {
                // add the value as an integer, always store indexes in JSON as Strings, not Numbers
                index.put(k, Integer.parseInt(o.toString()));
            }
        });
        return findMongoClient(cluster).getDatabase(database).getCollection(collection).createIndex(index, indexOptions);
    }

    public Multi<Document> get(String cluster, String database, String collection, Document filter) {
        GetOptions getOptions = new GetOptions();
        return get(cluster, database, collection, filter, getOptions);
    }

    public Multi<Document> get(String cluster, String database, String collection, Bson filter, GetOptions getOptions) {
        FindOptions findOptions = new FindOptions();
        findOptions.filter(filter);

        if (getOptions.getSort() != null) {
            Document document = new Document();
            document.put(getOptions.getSort(), getOptions.getSortDirection());
            findOptions.sort(document);
        }
        if (getOptions.getSkip() != null) {
            findOptions.skip(getOptions.getSkip());
        }
        if (getOptions.getLimit() != null) {
            findOptions.limit(Math.min(getOptions.getLimit(), cmsProperties.getLimit()));
        } else {
            findOptions.limit(cmsProperties.getLimit());
        }

        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .find(findOptions);
    }

    public Uni<CollectionMeta> getMeta(String cluster, String database, String collection) {
        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .estimatedDocumentCount()
                .map(l -> {
                    CollectionMeta collectionMeta = new CollectionMeta();
                    collectionMeta.setEstimatedDocumentCount(l);
                    return collectionMeta;
                });
    }

    public Uni<InsertOneResult> post(String cluster, String database, String collection, Document data) {
        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .insertOne(data);
    }

    public Uni<UpdateResult> put(String cluster, String database, String collection, Document filter, Document data) {
        return put(cluster, database, collection, filter, data, false);
    }

    public Uni<UpdateResult> put(String cluster, String database, String collection, Document filter, Document data, boolean upsert) {
        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .replaceOne(filter, data, new ReplaceOptions().upsert(upsert));
    }

    public Uni<UpdateResult> patch(String cluster, String database, String collection, Document filter, Document data, boolean upsert) {
        return mongoClients.get(cluster).getDatabase(database)
                .getCollection(collection)
                .updateOne(filter, new Document("$set", data), new UpdateOptions().upsert(upsert));
    }

    public Uni<DeleteResult> delete(String cluster, String database, String collection, Document filter) {
        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .deleteMany(filter);
    }

    public Uni<Document> findOneAndUpdate(String cluster, String database, String collection, Document filter, Document data, FindOneAndUpdateOptions findOneAndUpdateOptions) {
        return mongoClients.get(cluster).getDatabase(database)
                .getCollection(collection)
                .findOneAndUpdate(filter, data, findOneAndUpdateOptions);
    }
}
