package io.hops.hopsworks.apiV2.mapper;

import io.hops.hopsworks.apiV2.ErrorResponse;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.dela.exception.ThirdPartyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

  private final static Logger LOG = Logger.getLogger(AppExceptionMapper.class.
    getName());

  @Override
  public Response toResponse(AppException ex) {
    if(ex instanceof ThirdPartyException) {
      return handleThirdPartyException((ThirdPartyException)ex);
    } else {
      return handleAppException(ex);
    }
  }

  private Response handleThirdPartyException(ThirdPartyException tpe) {
    LOG.log(Level.WARNING, "Source:<{0}:{1}>ThirdPartyException: {2}",
      new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
    io.hops.hopsworks.common.util.JsonResponse jsonResponse = new io.hops.hopsworks.common.util.JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(tpe.getSource() + ":" + tpe.getSourceDetails() + ":" + tpe.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }
  
  private Response handleAppException(AppException ae) {
    LOG.log(Level.WARNING, "AppExceptionMapper: {0}", ae.getClass());
    ErrorResponse json = new ErrorResponse();
    json.setDescription(ae.getMessage());
    return Response.status(ae.getStatus()).entity(json).type(MediaType.APPLICATION_JSON).build();
  }
}
