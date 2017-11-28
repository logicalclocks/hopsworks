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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentsHistoryFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.List;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.payment.LastPayment;
import io.hops.hopsworks.common.dao.project.payment.LastPaymentFacade;
import io.hops.hopsworks.common.dao.pythonDeps.OpStatus;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import java.util.Date;
import javax.ejb.Stateless;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsManagementController {

  @EJB
  private ProjectController projectController;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private PythonDepsFacade pythonDepsFacade;

  @EJB
  private ProjectServiceFacade projectServiceFacade;

  @EJB
  private HiveController hiveController;

  @EJB
  private ProjectPaymentsHistoryFacade projectPaymentsHistoryFacade;

  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;

  @EJB
  private Settings settings;

  @EJB
  private InodeFacade inodes;

  @EJB
  private DistributedFsService dfs;

  @EJB
  private LastPaymentFacade lastPaymentFacade;

  /**
   *
   * @param name
   * @return
   * @throws AppException
   */
  public HdfsInodeAttributes getHDFSQuotas(String name) throws AppException {
    String pathname = Settings.getProjectPath(name);
    Inode inode = inodes.getInodeAtPath(pathname);
    if(inode!=null){
      return projectController.getHdfsQuotas(inode.getId());
    }else{
      return null;
    }
  }

  /**
   * Get the Hdfs quota information for the HiveDB directory of the project
   * @param projectName
   * @return
   * @throws AppException
   */
  public HdfsInodeAttributes getHiveHDFSQuotas(String projectName) throws AppException {
    Project project = projectFacade.findByName(projectName);

    if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.HIVE)) {
      List<Dataset> datasets = (List<Dataset>)project.getDatasetCollection();
      for (Dataset ds : datasets) {
        if (ds.getType() == DatasetType.HIVEDB) {
          return projectController.getHdfsQuotas(ds.getInodeId());
        }
      }
    }

    return null;
  }

  /**
   *
   * @param projectname
   * @param quotaInMBs
   * size of quota for project subtree in HDFS in MBs
   * @throws IOException
   */
  public void setHdfsSpaceQuota(String projectname, long quotaInMBs) throws
      IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      projectController.setHdfsSpaceQuotaInMBs(projectname, quotaInMBs, dfso);
    } catch (Exception e) {
      // Do something
    } finally {
      if (dfso != null) {
        dfso.close();
      }

    }
  }

  /**
   * Set the HopsFs quota for the Hive Database
   * @param projectName
   * @param quotaInMBs: size of quota for project subtree in HDFS in MBs
   * @throws IOException
   */
  public void setHiveHdfsQuota(String projectName, long quotaInMBs) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      dfso.setHdfsSpaceQuotaInMBs(hiveController.getDbPath(projectName), quotaInMBs);
    } finally {
      if (dfso != null) {
        dfso.close();
      }

    }
  }

  /**
   *
   * @param projectname
   * @param paymentType
   * size of quota for project subtree in HDFS in MBs
   * @throws IOException
   */
  public void setPaymentType(String projectname, PaymentType paymentType) throws
      IOException {
    projectController.setPaymentType(projectname, paymentType);
  }
  
  public PaymentType getPaymentType(String projectName) throws AppException {
    return projectFacade.findByName(projectName).getPaymentType();
  }
  
  public List<Project> getAllProjects() {
    return projectFacade.findAll();
  }

  public void disableProject(String projectname) {
    projectFacade.archiveProject(projectname);
  }

  public void enableProject(String projectname) {
    projectFacade.unarchiveProject(projectname);
  }

  public void changeYarnQuota(String projectname, float quota) {
    yarnProjectsQuotaFacade.changeYarnQuota(projectname, quota);
  }

  public YarnProjectsQuota getYarnQuotas(String name) throws AppException {
    return yarnProjectsQuotaFacade.findByProjectName(name);
  }

  public Date getLastPaymentDate(String projectName) {
    LastPayment lastPayment = lastPaymentFacade.findByProjectName(projectName);
    if (lastPayment != null) {
      return lastPayment.getTransactionDate();
    } else {
      return null;
    }
  }
  
  public List<OpStatus> getCondaCommands(String projectName) throws AppException {
    Project proj = projectFacade.findByName(projectName);
    return pythonDepsFacade.opStatus(proj);
  }
  
  public void deleteProject(Project project, String sessionId) throws AppException {
    projectController.removeProject(project.getOwner().getEmail(), project.getId(), sessionId);
  }
}
