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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterManager;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
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
  private DistributedFsService dfs;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private Settings settings;
  @EJB
  private LivyController livyController;
  @EJB
  private JupyterFacade jupyterFacade;
  @Inject
  private JupyterManager jupyterManager;
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
  @EJB
  private JupyterJWTManager jupyterJWTManager;
  @Inject
  private JupyterNbVCSController jupyterNbVCSController;

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String convertIPythonNotebook(String hdfsUsername, String notebookPath, Project project, String pyPath,
                                     NotebookConversion notebookConversion)  throws ServiceException {

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
        .addCommand(notebookConversion.name())
        .setWaitTimeout(60l, TimeUnit.SECONDS) //on a TLS VM the timeout needs to be greater than 20s
        .redirectErrorStream(true)
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
      String stdOut = processResult.getStdout();
      if(!Strings.isNullOrEmpty(stdOut) && notebookConversion.equals(NotebookConversion.HTML)) {
        StringBuilder renderedNotebookSB = new StringBuilder(stdOut);
        int startIndex = renderedNotebookSB.indexOf("<html>");
        int stopIndex = renderedNotebookSB.length();
        return renderedNotebookSB.substring(startIndex, stopIndex);
      }
      return null;
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.SEVERE, null, ex.getMessage(),
          ex);
    }
  }

  public void shutdownOrphan(Long pid, Integer port) throws ServiceException {
    try {
      jupyterManager.stopOrphanedJupyterServer(pid, port);
    } finally {
      jupyterJWTManager.cleanJWT(pid, port);
    }
  }

  public void shutdownQuietly(Project project, String hdfsUser, Users user, String secret,
      long pid, int port) {
    try {
      shutdown(project, hdfsUser, user, secret, pid, port, true);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Encountered exception while cleaning up", e);
    }
    jupyterJWTManager.cleanJWT(pid, port);
  }
  
  public void shutdown(Project project, String hdfsUser, Users user, String secret,
      long pid, int port) throws ServiceException {
    shutdown(project, hdfsUser, user, secret, pid, port, false);
  }
  
  public void shutdown(Project project, String hdfsUser, Users user, String secret,
                              long pid, int port, boolean quiet) throws ServiceException {
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
    String jupyterHomePath = jupyterManager.getJupyterHome(settings, hdfsUser, project, secret);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    try {
      JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUser);
      JupyterSettings jupyterSettings = jupyterSettingsFacade.findByProjectUser(project.getId(), user.getEmail());
      // If we fail to start Jupyter server, then we call this shutdown to clean up
      // Do some sanity check before using jupyter settings
      if (jupyterProject != null && jupyterSettings != null) {
        if (jupyterSettings.isGitBackend() && jupyterSettings.getGitConfig().getShutdownAutoPush()) {
          try {
            jupyterNbVCSController.push(jupyterProject, jupyterSettings, user);
          } catch (ServiceException ex) {
            if (!quiet) {
              throw ex;
            }
            LOGGER.log(Level.WARNING, "Could not push Git repository, shutting down Jupyter nevertheless", ex);
          }
        }
      }
      jupyterManager.stopJupyterServer(project, user, hdfsUser, jupyterHomePath, pid, port);
    } finally {
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
      FileUtils.deleteQuietly(new File(jupyterHomePath));
      jupyterJWTManager.cleanJWT(pid, port);
      livyController.deleteAllLivySessions(hdfsUser);
    }
  }

  public void stopSession(Project project, Users user, String appId) {

    List<LivyMsg.Session> sessions = livyController.getLivySessionsForProjectUser(project, user);

    for(LivyMsg.Session session: sessions) {
      if(session.getAppId().equalsIgnoreCase(appId)) {
        livyController.deleteLivySession(session.getId());
        break;
      }
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
    jupyterManager.projectCleanup(project);
  }

  public void updateExpirationDate(Project project, Users user, JupyterSettings jupyterSettings) {

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
  }

  public void versionProgram(Project project, Users user, String sessionKernelId, Path outputPath)
      throws ServiceException {

    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);

    String relativeNotebookPath = null;
    try {
      JSONArray sessionsArray = new JSONArray(ClientBuilder.newClient()
          .target(settings.getRestEndpoint() + "/hopsworks-api/jupyter/" + jp.getPort() + "/api/sessions?token="
              + jp.getToken())
          .request()
          .method("GET")
          .readEntity(String.class));

      boolean foundKernel = false;
      for (int i = 0; i < sessionsArray.length(); i++) {
        JSONObject session = (JSONObject) sessionsArray.get(i);
        JSONObject kernel = (JSONObject) session.get("kernel");
        String kernelId = kernel.getString("id");
        if (kernelId.equals(sessionKernelId)) {
          relativeNotebookPath = session.getString("path");
          foundKernel = true;
        }
      }

      if(!foundKernel) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_NOTEBOOK_VERSIONING_FAILED,
            Level.FINE, "failed to find kernel " + sessionKernelId);
      }

      //Get the content of the notebook should work in both spark and python kernels irregardless of contents manager
      if (!Strings.isNullOrEmpty(relativeNotebookPath)) {
        JSONObject notebookContents = new JSONObject(ClientBuilder.newClient()
            .target(settings.getRestEndpoint() + "/hopsworks-api/jupyter/" + jp.getPort() +
                "/api/contents/" + relativeNotebookPath + "?content=1&token=" + jp.getToken())
            .request()
            .method("GET")
            .readEntity(String.class));
        JSONObject notebookJSON = (JSONObject)notebookContents.get("content");
        DistributedFileSystemOps udfso = null;
        try {
          String username = hdfsUsersController.getHdfsUserName(project, user);
          udfso = dfs.getDfsOps(username);
          udfso.create(outputPath, notebookJSON.toString());
        } catch(IOException e) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_NOTEBOOK_VERSIONING_FAILED,
              Level.FINE, "failed to save notebook content", e.getMessage(), e);
        } finally {
          if (udfso != null) {
            dfs.closeDfsClient(udfso);
          }
        }
      }
    } catch(Exception e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_NOTEBOOK_VERSIONING_FAILED,
          Level.FINE, "failed to version notebook", e.getMessage(), e);
    }
  }

  public enum NotebookConversion {
    PY,
    HTML
  }
}
