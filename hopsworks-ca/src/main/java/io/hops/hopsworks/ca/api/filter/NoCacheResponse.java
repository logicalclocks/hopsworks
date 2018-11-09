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

package io.hops.hopsworks.ca.api.filter;

import io.hops.hopsworks.ca.api.exception.mapper.CAJsonResponse;

import javax.ejb.Stateless;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Stateless
public class NoCacheResponse {

  public ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    cc.setMaxAge(-1);
    cc.setMustRevalidate(true);

    return Response.status(status).cacheControl(cc);
  }

  public ResponseBuilder getNoCacheCORSResponseBuilder(Response.Status status) {
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    cc.setMaxAge(-1);
    cc.setMustRevalidate(true);
    return Response.status(status)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET")
            .cacheControl(cc);
  }
  
  public CAJsonResponse buildJsonResponse(Response.Status status, String message) {
    CAJsonResponse response = new CAJsonResponse();
    response.setSuccessMessage(message);
    
    return response;
  }
}
