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

package com.k8scms.cms.mongo;

import com.k8scms.cms.model.CollectionMeta;
import com.k8scms.cms.model.GetOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.k8scms.cms.CmsProperties;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MongoService {

    @Inject
    ReactiveMongoClient mongoClient;

    @Inject
    CmsProperties cmsProperties;

    public Uni<String> createIndex(String database, String collection, Document index, IndexOptions indexOptions) {
        return mongoClient.getDatabase(database).getCollection(collection).createIndex(index, indexOptions);
    }

    public Multi<Document> get(String database, String collection, Document filter) {
        GetOptions getOptions = new GetOptions();
        return get(database, collection, filter, getOptions);
    }

    public Multi<Document> get(String database, String collection, Bson filter, GetOptions getOptions) {
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

        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .find(findOptions);
    }

    public Uni<CollectionMeta> getMeta(String database, String collection) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .estimatedDocumentCount()
                .map(l -> {
                    CollectionMeta collectionMeta = new CollectionMeta();
                    collectionMeta.setEstimatedDocumentCount(l);
                    return collectionMeta;
                });
    }

    public Uni<InsertOneResult> post(String database, String collection, Document data) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .insertOne(data);
    }

    public Uni<UpdateResult> put(String database, String collection, Document filter, Document data) {
        return put(database, collection, filter, data, false);
    }

    public Uni<UpdateResult> put(String database, String collection, Document filter, Document data, boolean upsert) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .replaceOne(filter, data, new ReplaceOptions().upsert(upsert));
    }

    // TODO not tested
    public Uni<UpdateResult> patch(String database, String collection, Document filter, Document data) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .updateOne(filter, data);
    }

    public Uni<DeleteResult> delete(String database, String collection, Document filter) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .deleteMany(filter);
    }

    public Uni<Document> findOneAndUpdate(String database, String collection, Document filter, Document data, FindOneAndUpdateOptions findOneAndUpdateOptions) {
        return mongoClient.getDatabase(database)
                .getCollection(collection)
                .findOneAndUpdate(filter, data, findOneAndUpdateOptions);
    }
}
