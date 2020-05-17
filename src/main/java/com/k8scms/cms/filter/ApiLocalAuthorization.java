package com.k8scms.cms.filter;

import com.k8scms.cms.resource.ApiResource;
import com.k8scms.cms.Constants;
import com.k8scms.cms.utils.Utils;
import io.quarkus.security.UnauthorizedException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.util.List;


@Provider
@Priority(Priorities.AUTHORIZATION)
@ApiLocalAuthorizationFilter
public class ApiLocalAuthorization implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLocalAuthorization.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        Document user = (Document) containerRequestContext.getProperty(Constants.CONTEXT_PROPERTY_USER);
        List<String> permissions = (List<String>) containerRequestContext.getProperty(Constants.CONTEXT_PROPERTY_USER_PERMISSIONS);

        MultivaluedMap<String, String> pathParameters = containerRequestContext.getUriInfo().getPathParameters();
        String database = pathParameters.get(ApiResource.PATH_PARAM_DATABASE).stream().findAny()
                .orElseThrow(() -> new IllegalArgumentException("path param " + ApiResource.PATH_PARAM_DATABASE + " not found"));
        String collection = pathParameters.get(ApiResource.PATH_PARAM_COLLECTION).stream().findAny()
                .orElseThrow(() -> new IllegalArgumentException("path param " + ApiResource.PATH_PARAM_COLLECTION + " not found"));

        String method = containerRequestContext.getMethod();
        // TODO
        if (containerRequestContext.getUriInfo().getPath().endsWith("/GET")) {
            method = "GET";
        }
        if (!Utils.hasPermission(database, collection, method, permissions)) {
            throw new UnauthorizedException("User not in role");
        }
    }
}
