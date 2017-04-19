package io.hops.hopsworks.api.exception.mapper;

import io.hops.hopsworks.api.util.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private final static Logger log = Logger.getLogger(
          ThrowableExceptionMapper.class.getName());

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    log.log(Level.INFO, "ThrowableExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    json.setErrorMsg("Oops! something went wrong :(");
    setHttpStatus(ex, json);
    //ex.printStackTrace();
    return Response.status(json.getStatusCode())
            .entity(json)
            .build();
  }

  private void setHttpStatus(Throwable ex, JsonResponse json) {
    if (ex instanceof WebApplicationException) {
      json.setStatusCode(((WebApplicationException) ex).getResponse().
              getStatus());
    } else if (ex instanceof PersistenceException) {
      Throwable e = ex;
      //get to the bottom of this
      while (e.getCause() != null) {
        e = e.getCause();
      }
      if (e.getMessage().contains("Connection refused") || e.getMessage().
              contains("Cluster Failure")) {
        log.log(Level.SEVERE, "Database Exception: {0}", e.getMessage());
        json.setErrorMsg("The database is temporarily unavailable. "
                + "Please try again later.");
        json.setStatus("Database unavailable.");
        json.setStatusCode(Response.Status.SERVICE_UNAVAILABLE.
                getStatusCode());
      } else {
        json.setErrorMsg("Persistence Exception :(");
        json.setStatus("Persistence Exception.");
        json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      }
    } else {
      //defaults to internal server error 500
      json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
  }

}
