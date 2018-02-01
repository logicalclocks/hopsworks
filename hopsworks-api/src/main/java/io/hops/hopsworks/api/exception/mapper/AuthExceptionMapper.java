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

import io.hops.hopsworks.api.util.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessLocalException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * AuthExceptionMapper maps all Access exceptions to authorization exception and
 * sends unauthorized status code to the client.
 */
@Provider
public class AuthExceptionMapper implements
        ExceptionMapper<AccessLocalException> {

  private final static Logger log = Logger.getLogger(AuthExceptionMapper.class.
          getName());

  @Override
  public Response toResponse(AccessLocalException ex) {
    log.log(Level.INFO, "AuthExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    json.setErrorMsg(ex.getMessage());
    return Response.status(Response.Status.UNAUTHORIZED)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
