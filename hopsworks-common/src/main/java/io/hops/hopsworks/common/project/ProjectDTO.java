/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.project;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.persistence.entity.project.CreationStatus;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.hdfs.inode.InodeView;

@XmlRootElement
public class ProjectDTO {

  private Integer projectId;
  private String projectName;
  private String owner;
  private String description;
  private String dockerImage;
  private Date created;
  private List<String> services;
  private List<ProjectTeam> projectTeam;
  private List<InodeView> datasets;
  private Long inodeid;
  private Quotas quotas;
  private boolean isPreinstalledDockerImage;
  private boolean isOldDockerImage;
  private CreationStatus creationStatus;
  private String featureStoreTopic;

  public ProjectDTO() {
  }

  public ProjectDTO(Project project, Long inodeid, List<String> services,
                    List<ProjectTeam> projectTeam, Quotas quotas,
                    boolean isPreinstalledDockerImage, boolean isOldDockerImage) {
    this.projectId = project.getId();
    this.inodeid = inodeid;
    this.projectName = project.getName();
    this.owner = project.getOwner().getEmail();
    this.created = project.getCreated();
    this.description = project.getDescription();
    this.dockerImage = project.getDockerImage();
    this.services = services;
    this.projectTeam = projectTeam;
    this.quotas = quotas;
    this.isPreinstalledDockerImage = isPreinstalledDockerImage;
    this.isOldDockerImage = isOldDockerImage;
    this.creationStatus = project.getCreationStatus();
    this.featureStoreTopic = project.getTopicName();
  }

  public ProjectDTO(Project project, Long inodeid, List<String> services,
      List<ProjectTeam> projectTeam, List<InodeView> datasets, boolean isPreinstalledDockerImage,
                    boolean isOldDockerImage) {
    this.projectId = project.getId();
    //the inodeid of the current project comes from hops database
    this.inodeid = inodeid;
    this.projectName = project.getName();
    this.owner = project.getOwner().getEmail();
    this.created = project.getCreated();
    this.description = project.getDescription();
    this.dockerImage = project.getDockerImage();
    this.services = services;
    this.projectTeam = projectTeam;
    this.datasets = datasets;
    this.isPreinstalledDockerImage = isPreinstalledDockerImage;
    this.isOldDockerImage = isOldDockerImage;
    this.creationStatus = project.getCreationStatus();
    this.featureStoreTopic = project.getTopicName();
  }

  public Integer getProjectId() {
    return projectId;
  }

  public Long getInodeid() {
    return this.inodeid;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public void setInodeid(Long inodeid) {
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

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
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

  public String getDockerImage() {
    return dockerImage;
  }

  public void setDockerImage(String dockerImage) {
    this.dockerImage = dockerImage;
  }

  public List<InodeView> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<InodeView> datasets) {
    this.datasets = datasets;
  }

  public Quotas getQuotas() {
    return quotas;
  }

  public void setQuotas(Quotas quotas) {
    this.quotas = quotas;
  }

  public boolean getIsPreinstalledDockerImage() {
    return this.isPreinstalledDockerImage;
  }

  public void setIsPreinstalledDockerImage(boolean isPreinstalledDockerImage) {
    this.isPreinstalledDockerImage = isPreinstalledDockerImage;
  }

  public boolean getIsOldDockerImage() {
    return this.isOldDockerImage;
  }

  public void setIsOldDockerImage(boolean isOldDockerImage) {
    this.isOldDockerImage = isOldDockerImage;
  }
  
  public CreationStatus getCreationStatus() {
    return creationStatus;
  }
  
  public void setCreationStatus(CreationStatus creationStatus) {
    this.creationStatus = creationStatus;
  }

  public String getFeatureStoreTopic() {
    return featureStoreTopic;
  }

  public void setFeatureStoreTopic(String topicName) {
    this.featureStoreTopic = topicName;
  }
  
  @Override
  public String toString() {
    return "ProjectDTO{" + "projectName=" + projectName + ", owner=" + owner
        + ", description=" + description + ", created=" + created + ", services="
        + services + ", projectTeam=" + projectTeam +
        ", isPreinstalledDockerImage=" + isPreinstalledDockerImage + '}';
  }
}
