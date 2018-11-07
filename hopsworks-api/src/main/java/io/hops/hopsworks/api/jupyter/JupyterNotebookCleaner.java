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
package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg.Session;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class JupyterNotebookCleaner {

  private static final  Logger LOGGER = Logger.getLogger(
      JupyterNotebookCleaner.class.getName());

  @EJB
  private LivyController livyService;
  @EJB
  private Settings settings;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUserscontroller;
  @EJB
  private ElasticController elasticController;
  @EJB
  private UserFacade usersFacade;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private CertificateMaterializer certificateMaterializer;

  public JupyterNotebookCleaner() {
  }

  // Run once per hour
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {
  
    //TODO(Theofilos): Remove check for ca module for 0.7.0 onwards
    try {
      String applicationName = InitialContext.doLookup("java:app/AppName");
      String moduleName = InitialContext.doLookup("java:module/ModuleName");
      if(applicationName.contains("hopsworks-ca") || moduleName.contains("hopsworks-ca")){
        return;
      }
    } catch (NamingException e) {
      LOGGER.log(Level.SEVERE, null, e);
    }
    LOGGER.log(Level.INFO, "Running JupyterNotebookCleaner.");
    // 1. Get all Running Jupyter Notebook Servers
    List<JupyterProject> servers = jupyterFacade.getAllNotebookServers();

    if (servers != null) {
      // 2. For each running Notebook Server, get the project_user and
      // then get the Livy sessions for that project_user
      for (JupyterProject jp : servers) {
        List<Session> sessions = livyService.getLivySessions(jp.getProjectId(), ProjectServiceEnum.JUPYTER);

        HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());

        Users user = usersFacade.findByUsername(hdfsUser.getUsername());
        JupyterSettings js = jupyterSettingsFacade.findByProjectUser(jp.getProjectId().getId(), user.getEmail());

        // If notebook hasn't been used in the last X hours, kill it.
        if (jp.getLastAccessed().before(
            new Date(System.currentTimeMillis() - (js.getShutdownLevel() * 60 * 60 * 1000)))) {

          livyService.deleteAllLivySessions(hdfsUser.getName(), ProjectServiceEnum.JUPYTER);

          int retries = 3;
          while(retries > 0 &&
              livyService.getLivySessionsForProjectUser(jp.getProjectId(), user,
                  ProjectServiceEnum.JUPYTER).size() > 0) {
            LOGGER.log(Level.SEVERE, "Failed previous attempt to delete livy sessions for project " +
                jp.getProjectId().getName() +
                " user " + hdfsUser + ", retrying...");
            livyService.deleteAllLivySessions(hdfsUser.getName(), ProjectServiceEnum.JUPYTER);

            try {
              Thread.sleep(1000);
            } catch(InterruptedException ie) {
              LOGGER.log(Level.SEVERE, "Interrupted while sleeping");
            }
            retries--;
          }

          String jupyterHomePath = null;

          try {
            jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser.getName(), jp);

            // stop the server, remove the user in this project's local dirs
            // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
            jupyterProcessFacade.killServerJupyterUser(hdfsUser.getName(), jupyterHomePath, jp.getPid(), jp.
                getPort());
          } catch(ServiceException ex) {
            Logger.getLogger(JupyterNotebookCleaner.class.getName()).log(Level.SEVERE, null, ex);
          }

          String[] project_user = hdfsUser.getName().split(HdfsUsersController.USER_NAME_DELIMITER);

          DistributedFileSystemOps dfso = dfsService.getDfsOps();
          try {
            String certificatesDir = Paths.get(jupyterHomePath, "certificates").toString();
            HopsUtils.cleanupCertificatesForUserCustomDir(project_user[1], jp.getProjectId()
              .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
            certificateMaterializer.removeCertificatesLocal(project_user[1], jp.getProjectId().getName());
          } finally {
            if (dfso != null) {
              dfsService.closeDfsClient(dfso);
            }
          }

          try {
            String experimentsIndex = jp.getProjectId().getName().toLowerCase()
                + "_" + Settings.ELASTIC_EXPERIMENTS_INDEX;
            // when jupyter is shutdown the experiment status should be updated accordingly as KILLED
            for (LivyMsg.Session session : sessions) {
              String sessionAppId = session.getAppId();

              String experiment = elasticController.findExperiment(experimentsIndex, sessionAppId);

              JSONObject json = new JSONObject(experiment);
              json = json.getJSONObject("hits");
              JSONArray hits = json.getJSONArray("hits");
              for(int i = 0; i < hits.length(); i++) {
                JSONObject obj = (JSONObject)hits.get(i);
                JSONObject source = obj.getJSONObject("_source");
                String status = source.getString("status");

                if(status.equalsIgnoreCase(JobState.RUNNING.name())) {
                  source.put("status", "KILLED");
                  elasticController.updateExperiment(experimentsIndex, obj.getString("_id"), source);
                }
              }
            }
          } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Exception while updating RUNNING status to KILLED on experiments", e);
          }

        }
      }

    } else {
      LOGGER.info("No Jupyter Notebook Servers running. Sleeping again.");
    }
  }

}
