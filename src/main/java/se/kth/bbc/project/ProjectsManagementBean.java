/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kth.bbc.project;

import org.primefaces.event.RowEditEvent;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.util.List;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean {

  @EJB
  private ProjectsManagementController projectsManagementController;

  public String action;

  private List<ProjectsManagement> filteredProjects;

  private List<ProjectsManagement> allProjects;

  private String hdfsquota;

  public String getHdfsquota() {
    return hdfsquota;
  }

  public void setHdfsquota(String hdfsquota) {
    this.hdfsquota = hdfsquota;
  }

  public void setFilteredProjects(List<ProjectsManagement> filteredProjects) {
    this.filteredProjects = filteredProjects;
  }

  public List<ProjectsManagement> getFilteredProjects() {
    return filteredProjects;
  }

  public void setAllProjects(List<ProjectsManagement> allProjects) {
    this.allProjects = allProjects;
  }

  public List<ProjectsManagement> getAllProjects() {
    if (allProjects == null) {
      allProjects = projectsManagementController.getAllProjects();
    }
    return allProjects;
  }

  public int getHdfsQuota(String projectname) throws IOException {
    long quota = projectsManagementController.getHdfsSpaceQuota(projectname);
    this.hdfsquota = String.valueOf(quota);
    return (int) quota;
  }

  public int getHDFSUsedQuota(String projectname) throws IOException {
    return (int) projectsManagementController.getHDFSUsedSpaceQuota(projectname);
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }


  public void disableProject(String projectname) {
    projectsManagementController.disableProject(projectname);
  }

  public void enableProject(String projectname) {
    projectsManagementController.enableProject(projectname);
  }


  public void changeYarnQuota(String projectname, int quota) {
    projectsManagementController.changeYarnQuota(projectname, quota);
  }

  public void onRowEdit(RowEditEvent event)
      throws IOException {
    ProjectsManagement row = (ProjectsManagement) event.getObject();
    if (row.getDisabled()) {
      projectsManagementController.disableProject(row.getProjectname());
    } else {
      projectsManagementController.enableProject(row.getProjectname());
    }
    projectsManagementController.changeYarnQuota(row.getProjectname(), row
        .getYarnQuotaRemaining());
    projectsManagementController.setHdfsSpaceQuota(row.getProjectname(),
        Long.parseLong(hdfsquota));
  }

  public void onRowCancel(RowEditEvent event) {
  }

}
