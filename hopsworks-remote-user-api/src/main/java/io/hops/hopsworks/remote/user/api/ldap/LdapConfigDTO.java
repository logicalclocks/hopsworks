/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.ldap;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement
public class LdapConfigDTO extends RestDTO<LdapConfigDTO> {
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
  private String groupsTarget;
  private String groupsSearchFilter;
  private String groupMembersSearchFilter;
  
  public LdapConfigDTO() {
  }
  
  public LdapConfigDTO(Settings settings) {
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
    this.groupsSearchFilter = settings.getLdapGroupsSearchFilter();
    this.groupsTarget = settings.getLdapGroupsTarget();
    this.groupMembersSearchFilter = settings.getLdapGroupMembersFilter();
  }
  
  public UserAccountStatus getAccountStatus() {
    return accountStatus;
  }
  
  public void setAccountStatus(UserAccountStatus accountStatus) {
    this.accountStatus = accountStatus;
  }
  
  public String getGroupMapping() {
    return groupMapping;
  }
  
  public void setGroupMapping(String groupMapping) {
    this.groupMapping = groupMapping;
  }
  
  public String getUserId() {
    return userId;
  }
  
  public void setUserId(String userId) {
    this.userId = userId;
  }
  
  public String getUserGivenName() {
    return userGivenName;
  }
  
  public void setUserGivenName(String userGivenName) {
    this.userGivenName = userGivenName;
  }
  
  public String getUserSurname() {
    return userSurname;
  }
  
  public void setUserSurname(String userSurname) {
    this.userSurname = userSurname;
  }
  
  public String getUserEmail() {
    return userEmail;
  }
  
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }
  
  public String getUserSearchFilter() {
    return userSearchFilter;
  }
  
  public void setUserSearchFilter(String userSearchFilter) {
    this.userSearchFilter = userSearchFilter;
  }
  
  public String getGroupSearchFilter() {
    return groupSearchFilter;
  }
  
  public void setGroupSearchFilter(String groupSearchFilter) {
    this.groupSearchFilter = groupSearchFilter;
  }
  
  public String getPrincipalSearchFilter() {
    return principalSearchFilter;
  }
  
  public void setPrincipalSearchFilter(String principalSearchFilter) {
    this.principalSearchFilter = principalSearchFilter;
  }
  
  public String getGroupTarget() {
    return groupTarget;
  }
  
  public void setGroupTarget(String groupTarget) {
    this.groupTarget = groupTarget;
  }
  
  public String getDynGroupTarget() {
    return dynGroupTarget;
  }
  
  public void setDynGroupTarget(String dynGroupTarget) {
    this.dynGroupTarget = dynGroupTarget;
  }
  
  public String getUserDn() {
    return userDn;
  }
  
  public void setUserDn(String userDn) {
    this.userDn = userDn;
  }
  
  public String getGroupDn() {
    return groupDn;
  }
  
  public void setGroupDn(String groupDn) {
    this.groupDn = groupDn;
  }
  
  public String getGroupsTarget() {
    return groupsTarget;
  }
  
  public void setGroupsTarget(String groupsTarget) {
    this.groupsTarget = groupsTarget;
  }
  
  public String getGroupsSearchFilter() {
    return groupsSearchFilter;
  }
  
  public void setGroupsSearchFilter(String groupsSearchFilter) {
    this.groupsSearchFilter = groupsSearchFilter;
  }
  
  public String getGroupMembersSearchFilter() {
    return groupMembersSearchFilter;
  }
  
  public void setGroupMembersSearchFilter(String groupMembersSearchFilter) {
    this.groupMembersSearchFilter = groupMembersSearchFilter;
  }
  
  public Map<String, String> variables(Settings settings) {
    Map<String, String> variablesToUpdate = new HashMap<>();
    variablesToUpdate.put(settings.getVarLdapAccountStatus(), this.accountStatus.getValue() + "");
    variablesToUpdate.put(settings.getVarLdapGroupMapping(), this.groupMapping);
    variablesToUpdate.put(settings.getVarLdapUserId(), this.userId);
    variablesToUpdate.put(settings.getVarLdapUserGivenName(), this.userGivenName);
    variablesToUpdate.put(settings.getVarLdapUserSurname(), this.userSurname);
    variablesToUpdate.put(settings.getVarLdapUserMail(), this.userEmail);
    variablesToUpdate.put(settings.getVarLdapUserSearchFilter(), this.userSearchFilter);
    variablesToUpdate.put(settings.getVarLdapGroupSearchFilter(), this.groupSearchFilter);
    variablesToUpdate.put(settings.getVarKrbUserSearchFilter(), this.principalSearchFilter);
    variablesToUpdate.put(settings.getVarLdapGroupTarget(), this.groupTarget);
    variablesToUpdate.put(settings.getVarLdapDynGroupTarget(), this.dynGroupTarget);
    variablesToUpdate.put(settings.getVarLdapUserDN(), this.userDn);
    variablesToUpdate.put(settings.getVarLdapGroupDN(), this.groupDn);

    return variablesToUpdate;
  }
  
  @Override
  public String toString() {
    return "LdapConfigDTO{" +
      "accountStatus=" + accountStatus +
      ", groupMapping='" + groupMapping + '\'' +
      ", userId='" + userId + '\'' +
      ", userGivenName='" + userGivenName + '\'' +
      ", userSurname='" + userSurname + '\'' +
      ", userEmail='" + userEmail + '\'' +
      ", userSearchFilter='" + userSearchFilter + '\'' +
      ", groupSearchFilter='" + groupSearchFilter + '\'' +
      ", principalSearchFilter='" + principalSearchFilter + '\'' +
      ", groupTarget='" + groupTarget + '\'' +
      ", dynGroupTarget='" + dynGroupTarget + '\'' +
      ", userDn='" + userDn + '\'' +
      ", groupDn='" + groupDn + '\'' +
      ", groupsTarget='" + groupsTarget + '\'' +
      ", groupsSearchFilter='" + groupsSearchFilter + '\'' +
      ", groupMembersSearchFilter='" + groupMembersSearchFilter + '\'' +
      '}';
  }
}
