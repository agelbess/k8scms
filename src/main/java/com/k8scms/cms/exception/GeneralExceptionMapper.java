package com.k8scms.cms.exception;

import com.k8scms.cms.CmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = LoggerFactory.getLogger(GeneralExceptionMapper.class);

    @Inject
    CmsProperties cmsProperties;

    @Override
    public Response toResponse(Exception exception) {
        log.error("General exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(String.format("%s%nContact %s",
                exception.getMessage(),
                cmsProperties.getErrorContact()))
                .build();
    }
}
