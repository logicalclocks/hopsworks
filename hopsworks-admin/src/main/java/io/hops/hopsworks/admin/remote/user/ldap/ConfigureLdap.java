/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.admin.remote.user.ldap;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class ConfigureLdap implements Serializable {
  
  private final Logger LOGGER = Logger.getLogger(ConfigureLdap.class.getName());
  
  private UserAccountStatus accountStatus;
  private String groupMapping;
  private String userId;
  private String userGivenName;
  private String userSurname;
  private String userEmail;
  private String userSearchFilter;
  private String groupSearchFilter;
  private String principalSearchFilter;
  
  private String groupTarget;
  private String dynGroupTarget;
  private String userDn;
  private String groupDn;
  private Map<String, String> variablesToUpdate;
  private boolean ldapEnabled;
  
  @EJB
  private Settings settings;
  
  @PostConstruct
  public void init() {
    this.accountStatus = UserAccountStatus.fromValue(settings.getLdapAccountStatus());
    this.groupMapping = settings.getLdapGroupMapping();
    this.userId = settings.getLdapUserId();
    this.userGivenName = settings.getLdapUserGivenName();
    this.userSurname = settings.getLdapUserSurname();
    this.userEmail = settings.getLdapUserMail();
    this.userSearchFilter = settings.getLdapUserSearchFilter();
    this.groupSearchFilter = settings.getLdapGroupSearchFilter();
    this.principalSearchFilter = settings.getKrbUserSearchFilter();
    this.groupTarget = settings.getLdapGroupTarget();
    this.dynGroupTarget = settings.getLdapDynGroupTarget();
    this.userDn = settings.getLdapUserDN();
    this.groupDn = settings.getLdapGroupDN();
    this.variablesToUpdate = new HashMap<>();
    this.ldapEnabled = settings.isLdapEnabled() || settings.isKrbEnabled();
  }
  
  public UserAccountStatus getAccountStatus() {
    return accountStatus;
  }
  
  public void setAccountStatus(UserAccountStatus accountStatus) {
    update(settings.getLdapAccountStatus() + "", accountStatus.getValue() + "", settings.
      getVarLdapAccountStatus());
    this.accountStatus = accountStatus;
  }
  
  public UserAccountStatus[] getAccountStatuses() {
    return UserAccountStatus.values();
  }
  
  public String getGroupMapping() {
    return groupMapping;
  }
  
  public void setGroupMapping(String groupMapping) {
    update(settings.getLdapGroupMapping(), groupMapping, settings.getVarLdapGroupMapping());
    this.groupMapping = groupMapping;
  }
  
  public String getUserId() {
    return userId;
  }
  
  public void setUserId(String userId) {
    update(settings.getLdapUserId(), userId, settings.getVarLdapUserId());
    this.userId = userId;
  }
  
  public String getUserGivenName() {
    return userGivenName;
  }
  
  public void setUserGivenName(String userGivenName) {
    update(settings.getLdapUserGivenName(), userGivenName, settings.getVarLdapUserGivenName());
    this.userGivenName = userGivenName;
  }
  
  public String getUserSurname() {
    return userSurname;
  }
  
  public void setUserSurname(String userSurname) {
    update(settings.getLdapUserSurname(), userSurname, settings.getVarLdapUserSurname());
    this.userSurname = userSurname;
  }
  
  public String getUserEmail() {
    return userEmail;
  }
  
  public void setUserEmail(String userEmail) {
    update(settings.getLdapUserMail(), userEmail, settings.getVarLdapUserMail());
    this.userEmail = userEmail;
  }
  
  public String getUserSearchFilter() {
    return userSearchFilter;
  }
  
  public void setUserSearchFilter(String userSearchFilter) {
    update(settings.getLdapUserSearchFilter(), userSearchFilter, settings.getVarLdapUserSearchFilter());
    this.userSearchFilter = userSearchFilter;
  }
  
  public String getGroupSearchFilter() {
    return groupSearchFilter;
  }
  
  public void setGroupSearchFilter(String groupSearchFilter) {
    update(settings.getLdapGroupSearchFilter(), groupSearchFilter, settings.getVarLdapGroupSearchFilter());
    this.groupSearchFilter = groupSearchFilter;
  }
  
  public String getPrincipalSearchFilter() {
    return principalSearchFilter;
  }
  
  public void setPrincipalSearchFilter(String principalSearchFilter) {
    update(settings.getKrbUserSearchFilter(), principalSearchFilter, settings.getVarKrbUserSearchFilter());
    this.principalSearchFilter = principalSearchFilter;
  }
  
  public String getGroupTarget() {
    return groupTarget;
  }
  
  public void setGroupTarget(String groupTarget) {
    update(settings.getLdapGroupTarget(), groupTarget, settings.getVarLdapGroupTarget());
    this.groupTarget = groupTarget;
  }
  
  public String getDynGroupTarget() {
    return dynGroupTarget;
  }
  
  public void setDynGroupTarget(String dynGroupTarget) {
    update(settings.getLdapDynGroupTarget(), dynGroupTarget, settings.getVarLdapDynGroupTarget());
    this.dynGroupTarget = dynGroupTarget;
  }
  
  public String getUserDn() {
    return userDn;
  }
  
  public void setUserDn(String userDn) {
    update(settings.getLdapUserDN(), userDn, settings.getVarLdapUserDN());
    this.userDn = userDn;
  }
  
  public String getGroupDn() {
    return groupDn;
  }
  
  public void setGroupDn(String groupDn) {
    update(settings.getLdapGroupDN(), groupDn, settings.getVarLdapGroupDN());
    this.groupDn = groupDn;
  }
  
  public boolean isLdapEnabled() {
    return ldapEnabled;
  }
  
  public void setLdapEnabled(boolean ldapEnabled) {
    this.ldapEnabled = ldapEnabled;
  }
  
  private void update(String oldVal, String newVal, String variable) {
    if (newVal.equals(oldVal)) {
      variablesToUpdate.remove(variable);
    } else {
      variablesToUpdate.put(variable, newVal);
    }
  }
  
  public void save() {
    try {
      settings.updateVariables(variablesToUpdate);
      MessagesController.addInfoMessage("Updated " + variablesToUpdate.size() + " variables.");
      init();
    } catch (Exception e) {
      MessagesController.addMessageToGrowl(e.getMessage());
    }
  }
}
