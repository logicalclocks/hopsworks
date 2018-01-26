package io.hops.hopsworks.api.exception.mapper;

import io.hops.hopsworks.api.util.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class LoginExceptionMapper implements ExceptionMapper<LoginException> {

  private final static Logger LOGGER = Logger.getLogger(LoginExceptionMapper.class.getName());

  @Override
  public Response toResponse(LoginException exception) {
    LOGGER.log(Level.INFO, "LoginExceptionMapper: {0}", exception.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    json.setErrorMsg(exception.getMessage());
    return Response.status(Response.Status.UNAUTHORIZED).entity(json).type(MediaType.APPLICATION_JSON).build();
  }

}
