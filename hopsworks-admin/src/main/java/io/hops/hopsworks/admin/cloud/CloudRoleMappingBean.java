/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.admin.cloud;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.cloud.CloudRoleMappingController;
import io.hops.hopsworks.common.dao.cloud.CloudRoleMappingFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.cloud.ProjectRoles;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTException;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class CloudRoleMappingBean {
  private static final Logger LOGGER = Logger.getLogger(CloudRoleMappingBean.class.getName());
  private List<CloudRoleMappingView> cloudRoleMappings;
  
  private String projectName;
  private String cloudRole;
  private ProjectRoles projectRole;
  private boolean defaultRole;
  private boolean cloud;
  private List<Project> projects;
  
  @EJB
  private CloudRoleMappingFacade cloudRoleMappingFacade;
  @EJB
  private CloudRoleMappingController cloudRoleMappingController;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private Settings settings;
  
  @PostConstruct
  public void init() {
    getMappings();
    this.projects = projectFacade.findAll();
    this.cloud = !settings.getCloudType().equals(Settings.CLOUD_TYPES.NONE);
  }
  
  private void getMappings() {
    List<CloudRoleMappingView> roleMappings = new ArrayList<>();
    this.setCloudRoleMappings(roleMappings);
    List<CloudRoleMapping> mappings = cloudRoleMappingFacade.findAllOrderById();
    mappings.forEach(cloudRoleMapping -> roleMappings.add(new CloudRoleMappingView(cloudRoleMapping)));
  }
  
  private void getMappingsWithRefresh() {
    getMappings();
    RequestContext.getCurrentInstance().update("mappings_form:mappings");
  }
  
  public List<CloudRoleMappingView> getCloudRoleMappings() {
    return cloudRoleMappings;
  }
  
  public ProjectRoles[] getProjectRoles() {
    return ProjectRoles.values();
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public String getCloudRole() {
    return cloudRole;
  }
  
  public void setCloudRole(String cloudRole) {
    this.cloudRole = cloudRole;
  }
  
  public ProjectRoles getProjectRole() {
    return projectRole;
  }
  
  public void setProjectRole(ProjectRoles projectRole) {
    this.projectRole = projectRole;
  }
  
  public boolean isCloud() {
    return cloud;
  }
  
  public void setCloud(boolean cloud) {
    this.cloud = cloud;
  }
  
  public boolean isDefaultRole() {
    return defaultRole;
  }
  
  public void setDefaultRole(boolean defaultRole) {
    this.defaultRole = defaultRole;
  }
  
  public List<Project> getProjects() {
    return projects;
  }
  
  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }
  
  public void onRowEdit(RowEditEvent event) {
    CloudRoleMappingView cloudRoleMapping = (CloudRoleMappingView) event.getObject();
    try {
      cloudRoleMappingController.update(cloudRoleMapping.getId(), cloudRoleMapping.getCloudRole(),
        ProjectRoles.fromDisplayName(cloudRoleMapping.getProjectRole()), cloudRoleMapping.isDefaultRole());
      getMappingsWithRefresh();
      MessagesController.addInfoMessage("Mapping Edited.");
    } catch (CloudException e) {
      getMappingsWithRefresh();
      MessagesController.addErrorMessage("Failed to update mapping.", getRestMsg(e));
    }
  }
  
  public void onRowCancel(RowEditEvent event) {
    MessagesController.addInfoMessage("Mapping Edit Cancelled.");
  }
  
  public void setCloudRoleMappings(List<CloudRoleMappingView> cloudRoleMappings) {
    this.cloudRoleMappings = cloudRoleMappings;
  }
  
  public void remove(CloudRoleMappingView mapping) {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.find(mapping.getId());
    if (cloudRoleMapping != null) {
      cloudRoleMappingFacade.remove(cloudRoleMapping);
      getMappingsWithRefresh();
      MessagesController.addInfoMessage("Mapping removed.");
    }
  }
  
  public void onAddNew() {
    Project project;
    try {
      project = projectController.findProjectByName(this.projectName);
    } catch (ProjectException e) {
      MessagesController.addErrorMessage("Could not create mapping.", getRestMsg(e));
      return;
    }
    try {
      cloudRoleMappingController.saveMapping(project, this.cloudRole, this.projectRole, this.defaultRole);
      reset();
      MessagesController.addInfoMessage("Mapping created.");
    } catch (CloudException e) {
      MessagesController.addErrorMessage("Could not create mapping.", getRestMsg(e));
    }
  }
  
  private void reset() {
    getMappingsWithRefresh();
    this.setProjectName(null);
    this.setProjectRole(null);
    this.setCloudRole(null);
  }
  
  private String getRestMsg(RESTException re) {
    return re.getUsrMsg() != null? re.getUsrMsg() : re.getErrorCode().getMessage();
  }
}
