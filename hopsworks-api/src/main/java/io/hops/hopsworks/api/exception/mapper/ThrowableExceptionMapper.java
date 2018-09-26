/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.api.exception.mapper;

import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.RESTException;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
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
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {
  
  private static final Logger LOGGER = Logger.getLogger(ThrowableExceptionMapper.class.getName());
  
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
    } else if (ex instanceof AccessControlException) {
      return handleAccessControlException((AccessControlException) ex);
    } else if (ex instanceof AccessLocalException) {
      return handleAccessLocalException((AccessLocalException) ex);
    } else if (ex instanceof RESTException) {
      return handleRESTException((RESTException) ex);
    } else if (ex instanceof RollbackException) {
      return handleRollbackException((RollbackException) ex);
    } else {
      if (ex instanceof WebApplicationException) {
        LOGGER.log(Level.SEVERE, "ThrowableExceptionMapper: " + ex.getClass(), ex);
        return handleRESTException(
          Response.Status.fromStatusCode(((WebApplicationException) ex).getResponse().getStatus()),
          new GenericException(RESTCodes.GenericErrorCode.WEBAPPLICATION, null, ex.getMessage()));
      } else if (ex instanceof PersistenceException) {
        Throwable e = ex;
        //get to the bottom of this
        while (e.getCause() != null) {
          e = e.getCause();
        }
        if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Cluster Failure")) {
          return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.DATABASE_UNAVAILABLE, null,
            e.getMessage()));
        } else {
          return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.PERSISTENCE_ERROR, null,
            e.getMessage()));
        }
      } else if (ex instanceof IOException &&
        ex.getMessage().contains("Requested storage index 0 isn't initialized, repository count is 0")) {
        return handleRESTException(new ServiceException(RESTCodes.ServiceErrorCode.ZEPPELIN_ADD_FAILURE, null,
          ex.getMessage()));
      }
    }
    LOGGER.log(Level.SEVERE, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, ex.getMessage()));
  }
  
  private Response handleIllegalArgumentException(IllegalArgumentException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, ex.getMessage()));
  }
  
  private Response handleLoginException(LoginException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new UserException(RESTCodes.UserErrorCode.AUTHORIZATION_FAILURE, null,
      ex.getMessage()));
  }
  
  private Response handleIllegalStateException(IllegalStateException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, null, ex.getMessage()));
  }
  
  private Response handleSecurityException(SecurityException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(
      new GenericException(RESTCodes.GenericErrorCode.SECURITY_EXCEPTION, null, ex.getMessage()));
  }
  
  private Response handleRollbackException(RollbackException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.ROLLBACK, null, ex.getMessage()));
  }
  
  private Response handleAccessControlException(AccessControlException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, null,
      ex.getMessage(), ex));
  }
  
  private Response handleRESTException(RESTException ex) {
    LOGGER.log(Level.FINE, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(ex.getErrorCode().getRespStatus(), ex);
  }
  
  private Response handleRESTException(Response.Status status, RESTException ex) {
    if(ex.getCause() != null){
      LOGGER.log(Level.SEVERE, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    } else {
      LOGGER.log(Level.FINE, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    }
    return Response.status(status).entity(ex.getJson().toString()).type(MediaType.APPLICATION_JSON).build();
  }
  
  private Response handleAccessLocalException(AccessLocalException ex) {
    LOGGER.log(Level.WARNING, "ThrowableExceptionMapper: " + ex.getClass(), ex);
    return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, null,
      ex.getMessage()));
  }
  
}