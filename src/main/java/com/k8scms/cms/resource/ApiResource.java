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
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.service.ModelService;
import com.k8scms.cms.utils.ModelUtils;
import com.k8scms.cms.utils.Utils;
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
import java.util.Optional;
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
    @Path("{cluster}/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> get(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("GET {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        applyUserFilters(httpRequest, cluster, database, collection, filter);
        ModelUtils.fromWire(filter, model);

        GetOptions getOptions = Utils.getOptionsFromUriInfo(uriInfo);
        // do not nest mongo reactive flows, first block to get the data and next execute other mongo flows that block
        List<Document> documents = mongoService.get(cluster, database, collection, filter, getOptions).collectItems().asList().await().indefinitely()
                .stream()
                .map(document -> ModelUtils.decryptSecrets(document, model, secretProperties))
                .collect(Collectors.toList());
        ModelUtils.validate(documents, model);
        documents = documents.stream()
                .map(document -> ModelUtils.addRelations(document, model, mongoService))
                .map(document -> ModelUtils.toWire(document, model))
                .collect(Collectors.toList());
        sortMeta(getOptions, documents);
        return documents;
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
            Document data) {
        return post(cmsProperties.getDatabase(), collection, data);
    }

    @POST
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        return post(cmsProperties.getCluster(), database, collection, data);
    }

    @POST
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        ModelUtils.fromWire(data, model);
        ModelUtils.encryptSecrets(data, model, secretProperties);

        return mongoService.post(cluster, database, collection, data).map(insertOneResult -> {
                    MethodResult methodResult = new MethodResult();
                    methodResult.setInsertedId(
                            Optional.ofNullable(insertOneResult.getInsertedId())
                                    .map(BsonValue::asObjectId)
                                    .map(BsonObjectId::getValue)
                                    .map(ObjectId::toHexString)
                                    .orElse(null));
                    return methodResult;
                }
        );
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

    @POST
    @Path("{cluster}/{database}/{collection}/GET")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Document> postGet(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document filterWithGetOptions) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        Document filter = Utils.filterFromDocument(filterWithGetOptions);
        applyUserFilters(httpRequest, cluster, database, collection, filter);
        ModelUtils.fromWire(filter, model);

        GetOptions getOptions = Utils.getOptionsFromDocument(filterWithGetOptions);
        // do not nest mongo reactive flows, first block to get the data and next execute other mongo flows that block
        List<Document> documents = mongoService.get(cluster, database, collection, filter, getOptions).collectItems().asList().await().indefinitely()
                .stream()
                .map(document -> ModelUtils.decryptSecrets(document, model, secretProperties))
                .collect(Collectors.toList());
        ModelUtils.validate(documents, model);
        documents = documents.stream()
                .map(document -> ModelUtils.addRelations(document, model, mongoService))
                .map(document -> ModelUtils.toWire(document, model))
                .collect(Collectors.toList());
        sortMeta(getOptions, documents);
        return documents;
    }

    @PUT
    @Path("{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        return put(cmsProperties.getDatabase(), collection, data);
    }

    @PUT
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        return put(cmsProperties.getCluster(), database, collection, data);
    }

    @PUT
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        log.debug("PUT {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        ModelUtils.fromWire(data, model);
        ModelUtils.encryptSecrets(data, model, secretProperties);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        applyUserFilters(httpRequest, cluster, database, collection, filter);
        ModelUtils.fromWire(filter, model);

        return mongoService.put(cluster, database, collection, filter, data, true).map(updateResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setMatchedCount(updateResult.getMatchedCount());
            methodResult.setModifiedCount(updateResult.getModifiedCount());
            methodResult.setUpsertedId(
                    Optional.ofNullable(updateResult.getUpsertedId())
                            .map(BsonValue::asObjectId)
                            .map(BsonObjectId::getValue)
                            .map(ObjectId::toHexString)
                            .orElse(null));
            return methodResult;
        });
    }

    @PATCH
    @Path("{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        return patch(cmsProperties.getDatabase(), collection, data);
    }

    @PATCH
    @Path("{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        return patch(cmsProperties.getCluster(), database, collection, data);
    }

    @PATCH
    @Path("{cluster}/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        log.debug("PATCH {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        ModelUtils.fromWire(data, model);
        ModelUtils.encryptSecrets(data, model, secretProperties);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        applyUserFilters(httpRequest, cluster, database, collection, filter);
        ModelUtils.fromWire(filter, model);

        return mongoService.patch(cluster, database, collection, filter, data, true).map(updateResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setMatchedCount(updateResult.getMatchedCount());
            methodResult.setModifiedCount(updateResult.getModifiedCount());
            methodResult.setUpsertedId(
                    Optional.ofNullable(updateResult.getUpsertedId())
                            .map(BsonValue::asObjectId)
                            .map(BsonObjectId::getValue)
                            .map(ObjectId::toHexString)
                            .orElse(null));
            return methodResult;
        });
    }

    @DELETE
    @Path("{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return delete(cmsProperties.getDatabase(), collection);
    }

    @DELETE
    @Path("{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        return delete(cmsProperties.getCluster(), database, collection);
    }

    @DELETE
    @Path("{cluster}/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_CLUSTER) String cluster,
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("DELETE {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(cluster, database, collection);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        applyUserFilters(httpRequest, cluster, database, collection, filter);
        ModelUtils.fromWire(filter, model);

        return mongoService.delete(cluster, database, collection, filter).map(deleteResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setDeleteCount(deleteResult.getDeletedCount());
            return methodResult;
        });
    }

    private static void sortMeta(GetOptions getOptions, List<Document> list) {
        if ("_meta.validationErrors".equals(getOptions.getSort())) {
            list.sort((d1, d2) -> getOptions.getSortDirection() * Meta.comparator.compare((Meta) d1.get("_meta"), (Meta) d2.get("_meta")));
        }

    }

    private static void applyUserFilters(HttpRequest httpRequest, String cluster, String database, String collection, Document filter) {
        List<Filter> filters = (List<Filter>) httpRequest.getAttribute(Constants.CONTEXT_PROPERTY_USER_FILTERS);
        filters.stream()
                .filter(f -> f.getCluster().equals(cluster) && f.getDatabase().equals(database) && f.getCollection().equals(collection))
                .forEach(f -> {
                    for (Map.Entry<String, Object> entry : f.getFilter().entrySet()) {
                        filter.put(entry.getKey(), entry.getValue());
                    }
                });
    }

}
