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

package com.k8scms.cms.resource;

import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.filter.ApiLocalAuthenticationFilter;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.service.LoginService;
import com.k8scms.cms.service.ModelService;
import com.k8scms.cms.utils.ModelUtils;
import com.k8scms.cms.utils.Utils;
import com.k8scms.cms.CmsProperties;
import org.bson.Document;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.stream.Collectors;

@Path(Constants.BASE_PATH_WEB_API)
public class WebApiResource {

    private static final Logger log = LoggerFactory.getLogger(WebApiResource.class);

    @Inject
    CmsProperties cmsProperties;

    @Inject
    SecretProperties secretProperties;

    @Inject
    LoginService loginService;

    @Inject
    ModelService modelService;

    @Inject
    MongoService mongoService;

    @Context
    HttpRequest httpRequest;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public CmsProperties getProperties() {
        log.debug("GET {}", uriInfo.getRequestUri());
        return cmsProperties;
    }

    @ApiLocalAuthenticationFilter
    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON)
    public Document getUser() {
        log.debug("GET {}", uriInfo.getRequestUri());
        Document user = (Document) httpRequest.getAttribute(Constants.CONTEXT_PROPERTY_USER);
        Model model = modelService.getModel(cmsProperties.getDatabase(), cmsProperties.getCollectionUser());

        ModelUtils.addRelations(user, model, mongoService, cmsProperties);
        ModelUtils.toWire(user, model, secretProperties);
        return user;
    }

    // return the models which the user has GET access to
    @ApiLocalAuthenticationFilter
    @GET
    @Path("/models")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Model> getModels() {
        log.debug("GET {}", uriInfo.getRequestUri());
        List<String> permissions = (List<String>) httpRequest.getAttribute(Constants.CONTEXT_PROPERTY_USER_PERMISSIONS);
        return modelService.getModels().entrySet().stream()
                .map(entry -> entry.getValue())
                .filter(model -> Utils.hasPermission(model.getDatabase(), model.getCollection(), "GET", permissions))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Document userNamePassword) {
        log.debug("POST {}", uriInfo.getRequestUri());

        Document user = loginService.login(userNamePassword);
        user.put("password", null);
        return Response.ok()
                .cookie(generateUIDCookie(user))
                .build();
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        log.debug("GET {}", uriInfo.getRequestUri());
        return Response.ok()
                .cookie(generateUIDCookieInvalidation())
                .build();
    }

    // it is only used to generate password hashes for development purposes
    @GET
    @Path("generate/{userName}/{password}")
    public Response generate(
            @PathParam("userName") String userName,
            @PathParam("password") String password) {
        if (cmsProperties.getEnv().equals("dev")) {
            return Response.ok().entity(Utils.encrypt1(userName + "." + password, secretProperties.getSecretEncryptionKey())).build();
        } else {
            return null;
        }
    }

    @POST
    @Path("/{database}/{collection}/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> validate(
            @PathParam(ApiResource.PATH_PARAM_DATABASE) String database,
            @PathParam(ApiResource.PATH_PARAM_COLLECTION) String collection,
            Document data) {
        log.debug("GET {}", uriInfo.getRequestUri());

        Model model = modelService.getModel(database, collection);
        return data.getList("data", HashMap.class).stream()
                .map(map -> ModelUtils.fromWire(new Document(map), model, secretProperties))
                .map(document -> ModelUtils.validate(document, model))
                .map(document -> ModelUtils.toWire(document, model, secretProperties))
                .collect(Collectors.toList());
    }

    public NewCookie generateUIDCookie(Document user) {
        String random = UUID.randomUUID().toString();
        // The value is random.encuptedUsername.timestamp
        String value = String.format("%s.%s.%s",
                random,
                Utils.encrypt2(user.toJson(), secretProperties.getSessionEncryptionKey() + "." + random),
                new Date().getTime());
        // Need to Base64 encode for '=' signs that are not permitted in cookie
        // values
        value = Base64.getEncoder().encodeToString(value.getBytes());
        return new NewCookie(
                Constants.COOKIE_UID,
                value,
                "/",
                null,
                null,
                secretProperties.getSessionTimeout(),
                cmsProperties.getEnv().equals(Constants.ENV_PROD),
                false
        );
    }

    public NewCookie generateUIDCookieInvalidation() {
        return new NewCookie(
                Constants.COOKIE_UID,
                null,
                "/",
                null,
                null,
                0,
                cmsProperties.getEnv().equals(Constants.ENV_PROD),
                false
        );
    }
}
