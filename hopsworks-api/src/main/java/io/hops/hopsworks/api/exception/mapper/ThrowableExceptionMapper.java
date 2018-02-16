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

import io.hops.hopsworks.common.util.JsonResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private final static Logger LOGGER = Logger.getLogger(ThrowableExceptionMapper.class.getName());

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    LOGGER.log(Level.INFO, "ThrowableExceptionMapper cause: {0}", ex.getClass());
    ex.printStackTrace();
    LOGGER.log(Level.INFO, "ThrowableExceptionMapper: {0}", ex.getMessage());
    if (ex instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) ex);
    } else if (ex instanceof IllegalStateException) {
      return handleIllegalStateException((IllegalStateException) ex);
    } else {
      JsonResponse json = new JsonResponse();
      json.setErrorMsg("Oops! something went wrong :(");
      setHttpStatus(ex, json);
      return Response.status(json.getStatusCode()).entity(json).build();
    }
  }

  private void setHttpStatus(Throwable ex, JsonResponse json) {
    if (ex instanceof WebApplicationException) {
      json.setStatusCode(((WebApplicationException) ex).getResponse().getStatus());
    } else if (ex instanceof PersistenceException) {
      Throwable e = ex;
      //get to the bottom of this
      while (e.getCause() != null) {
        e = e.getCause();
      }
      if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Cluster Failure")) {
        LOGGER.log(Level.SEVERE, "Database Exception: {0}", e.getMessage());
        json.setErrorMsg("The database is temporarily unavailable. Please try again later.");
        json.setStatus("Database unavailable.");
        json.setStatusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
      } else {
        json.setErrorMsg("Persistence Exception :(");
        json.setStatus("Persistence Exception.");
        json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      }
    } else if (ex instanceof IOException) {
      if (ex.getMessage().contains("Requested storage index 0 isn't initialized, repository count is 0")) {
        json.setErrorMsg("Zepplin notebook dir not found, or notebook storage isn't initialized.");
        json.setStatusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
      } else {
        json.setErrorMsg(ex.getMessage());
        json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      }
    } else {
      //defaults to internal server error 500
      json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
  }

  private Response handleIllegalArgumentException(IllegalArgumentException iae) {
    LOGGER.log(Level.INFO, "IllegalArgumentException: {0}", iae.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(iae.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleIllegalStateException(IllegalStateException ise) {
    LOGGER.log(Level.INFO, "IllegalStateException: {0}", ise.getMessage());
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(ise.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }
}