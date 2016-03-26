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
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package se.kth.bbc.project;

import org.primefaces.event.RowEditEvent;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.hopsworks.hdfs.fileoperations.HdfsInodeAttributes;
import se.kth.hopsworks.rest.AppException;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean {

  @EJB
  private ProjectsManagementController projectsManagementController;

  public String action;

  private List<ProjectsManagement> filteredProjects;

  private List<ProjectsManagement> allProjects;

  private String hdfsquota;
  private String hdfsNsquota;

  public String getHdfsNsquota() {
    return hdfsNsquota;
  }

  public void setHdfsNsquota(String hdfsNsquota) {
    this.hdfsNsquota = hdfsNsquota;
  }

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

  public long getHdfsQuota(String projectname) throws IOException {
    HdfsInodeAttributes quotas;
    long quota = -1;
    try {
      quotas = projectsManagementController.getHDFSQuotas(projectname);
      BigInteger sz = quotas.getDsquota();
      quota = sz.longValue();
      this.hdfsquota = String.valueOf(quota);
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    return quota;
  }

  public long getHdfsNsQuota(String projectname) throws IOException {
    HdfsInodeAttributes quotas;
    long quota = 1000000;
    try {
      quotas = projectsManagementController.getHDFSQuotas(projectname);
      BigInteger sz = quotas.getNsquota();
      quota = sz.longValue();
      this.hdfsNsquota = String.valueOf(quota);
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    return quota;
  }

  public long getHdfsNsUsed(String projectname) throws IOException {
    HdfsInodeAttributes quotas;
    long quota = -1;
    try {
      quotas = projectsManagementController.getHDFSQuotas(projectname);
      BigInteger sz = quotas.getNscount();
      quota = sz.longValue();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
    }
//    this.hdfsquota = String.valueOf(quota);
//    return (int) quota;
    return quota;
  }

  public long getHdfsUsed(String projectname) throws IOException {
    HdfsInodeAttributes quotas;
    long quota = -1;
    try {
      quotas = projectsManagementController.getHDFSQuotas(projectname);
      BigInteger sz = quotas.getDiskspace();
      quota = sz.longValue();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    return quota;

//    return (int) projectsManagementController.getHDFSUsedSpaceQuota(projectname);
  }
//  public HdfsInodeAttributes getHDFSQuotas(String projectname) throws AppException {
//    return (int) projectsManagementController.getHDFSUsedSpaceQuota(projectname);
//    return projectsManagementController.getHDFSQuotas(projectname);
//  }

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
