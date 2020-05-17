package com.k8scms.cms.resource;

import com.k8scms.cms.service.ModelService;
import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.filter.ApiLocalAuthenticationFilter;
import com.k8scms.cms.filter.ApiLocalAuthorizationFilter;
import com.k8scms.cms.filter.ApiLoggingFilter;
import com.k8scms.cms.model.CollectionMeta;
import com.k8scms.cms.model.MethodResult;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.ModelUtils;
import com.k8scms.cms.utils.Utils;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path(Constants.BASE_PATH_API)
@ApiLocalAuthenticationFilter
@ApiLocalAuthorizationFilter
@ApiLoggingFilter
public class ApiResource {

    private static final Logger log = LoggerFactory.getLogger(ApiResource.class);
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
    @Path("/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<Document> get(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("GET {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        ModelUtils.fromWire(filter, model, secretProperties);

        return mongoService.get(database, collection, filter, Utils.getOptionsFromUriInfo(uriInfo))
                .map(document -> ModelUtils.validate(document, model))
                .map(document -> ModelUtils.addRelations(document, model, mongoService, cmsProperties))
                .map(document -> ModelUtils.toWire(document, model, secretProperties));
    }

    @GET
    @Path("/{database}/{collection}/meta")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<CollectionMeta> getMeta(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("GET {}", uriInfo.getRequestUri());

        return mongoService.getMeta(database, collection);
    }

    @POST
    @Path("/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> post(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        ModelUtils.fromWire(data, model, secretProperties);

        return mongoService.post(database, collection, data).map(insertOneResult -> {
                    MethodResult methodResult = new MethodResult();
                    methodResult.setInsertedId(insertOneResult.getInsertedId().asObjectId().getValue().toHexString());
                    return methodResult;
                }
        );
    }

    @POST
    @Path("/{database}/{collection}/GET")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Multi<Document> postGet(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document filterWithGetOptions,
            @CookieParam("user") Cookie user) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        Document filter = Utils.filterFromDocument(filterWithGetOptions);
        ModelUtils.fromWire(filter, model, secretProperties);

        return mongoService.get(database, collection, filter, Utils.getOptionsFromDocument(filterWithGetOptions))
                .map(document -> ModelUtils.validate(document, model))
                .map(document -> ModelUtils.addRelations(document, model, mongoService, cmsProperties))
                .map(document -> ModelUtils.toWire(document, model, secretProperties));
    }

    @PUT
    @Path("/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> put(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data,
            @CookieParam("user") Cookie user) {
        log.debug("PUT {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        ModelUtils.fromWire(data, model, secretProperties);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        ModelUtils.fromWire(filter, model, secretProperties);

        return mongoService.put(database, collection, filter, data, true).map(updateResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setMatchedCount(updateResult.getMatchedCount());
            methodResult.setModifiedCount(updateResult.getModifiedCount());
            return methodResult;
        });
    }

    // TODO add PATCH option in UI
    @PATCH
    @Path("/{database}/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> patch(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection,
            Document data,
            @CookieParam("user") Cookie user) {
        log.debug("PATCH {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        ModelUtils.fromWire(data, model, secretProperties);
        Document filter = Utils.filterFromUriInfo(uriInfo);
        ModelUtils.fromWire(filter, model, secretProperties);

        return mongoService.patch(database, collection, filter, data).map(updateResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setMatchedCount(updateResult.getMatchedCount());
            methodResult.setModifiedCount(updateResult.getModifiedCount());
            return methodResult;
        });
    }

    @DELETE
    @Path("/{database}/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MethodResult> delete(
            @PathParam(PATH_PARAM_DATABASE) String database,
            @PathParam(PATH_PARAM_COLLECTION) String collection) {
        log.debug("PUT {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        Document filter = ModelUtils.fromWire(
                Utils.filterFromUriInfo(uriInfo)
                , model,
                secretProperties);

        return mongoService.delete(database, collection, filter).map(deleteResult -> {
            MethodResult methodResult = new MethodResult();
            methodResult.setDeleteCount(deleteResult.getDeletedCount());
            return methodResult;
        });
    }
}
