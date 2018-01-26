package io.hops.hopsworks.apiV2.mapper;

import io.hops.hopsworks.apiV2.ErrorResponse;
import org.apache.hadoop.security.AccessControlException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class AccessControlExceptionMapper implements
        ExceptionMapper<AccessControlException> {

  private final static Logger log = Logger.getLogger(
          AccessControlExceptionMapper.class.getName());

  @Override
  public Response toResponse(AccessControlException exception) {
    log.log(Level.INFO, "AccessControlException: {0}", exception.getClass());
    ErrorResponse json = new ErrorResponse();
    String cause = exception.getMessage();
    json.setDescription(cause);
    return Response.status(Response.Status.FORBIDDEN)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
