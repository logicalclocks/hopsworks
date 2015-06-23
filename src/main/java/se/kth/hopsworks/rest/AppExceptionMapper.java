/*
 */
package se.kth.hopsworks.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

  private final static Logger log = Logger.getLogger(AppExceptionMapper.class.
          getName());

  @Override
  public Response toResponse(AppException ex) {
    log.log(Level.INFO, "AppExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(ex.getStatus());
    json.setErrorMsg(ex.getMessage());
    return Response.status(ex.getStatus())
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
