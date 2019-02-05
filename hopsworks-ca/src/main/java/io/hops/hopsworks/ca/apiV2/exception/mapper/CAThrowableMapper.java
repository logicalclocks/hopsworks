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

package io.hops.hopsworks.ca.apiV2.exception.mapper;

import io.hops.hopsworks.common.exception.RESTException;
import io.hops.hopsworks.common.exception.ThrowableMapper;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class CAThrowableMapper extends ThrowableMapper {
  
  @EJB
  Settings settings;
  
  @Override
  public Response handleRESTException(Response.StatusType status, RESTException ex) {
    return Response.status(status)
      .entity(ex.buildJsonResponse(new CAJsonResponse(), settings.getHopsworksRESTLogLevel()))
      .type(MediaType.APPLICATION_JSON)
      .build();
  }
  
}
