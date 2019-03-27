/*
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
 */

package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterController {

  private static final Logger LOGGER = Logger.getLogger(JupyterController.class.getName());

  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private Settings settings;
  @EJB
  private LivyController livyController;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessMgr;
  @EJB
  private ElasticController elasticController;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void convertIPythonNotebook(String hdfsUsername, String notebookPath, Project project, String pyPath)
      throws ServiceException {

    String conversionDir = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
    notebookPath = notebookPath.replace(" ", "\\ ");
    pyPath = pyPath.replace(" " , "\\ ");

    String prog = settings.getHopsworksDomainDir() + "/bin/convert-ipython-notebook.sh";
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(prog)
        .addCommand(notebookPath)
        .addCommand(hdfsUsername)
        .addCommand(settings.getAnacondaProjectDir(project))
        .addCommand(pyPath)
        .addCommand(conversionDir)
        .setWaitTimeout(60l, TimeUnit.SECONDS) //on a TLS VM the timeout needs to be greater than 20s
        .build();

    LOGGER.log(Level.FINE, processDescriptor.toString());
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited() || processResult.getExitCode() != 0) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR,  Level.SEVERE,
            "error code: " + processResult.getExitCode(), "Failed to convert " + notebookPath
          + "\nstderr: " + processResult.getStderr()
          + "\nstdout: " + processResult.getStdout());
      }
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.SEVERE, null, ex.getMessage(),
          ex);
    }
  }

  public void shutdown(Project project, String hdfsUser, Users user, String secret,
                              long pid, int port) throws ServiceException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway

    List<LivyMsg.Session> sessions = livyController.
      getLivySessionsForProjectUser(project, user);

    int retries = 3;
    while(retries > 0 &&
      livyController.getLivySessionsForProjectUser(project, user).size() > 0) {
      LOGGER.log(Level.SEVERE, "Failed previous attempt to delete livy sessions for project " + project.getName() +
        " user " + hdfsUser + ", retrying...");
      livyController.deleteAllLivySessions(hdfsUser);

      try {
        Thread.sleep(200);
      } catch(InterruptedException ie) {
        LOGGER.log(Level.SEVERE, "Interrupted while sleeping");
      }
      retries--;
    }
    String jupyterHomePath = jupyterProcessMgr.getJupyterHome(hdfsUser, project, secret);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    jupyterProcessMgr.killServerJupyterUser(hdfsUser, jupyterHomePath, pid, port);

    String[] project_user = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER);
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      String certificatesDir = Paths.get(jupyterHomePath, "certificates").toString();
      HopsUtils.cleanupCertificatesForUserCustomDir(project_user[1], project
        .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
      certificateMaterializer.removeCertificatesLocal(project_user[1], project.getName());
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }
    for(LivyMsg.Session session: sessions) {
      updateRunningExperimentAsKilled(project, session);
    }
    livyController.deleteAllLivySessions(hdfsUser);
  }

  public void stopSession(Project project, Users user, String appId) {

    List<LivyMsg.Session> sessions = livyController.getLivySessionsForProjectUser(project, user);

    for(LivyMsg.Session session: sessions) {
      if(session.getAppId().equalsIgnoreCase(appId)) {
        livyController.deleteLivySession(session.getId());
        updateRunningExperimentAsKilled(project, session);
        break;
      }
    }
  }

  private void updateRunningExperimentAsKilled(Project project, LivyMsg.Session session) {
    try {
      String experimentsIndex = project.getName().toLowerCase()
        + "_" + Settings.ELASTIC_EXPERIMENTS_INDEX;
      // when jupyter is shutdown the experiment status should be updated accordingly as KILLED

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
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception while updating RUNNING status to KILLED on experiments", e);
    }
  }

  public void removeJupyter(Project project) throws ServiceException {

    for(JupyterProject jp: project.getJupyterProjectCollection()) {
      HdfsUsers hdfsUser = hdfsUsersFacade.findById(jp.getHdfsUserId());
      String username = hdfsUsersController.getUserName(hdfsUser.getName());
      Users user = userFacade.findByUsername(username);
      shutdown(project, hdfsUser.getName(), user,
        jp.getSecret(), jp.getPid(), jp.getPort());
    }
    jupyterProcessMgr.projectCleanup(project);
  }

  public JupyterSettings updateExpirationDate(Project project, Users user, JupyterSettings jupyterSettings) {

    //Save the current shutdown level
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(project.getId(), user.getEmail());
    js.setShutdownLevel(jupyterSettings.getShutdownLevel());
    jupyterSettingsFacade.update(js);

    //Increase hours on expirationDate
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUser);
    Date expirationDate = jupyterProject.getExpires();
    Calendar cal = Calendar.getInstance();
    cal.setTime(expirationDate);
    cal.add(Calendar.HOUR_OF_DAY, jupyterSettings.getShutdownLevel());
    expirationDate = cal.getTime();
    jupyterProject.setExpires(expirationDate);
    jupyterFacade.update(jupyterProject);

    return jupyterSettings;
  }
}
