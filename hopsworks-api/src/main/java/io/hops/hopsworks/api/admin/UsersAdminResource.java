/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.admin.dto.NewUserDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.user.BbcGroupDTO;
import io.hops.hopsworks.api.user.UserProfileBuilder;
import io.hops.hopsworks.api.user.UserProfileDTO;
import io.hops.hopsworks.api.user.UsersBeanParam;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.remote.group.mapping.RemoteGroupMappingHelper;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.user.UserValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.persistence.entity.util.FormatUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.logging.Logger;

@Path("/admin")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN, ApiScope.ADMINISTER_USERS}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersAdminResource {
  private final static Logger LOGGER = Logger.getLogger(UsersAdminResource.class.getName());
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private UserProfileBuilder userProfileBuilder;
  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @Inject
  private RemoteGroupMappingHelper remoteGroupMappingHelper;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  protected UsersController usersController;
  @EJB
  private UserValidator userValidator;
  
  @ApiOperation(value = "Get all users profiles.", response = UserProfileDTO.class)
  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUsers(
    @Context UriInfo uriInfo,
    @BeanParam Pagination pagination,
    @BeanParam UsersBeanParam usersBeanParam, @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(usersBeanParam.getSortBySet());
    resourceRequest.setFilter(usersBeanParam.getFilter());
    UserProfileDTO dto = usersBuilder.buildFullItems(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get user profile specified by id.", response = UserProfileDTO.class)
  @GET
  @Path("/users/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser(@Context UriInfo uriInfo, @PathParam("id") Integer id, @Context SecurityContext sc)
    throws UserException {
    UserProfileDTO dto = usersBuilder.buildById(uriInfo, id);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Update user profile specified by id.", response = UserProfileDTO.class)
  @PUT
  @Path("/users/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateUser(@Context HttpServletRequest req, @Context SecurityContext sc, @PathParam("id") Integer id,
    Users user) throws UserException {
    
    UserProfileDTO  userProfileDTO = userProfileBuilder.updateUser(
      id,
      user);
    
    return Response.ok(userProfileDTO).build();
  }
  
  @ApiOperation(value = "Accept user specified by id.", response = UserProfileDTO.class)
  @PUT
  @Path("/users/{id}/accepted")
  @Produces(MediaType.APPLICATION_JSON)
  public Response acceptUser(@Context HttpServletRequest req, @Context SecurityContext sc, @PathParam("id") Integer id,
    Users user) throws UserException, ServiceException {
    
    UserProfileDTO  userProfileDTO = userProfileBuilder.acceptUser(
      id,
      user);
    
    return Response.ok(userProfileDTO).build();
  }
  
  @ApiOperation(value = "Reject user specified by id.", response = UserProfileDTO.class)
  @PUT
  @Path("/users/{id}/rejected")
  public Response rejectUser(@Context HttpServletRequest req, @Context SecurityContext sc,
    @PathParam("id") Integer id) throws UserException, ServiceException {
    
    userProfileBuilder.rejectUser(id);
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Resend confirmation email to user specified by id.", response = UserProfileDTO.class)
  @PUT
  @Path("/users/{id}/pending")
  public Response pendingUser(@Context HttpServletRequest req, @PathParam("id") Integer id) throws UserException,
    ServiceException {
    String linkUrl = FormatUtils.getUserURL(req) + settings.getEmailVerificationEndpoint();
    UserProfileDTO userProfileDTO = userProfileBuilder.pendUser(linkUrl, id);
    return Response.ok(userProfileDTO).build();
  }
  
  @ApiOperation(value = "Reset password of a user specified by id.", response = NewUserDTO.class)
  @PUT
  @Path("/users/{id}/reset")
  @Produces(MediaType.APPLICATION_JSON)
  public Response resetPassword(@Context HttpServletRequest req, @Context SecurityContext sc,
      @PathParam("id") Integer id) throws UserException, MessagingException {
    String password = usersController.resetPassword(id, req.getRemoteUser());
    NewUserDTO newUserDTO = new NewUserDTO();
    newUserDTO.setPassword(password);
    return Response.ok(newUserDTO).build();
  }
  
  @ApiOperation(value = "Delete a user specified by id. Only users without projects can be deleted")
  @DELETE
  @Path("/users/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteUser(@Context HttpServletRequest req, @Context SecurityContext sc, @PathParam("id") Integer id)
      throws UserException {
    usersController.deleteUser(id);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get all user groups.")
  @GET
  @Path("/users/groups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllGroups(@Context UriInfo uriInfo, @Context SecurityContext sc) {
    BbcGroupDTO dto = usersBuilder.buildUserGroups(uriInfo);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Path("/users/remote/{id}/sync-group")
  @Produces(MediaType.APPLICATION_JSON)
  public Response syncRemoteGroup(@PathParam("id") Integer id, @Context SecurityContext sc) throws UserException {
    Users users = userFacade.find(id);
    RemoteUser remoteUser = remoteUserFacade.findByUsers(users);
    remoteGroupMappingHelper.syncMapping(remoteUser);
    return Response.noContent().build();
  }
  
  @POST
  @Path("/users/remote/by-email/{email}/sync-group")
  @Produces(MediaType.APPLICATION_JSON)
  public Response syncRemoteGroupByEmail(@PathParam("email") String email, @Context SecurityContext sc)
    throws UserException {
    Users users = userFacade.findByEmail(email);
    RemoteUser remoteUser = remoteUserFacade.findByUsers(users);
    remoteGroupMappingHelper.syncMapping(remoteUser);
    return Response.noContent().build();
  }
  
  @POST
  @Path("/users/remote/sync-group")
  @Produces(MediaType.APPLICATION_JSON)
  public Response syncRemoteGroup(@Context SecurityContext sc) {
    remoteGroupMappingHelper.syncMappingAsync();
    return Response.accepted().build();
  }
  
  @ApiOperation(value = "Register new user as admin.")
  @POST
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN, ApiScope.ADMINISTER_USERS, ApiScope.ADMINISTER_USERS_REGISTER},
    allowedUserRoles = {"HOPS_ADMIN"})
  public Response registerUser(@QueryParam("accountType") UserAccountType accountType,
    @QueryParam("uuid") String uuid, @QueryParam("email") String email, @QueryParam("password") String password,
    @QueryParam("givenName") String givenName, @QueryParam("surname") String surname,
    @QueryParam("maxNumProjects") int maxNumProjects, @QueryParam("type") RemoteUserType type,
    @QueryParam("status") UserAccountStatus status, @Context SecurityContext sc, @Context UriInfo uriInfo)
    throws GenericException, UserException {
    switch (accountType) {
      case M_ACCOUNT_TYPE:
        return createUser(email, password, givenName, surname,  maxNumProjects, status, uriInfo);
      case REMOTE_ACCOUNT_TYPE:
        return createRemoteUser(uuid, email, givenName, surname, type, status, uriInfo);
      default:
        throw new WebApplicationException("User account type not valid.", Response.Status.NOT_FOUND);
    }
  }
  
  private Response createUser(String email, String password, String givenName, String surname, int maxNumProjects,
                              UserAccountStatus status, UriInfo uriInfo)
      throws UserException {
    UserDTO newUser = new UserDTO();
    newUser.setEmail(email);
    newUser.setFirstName(givenName);
    newUser.setLastName(surname);
    newUser.setMaxNumProjects(maxNumProjects > 0 ? maxNumProjects : settings.getMaxNumProjPerUser());
    String passwordGen = password != null && !password.isEmpty()? password :
      securityUtils.generateRandomString(UserValidator.TEMP_PASSWORD_LENGTH);
    newUser.setChosenPassword(passwordGen);
    newUser.setRepeatedPassword(passwordGen);
    newUser.setTos(true);
    userValidator.isValidNewUser(newUser, passwordGen.equals(password));
    UserAccountStatus statusDefault = status != null ? status : UserAccountStatus.NEW_MOBILE_ACCOUNT;
    Users user =
      usersController.registerUser(newUser, Settings.DEFAULT_ROLE, statusDefault, UserAccountType.M_ACCOUNT_TYPE);
    NewUserDTO newUserDTO = new NewUserDTO(user);
    URI href = uriInfo.getAbsolutePathBuilder().path(user.getUid().toString()).build();
    if (!passwordGen.equals(password)) {
      newUserDTO.setPassword(passwordGen);
    }
    return Response.created(href).entity(newUserDTO).build();
  }
  
  private Response createRemoteUser(String uuid, String email, String givenName, String surname, RemoteUserType type,
    UserAccountStatus status, UriInfo uriInfo) throws GenericException, UserException {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
}