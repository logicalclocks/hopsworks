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
package io.hops.hopsworks.ca.api.exception.mapper;

import java.security.AccessControlException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessLocalException;
import javax.ejb.EJBException;
import javax.transaction.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
    } else if (exception.getCause() instanceof AccessControlException) {
      return handleAccessControlException((AccessControlException) exception.getCause());
    } else if (exception.getCause() instanceof ConstraintViolationException) {
      return handleConstraintViolation((ConstraintViolationException) exception.getCause());
    } else if (exception.getCause() instanceof RollbackException) {
      return handleRollbackException((RollbackException) exception.getCause());
    } else if (exception.getCause() instanceof AccessLocalException) {
      return handleAccessLocalException((AccessLocalException) exception.getCause());
    } else if(exception.getCause() instanceof IllegalStateException) {
      return handleIllegalStateException((IllegalStateException) exception.getCause());
    }

    LOG.log(Level.INFO, "EJBException Caused by: {0}", exception.getCause().toString());
    LOG.log(Level.INFO, "EJBException: {0}", exception.getCause().getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    jsonResponse.setErrorMsg(exception.getCause().getMessage());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse).build();
  }

  private Response handleConstraintViolation(ConstraintViolationException cve) {
    LOG.log(Level.INFO, "ConstraintViolationException: {0}", cve.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.BAD_REQUEST.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
    StringBuilder sb = new StringBuilder();
    Set<ConstraintViolation<?>> cvs = cve.getConstraintViolations();
    for (ConstraintViolation<?> cv : cvs) {
      LOG.log(Level.INFO, "Attribute: {0}, {1}", new Object[]{cv.getPropertyPath(), cv.getMessage()});
      sb.append(cv.getPropertyPath()).append(", ").append(cv.getMessage()).append("\n");
    }
    if (sb.toString().isEmpty()) {
      jsonResponse.setErrorMsg(cve.getMessage());
    } else {
      jsonResponse.setErrorMsg(sb.toString());
    }

    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }

  private Response handleAccessControlException(AccessControlException ace) {
    LOG.log(Level.INFO, "AccessControlException: {0}", ace.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.FORBIDDEN.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
    jsonResponse.setErrorMsg(ace.getMessage());
    return Response.status(Response.Status.FORBIDDEN).entity(jsonResponse).build();
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
    if (e instanceof ConstraintViolationException) {
      return handleConstraintViolation((ConstraintViolationException) e);      
    }
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.BAD_REQUEST.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
    jsonResponse.setErrorMsg(e.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }

  private Response handleAccessLocalException(AccessLocalException accessLocalException) {
    LOG.log(Level.INFO, "AccessLocalException: {0}", accessLocalException.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.UNAUTHORIZED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    jsonResponse.setErrorMsg(accessLocalException.getMessage());
    return Response.status(Response.Status.UNAUTHORIZED).entity(jsonResponse).build();
  }

  private Response handleIllegalStateException(IllegalStateException illegalStateException) {
    LOG.log(Level.INFO, "IllegalStateException: {0}", illegalStateException.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(illegalStateException.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }
}
