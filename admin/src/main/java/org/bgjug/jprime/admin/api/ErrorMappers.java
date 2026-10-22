package org.bgjug.jprime.admin.api;

import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.admin.dto.Dtos;
import org.bgjug.jprime.core.service.ValidationFailedException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Business errors as JSON for the React UI. */
public class ErrorMappers {

    public record ErrorResponse(String message, java.util.Map<String, String> errors) {
    }

    @ServerExceptionMapper
    public Response validation(ValidationFailedException e) {
        return Response.status(400).entity(new ErrorResponse("Validation failed", e.getErrors())).build();
    }

    @ServerExceptionMapper
    public Response illegalArgument(IllegalArgumentException e) {
        return Response.status(400).entity(new Dtos.Message(e.getMessage())).build();
    }

    @ServerExceptionMapper
    public Response illegalState(IllegalStateException e) {
        return Response.status(409).entity(new Dtos.Message(e.getMessage())).build();
    }
}
