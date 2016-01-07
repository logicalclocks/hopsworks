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

import org.apache.hadoop.fs.Path;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.List;

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

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public long getHdfsQuota(String projectname) throws IOException {
    return projectController.getQuota(projectname);
  }

  public long getHDFSUsedQuota(String projectname) throws IOException {
    return projectController.getUsedQuota(projectname);
  }

  public void setHdfsQuota(String projectname, long quota) throws IOException {
    projectController.setQuota(projectname, quota);
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
