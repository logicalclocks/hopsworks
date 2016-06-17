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

  private static final long GB = 1024*1024*1024;
  private static final long MB = 1024*1024;
  
  @EJB
  private ProjectsManagementController projectsManagementController;

  public String action;

  private List<ProjectsManagement> filteredProjects;

  private List<ProjectsManagement> allProjects;

  private long hdfsquota;
  private long hdfsNsquota;

  public long getHdfsNsquota() {
    return hdfsNsquota;
  }

  public void setHdfsNsquota(long hdfsNsquota) {
    this.hdfsNsquota = hdfsNsquota;
  }

  public long getHdfsquota() {
    return hdfsquota;
  }

  public void setHdfsquota(long hdfsquota) {
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
      quota /= MB;
//      this.hdfsquota = String.valueOf(quota);
      this.hdfsquota = quota;
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
      this.hdfsNsquota = quota;
//      this.hdfsNsquota = String.valueOf(quota);
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
    return quota;
  }

  public long getHdfsUsed(String projectname) throws IOException {
    HdfsInodeAttributes quotas;
    long quota = -1;
    try {
      quotas = projectsManagementController.getHDFSQuotas(projectname);
      BigInteger sz = quotas.getDiskspace();
      quota = sz.longValue();
      // convert from bytes to GB
      quota /= MB;
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    return quota;
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
    // convert quota to MB from bytes (1024^2)
    projectsManagementController.setHdfsSpaceQuota(row.getProjectname(),
        Long.parseLong(hdfsquota) * MB);
  }

  public void onRowCancel(RowEditEvent event) {
  }

}
