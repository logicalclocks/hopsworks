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
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserCardDTO;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.UserProjectDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeyDTO;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import org.apache.commons.codec.binary.Base64;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;
import javax.inject.Inject;

@Path("/user")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "User", description = "User service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserService {

  private final static Logger LOGGER = Logger.getLogger(UserService.class.getName());

  @EJB
  private UserFacade userBean;
  @EJB
  private UsersController userController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @Inject
  private UserActivitiesResource activitiesResource;
  @EJB
  private JWTHelper jWTHelper;

  @GET
  @Path("allcards")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findAllByUser() {

    List<Users> users = userBean.findAllUsers();
    List<UserCardDTO> userCardDTOs = new ArrayList<>();

    for (Users user : users) {
      UserCardDTO userCardDTO = new UserCardDTO(user);
      userCardDTOs.add(userCardDTO);
    }
    GenericEntity<List<UserCardDTO>> userCards = new GenericEntity<List<UserCardDTO>>(userCardDTOs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(userCards).build();
  }

  @GET
  @Path("profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProfile(@Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    UserDTO userDTO = new UserDTO(user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(userDTO).build();
  }

  @POST
  @Path("updateProfile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateProfile(@FormParam("firstName") String firstName,
          @FormParam("lastName") String lastName,
          @FormParam("telephoneNum") String telephoneNum,
          @FormParam("toursState") Integer toursState,
          @Context HttpServletRequest req, @Context SecurityContext sc) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    user = userController.updateProfile(user, firstName, lastName, telephoneNum, toursState, req);
    UserDTO userDTO = new UserDTO(user);
   
    json.setSuccessMessage(ResponseMessages.PROFILE_UPDATED);
    json.setData(userDTO);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(userDTO).build();
  }

  @POST
  @Path("changeLoginCredentials")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeLoginCredentials(
          @FormParam("oldPassword") String oldPassword,
          @FormParam("newPassword") String newPassword,
          @FormParam("confirmedPassword") String confirmedPassword,
          @Context HttpServletRequest req, @Context SecurityContext sc) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    userController.changePassword(user, oldPassword, newPassword, confirmedPassword, req);
    json.setSuccessMessage(ResponseMessages.PASSWORD_CHANGED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("changeSecurityQA")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeSecurityQA(@FormParam("oldPassword") String oldPassword,
          @FormParam("securityQuestion") String securityQuestion,
          @FormParam("securityAnswer") String securityAnswer,
          @Context HttpServletRequest req, @Context SecurityContext sc) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    userController.changeSecQA(user, oldPassword, securityQuestion, securityAnswer, req);
    json.setSuccessMessage(ResponseMessages.SEC_QA_CHANGED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("changeTwoFactor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeTwoFactor(@FormParam("password") String password,
          @FormParam("twoFactor") boolean twoFactor,
          @Context HttpServletRequest req, @Context SecurityContext sc) throws UserException {
    Users user = jWTHelper.getUserPrincipal(sc);

    byte[] qrCode;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (user.getTwoFactor() == twoFactor) {
      json.setSuccessMessage("No change made.");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }

    qrCode = userController.changeTwoFactor(user, password, req);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage("Tow factor authentication disabled.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(json).build();
  }

  @POST
  @Path("getQRCode")
  @Produces(MediaType.APPLICATION_JSON)
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
    qrCode = userController.getQRCode(user, password, req);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("addSshKey")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addSshkey(SshKeyDTO sshkey, @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    int id = user.getUid();
    SshKeyDTO dto = userController.addSshKey(id, sshkey.getName(), sshkey.
            getPublicKey());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }

  @POST
  @Path("removeSshKey")
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeSshkey(@FormParam("name") String name, @Context SecurityContext sc) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    int id = user.getUid();
    userController.removeSshKey(id, name);
    json.setSuccessMessage(ResponseMessages.SSH_KEY_REMOVED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("getSshKeys")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSshkeys(@Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    int id = user.getUid();
    List<SshKeyDTO> sshKeys = userController.getSshKeys(id);

    GenericEntity<List<SshKeyDTO>> sshKeyViews
            = new GenericEntity<List<SshKeyDTO>>(sshKeys) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            sshKeyViews).build();

  }

  @POST
  @Path("getRole")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRole(@FormParam("projectId") int projectId, @Context SecurityContext sc) throws
      ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);

    UserProjectDTO userDTO = new UserProjectDTO();
    userDTO.setEmail(user.getEmail());
    userDTO.setProject(projectId);
    Project project = projectController.findProjectById(projectId);
    ProjectTeam pt = projectTeamFacade.findByPrimaryKey(project, user);
    if (pt == null) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(userDTO).build();
    }
    userDTO.setRole(pt.getTeamRole());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(userDTO).build();
  }
  
  @Path("activities")
  public UserActivitiesResource activities() {
    return this.activitiesResource;
  }

}
