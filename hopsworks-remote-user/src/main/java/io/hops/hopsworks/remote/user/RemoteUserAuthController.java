/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteUserAuthController {

  private final static Logger LOGGER = Logger.getLogger(RemoteUserAuthController.class.getName());
  @EJB
  private AuthController authController;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private UsersController userController;
  @EJB
  private RemoteUserGroupMapper remoteUserGroupMapper;
  @EJB
  private SecurityUtils securityUtils;

  /**
   * Checks if the user is a remote user and returns the user password with salt
   * @param user
   * @param password
   * @return 
   */
  public String preRemoteUserLoginCheck(Users user, String password) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (!(user.getMode().equals(UserAccountType.REMOTE_ACCOUNT_TYPE))) {
      throw new IllegalArgumentException("User is not registerd as a remote user.");
    }
    return authController.getPasswordPlusSalt(password, user.getSalt()) + Settings.MOBILE_OTP_PADDING;
  }

  /**
   * 
   * @param user
   * @param projectId
   * @throws ProjectException
   * @throws UserException 
   */
  public void checkProjectMembership(Users user, Integer projectId) throws ProjectException, UserException {
    Project project = projectController.findProjectById(projectId);
    String userRole = projectTeamBean.findCurrentRole(project, user);

    if (userRole == null || userRole.isEmpty()) {
      LOGGER.log(Level.INFO, "User: {0} trying to access resource {1}, but have on role in this project.", new Object[]{
        user.getEmail(), project.getName()});
      throw new UserException(RESTCodes.UserErrorCode.NO_ROLE_FOUND, Level.FINE, RESTCodes.UserErrorCode.NO_ROLE_FOUND.
          getMessage());
    } else if (!userRole.equals(AllowedRoles.DATA_OWNER)) {
      LOGGER.log(Level.INFO, "Trying to access resource that is only allowed for: {0}, But you are a: {1}",
          new Object[]{AllowedRoles.DATA_OWNER, userRole});
      throw new UserException(RESTCodes.UserErrorCode.ACCESS_CONTROL, Level.FINE, RESTCodes.UserErrorCode.ACCESS_CONTROL
          .getMessage());
    }
  }

  /**
   * Gets or creates remote user
   * @param userDTO
   * @param consent
   * @param chosenEmail
   * @param type
   * @return
   * @throws LoginException 
   */
  public RemoteUserStateDTO getRemoteUserStatus(RemoteUserDTO userDTO, boolean consent, String chosenEmail,
      RemoteUserType type, UserAccountStatus status) throws LoginException {
    if (userDTO == null || !userDTO.isEmailVerified()) {
      throw new LoginException("User not found.");
    }
    RemoteUser remoteUser = remoteUserFacade.findByUUID(userDTO.getUuid());
    RemoteUserStateDTO remoteUserStateDTO;
    if (remoteUser == null) {
      if (consent) {
        remoteUser = createNewRemoteUser(userDTO, chosenEmail, type, status);
        persistRemoteUser(remoteUser);
        remoteUserGroupMapper.mapRemoteGroupToProject(remoteUser.getUid(), userDTO.getGroups());//add to project on
        // first login
      }
      remoteUserStateDTO = new RemoteUserStateDTO(consent, remoteUser, userDTO);
      return remoteUserStateDTO;
    }
    remoteUserStateDTO = new RemoteUserStateDTO(true, remoteUser, userDTO);
    if (remoteUserUpdated(userDTO, remoteUser.getUid())) {
      remoteUser = updateRemoteUser(userDTO, remoteUser);//do we need to ask again?
      remoteUserStateDTO.setRemoteUser(remoteUser);
      return remoteUserStateDTO;
    }
    return remoteUserStateDTO;
  }

  private RemoteUser createNewRemoteUser(RemoteUserDTO userDTO, String chosenEmail, RemoteUserType type,
    UserAccountStatus status) throws LoginException {
    if (userDTO.getEmail().size() != 1 && (chosenEmail == null || chosenEmail.isEmpty())) {
      throw new LoginException("Could not register user. Email not chosen.");
    }
    if (!userDTO.getEmail().contains(chosenEmail)) {
      throw new LoginException("Could not register user. Chosen email not in user email list.");
    }
    if (!userDTO.isEmailVerified()) {
      throw new LoginException("Email not verified.");
    }
    String email = userDTO.getEmail().size() == 1 ? userDTO.getEmail().get(0) : chosenEmail;
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      throw new LoginException("Failed to login. A user with the chosen email already exists.");
    }
    String authKey = securityUtils.generateSecureRandomString(16);
    Users user =
      userController.createNewRemoteUser(email, userDTO.getGivenName(), userDTO.getSurname(), authKey, status);
    remoteUserGroupMapper.mapRemoteGroupToGroup(user, userDTO.getGroups(), type);
    return new RemoteUser(userDTO.getUuid(), user, authKey, type);
  }

  private boolean remoteUserUpdated(RemoteUserDTO user, Users uid) {
    if (user == null || uid == null) {
      return false;
    }
    return !uid.getFname().equals(user.getGivenName()) || !uid.getLname().equals(user.getSurname());
  }

  private RemoteUser updateRemoteUser(RemoteUserDTO user, RemoteUser remoteUser) {
    if (!remoteUser.getUid().getFname().equals(user.getGivenName())) {
      remoteUser.getUid().setFname(user.getGivenName());
    }
    if (!remoteUser.getUid().getLname().equals(user.getSurname())) {
      remoteUser.getUid().setLname(user.getSurname());
    }
    return remoteUserFacade.update(remoteUser);
  }

  private void persistRemoteUser(RemoteUser remoteUser) {
    remoteUserFacade.save(remoteUser);
  }
}
