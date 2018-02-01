/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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