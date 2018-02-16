/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.project;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;

@XmlRootElement
public class ProjectDTO {

  private Integer projectId;
  private String projectName;
  private String owner;
  private String description;
  private Date retentionPeriod;
  private Date created;
  private boolean archived;
  private List<String> services;
  private List<ProjectTeam> projectTeam;
  private List<InodeView> datasets;
  private Integer inodeid;
  private QuotasDTO quotas;

  public ProjectDTO() {
  }

  public ProjectDTO(Integer projectId, String projectName, String owner) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.owner = owner;
  }

  public ProjectDTO(Project project, Integer inodeid, List<String> services,
      List<ProjectTeam> projectTeam, QuotasDTO quotas) {
    this.projectId = project.getId();
    this.inodeid = inodeid;
    this.projectName = project.getName();
    this.owner = project.getOwner().getEmail();
    this.retentionPeriod = project.getRetentionPeriod();
    this.created = project.getCreated();
    this.archived = project.getArchived();
    this.description = project.getDescription();
    this.services = services;
    this.projectTeam = projectTeam;
    this.quotas = quotas;
  }

  public ProjectDTO(Project project, Integer inodeid, List<String> services,
      List<ProjectTeam> projectTeam, List<InodeView> datasets) {
    this.projectId = project.getId();
    //the inodeid of the current project comes from hops database
    this.inodeid = inodeid;
    this.projectName = project.getName();
    this.owner = project.getOwner().getEmail();
    this.retentionPeriod = project.getRetentionPeriod();
    this.created = project.getCreated();
    this.archived = project.getArchived();
    this.description = project.getDescription();
    this.services = services;
    this.projectTeam = projectTeam;
    this.datasets = datasets;
  }

  public ProjectDTO(Integer projectId, String projectName, String owner,
      Date retentionPeriod, Date created, boolean archived, String description,
      List<String> services, List<ProjectTeam> projectTeam) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.owner = owner;
    this.retentionPeriod = retentionPeriod;
    this.created = created;
    this.archived = archived;
    this.description = description;
    this.services = services;
    this.projectTeam = projectTeam;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public Integer getInodeid() {
    return this.inodeid;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public void setInodeid(Integer inodeid) {
    this.inodeid = inodeid;
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

  public List<InodeView> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<InodeView> datasets) {
    this.datasets = datasets;
  }

  public QuotasDTO getQuotas() {
    return quotas;
  }

  public void setQuotas(QuotasDTO quotas) {
    this.quotas = quotas;
  }

  @Override
  public String toString() {
    return "ProjectDTO{" + "projectName=" + projectName + ", owner=" + owner
        + ", description=" + description + ", retentionPeriod="
        + retentionPeriod + ", created=" + created + ", archived="
        + archived + ", services="
        + services + ", projectTeam=" + projectTeam + '}';
  }

}
