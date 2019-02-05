/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.cluster.exception.mapper;

import io.hops.hopsworks.cluster.ClusterJsonResponse;
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

  private static final Logger LOGGER = Logger.getLogger(EJBExceptionMapper.class.getName());

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

    LOGGER.log(Level.INFO, "EJBException Caused by: {0}", exception.getCause().toString());
    LOGGER.log(Level.INFO, "EJBException: {0}", exception.getCause().getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(exception.getCause().getMessage());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse).build();
  }

  private Response handleIllegalArgumentException(IllegalArgumentException iae) {
    LOGGER.log(Level.INFO, "IllegalArgumentException: {0}", iae.getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(iae.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleRollbackException(RollbackException pe) {
    LOGGER.log(Level.INFO, "RollbackException: {0}", pe.getMessage());
    Throwable e = pe;
    //get to the bottom of this
    while (e.getCause() != null) {
      e = e.getCause();
    }
    LOGGER.log(Level.INFO, "RollbackException Caused by: {0}", e.getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(e.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }

  private Response handleIllegalStateException(IllegalStateException illegalStateException) {
    LOGGER.log(Level.INFO, "IllegalStateException: {0}", illegalStateException.getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(illegalStateException.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleMessagingException(MessagingException messagingException) {
    LOGGER.log(Level.INFO, "MessagingException: {0}", messagingException.getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(messagingException.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }

  private Response handleSecurityException(SecurityException securityException) {
    LOGGER.log(Level.INFO, "SecurityException: {0}", securityException.getMessage());
    ClusterJsonResponse jsonResponse = new ClusterJsonResponse();
    jsonResponse.setErrorMsg(securityException.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse).build();
  }
}
