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

package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.exception.AppException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/variables")
@Stateless
@Api(value = "Variables Service", description = "Variables Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class VariablesService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings vf;

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVar(@PathParam("id") String id) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(vf.findById(id).getValue());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("twofactor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTwofactor() throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(vf.getTwoFactorAuth());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
  
  @GET
  @Path("ldap")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getLDAPAuthStatus() throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(vf.getLDAPAuthStatus());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
  
  @GET
  @Path("authStatus")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAuthStatus() throws AppException {
    AuthStatus authStatus = new AuthStatus(vf.getTwoFactorAuth(), vf.getLDAPAuthStatus());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(authStatus).build();
  }
  
}
