/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeam;

/**
 *
 * @author Ermias
 */
@XmlRootElement
public class ProjectDTO {

  private Integer projectId;
  private String projectName;
  private String owner;
  private String description;
  private Date retentionPeriod;
  private Date created;
  private String ethicalStatus;
  private boolean archived;
  private List<String> services;
  private List<ProjectTeam> projectTeam;

  public ProjectDTO() {
  }

  public ProjectDTO(Integer projectId, String projectName, String owner) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.owner = owner;
  }

  public ProjectDTO(Project project, List<String> services,
          List<ProjectTeam> projectTeam) {
    this.projectId = project.getId();
    this.projectName = project.getName();
    this.owner = project.getOwner().getEmail();
    this.retentionPeriod = project.getRetentionPeriod();
    this.created = project.getCreated();
    this.ethicalStatus = project.getEthicalStatus();
    this.archived = project.getArchived();
    this.description = project.getDescription();
    this.services = services;
    this.projectTeam = projectTeam;
  }

  public ProjectDTO(Integer projectId, String projectName, String owner,
          Date retentionPeriod, Date created,
          String ethicalStatus, boolean archived, String description,
          List<String> services,
          List<ProjectTeam> projectTeam) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.owner = owner;
    this.retentionPeriod = retentionPeriod;
    this.created = created;
    this.ethicalStatus = ethicalStatus;
    this.archived = archived;
    this.description = description;
    this.services = services;
    this.projectTeam = projectTeam;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Date getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getEthicalStatus() {
    return ethicalStatus;
  }

  public void setEthicalStatus(String ethicalStatus) {
    this.ethicalStatus = ethicalStatus;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services;
  }

  public List<ProjectTeam> getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(List<ProjectTeam> projectTeams) {
    this.projectTeam = projectTeams;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ProjectDTO{" + "projectName=" + projectName + ", owner=" + owner
            + ", description=" + description + ", retentionPeriod="
            + retentionPeriod + ", created=" + created + ", ethicalStatus="
            + ethicalStatus + ", archived=" + archived + ", services="
            + services + ", projectTeam=" + projectTeam + '}';
  }

}
