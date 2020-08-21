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
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserStatus;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.persistence.entity.util.VariablesVisibility;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.DualListModel;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  private String groupsTarget;
  private String groupsSearchFilter;
  private String groupMembersSearchFilter;
  
  private List<String> allProjects;
  private DualListModel<String> ldapGroups;
  private String project;
  private String project_role;
  private List<String> project_roles;
  private String groupQuery;
  private RemoteUsersDTO remoteUsersDTO;
  private List <RemoteGroupProjectMapping> remoteGroupProjectMappings;
  private List<RemoteUser> remoteUsers;
  private boolean syncPanelCollapsed = true;
  
  @EJB
  private Settings settings;
  @EJB
  private ProjectController projectController;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private RemoteGroupProjectMappingFacade remoteGroupProjectMappingFacade;
  @EJB
  private LdapConfigHelper ldapConfigHelper;


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
    this.ldapEnabled = ldapConfigHelper.getLdapHelper().isLdapAvailable();
    this.groupsSearchFilter = settings.getLdapGroupsSearchFilter();
    this.groupsTarget = settings.getLdapGroupsTarget();
    this.groupMembersSearchFilter = settings.getLdapGroupMembersFilter();
    initMapping();
    getGroups();
    initRemoteUsers();
  }
  
  private void initMapping() {
    this.allProjects = projectController.findProjectNames();
    this.project_role = AllowedRoles.DATA_SCIENTIST;//default
    this.project_roles = new ArrayList<>();
    this.project_roles.add(AllowedRoles.DATA_OWNER);
    this.project_roles.add(AllowedRoles.DATA_SCIENTIST);
    this.remoteGroupProjectMappings = remoteGroupProjectMappingFacade.findAll();
  }
  
  private void initRemoteUsers() {
    remoteUsers = remoteUserFacade.findAll();
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
  
  public DualListModel<String> getLdapGroups() {
    return ldapGroups;
  }
  
  public void setLdapGroups(DualListModel<String> ldapGroups) {
    this.ldapGroups = ldapGroups;
  }
  
  public String getProject() {
    return project;
  }
  
  public void setProject(String project) {
    this.project = project;
  }
  
  public String getProject_role() {
    return project_role;
  }
  
  public void setProject_role(String project_role) {
    this.project_role = project_role;
  }
  
  public List<String> getProject_roles() {
    return project_roles;
  }
  
  public void setProject_roles(List<String> project_roles) {
    this.project_roles = project_roles;
  }
  
  public List<RemoteGroupProjectMapping> getRemoteGroupProjectMappings() {
    return remoteGroupProjectMappings;
  }
  
  public void setRemoteGroupProjectMappings(
    List<RemoteGroupProjectMapping> remoteGroupProjectMappings) {
    this.remoteGroupProjectMappings = remoteGroupProjectMappings;
  }
  
  public String getGroupQuery() {
    return groupQuery;
  }
  
  public void setGroupQuery(String groupQuery) {
    this.groupQuery = groupQuery;
  }
  
  public List<String> getAllProjects() {
    return allProjects;
  }
  
  public void setAllProjects(List<String> allProjects) {
    this.allProjects = allProjects;
  }
  
  public RemoteUsersDTO getRemoteUsersDTO() {
    return remoteUsersDTO;
  }
  
  public List<RemoteUser> getRemoteUsers() {
    return remoteUsers;
  }
  
  public void setRemoteUsers(List<RemoteUser> remoteUsers) {
    this.remoteUsers = remoteUsers;
  }
  
  public void setRemoteUsersDTO(RemoteUsersDTO remoteUsersDTO) {
    this.remoteUsersDTO = remoteUsersDTO;
  }
  
  public boolean isSyncPanelCollapsed() {
    return syncPanelCollapsed;
  }
  
  public void setSyncPanelCollapsed(boolean syncPanelCollapsed) {
    this.syncPanelCollapsed = syncPanelCollapsed;
  }
  
  public List<String> completeProject(String query) {
    if (allProjects == null || allProjects.isEmpty()) {
      return new ArrayList<>();
    }
    if (query == null || query.isEmpty()) {
      return allProjects;
    }
    String queryLowerCase = query.toLowerCase();
    return allProjects.stream().filter(t -> t.toLowerCase().startsWith(queryLowerCase)).collect(Collectors.toList());
  }
  
  public void getGroups() {
    if (this.groupQuery == null || this.groupQuery.isEmpty()) {
      this.groupQuery = "(objectCategory=group)";
    }
    List<String> ldapGroupsTarget = new ArrayList<>();
    List<String> ldapGroupsSource = ldapConfigHelper.getLdapHelper().getLDAPGroups(this.groupQuery);
    if (ldapGroupsSource == null) {
      ldapGroupsSource = new ArrayList<>();
    }
    this.setLdapGroups(new DualListModel<>(ldapGroupsSource, ldapGroupsTarget));
  }
  
  public void getMembers() {
    this.remoteUsersDTO = ldapConfigHelper.getLdapHelper().getMembers(this.ldapGroups.getTarget());
    RequestContext ctx = RequestContext.getCurrentInstance();
    FacesContext context = FacesContext.getCurrentInstance();
    boolean validationFailed = context.isValidationFailed();
    if(!validationFailed) {
      ctx.execute("PF('groupDialog').show();");
    }
  }
  
  public void submit() {
    Project project1;
    try {
      project1 = projectController.findProjectByName(this.project);
    } catch (ProjectException e) {
      String msg = e.getUsrMsg() != null ? e.getUsrMsg() : e.getErrorCode().getMessage();
      MessagesController.addErrorMessage("Project not found: ", msg);
      return;
    }
    for (String group : this.ldapGroups.getTarget()) {
      RemoteGroupProjectMapping lgpm = remoteGroupProjectMappingFacade.findByGroupAndProject(group, project1);
      if (lgpm != null) {
        MessagesController.addErrorMessage("Mapping: ", group + " to " + this.project + " already exists.");
        continue;
      }
      ldapConfigHelper.getLdapHelper()
        .addNewGroupProjectMapping(new RemoteGroupProjectMapping(group, project1, this.project_role));
    }
    this.remoteGroupProjectMappings = remoteGroupProjectMappingFacade.findAll();
    MessagesController
      .addInfoMessage("Mappings added: ", this.ldapGroups.getTarget().toString() + " to " + this.project);
  }
  
  public void groupPickListValidator(FacesContext ctx, UIComponent uc, Object object) throws ValidatorException {
    DualListModel<String> dualListModel;
    if (object instanceof DualListModel) {
      dualListModel = (DualListModel<String>) object;
      List<String> target = dualListModel.getTarget();
      if (target.isEmpty()) {
        ((UIInput) uc).setValid(false);
        FacesMessage msgError = new FacesMessage();
        msgError.setSeverity(FacesMessage.SEVERITY_ERROR);
        msgError.setSummary("Error");
        msgError.setDetail("You have to choose a group.");
        throw new ValidatorException(msgError);
      }
    }
  }
  
  public void projectPickValidator(FacesContext ctx, UIComponent uc, Object object) throws ValidatorException {
    String projectName;
    if (object != null && object instanceof String) {
      projectName = (String) object;
      if (!this.allProjects.contains(projectName)) {
        ((UIInput) uc).setValid(false);
        FacesMessage msgError = new FacesMessage();
        msgError.setSeverity(FacesMessage.SEVERITY_ERROR);
        msgError.setSummary("Error");
        msgError.setDetail("Project not found.");
        throw new ValidatorException(msgError);
      }
    } else {
      ((UIInput) uc).setValid(false);
      FacesMessage msgError = new FacesMessage();
      msgError.setSeverity(FacesMessage.SEVERITY_ERROR);
      msgError.setSummary("Error");
      msgError.setDetail("You have to choose a project.");
      throw new ValidatorException(msgError);
    }
  }
  
  public void pickListValueChanged() {
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
      List<Variables> variablesList = variablesToUpdate.entrySet().stream()
        .map(v -> new Variables(v.getKey(), v.getValue(), VariablesVisibility.ADMIN))
        .collect(Collectors.toList());
      settings.updateVariables(variablesList);
      MessagesController.addInfoMessage("Updated " + variablesToUpdate.size() + " variables.");
      init();
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void cancel() {
    MessagesController.addInfoMessage("Cancel ", "Mapping canceled.");
  }
  
  public void onRowEdit(RowEditEvent event) {
    try {
      RemoteGroupProjectMapping mapping = (RemoteGroupProjectMapping) event.getObject();
      remoteGroupProjectMappingFacade.update(mapping);
      this.remoteGroupProjectMappings = remoteGroupProjectMappingFacade.findAll();
      MessagesController.addInfoMessage("Mapping edited.");
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void onRowCancel(RowEditEvent event) {
    MessagesController.addInfoMessage("RowEditCancel ");
  }
  
  public void remove(RemoteGroupProjectMapping mapping) {
    try {
      remoteGroupProjectMappingFacade.remove(mapping);
      this.remoteGroupProjectMappings = remoteGroupProjectMappingFacade.findAll();
      MessagesController
        .addInfoMessage("Mapping: ", mapping.getRemoteGroup() + " to " + mapping.getProject().getName() + " removed.");
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void syncRemoteGroup(Users user) {
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    try {
      ldapConfigHelper.getRemoteGroupMappingHelper().syncMapping(remoteUser);
      initRemoteUsers();
      MessagesController.addInfoMessage("Synchronization completed with success.");
    } catch (UserException e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void syncAllRemoteGroups() {
    try {
      ldapConfigHelper.getRemoteGroupMappingHelper().syncMappingAsync();
      MessagesController.addInfoMessage("Synchronization started ...");
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void deleteRemoteUserFromAllProjects(Users user) {
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    try {
      ldapConfigHelper.getRemoteGroupMappingHelper().removeFromAllProjects(remoteUser);
      initRemoteUsers();
      MessagesController.addInfoMessage("Removing user from all projects completed with success.");
    } catch (UserException e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public void reactivateRemoteUser(Users user) {
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    try {
      remoteUser.setStatus(RemoteUserStatus.ACTIVE);
      remoteUserFacade.update(remoteUser);
      initRemoteUsers();
      MessagesController.addInfoMessage("Remote user reactivated.");
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage());
    }
  }
  
  public boolean checkForDeleted() {
    List<RemoteUser> remoteUsers = remoteUserFacade.findByStatus(RemoteUserStatus.DELETED);
    return remoteUsers != null && !remoteUsers.isEmpty() && this.isSyncPanelCollapsed();
  }
  
  public void openSyncPanel() {
    setSyncPanelCollapsed(false);
  }
}