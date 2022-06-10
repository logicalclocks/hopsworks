/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.ldap;

import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.remote.user.RemoteUserAuthController;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LdapUserController {
  
  private final static Logger LOGGER = Logger.getLogger(LdapUserController.class.getName());
  @EJB
  private LdapRealm ldapRealm;
  @EJB
  private RemoteUserAuthController remoteUserAuthController;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private RemoteGroupProjectMappingFacade remoteGroupProjectMappingFacade;
  @EJB
  private Settings settings;
  
  /**
   * Try to login ldap user.
   *
   * @param username
   * @param password
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO login(String username, String password, boolean consent, String chosenEmail) throws
    LoginException, UserException {
    RemoteUserDTO userDTO = null;
    try {
      userDTO = ldapRealm.findAndBind(username, password);// login user
    } catch (EJBException | NamingException ee) {
      LOGGER.log(Level.INFO, "Error binding user", ee);
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.LDAP,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
  /**
   * Get kerberos user from ldap
   *
   * @param principalName
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO getKrbLdapUser(String principalName, boolean consent, String chosenEmail) throws
    LoginException, UserException {
    RemoteUserDTO userDTO = null;
    try {
      userDTO = ldapRealm.findKrbUser(principalName);
    } catch (EJBException | NamingException ee) {
      LOGGER.log(Level.WARNING, "Could not reach LDAP server. {0}", ee.getMessage());
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.KRB,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
  /**
   * @param user
   * @param password
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO getLdapUser(Users user, String password, boolean consent, String chosenEmail)
    throws LoginException, UserException {
    RemoteUserDTO userDTO = null;
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    try {
      userDTO = ldapRealm.getLdapUser(remoteUser, password);
    } catch (EJBException | NamingException e) {
      LOGGER.log(Level.WARNING, "Could not reach LDAP server. {0}", e.getMessage());
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.LDAP,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
  public List<String> getLDAPGroups(String groupSearchFilter) {
    if (groupSearchFilter == null || groupSearchFilter.isEmpty()) {
      throw new IllegalArgumentException("Group search filter can not be empty.");
    }
    return ldapRealm.getAllGroups(groupSearchFilter);
  }
  
  public RemoteUsersDTO getMembers(List<String> groups) {
    if (groups == null || groups.isEmpty()) {
      throw new IllegalArgumentException("Groups can not be empty.");
    }
    RemoteUsersDTO remoteUsersDTO = null;
    try {
      remoteUsersDTO = ldapRealm.getAllMembers(groups);
    } catch (NamingException e) {
      LOGGER.log(Level.WARNING, "Failed to get members. {0}", e.getMessage());
    }
    return remoteUsersDTO;
  }
  
  public void addNewGroupProjectMapping(RemoteGroupProjectMapping remoteGroupProjectMapping) {
    remoteGroupProjectMappingFacade.save(remoteGroupProjectMapping);
  }
  
  public RemoteUserDTO getRemoteUserByUuid(String uuid) throws UserException {
    try {
      return ldapRealm.getUserByUuid(uuid);
    } catch (NamingException e) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found.",
        e.getMessage());
    }
  }
}
