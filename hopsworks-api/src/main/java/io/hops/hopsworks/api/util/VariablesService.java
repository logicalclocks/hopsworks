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
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Path("/variables")
@Stateless
@Api(value = "Variables Service",
    description = "Variables Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class VariablesService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVar(@PathParam("id") String id) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage(settings.findById(id).getValue());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("twofactor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTwofactor() {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage(settings.getTwoFactorAuth());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("ldap")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getLDAPAuthStatus() {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage(settings.getLDAPAuthStatus());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("authStatus")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAuthStatus(){
    AuthStatus authStatus = new AuthStatus(settings.getTwoFactorAuth(), settings.getLDAPAuthStatus());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(authStatus).build();
  }

  @GET
  @Path("versions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersions(){
    VersionsDTO dto = new VersionsDTO(settings);
    List<VersionsDTO.Version> list = dto.getVersions();
    Collections.sort(list);
    GenericEntity<List<VersionsDTO.Version>> versions
        = new GenericEntity<List<VersionsDTO.Version>>(list) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(versions).build();
  }

}
