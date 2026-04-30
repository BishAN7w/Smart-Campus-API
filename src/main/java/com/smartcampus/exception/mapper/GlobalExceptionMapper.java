package com.smartcampus.exception.mapper;

import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catches any Throwable not handled by a more specific mapper.
 *
 * WebApplicationExceptions (404, 405, 415, etc.) are JAX-RS exceptions that
 * already carry the correct HTTP status. They are returned as-is with a clean
 * JSON body rather than being incorrectly wrapped as 500.
 *
 * All other unexpected exceptions (NullPointerException, etc.) are logged
 * server-side and returned as a generic 500. The full stack trace is never
 * exposed to the client.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {

        // JAX-RS exceptions (404, 405, 415, etc.) already have the right status.
        // Return them with our standard JSON error body instead of wrapping as 500.
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            int status = wae.getResponse().getStatus();
            String reason = wae.getResponse().getStatusInfo().getReasonPhrase();
            ErrorResponse error = new ErrorResponse(status, reason, exception.getMessage());
            return Response.status(status)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Genuine unexpected error — log full detail server-side, return safe generic message
        LOGGER.log(Level.SEVERE, "Unhandled exception: " + exception.getMessage(), exception);

        ErrorResponse error = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
