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
import com.k8scms.cms.resource.DataFilter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClientSettings;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            if (getOptions.getNoLimit()) {
                // no limit
                logger.debug("get without limit");
            } else {
                findOptions.limit(cmsProperties.getLimit());
            }
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

    public Uni<BulkWriteResult> post(String cluster, String database, String collection, List<Document> data, boolean ordered) {
        return bulkWrite(
                cluster,
                database,
                collection,
                data.stream().map(InsertOneModel::new).collect(Collectors.toList()),
                ordered);
    }

    // when updating from the UI form the _id is used, on upload the field id
    public Uni<BulkWriteResult> put(String cluster, String database, String collection, List<DataFilter> dataFilters, boolean upsert, boolean ordered) {
        List<ReplaceOneModel<Document>> replaceOneModels = new ArrayList<>();
        dataFilters.forEach(dataFilter -> {
            ReplaceOneModel<Document> replaceOneModel = new ReplaceOneModel<>(dataFilter.getFilter(), dataFilter.getData(), new ReplaceOptions().upsert(upsert));
            replaceOneModels.add(replaceOneModel);
        });
        return bulkWrite(
                cluster,
                database,
                collection,
                replaceOneModels,
                ordered);
    }

    public Uni<BulkWriteResult> patch(String cluster, String database, String collection, List<DataFilter> dataFilters, boolean upsert, boolean ordered) {
        List<UpdateOneModel<Document>> updateOneModels = new ArrayList<>();
        dataFilters.forEach(dataFilter -> {
            UpdateOneModel<Document> updateOneModel = new UpdateOneModel<>(dataFilter.getFilter(), new Document("$set", dataFilter.getData()), new UpdateOptions().upsert(upsert));

            updateOneModels.add(updateOneModel);
        });
        return bulkWrite(
                cluster,
                database,
                collection,
                updateOneModels,
                ordered);
    }

    public Uni<BulkWriteResult> delete(String cluster, String database, String collection, List<Document> filters, boolean ordered) {
        List<DeleteOneModel<Document>> deleteOneModels = new ArrayList<>();
        filters.forEach(filter -> {
            DeleteOneModel<Document> deleteOneModel = new DeleteOneModel<>(filter);
            deleteOneModels.add(deleteOneModel);
        });
        return bulkWrite(
                cluster,
                database,
                collection,
                deleteOneModels,
                ordered);
    }

    private Uni<BulkWriteResult> bulkWrite(String cluster, String database, String collection, List<? extends WriteModel<Document>> writeModels, boolean ordered) {
        return findMongoClient(cluster).getDatabase(database)
                .getCollection(collection)
                .bulkWrite(writeModels, new BulkWriteOptions().ordered(ordered))
                .onFailure()
                .recoverWithItem(throwable -> {
                    if (throwable instanceof MongoBulkWriteException) {
                        return new MongoBulkWriteExceptionBulkWriteResult((MongoBulkWriteException) throwable);
                    } else {
                        throw new RuntimeException(throwable);
                    }
                });
    }
}
