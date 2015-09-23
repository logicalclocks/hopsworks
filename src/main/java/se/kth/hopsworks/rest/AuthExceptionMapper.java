package se.kth.hopsworks.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessLocalException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * AuthExceptionMapper maps all Access exceptions to authorization exception and
 * sends unauthorized status code to the client.
 * <p/>
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Provider
public class AuthExceptionMapper implements
        ExceptionMapper<AccessLocalException> {

  private final static Logger log = Logger.getLogger(AuthExceptionMapper.class.
          getName());

  @Override
  public Response toResponse(AccessLocalException ex) {
    log.log(Level.INFO, "AuthExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    json.setErrorMsg(ex.getMessage());
    return Response.status(Response.Status.UNAUTHORIZED)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
