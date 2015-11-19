
package se.kth.hopsworks.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.hadoop.security.AccessControlException;

@Provider
public class AccessControlExceptionMapper implements
        ExceptionMapper<AccessControlException>{
private final static Logger log = Logger.getLogger(
          AccessControlExceptionMapper.class.getName());
  @Override
  public Response toResponse(AccessControlException exception) {
    log.log(Level.INFO, "AccessControlException: {0}", exception.getClass());
    JsonResponse json = new JsonResponse();
    String cause = exception.getMessage();
    json.setErrorMsg(cause);
    return Response.status(Response.Status.FORBIDDEN)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }
  
}
