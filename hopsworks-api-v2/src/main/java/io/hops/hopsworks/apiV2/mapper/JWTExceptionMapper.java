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
package io.hops.hopsworks.apiV2.mapper;

import io.hops.hopsworks.apiV2.ErrorResponse;
import io.hops.hopsworks.jwt.exception.AccessException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JWTExceptionMapper implements ExceptionMapper<JWTException> {

  private final static Logger LOGGER = Logger.getLogger(JWTExceptionMapper.class.getName());

  @Override
  public Response toResponse(JWTException exception) {
    ErrorResponse json = new ErrorResponse();
    json.setDescription(exception.getMessage());
    LOGGER.log(Level.INFO, "JWT Exception Mapper: {0}", exception.getMessage());
    if (exception instanceof AccessException) {
      return Response.status(Response.Status.FORBIDDEN).entity(json).build();
    } else if (exception instanceof InvalidationException) {
      return Response.status(Response.Status.EXPECTATION_FAILED).entity(json).build();
    } else if (exception instanceof NotRenewableException) {
      return Response.status(Response.Status.BAD_REQUEST).entity(json).build();
    } else if (exception instanceof SigningKeyNotFoundException) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(json).build();
    } else if (exception instanceof VerificationException) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(json).build();
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(json).build();
    }
  }

}
