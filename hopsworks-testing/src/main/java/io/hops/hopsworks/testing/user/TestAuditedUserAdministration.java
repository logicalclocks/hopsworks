/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.testing.user;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/test/user")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "User Testing Service", description = "User Testing Service")
public class TestAuditedUserAdministration {
  
  @EJB
  private UsersController usersController;
  
  @PUT
  @Path("{userId}/addRole")
  public Response addRole(@PathParam("userId") Integer userId, @QueryParam("role") String role) throws UserException {
    usersController.addRole(role, userId);
    return Response.ok().build();
  }
  
  @PUT
  @Path("{userId}/removeRole")
  public Response removeRole(@PathParam("userId") Integer userId, @QueryParam("role") String role)
    throws UserException {
    usersController.removeRole(role, userId);
    return Response.ok().build();
  }
}
