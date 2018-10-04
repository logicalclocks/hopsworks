package io.hops.hopsworks.common.exception;

import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.AccessLocalException;
import javax.persistence.PersistenceException;
import javax.security.auth.login.LoginException;
import javax.transaction.RollbackException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class ThrowableMapper implements ExceptionMapper<Throwable> {
  
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  
  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    
    //Order is important, check children first
    if (ex instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) ex);
    } else if (ex instanceof IllegalStateException) {
      return handleIllegalStateException((IllegalStateException) ex);
    } else if (ex instanceof SecurityException) {
      return handleSecurityException((SecurityException) ex);
    } else if (ex instanceof LoginException) {
      return handleLoginException((LoginException) ex);
    } else if (ex instanceof org.apache.hadoop.security.AccessControlException) {
      return handleAccessControlException((org.apache.hadoop.security.AccessControlException) ex);
    } else if (ex instanceof AccessLocalException) {
      return handleAccessLocalException((AccessLocalException) ex);
    } else if (ex instanceof RESTException) {
      return handleRESTException((RESTException) ex);
    } else if (ex instanceof RollbackException) {
      return handleRollbackException((RollbackException) ex);
    } else if (ex instanceof WebApplicationException) {
      logger.log(Level.SEVERE, ex.getClass().getName(), ex);
      return handleRESTException(
        Response.Status.fromStatusCode(((WebApplicationException) ex).getResponse().getStatus()),
        new GenericException(RESTCodes.GenericErrorCode.WEBAPPLICATION, null, ex.getMessage(), ex));
    } else if (ex instanceof PersistenceException) {
      Throwable e = ex;
      //get to the bottom of this
      while (e.getCause() != null) {
        e = e.getCause();
      }
      if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Cluster Failure")) {
        return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.DATABASE_UNAVAILABLE, null,
          e.getMessage(), e));
      } else {
        return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.PERSISTENCE_ERROR, null,
          e.getMessage(), e));
      }
    } else if (ex instanceof IOException
      && ex.getMessage().contains("Requested storage index 0 isn't initialized, repository count is 0")) {
      return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.ZEPPELIN_ADD_FAILURE, null,
        ex.getMessage(), ex));
    } else {
      return handleUnknownException(ex);
    }
  }
  
  public Response handleIllegalArgumentException(IllegalArgumentException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, null, ex.getMessage(),
      ex));
  }
  
  public Response handleLoginException(LoginException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new UserException(RESTCodes.UserErrorCode.AUTHORIZATION_FAILURE, null,
      ex.getMessage(), ex));
  }
  
  public Response handleIllegalStateException(IllegalStateException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, null, ex.getMessage(),
      ex));
  }
  
  public Response handleSecurityException(SecurityException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(
      new GenericException(RESTCodes.GenericErrorCode.SECURITY_EXCEPTION, null, ex.getMessage()));
  }
  
  public Response handleRollbackException(RollbackException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ROLLBACK, null, ex.getMessage()));
  }
  
  public Response handleAccessControlException(AccessControlException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, null,
      ex.getMessage(), ex));
  }
  
  public Response handleRESTException(RESTException ex) {
    if (ex.getCause() != null) {
      logger.log(Level.SEVERE, ex.getClass().getName(), ex);
    } else {
      logger.log(Level.FINE, ex.getClass().getName(), ex);
    }
    return handleRESTException(ex.getErrorCode().getRespStatus(), ex);
  }
  
  public abstract Response handleRESTException(Response.StatusType status, RESTException ex);
  
  public Response handleAccessLocalException(AccessLocalException ex) {
    logger.log(Level.WARNING, ex.getClass().getName(), ex);
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, null,
      ex.getMessage(), ex));
  }
  
  /**
   * Classes extending this mapper, can override this method to provide particular functionality for exceptions not
   * handled in this mapper.
   *
   * @param ex
   * @return
   */
  public Response handleUnknownException(Throwable ex) {
    logger.log(Level.SEVERE, ex.getClass().getName(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, null, ex.getMessage(),
      ex));
  }
  
  public abstract Settings.LOG_LEVEL getRESTLogLevel();
  
}
