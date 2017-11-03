package io.hops.hopsworks.cluster.exception.mapper;

import io.hops.hopsworks.cluster.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.mail.MessagingException;
import javax.transaction.RollbackException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

  private final static Logger LOG = Logger.getLogger(EJBExceptionMapper.class.getName());

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(EJBException exception) {
    if (exception.getCause() instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) exception.getCause());
    } else if (exception.getCause() instanceof RollbackException) {
      return handleRollbackException((RollbackException) exception.getCause());
    } else if(exception.getCause() instanceof IllegalStateException) {
      return handleIllegalStateException((IllegalStateException) exception.getCause());
    } else if (exception.getCause() instanceof MessagingException) {
      return handleMessagingException((MessagingException) exception.getCause());
    } else if (exception.getCause() instanceof SecurityException) {
      return handleSecurityException((SecurityException) exception.getCause());
    }

    LOG.log(Level.INFO, "EJBException Caused by: {0}", exception.getCause().toString());
    LOG.log(Level.INFO, "EJBException: {0}", exception.getCause().getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    jsonResponse.setErrorMsg(exception.getCause().getMessage());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse).build();
  }

  private Response handleIllegalArgumentException(IllegalArgumentException iae) {
    LOG.log(Level.INFO, "IllegalArgumentException: {0}", iae.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(iae.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleRollbackException(RollbackException pe) {
    LOG.log(Level.INFO, "RollbackException: {0}", pe.getMessage());
    Throwable e = pe;
    //get to the bottom of this
    while (e.getCause() != null) {
      e = e.getCause();
    }
    LOG.log(Level.INFO, "RollbackException Caused by: {0}", e.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.BAD_REQUEST.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
    jsonResponse.setErrorMsg(e.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }

  private Response handleIllegalStateException(IllegalStateException illegalStateException) {
    LOG.log(Level.INFO, "IllegalStateException: {0}", illegalStateException.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(illegalStateException.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleMessagingException(MessagingException messagingException) {
    LOG.log(Level.INFO, "MessagingException: {0}", messagingException.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(messagingException.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleSecurityException(SecurityException securityException) {
    LOG.log(Level.INFO, "SecurityException: {0}", securityException.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.FORBIDDEN.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
    jsonResponse.setErrorMsg(securityException.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }
}
