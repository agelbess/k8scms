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

package com.k8scms.cms.resource;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.filter.ApiLocalAuthenticationFilter;
import com.k8scms.cms.filter.ApiLocalAuthorizationFilter;
import com.k8scms.cms.filter.ApiLoggingFilter;
import com.k8scms.cms.model.*;
import com.k8scms.cms.mongo.MongoBulkWriteExceptionBulkWriteResult;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.service.ModelService;
import com.k8scms.cms.utils.ModelUtils;
import com.k8scms.cms.utils.Utils;
import com.mongodb.bulk.BulkWriteInsert;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import io.smallrye.mutiny.Uni;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path(Constants.BASE_PATH_API)
@ApiLocalAuthenticationFilter
@ApiLocalAuthorizationFilter
@ApiLoggingFilter
public class ApiResource {

    private static final Logger log = LoggerFactory.getLogger(ApiResource.class);
    public static final String PATH_PARAM_CLUSTER = "cluster";
    public static final String PATH_PARAM_DATABASE = "database";
    public static final String PATH_PARAM_COLLECTION = "collection";

    @Inject
    MongoService mongoService;

    @Inject
    CmsProperties cmsProperties;

    @Inject
    ModelService modelService;

    @Inject
    SecretProperties secretProperties;

    @Context
    UriInfo uriInfo;

    @Context
    HttpRequest httpRequest;

    /**
     * Query params are always passed as strings, oid or date type.
     * For proper usage of numbers, use the POST/get
     */
    @GET
    @Path("{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> get(
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return get(cmsProperties.getDatabase(), collection);
    }

    @GET
    @Path("{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> get(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return get(cmsProperties.getCluster(), database, collection);
    }

    @GET
    @Path("{collection}/meta")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CollectionMeta> getMeta(
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return getMeta(cmsProperties.getDatabase(), collection);
    }

    @GET
    @Path("{database}/{collection}/meta")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CollectionMeta> getMeta(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return getMeta(cmsProperties.getCluster(), database, collection);
    }

    @GET
    @Path("{cluster}/{database}/{collection}/meta")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CollectionMeta> getMeta(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("GET {}", uriInfo.getRequestUri());

        return mongoService.getMeta(cluster, database, collection);
    }

    @POST
    @Path("{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> data) {
        return post(cmsProperties.getCluster(), cmsProperties.getDatabase(), collection, ordered, data);
    }

    @POST
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> data) {
        return post(cmsProperties.getCluster(), database, collection, ordered, data);
    }

    @POST
    @Path("{collection}/GET")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Document> postGet(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document filterWithGetOptions) {
        return postGet(cmsProperties.getDatabase(), collection, filterWithGetOptions);
    }

    @POST
    @Path("{database}/{collection}/GET")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Document> postGet(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document filterWithGetOptions) {
        return postGet(cmsProperties.getCluster(), database, collection, filterWithGetOptions);
    }

    @PUT
    @Path("{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        return put(cmsProperties.getCluster(), cmsProperties.getDatabase(), collection, ordered, dataFilters);
    }

    @PUT
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        return put(cmsProperties.getCluster(), database, collection, ordered, dataFilters);
    }

    @PATCH
    @Path("{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        return patch(cmsProperties.getCluster(), cmsProperties.getDatabase(), collection, ordered, dataFilters);
    }

    @PATCH
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        return patch(cmsProperties.getCluster(), database, collection, ordered, dataFilters);
    }

    @DELETE
    @Path("{database}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> filters) {
        return delete(cmsProperties.getCluster(), cmsProperties.getDatabase(), collection, ordered, filters);
    }

    @DELETE
    @Path("{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> filters) {
        return delete(cmsProperties.getCluster(), database, collection, ordered, filters);
    }

    @GET
    @Path("{cluster}/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> get(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("GET {}", uriInfo.getRequestUri());

        Document documentWithGetOptions = Utils.documentFromUriInfo(uriInfo);

        return get(cluster, database, collection, documentWithGetOptions);
    }

    @POST
    @Path("{cluster}/{database}/{collection}/GET")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Document> postGet(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document documentWithGetOptions) {
        log.debug("POST {}", uriInfo.getRequestUri());

        return get(cluster, database, collection, documentWithGetOptions);
    }

    private List<Document> get(String cluster, String database, String collection, Document documentWithGetOptions) {
        Model model = modelService.getModel(cluster, database, collection);

        documentWithGetOptions = ModelUtils.getNormalizedDocument(documentWithGetOptions, model);
        Document filter = Utils.getDocumentWithoutGetOptions(documentWithGetOptions);
        applyUserFilters(model, filter);

        GetOptions getOptions = Utils.getGetOptionsFromDocument(documentWithGetOptions);

        // do not nest mongo reactive flows, first block to get the data and next execute other mongo flows that block
        return methodGetResult(cluster, database, collection, model, filter, getOptions);
    }

    @POST
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> documents) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);

        List<Document> data = documents.stream().map(document -> ModelUtils.getNormalizedDocument(document, model))
                .map(document -> {
                    ModelUtils.encryptSecrets(document, model, secretProperties);
                    return document;
                })
                .map(document -> {
                    ModelUtils.applySystemFields(httpRequest.getHttpMethod(), document, model);
                    return document;
                })
                .collect(Collectors.toList());

        return mongoService.post(cluster, database, collection, data, ordered).map(this::mapBulkWriteResult);
    }

    @PUT
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        log.debug("PUT {}", uriInfo.getRequestUri());

        putPatch(cluster, database, collection, dataFilters);

        return mongoService.put(cluster, database, collection, dataFilters, true, ordered).map(this::mapBulkWriteResult);
    }

    @PATCH
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<DataFilter> dataFilters) {
        log.debug("PUT {}", uriInfo.getRequestUri());

        putPatch(cluster, database, collection, dataFilters);

        return mongoService.patch(cluster, database, collection, dataFilters, true, ordered).map(this::mapBulkWriteResult);
    }

    private void putPatch(String cluster, String database, String collection, List<DataFilter> dataFilters) {
        Model model = modelService.getModel(cluster, database, collection);
        dataFilters.stream()
                .forEach(dataFilter -> {
                    Document filter = ModelUtils.getNormalizedDocument(dataFilter.getFilter(), model);
                    applyUserFilters(model, filter);
                    dataFilter.setFilter(filter);
                    Document data = ModelUtils.getNormalizedDocument(dataFilter.getData(), model);
                    ModelUtils.encryptSecrets(data, model, secretProperties);
                    ModelUtils.applySystemFields(httpRequest.getHttpMethod(), data, model);
                    dataFilter.setData(data);
                });
    }

    @DELETE
    @Path("{cluster}/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            @QueryParam("ordered") @DefaultValue("true") boolean ordered,
            List<Document> filters) {
        log.debug("DELETE {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        filters = filters.stream().map(document -> {
            Document filter = ModelUtils.getNormalizedDocument(document, model);
            applyUserFilters(model, filter);
            return filter;
        }).collect(Collectors.toList());

        return mongoService.delete(cluster, database, collection, filters, ordered).map(deleteResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setDeleteCount((long) deleteResult.getDeletedCount());
            return methodResult;
        });
    }

    private MethodResult mapBulkWriteResult(BulkWriteResult bulkWriteResult) {
        MethodResult methodResult = new MethodResult();
        methodResult.setMatchedCount((long) bulkWriteResult.getMatchedCount());
        methodResult.setModifiedCount((long) bulkWriteResult.getModifiedCount());
        methodResult.setUpsertedIds(
                bulkWriteResult.getUpserts()
                        .stream()
                        .map(BulkWriteUpsert::getId)
                        .map(BsonValue::asObjectId)
                        .map(BsonObjectId::getValue)
                        .map(ObjectId::toHexString)
                        .collect(Collectors.toList()));
        methodResult.setInsertedIds(
                bulkWriteResult.getInserts()
                        .subList(0, bulkWriteResult.getInsertedCount())
                        .stream()
                        .map(BulkWriteInsert::getId)
                        .map(BsonValue::asObjectId)
                        .map(BsonObjectId::getValue)
                        .map(ObjectId::toHexString)
                        .collect(Collectors.toList()));
        if (bulkWriteResult instanceof MongoBulkWriteExceptionBulkWriteResult) {
            methodResult.setBulkWriteErrors(((MongoBulkWriteExceptionBulkWriteResult) bulkWriteResult).getMongoBulkWriteException().getWriteErrors());
        }
        return methodResult;
    }

    private List<Document> methodGetResult(String cluster, String database, String collection, Model model, Document filter, GetOptions getOptions) {
        List<Document> documents = mongoService.get(cluster, database, collection, filter, getOptions).collectItems().asList().await().indefinitely()
                .stream()
                .map(document -> ModelUtils.decryptSecrets(document, model, secretProperties))
                .collect(Collectors.toList());
        ModelUtils.validate(documents, model);
        documents = documents.stream()
                .map(document -> ModelUtils.addRelations(document, model, mongoService))
                .map(ModelUtils::toWire)
                .collect(Collectors.toList());
        sortMeta(getOptions, documents);
        return documents;
    }

    private static void sortMeta(GetOptions getOptions, List<Document> list) {
        if ("_meta.validationErrors".equals(getOptions.getSort())) {
            list.sort((d1, d2) -> getOptions.getSortDirection() * Meta.comparator.compare((Meta) d1.get("_meta"), (Meta) d2.get("_meta")));
        }

    }

    private void applyUserFilters(Model model, Document filter) {
        List<Filter> filters = (List<Filter>) httpRequest.getAttribute(Constants.CONTEXT_PROPERTY_USER_FILTERS);
        filters.stream()
                .filter(f -> f.getCluster().equals(model.getCluster()) && f.getDatabase().equals(model.getDatabase()) && f.getCollection().equals(model.getCollection()))
                .forEach(f -> {
                    for (Map.Entry<String, Object> entry : f.getFilter().entrySet()) {
                        filter.put(entry.getKey(), entry.getValue());
                    }
                });
    }

}
