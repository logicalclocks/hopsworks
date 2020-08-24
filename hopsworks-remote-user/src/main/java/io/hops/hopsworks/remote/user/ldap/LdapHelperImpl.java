/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.ldap;

import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.common.remote.ldap.LdapHelper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.remote.user.RemoteAuthStereotype;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@RemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LdapHelperImpl implements LdapHelper {
  
  @EJB
  private Settings settings;
  @EJB
  private LdapUserController ldapUserController;
  
  @Override
  public boolean isLdapAvailable() {
    return settings.isLdapEnabled() || settings.isKrbEnabled();
  }
  
  @Override
  public List<String> getLDAPGroups(String groupQuery) {
    return ldapUserController.getLDAPGroups(groupQuery);
  }
  
  @Override
  public RemoteUsersDTO getMembers(List<String> target) {
    return ldapUserController.getMembers(target);
  }
  
  @Override
  public void addNewGroupProjectMapping(RemoteGroupProjectMapping remoteGroupProjectMapping) {
    ldapUserController.addNewGroupProjectMapping(remoteGroupProjectMapping);
  }
}
