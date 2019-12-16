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

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.user.UserProfileBuilder;
import io.hops.hopsworks.api.user.BbcGroupDTO;
import io.hops.hopsworks.api.user.UserProfileDTO;
import io.hops.hopsworks.api.user.UsersBeanParam;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Path("/admin")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersAdminResource {

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private UserProfileBuilder userProfileBuilder;
  
  @ApiOperation(value = "Get all users profiles.")
  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUsers(
    @Context UriInfo uriInfo,
    @BeanParam Pagination pagination,
    @BeanParam UsersBeanParam usersBeanParam) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(usersBeanParam.getSortBySet());
    resourceRequest.setFilter(usersBeanParam.getFilter());
    UserProfileDTO dto = usersBuilder.buildFullItems(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get user profile specified by id.")
  @GET
  @Path("/users/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser(@Context UriInfo uriInfo, @PathParam("id") Integer id) throws UserException {
    UserProfileDTO dto = usersBuilder.buildById(uriInfo, id);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Update user profile specified by id.")
  @PUT
  @Path("/users/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateUser(
    @Context HttpServletRequest req,
    @Context SecurityContext sc,
    @PathParam("id") Integer id,
    Users user) throws UserException {
    
    userProfileBuilder.updateUser(
      id,
      req,
      user,
      jWTHelper.getUserPrincipal(sc));
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Accept user specified by id.")
  @PUT
  @Path("/users/{id}/accepted")
  @Produces(MediaType.APPLICATION_JSON)
  public Response acceptUser(@Context HttpServletRequest req, @Context SecurityContext sc,
    @PathParam("id") Integer id, Users user) throws UserException, ServiceException {
    
    userProfileBuilder.acceptUser(
      req,
      jWTHelper.getUserPrincipal(sc),
      id,
      user);
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Reject user specified by id.")
  @PUT
  @Path("/users/{id}/rejected")
  public Response rejectUser(@Context HttpServletRequest req, @Context SecurityContext sc,
    @PathParam("id") Integer id) throws UserException, ServiceException {
    
    userProfileBuilder.rejectUser(
      req,
      jWTHelper.getUserPrincipal(sc),
      id);
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Resend confirmation email to user specified by id.")
  @PUT
  @Path("/users/{id}/pending")
  public Response pendingUser(@Context HttpServletRequest req, @PathParam("id") Integer id)
    throws UserException, ServiceException {
    
    userProfileBuilder.pendUser(req, id);
  
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get all user groups.")
  @GET
  @Path("/users/groups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllGroups(@Context UriInfo uriInfo) {
    BbcGroupDTO dto = usersBuilder.buildUserGroups(uriInfo);
    return Response.ok().entity(dto).build();
  }

  
}
