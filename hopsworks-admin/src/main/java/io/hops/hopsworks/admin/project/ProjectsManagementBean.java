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

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.pythonDeps.OpStatus;
import io.hops.hopsworks.common.exception.AppException;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean implements Serializable {
  private static final long serialVersionUID = -1L;
  private final Logger LOG = Logger.getLogger(ProjectsManagementBean.class.getName());

  @EJB
  private ProjectsManagementController projectsManagementController;

  public String action;

  private List<Project> filteredProjects;

  private List<Project> allProjects;

  private long hdfsQuota = -1;
  private float yarnQuota = -1;
  private float totalYarnQuota = -1;
  private long hdfsNsQuota = 1;
  private String hdfsQuotaString = "";
  private long hiveHdfsQuota = -1;
  private long hiveHdfsNsQuota = 1;
  private String hiveHdfsQuotaString = "";
  private List<String> paymentTypes = new ArrayList<>();
  private PaymentType paymentType;
  private Project toBeDeletedProject;
  private String projectNameForceCleanup;
  private final Map<String, Object> dialogOptions;

  public long getHiveHdfsQuota() { return hiveHdfsQuota; }

  public void setHiveHdfsQuota(long hiveHdfsQuota) {
    this.hiveHdfsQuota = hiveHdfsQuota;
  }

  public long getHiveHdfsNsQuota() { return hiveHdfsNsQuota; }

  public void setHiveHdfsNsQuota(long hiveHdfsNsQuota) {
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
  }

  public String getHiveHdfsQuotaString() { return hiveHdfsQuotaString; }

  public void setHiveHdfsQuotaString(String hiveHdfsQuotaString) {
    this.hiveHdfsQuotaString = hiveHdfsQuotaString;
  }

  public ProjectsManagementBean() {
    for (PaymentType paymentType : PaymentType.values()) {
      paymentTypes.add(paymentType.name());
    }
    
    dialogOptions = new HashMap<>(3);
    dialogOptions.put("resizable", false);
    dialogOptions.put("draggable", false);
    dialogOptions.put("modal", true);
  }

  public long getHdfsNsQuota() {
    return hdfsNsQuota;
  }

  public void setHdfsNsQuota(long hdfsNsQuota) {
    this.hdfsNsQuota = hdfsNsQuota;
  }

  public long getHdfsQuota() {
    return hdfsQuota;
  }

  public void setHdfsQuota(long hdfsQuota) {
    this.hdfsQuota = hdfsQuota;
  }

  public String getHdfsquotaString() {
    return hdfsQuotaString;
  }

  public void setHdfsquotaString(String hdfsquotaString) {
    this.hdfsQuotaString = hdfsquotaString;
  }

  public float getYarnQuota() {
    return yarnQuota;
  }

  public void setYarnQuota(float yarnQuota) {
    this.yarnQuota = yarnQuota;
  }

  public float getTotalYarnquota() {
    return totalYarnQuota;
  }

  public void setTotalYarnquota(float totalyarnquota) {
    this.totalYarnQuota = totalyarnquota;
  }

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public PaymentType getPaymentType(String projectName) {
    try{
      this.paymentType = projectsManagementController.getPaymentType(projectName);
    }catch(AppException ex){
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    return paymentType;
  }
  
  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
  }

  public List<String> getPaymentTypes() {
    return paymentTypes;
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
    if (projectname == null) {
      return "-1";
    }
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      if(quotas!=null){
        this.hdfsQuota = quotas.getDsquotaInMBs();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    DecimalFormat df = new DecimalFormat("##.##");
    if(this.hdfsQuota > 1048576){
      float tbSize = this.hdfsQuota / 1048576;
      this.hdfsQuotaString = df.format(tbSize) + "TB";
    } else if(this.hdfsQuota > 1024){
      float gbSize = this.hdfsQuota / 1024;
      this.hdfsQuotaString = df.format(gbSize) + "GB";
    } else {
      this.hdfsQuotaString = df.format(this.hdfsQuota) + "MB";
    }
    return this.hdfsQuotaString;

  }

  public long getHdfsNsQuota(String projectname) throws IOException {
    if (projectname == null) {
      return -1;
    }
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      if(quotas!=null && quotas.getNsquota()!=null){
        BigInteger sz = quotas.getNsquota();
        this.hdfsNsQuota = sz.longValue();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    return this.hdfsNsQuota;
  }

  public long getHdfsNsUsed(String projectname) throws IOException {
    long quota = -1l;
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHDFSQuotas(
              projectname);
      if(quotas!=null && quotas.getNscount()!=null){
        BigInteger sz = quotas.getNscount();
        quota = sz.longValue();
      }
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
      if(quotas!=null){
        quota = quotas.getDiskspaceInMBs();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    return quota;
  }

  public String getHiveHdfsQuota(String projectname) throws IOException {
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHiveHDFSQuotas(
              projectname);
      if (quotas != null) {
        this.hiveHdfsQuota = quotas.getDsquotaInMBs();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }

    DecimalFormat df = new DecimalFormat("##.##");
    if(this.hiveHdfsQuota > 1000000){
      float tbSize = this.hiveHdfsQuota / 1000000;
      this.hiveHdfsQuotaString = df.format(tbSize) + "TB";
    }
    else if(this.hiveHdfsQuota > 1000){
      float gbSize = this.hiveHdfsQuota / 1000;
      this.hiveHdfsQuotaString = df.format(gbSize) + "GB";
    }else {
      this.hiveHdfsQuotaString =df.format(this.hiveHdfsQuota) + "MB";
    }
    return this.hiveHdfsQuotaString;
  }

  public long getHiveHdfsNsQuota(String projectname) throws IOException {
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHiveHDFSQuotas(
              projectname);
      if (quotas != null) {
        BigInteger sz = quotas.getNsquota();
        this.hiveHdfsNsQuota = sz.longValue();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return this.hiveHdfsNsQuota;
  }

  public long getHiveHdfsNsUsed(String projectname) throws IOException {
    long quota = -1l;
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHiveHDFSQuotas(
              projectname);
      if (quotas != null) {
        BigInteger sz = quotas.getNscount();
        quota = sz.longValue();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return quota;
  }

  public long getHiveHdfsUsed(String projectname) throws IOException {
    long quota = -1l;
    try {
      HdfsInodeAttributes quotas = projectsManagementController.getHiveHDFSQuotas(
              projectname);
      if (quotas != null) {
        quota = quotas.getDiskspaceInMBs();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    return quota;
  }

  public float getYarnQuota(String projectName) throws IOException {
    try {
      YarnProjectsQuota quotas = projectsManagementController.getYarnQuotas(projectName);
      if(quotas!=null){
        this.yarnQuota = quotas.getQuotaRemaining();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    return this.yarnQuota;
  }

  public float getTotalYarnQuota(String projectName) throws IOException {
    try {
      YarnProjectsQuota quotas = projectsManagementController.getYarnQuotas(projectName);
      if(quotas!=null){
        this.totalYarnQuota = quotas.getTotal();
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE,
          null, ex);
    }
    return this.totalYarnQuota;
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
  
  public Project getToBeDeletedProject() {
    return toBeDeletedProject;
  }
  
  public void setToBeDeletedProject(Project toBeDeletedProject) {
    this.toBeDeletedProject = toBeDeletedProject;
  }
  
  public String getProjectNameForceCleanup() {
    return projectNameForceCleanup;
  }
  
  public void setProjectNameForceCleanup(String projectNameForceCleanup) {
    this.projectNameForceCleanup = projectNameForceCleanup;
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


  public void onRowEdit(RowEditEvent event) throws IOException {
    Project row = (Project) event.getObject();
    if (row.getArchived()) {
      projectsManagementController.disableProject(row.getName());
    } else {
      projectsManagementController.enableProject(row.getName());
    }
    projectsManagementController.changeYarnQuota(row.getName(), this.yarnQuota);

    if(hdfsQuotaString!=null){
      hdfsQuota = convertSpaceQuotaString(hdfsQuotaString);
      projectsManagementController.setHdfsSpaceQuota(row.getName(),this.hdfsQuota);
    }

    // If necessary set the quota for the Hive DB
    if (hiveHdfsQuotaString != null && !hiveHdfsQuotaString.equals("")) {
      hiveHdfsQuota = convertSpaceQuotaString(hiveHdfsQuotaString);
      if(hiveHdfsQuota >=0){
        projectsManagementController.setHiveHdfsQuota(row.getName(), this.hiveHdfsQuota);
      }
    }

    if (this.paymentType != null){
      projectsManagementController.setPaymentType(row.getName(), this.paymentType);
    }
  }

  private long convertSpaceQuotaString(String quotaString){
    long quota = -1l;
    if (quotaString.endsWith("TB")) {
      Long value = Long.parseLong(quotaString.substring(0, quotaString.length()-2));
      quota = value * 1048576;
    } else if (quotaString.endsWith("GB")) {
      Long value = Long.parseLong(quotaString.substring(0, quotaString.length() - 2));
      quota = value * 1024;
    } else if (quotaString.endsWith("MB")) {
      quota = Long.parseLong(quotaString.substring(0, quotaString.length()-2));
    } else {
      quota = Long.parseLong(quotaString);
    }

    return quota;
  }

  public void onRowCancel(RowEditEvent event) {
  }

  public String getCondaCommands(String projectname) {
    StringBuffer sb = new StringBuffer();
    try {
      List<OpStatus> ops = projectsManagementController.getCondaCommands(projectname);
      for (OpStatus op : ops) {
        sb.append(op.toString()).append(" -- ");
      }
    } catch (AppException ex) {
      Logger.getLogger(ProjectsManagementBean.class.getName()).log(Level.SEVERE, null, ex);
      sb.append("Error getting ops. Report a bug.");
    }
    return sb.toString();
  }
  
  public void deleteProject() {
    LOG.log(Level.INFO, "Deleting project: " + toBeDeletedProject);
    try {
      projectsManagementController.deleteProject(toBeDeletedProject, getSessionId());
      allProjects.remove(toBeDeletedProject);
      toBeDeletedProject = null;
      MessagesController.addInfoMessage("Project deleted!");
    } catch (AppException ex) {
      LOG.log(Level.SEVERE, "Failed to delete project " + toBeDeletedProject, ex);
      MessagesController.addErrorMessage("Deletion failed", "Failed deleting project "
          + toBeDeletedProject.getName());
    }
  }
  
  public void dialogForceCleanup() {
    RequestContext.getCurrentInstance().openDialog("projectForceRemoveDialog", dialogOptions, null);
  }
  
  public void selectedProjectForceCleanup() {
    RequestContext.getCurrentInstance().closeDialog(projectNameForceCleanup);
  }
  
  public void onProjectForceCleanupChosen(SelectEvent event) {
    String projectName = (String) event.getObject();
    LOG.log(Level.INFO, "Project force cleanup: " + projectName);
    String userEmail = FacesContext.getCurrentInstance().getExternalContext()
        .getUserPrincipal().getName();
    MessagesController.addInfoMessage("Force project deletion", "Check Inbox for status");
    projectsManagementController.forceDeleteProject(projectName, userEmail, getSessionId());
  }
  
  private String getSessionId() {
    Cookie[] cookies = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
        .getCookies();
    String sessionId = "";
    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase("SESSION")) {
        sessionId = cookie.getValue();
        break;
      }
    }
    return sessionId;
  }
}
