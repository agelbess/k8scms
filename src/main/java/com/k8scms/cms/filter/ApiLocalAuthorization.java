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
