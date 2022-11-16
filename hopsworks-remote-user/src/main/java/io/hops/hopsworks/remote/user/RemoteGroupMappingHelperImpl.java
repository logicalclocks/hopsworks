/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.remote.group.mapping.RemoteGroupMappingHelper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@RemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteGroupMappingHelperImpl  implements RemoteGroupMappingHelper {
  
  @EJB
  private Settings settings;
  @EJB
  private RemoteGroupMapping remoteGroupMapping;
  
  @Override
  public boolean isRemoteGroupMappingAvailable() {
    return settings.isLdapEnabled() || settings.isKrbEnabled();
  }
  
  @Override
  public void syncMapping(RemoteUser remoteUser) throws UserException {
    check();
    remoteGroupMapping.syncMapping(remoteUser, null);
  }
  
  @Override
  public void syncMappingAsync() {
    check();
    remoteGroupMapping.syncMapping();
  }
  
  @Override
  public void removeFromAllProjects(RemoteUser remoteUser) throws UserException {
    check();
    remoteGroupMapping.removeFromAllProjects(remoteUser);
  }
  
  private void check() {
    if (!isRemoteGroupMappingAvailable()) {
      throw new UnsupportedOperationException("Remote Group Mapping not supported.");
    }
  }
}
