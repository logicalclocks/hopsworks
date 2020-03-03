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
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.user.apiKey.ApiKeyResource;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserProjectDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.binary.Base64;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.UriInfo;

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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all users.", response = UserDTO.class)
  public Response findAll(@BeanParam Pagination pagination,
      @BeanParam UsersBeanParam usersBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) {
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
  public Response findById(
      @PathParam("userId") Integer userId,
      @Context UriInfo uriInfo,
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
  public Response getUserProfile(@Context UriInfo uriInfo, @Context SecurityContext sc) throws UserException {
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
  @ApiOperation(value = "Updates logged in User\'s info.", response = UserProfileDTO.class)
  public Response updateProfile(@FormParam("firstname") String firstName,
      @FormParam("lastname") String lastName,
      @FormParam("phoneNumber") String phoneNumber,
      @FormParam("toursState") Integer toursState,
      @Context UriInfo uriInfo, @Context HttpServletRequest req,
      @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    user = userController.updateProfile(user, firstName, lastName, phoneNumber, toursState);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    UserProfileDTO userDTO = usersBuilder.buildFull(uriInfo, resourceRequest, user);
    return Response.created(userDTO.getHref()).entity(userDTO).build();
  }

  @POST
  @Path("credentials")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Updates logedin User\'s credentials.", response = RESTApiJsonResponse.class)
  public Response changeLoginCredentials(
    @FormParam("oldPassword") String oldPassword,
    @FormParam("newPassword") String newPassword,
    @FormParam("confirmedPassword") String confirmedPassword,
    @Context HttpServletRequest req,
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
  @ApiOperation(value = "Stores a secret for user")
  public Response addSecret(SecretDTO secret, @Context SecurityContext sc, @Context HttpServletRequest req)
    throws UserException {
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
  public Response getAllSecrets(@Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<SecretPlaintext> secrets = secretsController.getAllForUser(user);
    SecretDTO dto = secretsBuilder.build(secrets, false);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("secrets/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the value of a shared secret", response = SecretDTO.class)
  public Response getSharedSecret(@QueryParam("name") String secretName, @QueryParam("owner") String ownerUsername,
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
  public Response getSecret(@PathParam("secretName") String name, @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    SecretPlaintext secret = secretsController.get(user, name);
    SecretDTO dto = secretsBuilder.build(Arrays.asList(secret), true);
    return Response.ok().entity(dto).build();
  }

  @DELETE
  @Path("secrets/{secretName}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Deletes a secret by its name")
  public Response deleteSecret(@PathParam("secretName") String name, @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    secretsController.delete(user, name);
    return Response.ok().build();
  }

  @DELETE
  @Path("secrets")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Deletes all secrets of a user")
  public Response deleteAllSecrets(@Context SecurityContext sc, @Context HttpServletRequest req) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    secretsController.deleteAll(user);
    return Response.ok().build();
  }

  @POST
  @Path("securityQA")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Updates logedin User\'s security question and answer.", response = RESTApiJsonResponse.class)
  public Response changeSecurityQA(@FormParam("oldPassword") String oldPassword,
    @FormParam("securityQuestion") String securityQuestion,
    @FormParam("securityAnswer") String securityAnswer,
    @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    userController.changeSecQA(user, oldPassword, securityQuestion, securityAnswer);
    json.setSuccessMessage(ResponseMessages.SEC_QA_CHANGED);
    return Response.ok().entity(json).build();
  }

  @POST
  @Path("twoFactor")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Updates logedin User\'s two factor setting.", response = RESTApiJsonResponse.class)
  public Response changeTwoFactor(@FormParam("password") String password,
    @FormParam("twoFactor") boolean twoFactor, @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    byte[] qrCode;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (user.getTwoFactor() == twoFactor) {
      json.setSuccessMessage("No change made.");
      return Response.ok().entity(json).build();
    }
    qrCode = userController.changeTwoFactor(user, password);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage("Tow factor authentication disabled.");
    }
    return Response.ok().entity(json).build();
  }

  @POST
  @Path("getQRCode")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the logged in User\'s QR code.", response = RESTApiJsonResponse.class)
  public Response getQRCode(@FormParam("password") String password, @Context HttpServletRequest req,
    @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password was not provided.");
    }
    byte[] qrCode;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    qrCode = userController.getQRCode(user, password);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    return Response.ok().entity(json).build();
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
  
  @Path("activities")
  public UserActivitiesResource activities() {
    return this.activitiesResource;
  }
  
  @Path("apiKey")
  public ApiKeyResource apiKey() {
    return this.apiKeyResource;
  }

}
