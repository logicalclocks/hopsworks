/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.exception;

import com.google.common.base.Strings;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.AccessLocalException;
import javax.ejb.EJBException;
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
      Response.Status status = Response.Status.fromStatusCode(((WebApplicationException) ex).getResponse().getStatus());
      Level logLevel;
      //If the error is not 5xx, then log it in FINE level to not pollute the log.
      if(status.getStatusCode() < Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()){
        logLevel = Level.FINE;
      } else {
        logLevel = Level.WARNING;
      }
      return handleRESTException(
        Response.Status.fromStatusCode(((WebApplicationException) ex).getResponse().getStatus()),
        new GenericException(RESTCodes.GenericErrorCode.WEBAPPLICATION, logLevel, ex.getMessage(), null, ex));
    } else if (ex instanceof PersistenceException) {
      Throwable e = ex;
      //get to the bottom of this
      while (e.getCause() != null) {
        e = e.getCause();
      }
      if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Cluster Failure")) {
        return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.DATABASE_UNAVAILABLE, Level.SEVERE,
          e.getMessage(), null, e));
      } else {
        return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.PERSISTENCE_ERROR, Level.SEVERE,
          e.getMessage(), null, e));
      }
    } else if (ex instanceof IOException
      && ex.getMessage().contains("Requested storage index 0 isn't initialized, repository count is 0")) {
      return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.ZEPPELIN_ADD_FAILURE, Level.SEVERE,
        ex.getMessage(), null, ex));
    } else {
      return handleUnknownException(ex);
    }
  }
  
  public Response handleIllegalArgumentException(IllegalArgumentException ex) {
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        ex.getMessage(), null, ex));
  }
  
  public Response handleLoginException(LoginException ex) {
    return handleRESTException(new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.WARNING,
      ex.getMessage(), null, ex));
  }
  
  public Response handleIllegalStateException(IllegalStateException ex) {
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.WARNING,
      ex.getMessage(), null, ex));
  }
  
  public Response handleSecurityException(SecurityException ex) {
    return handleRESTException(
      new GenericException(RESTCodes.GenericErrorCode.SECURITY_EXCEPTION, Level.SEVERE, ex.getMessage(), null));
  }
  
  public Response handleRollbackException(RollbackException ex) {
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ROLLBACK, Level.SEVERE,
      ex.getMessage()));
  }
  
  public Response handleAccessControlException(AccessControlException ex) {
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.INFO,
      ex.getMessage(), null, ex));
  }
  
  public Response handleRESTException(RESTException ex) {
    if (!(ex.getCause() instanceof EJBException && ex.getCause().getMessage() != null
      && ex.getCause().getMessage().contains("Client not authorized for this invocation"))) {
      StringBuilder sb = new StringBuilder();
      sb.append("errorCode=").append(ex.getErrorCode().getCode());
      if (!Strings.isNullOrEmpty(ex.getUsrMsg())) {
        sb.append(", usrMsg=").append(ex.getUsrMsg());
      }
      logger.log(ex.getLevel(), sb.toString(), ex);
    }
    return handleRESTException(ex.getErrorCode().getRespStatus(), ex);
  }
  
  public abstract Response handleRESTException(Response.StatusType status, RESTException ex);
  
  public Response handleAccessLocalException(AccessLocalException ex) {
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, Level.INFO,
      ex.getMessage(), null, ex));
  }
  
  /**
   * Classes extending this mapper, can override this method to provide particular functionality for exceptions not
   * handled in this mapper.
   *
   * @param ex
   * @return
   */
  public Response handleUnknownException(Throwable ex) {
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.INFO,
      ex.getMessage(), null, ex));
  }
  
}
