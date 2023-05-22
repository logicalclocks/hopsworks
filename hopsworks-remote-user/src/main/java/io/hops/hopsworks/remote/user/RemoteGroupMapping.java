/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.remote.user.ldap.LdapRealm;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.AccessTimeout;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
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
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
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
  @EJB
  private GroupMappingProcessor groupMappingProcessor;
  
  public void syncMapping() {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    groupMappingProcessor.syncMapping(ldapRealm, remoteUserGroupMapper, remoteUserFacade);
  }

  @Asynchronous
  public void syncUserMappingAsync(RemoteUser user, List<String> groups) {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    try {
      LOGGER.log(Level.FINE, "Synchronizing remote user " + user + " groups with Projects");
      groupMappingProcessor.syncMapping(user, groups, ldapRealm, remoteUserGroupMapper, remoteUserFacade);
      LOGGER.log(Level.FINE, "Finished synchronizing remote user " + user + " groups with Projects");
    } catch (UserException ex) {
      LOGGER.log(Level.SEVERE, "Failed to synchronize remote user " + user + " groups with Projects", ex);
    }
  }
  
  public void syncMapping(RemoteUser remoteUser, List<String> groups) throws UserException {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    groupMappingProcessor.syncMapping(remoteUser, groups, ldapRealm, remoteUserGroupMapper, remoteUserFacade);
  }
  
  
  public void removeFromAllProjects(RemoteUser remoteUser) throws UserException {
    groupMappingProcessor.removeFromAllProjects(remoteUser, remoteUserGroupMapper, remoteUserFacade);
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
}
