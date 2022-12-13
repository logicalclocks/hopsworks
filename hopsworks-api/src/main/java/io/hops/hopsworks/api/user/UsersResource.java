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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.activities.UserActivitiesResource;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.JWTNotRequired;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.user.apiKey.ApiKeyResource;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.helper.AuditAction;
import io.hops.hopsworks.audit.helper.UserIdentifier;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserProjectDTO;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.QrCode;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Path("/users")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Users",
    description = "Users service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersResource {

  private final static Logger LOGGER = Logger.getLogger(UsersResource.class.getName());

  @EJB
  private UsersController userController;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @Inject
  private UserActivitiesResource activitiesResource;
  @Inject
  private ApiKeyResource apiKeyResource;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private SecretsController secretsController;
  @EJB
  private SecretsBuilder secretsBuilder;
  @EJB
  private Settings settings;
  @EJB
  private AuthController authController;
  @EJB
  private GitProvidersSecretsBuilder gitProvidersSecretsBuilder;
  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all users.", response = UserDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response findAll(@BeanParam Pagination pagination, @BeanParam UsersBeanParam usersBeanParam,
                          @Context HttpServletRequest req,
                          @Context UriInfo uriInfo, @Context SecurityContext sc) throws UserException {
    if (!settings.isUserSearchEnabled()) {
      throw new UserException(RESTCodes.UserErrorCode.USER_SEARCH_NOT_ALLOWED, Level.FINE);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(usersBeanParam.getSortBySet());
    resourceRequest.setFilter(usersBeanParam.getFilter());
    UserDTO userDTO = usersBuilder.buildItems(uriInfo, resourceRequest);
    return Response.ok().entity(userDTO).build();
  }

  @GET
  @Path("{userId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find User by Id.", response = UserProfileDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response findById(
      @PathParam("userId") Integer userId,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest req,
      @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    BbcGroup adminGroup = bbcGroupFacade.findByGroupName("HOPS_ADMIN");
    if (!Objects.equals(user.getUid(), userId) && !user.getBbcGroupCollection().contains(adminGroup)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCESS_CONTROL, Level.SEVERE);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    UserProfileDTO userDTO = usersBuilder.build(uriInfo, resourceRequest, userId);
    return Response.ok().entity(userDTO).build();
  }

  @GET
  @Path("profile")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets logged in User\'s info.", response = UserProfileDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getUserProfile(@Context UriInfo uriInfo,
                                 @Context HttpServletRequest req,
                                 @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    UserProfileDTO userDTO = usersBuilder.buildFull(uriInfo, resourceRequest, user);
    return Response.ok().entity(userDTO).build();
  }

  @POST
  @Path("profile")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User updated profile")
  @ApiOperation(value = "Updates logged in User\'s info.", response = UserProfileDTO.class)
  public Response updateProfile(@FormParam("firstname") String firstName,
      @FormParam("lastname") String lastName,
      @FormParam("toursState") Integer toursState,
      @Context UriInfo uriInfo,
      @AuditTarget(UserIdentifier.REQ) @Context HttpServletRequest req,
      @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    user = userController.updateProfile(user, firstName, lastName, toursState);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    UserProfileDTO userDTO = usersBuilder.buildFull(uriInfo, resourceRequest, user);
    return Response.created(userDTO.getHref()).entity(userDTO).build();
  }

  @POST
  @Path("credentials")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User updated credentials")
  @ApiOperation(value = "Updates logedin User\'s credentials.", response = RESTApiJsonResponse.class)
  public Response changeLoginCredentials(
    @Secret @FormParam("oldPassword") String oldPassword,
    @Secret @FormParam("newPassword") String newPassword,
    @Secret @FormParam("confirmedPassword") String confirmedPassword,
    @AuditTarget(UserIdentifier.REQ) @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    userController.changePassword(user, oldPassword, newPassword, confirmedPassword);
    json.setSuccessMessage(ResponseMessages.PASSWORD_CHANGED);
    return Response.ok().entity(json).build();
  }

  @POST
  @Path("secrets")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User added secret")
  @ApiOperation(value = "Stores a secret for user")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response addSecret(@Secret SecretDTO secret, @AuditTarget(UserIdentifier.REQ) @Context SecurityContext sc,
    @Context HttpServletRequest req) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    secretsController.add(user, secret.getName(), secret.getSecret(), secret.getVisibility(),
        secret.getProjectIdScope());

    RESTApiJsonResponse response = new RESTApiJsonResponse();
    response.setSuccessMessage("Added new secret");
    return Response.ok().entity(response).build();
  }

  @GET
  @Path("secrets")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Retrieves all secrets' names of a user", response = SecretDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAllSecrets(@Context SecurityContext sc,
                                @Context HttpServletRequest req) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<SecretPlaintext> secrets = secretsController.getAllForUser(user);
    SecretDTO dto = secretsBuilder.build(secrets, false);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("secrets/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the value of a shared secret", response = SecretDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSharedSecret(@QueryParam("name") String secretName, @QueryParam("owner") String ownerUsername,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc)
    throws UserException, ServiceException, ProjectException {
    Users caller = jWTHelper.getUserPrincipal(sc);
    SecretPlaintext secret = secretsController.getShared(caller, ownerUsername, secretName);
    SecretDTO dto = secretsBuilder.build(Arrays.asList(secret), true);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("secrets/{secretName}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the value of a private secret", response = SecretDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSecret(@PathParam("secretName") String name,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    SecretPlaintext secret = secretsController.get(user, name);
    SecretDTO dto = secretsBuilder.build(Arrays.asList(secret), true);
    return Response.ok().entity(dto).build();
  }

  @DELETE
  @Path("secrets/{secretName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User deleted a secret")
  @ApiOperation(value = "Deletes a secret by its name")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteSecret(@PathParam("secretName") String name, @Context HttpServletRequest req,
    @AuditTarget(UserIdentifier.REQ) @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    secretsController.delete(user, name);
    return Response.ok().build();
  }

  @DELETE
  @Path("secrets")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User deleted all secrets")
  @ApiOperation(value = "Deletes all secrets of a user")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.USER}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteAllSecrets(@AuditTarget(UserIdentifier.REQ) @Context SecurityContext sc,
    @Context HttpServletRequest req) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    secretsController.deleteAll(user);
    return Response.ok().build();
  }

  @POST
  @Path("twoFactor")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.TWO_FACTOR, message = "User changed " +
    "two factor setting")
  @ApiOperation(value = "Updates logged in User\'s two factor setting.")
  public Response changeTwoFactor(@Secret @FormParam("password") String password,
    @FormParam("twoFactor") boolean twoFactor, @AuditTarget(UserIdentifier.REQ) @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (user.getTwoFactor() == twoFactor) {
      json.setSuccessMessage("No change made.");
      return Response.ok().entity(json).build();
    }
    QrCode qrCode = userController.changeTwoFactor(user, password);
    if (qrCode != null) {
      return Response.ok(qrCode).build();
    } else {
      json.setSuccessMessage("Two factor authentication disabled.");
    }
    return Response.ok().entity(json).build();
  }
  
  @POST
  @Path("resetTwoFactor")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.TWO_FACTOR, message = "User reset two factor")
  @ApiOperation(value = "Reset logged in User\'s two factor setting.", response = QrCode.class)
  public Response resetTwoFactor(@Secret @FormParam("password") String password,
    @AuditTarget(UserIdentifier.REQ) @Context HttpServletRequest req, @Context SecurityContext sc)
    throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    QrCode qrCode = userController.resetQRCode(user, password);
    return Response.ok(qrCode).build();
  }

  @POST
  @Path("getQRCode")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the logged in User\'s QR code.", response = QrCode.class)
  public Response getQRCode(@Secret @FormParam("password") String password,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password was not provided.");
    }
    QrCode qrCode = userController.getQRCode(user, password);
    return Response.ok(qrCode).build();
  }

  @POST
  @Path("getRole")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the logged in User\'s role in project.", response = UserProjectDTO.class)
  public Response getRole(@FormParam("projectId") int projectId, @Context SecurityContext sc,
                          @Context HttpServletRequest req) throws ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    UserProjectDTO userDTO = new UserProjectDTO();
    userDTO.setEmail(user.getEmail());
    userDTO.setProject(projectId);
    Project project = projectController.findProjectById(projectId);
    ProjectTeam pt = projectTeamFacade.findByPrimaryKey(project, user);
    if (pt == null) {
      return Response.ok().entity(userDTO).build();
    }
    userDTO.setRole(pt.getTeamRole());
    return Response.ok().entity(userDTO).build();
  }


  @ApiOperation(value = "Get git providers with the configured secrets", response= GitProviderSecretsDTO.class)
  @GET
  @Path("/git/provider")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response gitProviders(@Context SecurityContext sc,
                               @Context HttpServletRequest req,
                               @Context UriInfo uriInfo) {
    Users user = jWTHelper.getUserPrincipal(sc);
    GitProviderSecretsDTO dto = gitProvidersSecretsBuilder.build(uriInfo, user);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Configure a Git Provider secrets", response= GitProviderSecretsDTO.class)
  @POST
  @Path("/git/provider")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response setGitProvider(@Context SecurityContext sc,
                                 @Context HttpServletRequest req,
                                 @Context UriInfo uriInfo,
                                 GitProviderSecretsDTO gitProviderSecretsDTO) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    gitCommandOperationUtil.createAuthenticationSecret(user,
        gitProviderSecretsDTO.getUsername(),
        gitProviderSecretsDTO.getToken(),
        gitProviderSecretsDTO.getGitProvider());
    GitProviderSecretsDTO dto = gitProvidersSecretsBuilder.build(uriInfo, user);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Path("/validate/otp")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  @ApiOperation(value = "Validate OTP")
  public Response validateOTP(@Secret @FormParam("otp") String otp,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    authController.validateOTP(user, otp);
    return Response.ok().build();
  }
  
  @Logged(logLevel = LogLevel.OFF)
  @Path("activities")
  public UserActivitiesResource activities() {
    return this.activitiesResource;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  @Path("apiKey")
  public ApiKeyResource apiKey() {
    return this.apiKeyResource;
  }

}
