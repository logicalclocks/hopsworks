/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UserAccountHandler;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.restutils.RESTCodes;
import java.util.ArrayList;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.util.List;
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
  private BbcGroupFacade groupFacade;
  @EJB
  private RemoteUserGroupMapper remoteUserGroupMapper;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private Settings settings;
  @Inject
  @Any
  private Instance<UserAccountHandler> userAccountHandlers;
  
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
    return authController.getPasswordPlusSalt(password, user.getSalt());
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
    RemoteUserType type, UserAccountStatus status, boolean updateGroup) throws LoginException, UserException {
    if (userDTO == null) {
      throw new LoginException("User not found.");
    }
    //shouldValidateEmailVerified will disable check globally
    //isEmailVerified is set based on client configuration in OAuth login
    if (!userDTO.isEmailVerified() && settings.shouldValidateEmailVerified()) {
      throw new LoginException("User email not yet verified.");
    }
    RemoteUser remoteUser = remoteUserFacade.findByUUID(userDTO.getUuid());
    if (remoteUser == null) {
      if (consent) {
        validateRemoteUser(userDTO, chosenEmail);
        remoteUser = createNewRemoteUser(userDTO, chosenEmail, type, status);
      } else {
        LOGGER.log(Level.FINE, "Remote user doesn't exists in the database, and consent is false");
      }
      return new RemoteUserStateDTO(consent, remoteUser, userDTO);
    }

    // the user already exists, check if it needs to be updated and return the updated state
    // update the user groups, in case they have been changed in the identity provider
    if (updateGroup) {
      updateGroups(userDTO, remoteUser, type);
    }
    
    remoteUser = updateRemoteUser(userDTO, remoteUser);
    return new RemoteUserStateDTO(true, remoteUser, userDTO);
  }
  
  private RemoteUser createNewRemoteUser(RemoteUserDTO userDTO, String chosenEmail, RemoteUserType type,
    UserAccountStatus status) throws LoginException, UserException {
    String email = userDTO.getEmail().size() == 1 ? userDTO.getEmail().get(0) : chosenEmail;
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      throw new LoginException("Failed to login. A user with the chosen email already exists.");
    }
    List<String> groups = remoteUserGroupMapper.getMappedGroups(userDTO.getGroups(), type);
    if (settings.getRejectRemoteNoGroup() && groups.isEmpty()) {
      //the user won't belong to any group in hopsworks, do not create the user at all
      throw new LoginException("Remote user has no valid groups");
    }
    return createRemoteUser(userDTO.getUuid(), email, userDTO.getGivenName(), userDTO.getSurname(),
      type, status, groups);
  }
  
  /**
   * Keep the mapped groups up to date with the ones in the identity provider
   * @param userDTO
   * @param type
   */
  private RemoteUser updateGroups(RemoteUserDTO userDTO, RemoteUser remoteUser, RemoteUserType type)
    throws UserException {
    List<String> groups = remoteUserGroupMapper.getMappedGroups(userDTO.getGroups(), type);
    Users user = remoteUser.getUid();
    BbcGroup group;
    for (String grp : groups) {
      group = groupFacade.findByGroupName(grp);
      if (group != null && !user.getBbcGroupCollection().contains(group)) {
        userFacade.addGroup(user.getEmail(), group.getGid());
        user.getBbcGroupCollection().add(group);
      }
    }
    List<BbcGroup> toRemove = new ArrayList<>();
    for (BbcGroup g : user.getBbcGroupCollection()) {
      //empty group mapping is used to remove users from cluster in the cloud
      if (!groups.contains(g.getGroupName())) {
        toRemove.add(g);
        userFacade.removeGroup(user.getEmail(), g.getGid());
      }
    }
    user.getBbcGroupCollection().removeAll(toRemove);
    
    // trigger user account handlers
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, remoteUser.getUid());
  
    return remoteUser;
  }

  public void createRemoteUser(RemoteUserDTO userDTO, String email, String givenName, String surname,
    RemoteUserType type, UserAccountStatus status) throws UserException, GenericException {
    if (userDTO == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found.");
    }
    if (Strings.isNullOrEmpty(userDTO.getUuid())) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Uuid not provided.");
    }
    if (type == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Remote type not provided.");
    }
    RemoteUser remoteUser = remoteUserFacade.findByUUID(userDTO.getUuid());
    if (remoteUser != null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_EXISTS, Level.FINE, "Remote user exists.");
    }
    String chosenEmail = getStringOrDefault(email, !userDTO.getEmail().isEmpty()? userDTO.getEmail().get(0) : null,
      "Email");
    
    String fname = getStringOrDefault(givenName, userDTO.getGivenName(), "GivenName");
    String lname = getStringOrDefault(surname, userDTO.getSurname(), "Surname");
    
    Users u = userFacade.findByEmail(chosenEmail);
    if (u != null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_EXISTS, Level.FINE, "User with the same email " +
        "already exists.");
    }
    List<String> groups = remoteUserGroupMapper.getMappedGroups(userDTO.getGroups(), type);
    createRemoteUser(userDTO.getUuid(), chosenEmail, fname, lname, type, status, groups);
  }
  
  private String getStringOrDefault(String val, String defaultVal, String msg) throws GenericException {
    String value;
    if (val != null && !val.isEmpty()) {
      value = val;
    } else if (defaultVal != null && !defaultVal.isEmpty()) {
      value = defaultVal;
    } else {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, msg + " not provided.");
    }
    return value;
  }
  
  public RemoteUser getRemoteUser(String uuid) throws UserException {
    RemoteUser remoteUser = remoteUserFacade.findByUUID(uuid);
    if (remoteUser == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found.");
    }
    return remoteUser;
  }
  
  private RemoteUser createRemoteUser(String uuid, String email, String givenName, String surname,
    RemoteUserType type, UserAccountStatus status, List<String> groups) throws UserException {
    String authKey = securityUtils.generateSecureRandomString(16);
    Users user = userController.createNewRemoteUser(email.toLowerCase(), givenName, surname, authKey, status);
    BbcGroup group;
    for (String grp : groups) {
      group = groupFacade.findByGroupName(grp);
      if (group != null) {
        user.getBbcGroupCollection().add(group);
      }
    }
    RemoteUser remoteUser = new RemoteUser(uuid, user, authKey, type);
    remoteUserFacade.save(remoteUser);
    
    // trigger user account handlers
    UserAccountHandler.runUserAccountCreateHandlers(userAccountHandlers, remoteUser.getUid());
    
    return remoteUser;
  }
  
  private boolean remoteUserUpdated(RemoteUserDTO user, Users uid) {
    if (user == null || uid == null) {
      return false;
    }
    return !uid.getFname().equals(user.getGivenName()) || !uid.getLname().equals(user.getSurname());
  }
  
  private RemoteUser updateRemoteUser(RemoteUserDTO user, RemoteUser remoteUser) throws UserException {
    if (remoteUserUpdated(user, remoteUser.getUid())) {
      remoteUser.getUid().setFname(user.getGivenName());
      remoteUser.getUid().setLname(user.getSurname());
      RemoteUser updatedUser = remoteUserFacade.update(remoteUser);
      
      // trigger user account handlers
      UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, updatedUser.getUid());
      
      return updatedUser;
    }
    return remoteUser;
  }
  
  private void validateRemoteUser(RemoteUserDTO user, String chosenEmail) throws LoginException {
    if (user.getUuid() == null || user.getUuid().isEmpty()) {
      throw new LoginException("Could not find UUID for remote user.");
    }
    if (user.getEmail() == null || user.getEmail().isEmpty()) {
      throw new LoginException("Could not find email for remote user.");
    }
    if (user.getEmail().size() != 1 && (chosenEmail == null || chosenEmail.isEmpty())) {
      throw new LoginException("Could not register user. Email not chosen.");
    }
    if (!user.getEmail().contains(chosenEmail)) {
      throw new LoginException("Could not register user. Chosen email not in user email list.");
    }
    if (user.getGivenName() == null || user.getGivenName().isEmpty()) {
      throw new LoginException("Could not find givenName for remote user.");
    }
    if (user.getSurname() == null || user.getSurname().isEmpty()) {
      throw new LoginException("Could not find surname for remote user.");
    }
  }
  
}
