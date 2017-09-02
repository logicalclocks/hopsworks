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
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.AppException;
import org.primefaces.event.RowEditEvent;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean {

  @EJB
  private ProjectsManagementController projectsManagementController;

  public String action;

  private List<Project> filteredProjects;

  private List<Project> allProjects;

  private long hdfsquota = -1;
  private float yarnquota = -1;
  private float totalyarnquota = -1;
  private long hdfsNsquota = 01;
  private String hdfsquotastring = "";

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

  public String getHdfsquotastring() {
    return hdfsquotastring;
  }

  public void setHdfsquotastring(String hdfsquotastring) {
    this.hdfsquotastring = hdfsquotastring;
  }

  public float getYarnquota() {
    return yarnquota;
  }

  public void setYarnquota(float yarnquota) {
    this.yarnquota = yarnquota;
  }
  
  public float getTotalYarnquota() {
    return totalyarnquota;
  }

  public void setTotalYarnquota(float totalyarnquota) {
    this.totalyarnquota = totalyarnquota;
  }
  
  public void setFilteredProjects(List<Project> filteredProjects) {
    this.filteredProjects = filteredProjects;
  }

  public List<Project> getFilteredProjects() {
    return filteredProjects;
  }

  public void setAllProjects(List<Project> allProjects) {
    this.allProjects = allProjects;
  }

  public List<Project> getAllProjects() {
    if (allProjects == null) {
      allProjects = projectsManagementController.getAllProjects();
    }
    return allProjects;
  }

  public String getHdfsQuota(String projectname) throws IOException {
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      this.hdfsquota = quotas.getDsquotaInMBs();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    DecimalFormat df = new DecimalFormat("##.##");
    if(this.hdfsquota > 1000000){
      float tbSize = this.hdfsquota / 1000000;
      return df.format(tbSize) + "TB";
    }
    else if(this.hdfsquota > 1000){
      float gbSize = this.hdfsquota / 1000;
      return df.format(gbSize) + "GB";
    }else {
      return df.format(this.hdfsquota) + "MB";
    }
  
  }

  public long getHdfsNsQuota(String projectname) throws IOException {
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      BigInteger sz = quotas.getNsquota();
      this.hdfsNsquota = sz.longValue();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return this.hdfsNsquota;
  }

  public long getHdfsNsUsed(String projectname) throws IOException {
    long quota = -1l;
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      BigInteger sz = quotas.getNscount();
      quota = sz.longValue();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return quota;
  }

  public long getHdfsUsed(String projectname) throws IOException {
    long quota = -1l;
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      quota = quotas.getDiskspaceInMBs();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return quota;
  }

  public float getYarnQuota(String projectName) throws IOException {
    try {
      YarnProjectsQuota quotas = projectsManagementController.getYarnQuotas(projectName);
      this.yarnquota = quotas.getQuotaRemaining();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return this.yarnquota;
  }
  
  public float getTotalYarnQuota(String projectName) throws IOException {
    try {
      YarnProjectsQuota quotas = projectsManagementController.getYarnQuotas(projectName);
      this.totalyarnquota = quotas.getTotal();
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return this.totalyarnquota;
  }
  
  public Date getLastPaymentDate(String projectName) {
    return projectsManagementController.getLastPaymentDate(projectName);
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

//  , DistributedFileSystemOps dfso
  public void onRowEdit(RowEditEvent event)
          throws IOException {
    Project row = (Project) event.getObject();
    if (row.getArchived()) {
      projectsManagementController.disableProject(row.getName());
    } else {
      projectsManagementController.enableProject(row.getName());
    }
    projectsManagementController.changeYarnQuota(row.getName(), this.yarnquota);
    if(this.hdfsquotastring!=null){
      convertHdfsQuotaString();
    }
    projectsManagementController.setHdfsSpaceQuota(row.getName(),
            this.hdfsquota);
  }

  private void convertHdfsQuotaString(){
    if(this.hdfsquotastring.endsWith("TB")){
      Long value = Long.parseLong(this.hdfsquotastring.substring(0, this.hdfsquotastring.length()-2));
      this.hdfsquota = value * 1000000;
    }else if(this.hdfsquotastring.endsWith("GB")){
      Long value = Long.parseLong(this.hdfsquotastring.substring(0, this.hdfsquotastring.length()-2));
      this.hdfsquota = value * 1000;
    }else if(this.hdfsquotastring.endsWith("MB")){
      Long value = Long.parseLong(this.hdfsquotastring.substring(0, this.hdfsquotastring.length()-2));
      this.hdfsquota = value;
    }else {
      Long value = Long.parseLong(this.hdfsquotastring);
      this.hdfsquota = value;
    }
  }
  
  public void onRowCancel(RowEditEvent event) {
  }

}
