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
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.users.UserView;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.activity.Activity;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ProjectView {
  
  private List<MemberView> team;
  private List<String> services;
  private Integer projectId;
  private String description;
  private Date created;
  private boolean archived;
  private String name;
  private UserView owner;
  private List<ActivityView> activity;
  private List<DatasetView> datasets;
  private Date retentionPeriod;
  
  public ProjectView(){}
  
  public ProjectView(Project project){
    this.projectId = project.getId();
    this.name = project.getName();
    this.created = project.getCreated();
    this.description = project.getDescription();
    this.owner = new UserView(project.getOwner());
    this.activity = new ArrayList<>();
    for (Activity activityElement : project.getActivityCollection()) {
      activity.add(new ActivityView(activityElement));
    }
    this.datasets = new ArrayList<>();
    for (Dataset dataset : project.getDatasetCollection()) {
      datasets.add(new DatasetView(dataset));
    }
    this.team = new ArrayList<>();
    for (ProjectTeam projectTeam : project.getProjectTeamCollection()) {
      team.add(new MemberView(projectTeam));
    }
    this.services = new ArrayList<>();
    for (ProjectServices projectServices : project.getProjectServicesCollection()) {
      services.add(projectServices.getProjectServicesPK().getService().toString());
    }
    this.archived = project.getArchived();
    retentionPeriod = project.getRetentionPeriod();
  }
  
  public List<MemberView> getTeam() {
    return team;
  }
  
  public void setTeam(List<MemberView> team) {
    this.team = team;
  }
  
  public List<String> getServices() {
    return services;
  }
  
  public void setServices(List<String> services) {
    this.services = services;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
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
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public UserView getOwner() {
    return owner;
  }
  
  public void setOwner(UserView owner) {
    this.owner = owner;
  }
  
  public List<ActivityView> getActivity() {
    return activity;
  }
  
  public void setActivity(List<ActivityView> activity) {
    this.activity = activity;
  }
  
  public List<DatasetView> getDatasets() {
    return datasets;
  }
  
  public void setDatasets(List<DatasetView> datasets) {
    this.datasets = datasets;
  }
  
  public Date getRetentionPeriod() {
    return retentionPeriod;
  }
  
  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }
}
