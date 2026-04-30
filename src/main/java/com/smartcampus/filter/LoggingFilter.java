package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Logs every incoming request (method + URI) and every outgoing response (status code).
 *
 * Using a filter for logging rather than adding Logger calls to every resource method
 * keeps concerns separated. New endpoints automatically get logging without any extra
 * work, the format stays consistent, and the resource methods stay focused on business
 * logic rather than observability.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  --> %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] <-- %d %s  (%s %s)",
                responseContext.getStatus(),
                responseContext.getStatusInfo().getReasonPhrase(),
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }
}
