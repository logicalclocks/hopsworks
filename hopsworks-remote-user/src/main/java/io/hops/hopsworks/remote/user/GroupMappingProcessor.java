/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import fish.payara.cluster.Clustered;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserStatus;
import io.hops.hopsworks.remote.user.ldap.LdapRealm;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Clustered
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GroupMappingProcessor implements Serializable {
  private final static Logger LOGGER = Logger.getLogger(GroupMappingProcessor.class.getName());
  
  private static final long serialVersionUID = 259100858106702492L;
  
  public void syncMapping(LdapRealm ldapRealm, RemoteUserGroupMapper remoteUserGroupMapper,
      RemoteUserFacade remoteUserFacade) {
    List<RemoteUser> remoteUsers = remoteUserFacade.findAll();
    for (RemoteUser remoteUser : remoteUsers) {
      try {
        syncMapping(remoteUser, null, ldapRealm, remoteUserGroupMapper, remoteUserFacade);
      } catch (UserException ignored) {
      }
    }
  }
  
  public void syncMapping(RemoteUser remoteUser, List<String> groups, LdapRealm ldapRealm,
      RemoteUserGroupMapper remoteUserGroupMapper, RemoteUserFacade remoteUserFacade) throws UserException {
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
        setStatus(remoteUser, RemoteUserStatus.ACTIVE, remoteUserFacade);
      }
      remoteUserGroupMapper.mapRemoteGroupToProject(remoteUser.getUid(), groups);
      remoteUserGroupMapper.cleanRemoteGroupToProjectMapping(remoteUser.getUid(), groups);
    } catch (NamingException e) {
      if (RemoteUserStatus.ACTIVE.equals(remoteUser.getStatus())) {
        setStatus(remoteUser, RemoteUserStatus.DELETED, remoteUserFacade);
        LOGGER.log(Level.INFO, "Failed to get user groups from LDAP. Marked user {0} as deleted. {1}",
          new Object[]{remoteUser.getUid().getUsername(), e.getMessage()});
      } else {
        LOGGER.log(Level.INFO, "Failed to get user groups from LDAP. {0}", e.getMessage());
      }
    }
  }
  
  public void removeFromAllProjects(RemoteUser remoteUser, RemoteUserGroupMapper remoteUserGroupMapper,
      RemoteUserFacade remoteUserFacade) throws UserException {
    if (remoteUser == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "Remote user not found");
    }
    if (RemoteUserStatus.ACTIVE.equals(remoteUser.getStatus())) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE,
        "Remote user is still active.");
    }
    remoteUserGroupMapper.removeFromAllProjects(remoteUser.getUid());
    setStatus(remoteUser, RemoteUserStatus.DEACTIVATED, remoteUserFacade);
  }
  
  private void setStatus(RemoteUser remoteUser, RemoteUserStatus status, RemoteUserFacade remoteUserFacade) {
    remoteUser.setStatus(status);
    remoteUserFacade.update(remoteUser);
  }
}
