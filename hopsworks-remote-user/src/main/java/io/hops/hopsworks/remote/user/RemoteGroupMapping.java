/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserStatus;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.remote.user.ldap.LdapRealm;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.AccessTimeout;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RemoteGroupMapping {
  private final static Logger LOGGER = Logger.getLogger(RemoteGroupMapping.class.getName());
  
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private LdapRealm ldapRealm;
  @EJB
  private RemoteUserGroupMapper remoteUserGroupMapper;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  
  public void syncMapping() {
    List<RemoteUser> remoteUsers = remoteUserFacade.findAll();
    for (RemoteUser remoteUser : remoteUsers) {
      try {
        syncMapping(remoteUser, null);
      } catch (UserException e) {
      }
    }
  }
  
  @Asynchronous
  public void syncMappingAsync() {
    List<RemoteUser> remoteUsers = remoteUserFacade.findAll();
    for (RemoteUser remoteUser : remoteUsers) {
      try {
        syncMapping(remoteUser, null);
      } catch (UserException e) {
      }
    }
  }

  @Asynchronous
  public void syncUserMappingAsync(RemoteUser user, List<String> groups) {
    try {
      LOGGER.log(Level.FINE, "Synchronizing remote user " + user + " groups with Projects");
      syncMapping(user, groups);
      LOGGER.log(Level.FINE, "Finished synchronizing remote user " + user + " groups with Projects");
    } catch (UserException ex) {
      LOGGER.log(Level.SEVERE, "Failed to synchronize remote user " + user + " groups with Projects", ex);
    }
  }
  
  public void syncMapping(RemoteUser remoteUser, List<String> groups) throws UserException {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    if (remoteUser == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found");
    }
    try {
      if (groups == null) {
        groups = ldapRealm.getUserGroups(remoteUser);
      }
      //if the user is not active but exists reactivate
      if (groups != null && !groups.isEmpty() && (RemoteUserStatus.DELETED.equals(remoteUser.getStatus()) ||
        RemoteUserStatus.DEACTIVATED.equals(remoteUser.getStatus()))) {
        setStatus(remoteUser, RemoteUserStatus.ACTIVE);
      }
      remoteUserGroupMapper.mapRemoteGroupToProject(remoteUser.getUid(), groups);
      remoteUserGroupMapper.cleanRemoteGroupToProjectMapping(remoteUser.getUid(), groups);
    } catch (NamingException e) {
      if (RemoteUserStatus.ACTIVE.equals(remoteUser.getStatus())) {
        setStatus(remoteUser, RemoteUserStatus.DELETED);
        LOGGER.log(Level.INFO, "Failed to get user groups from LDAP. Marked user {0} as deleted. {1}",
          new Object[]{remoteUser.getUid().getUsername(), e.getMessage()});
      } else {
        LOGGER.log(Level.INFO, "Failed to get user groups from LDAP. {0}", e.getMessage());
      }
    }
  }
  
  
  public void removeFromAllProjects(RemoteUser remoteUser) throws UserException {
    if (remoteUser == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found");
    }
    if (RemoteUserStatus.ACTIVE.equals(remoteUser.getStatus())) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE,
        "Remote user is still active.");
    }
    remoteUserGroupMapper.removeFromAllProjects(remoteUser.getUid());
    setStatus(remoteUser, RemoteUserStatus.DEACTIVATED);
  }
  
  public List<String> getGroups(String email) throws NamingException, UserException {
    Users users = userFacade.findByEmail(email);
    if (users == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    RemoteUser remoteUser = remoteUserFacade.findByUsers(users);
    if (remoteUser == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found");
    }
    return ldapRealm.getUserGroups(remoteUser);
  }
  
  private void setStatus(RemoteUser remoteUser, RemoteUserStatus status) {
    remoteUser.setStatus(status);
    remoteUserFacade.update(remoteUser);
  }
}
