package se.kth.hopsworks.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private final static Logger log = Logger.getLogger(
          ThrowableExceptionMapper.class.getName());

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    log.log(Level.INFO, "ThrowableExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    setHttpStatus(ex, json);
    json.setErrorMsg("Oops! something went wrong :(");
    ex.printStackTrace();
    return Response.status(json.getStatusCode())
            .entity(json)
            .build();
  }

  private void setHttpStatus(Throwable ex, JsonResponse json) {
    if (ex instanceof WebApplicationException) {
      json.setStatusCode(((WebApplicationException) ex).getResponse().
              getStatus());
    } else {
      json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); //defaults to internal server error 500
    }
  }

}
