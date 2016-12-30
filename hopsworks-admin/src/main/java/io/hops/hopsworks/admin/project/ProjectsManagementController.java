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
package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentsHistoryFacade;
import io.hops.hopsworks.common.dao.project.management.ProjectsManagement;
import io.hops.hopsworks.common.dao.project.management.ProjectsManagementFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.List;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsManagementController {

  @EJB
  private ProjectsManagementFacade projectsManagementFacade;

  @EJB
  private ProjectController projectController;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private ProjectPaymentsHistoryFacade projectPaymentsHistoryFacade;

  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;

  @EJB
  private Settings settings;

  @EJB
  private InodeFacade inodes;
  
  /**
   *
   * @param projectname
   * @return size of quota for project subtree in HDFS in GBs
   * @throws IOException
   */
//  public long getHdfsSpaceQuota(String projectname) throws IOException {
//    try {
//      long numBytes = projectController.getHdfsSpaceQuotaInBytes(projectname);
//      return numBytes / (1024*1024*1024);
//    } catch (AppException ex) {
//      throw new IOException(ex);
//    }
//  }

  /**
   *
   * @return size of quota for project subtree in HDFS in GBs
   */
//  public long getHDFSUsedSpaceQuota(String projectname) throws IOException {
//    try {
//      return projectController.getHdfsSpaceQuotaInBytes(projectname);
//    } catch (AppException ex) {
//      throw new IOException(ex);
//    }
//  }
  
  /**
   * 
   * @param name
   * @return
   * @throws AppException 
   */
  public HdfsInodeAttributes getHDFSQuotas(String name) throws AppException {
    String pathname = Settings.getProjectPath(name);
    Inode inode = inodes.getInodeAtPath(pathname);
    return projectController.getHdfsQuotas(inode.getId());
  }  
  

  /**
   *
   * @param projectname
   * @param quotaInGBs size of quota for project subtree in HDFS in MBs
   * @throws IOException
   */
  public void setHdfsSpaceQuota(String projectname, long quotaInMBs,
          DistributedFileSystemOps dfso) throws IOException {
    projectController.setHdfsSpaceQuotaInMBs(projectname, quotaInMBs, dfso);
  }
  
  public void setHdfsNumFilesQuota(String projectname, long quotaInMBs,
          DistributedFileSystemOps dfso) throws IOException {
//    projectController.setHdfsSpaceQuotaInMBs(projectname, quotaInMBs, dfso);
  }
  

  public List<ProjectsManagement> getAllProjects() {
    return projectsManagementFacade.findAll();
  }

  public void disableProject(String projectname) {
    projectFacade.archiveProject(projectname);
  }

  public void enableProject(String projectname) {
    projectFacade.unarchiveProject(projectname);
  }

  public void changeYarnQuota(String projectname, int quota) {
    yarnProjectsQuotaFacade.changeYarnQuota(projectname, quota);
  }
}
