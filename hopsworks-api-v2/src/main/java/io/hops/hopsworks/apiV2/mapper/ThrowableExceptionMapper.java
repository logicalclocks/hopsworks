package io.hops.hopsworks.apiV2.mapper;

import io.hops.hopsworks.apiV2.ErrorResponse;

import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private final static Logger LOGGER = Logger.getLogger(ThrowableExceptionMapper.class.getName());

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    LOGGER.log(Level.INFO, "ThrowableExceptionMapper cause: {0}", ex.getClass());
    ex.printStackTrace();
    LOGGER.log(Level.INFO, "ThrowableExceptionMapper: {0}", ex.getMessage());
    if (ex instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) ex);
    } else if (ex instanceof IllegalStateException) {
      return handleIllegalStateException((IllegalStateException) ex);
    } else {
      ErrorResponse json = new ErrorResponse();
      json.setDescription("Oops! something went wrong :(");
      Response.Status httpStatus = getHttpStatus(ex, json);
      return Response.status(httpStatus).entity(json).build();
    }
  }

  private Response.Status getHttpStatus(Throwable ex, ErrorResponse json) {
    int statusCode;
    if (ex instanceof WebApplicationException) {
      statusCode = ((WebApplicationException) ex).getResponse().getStatus();
    } else if (ex instanceof PersistenceException) {
      Throwable e = ex;
      //get to the bottom of this
      while (e.getCause() != null) {
        e = e.getCause();
      }
      if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Cluster Failure")) {
        LOGGER.log(Level.SEVERE, "Database Exception: {0}", e.getMessage());
        json.setDescription("The database is temporarily unavailable. Please try again later.");
        statusCode = Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
      } else {
        json.setDescription("Persistence Exception :(");
        statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
      }
    } else if (ex instanceof IOException) {
      if (ex.getMessage().contains("Requested storage index 0 isn't initialized, repository count is 0")) {
        json.setDescription("Zepplin notebook dir not found, or notebook storage isn't initialized.");
        statusCode = Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
      } else {
        json.setDescription(ex.getMessage());
        statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
      }
    } else {
      //defaults to internal server error 500
      statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
    return Response.Status.fromStatusCode(statusCode);
  }

  private Response handleIllegalArgumentException(IllegalArgumentException iae) {
    LOGGER.log(Level.INFO, "IllegalArgumentException: {0}", iae.getMessage());
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setDescription(iae.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(errorResponse).build();
  }

  private Response handleIllegalStateException(IllegalStateException ise) {
    LOGGER.log(Level.INFO, "IllegalStateException: {0}", ise.getMessage());
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setDescription(ise.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(errorResponse).build();
  }
}