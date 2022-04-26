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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
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
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private DistributedFsService dfsService;
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
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private XAttrsController xAttrsController;

  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JaxbAnnotationModule());
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String convertIPythonNotebook(String hdfsUsername, String notebookPath, Project project, String pyPath,
                                     NotebookConversion notebookConversion)  throws ServiceException {

    File baseDir = new File(settings.getStagingDir() + settings.CONVERSION_DIR);
    if(!baseDir.exists()){
      baseDir.mkdir();
    }
    File conversionDir = new File(baseDir, DigestUtils.
        sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt())));
    conversionDir.mkdir();

    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUsername);
    try{
      String prog = settings.getSudoersDir() + "/convert-ipython-notebook.sh";
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand(notebookPath)
          .addCommand(hdfsUsername)
          .addCommand(settings.getAnacondaProjectDir())
          .addCommand(pyPath)
          .addCommand(conversionDir.getAbsolutePath())
          .addCommand(notebookConversion.name())
          .addCommand(projectUtils.getFullDockerImageName(project, true))
          .setWaitTimeout(60l, TimeUnit.SECONDS) //on a TLS VM the timeout needs to be greater than 20s
          .redirectErrorStream(true)
          .build();

      LOGGER.log(Level.FINE, processDescriptor.toString());
      certificateMaterializer.
          materializeCertificatesLocalCustomDir(user.getUsername(), project.getName(), conversionDir.getAbsolutePath());
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited() || processResult.getExitCode() != 0) {
        LOGGER.log(Level.WARNING, "error code: " + processResult.getExitCode(), "Failed to convert "
            + notebookPath + "\nstderr: " + processResult.getStderr() + "\nstdout: " + processResult.getStdout());
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
    } catch (IOException | ServiceDiscoveryException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.SEVERE, null, ex.getMessage(),
          ex);
    } finally {
      certificateMaterializer.removeCertificatesLocalCustomDir(user.getUsername(), project.getName(), conversionDir.
          getAbsolutePath());
    }
  }

  public void shutdownOrphan(String cid, Integer port) throws ServiceException {
    try {
      jupyterManager.stopOrphanedJupyterServer(cid, port);
    } finally {
      jupyterJWTManager.cleanJWT(cid, port);
    }
  }

  public void shutdownQuietly(Project project, String hdfsUser, Users user, String secret,
      String cid, int port) {
    try {
      shutdown(project, hdfsUser, user, secret, cid, port, true);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Encountered exception while cleaning up", e);
    }
    jupyterJWTManager.cleanJWT(cid, port);
  }
  
  public void shutdown(Project project, String hdfsUser, Users user, String secret,
      String cid, int port) throws ServiceException {
    shutdown(project, hdfsUser, user, secret, cid, port, false);
  }
  
  public void shutdown(Project project, String hdfsUser, Users user, String secret,
                              String cid, int port, boolean quiet) throws ServiceException {
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
    String jupyterHomePath = jupyterManager.getJupyterHome(hdfsUser, project, secret);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    try {
      JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUser);
      JupyterSettings jupyterSettings = jupyterSettingsFacade.findByProjectUser(project, user.getEmail());
      // If we fail to start Jupyter server, then we call this shutdown to clean up
      // Do some sanity check before using jupyter settings
      jupyterManager.stopJupyterServer(project, user, hdfsUser, jupyterHomePath, cid, port);
    } finally {
      String[] project_user = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER);
      DistributedFileSystemOps dfso = dfsService.getDfsOps();
      try {
        String certificatesDir = Paths.get(jupyterHomePath, "certificates").toString();
        HopsUtils.cleanupCertificatesForUserCustomDir(project_user[1], project
            .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }
      FileUtils.deleteQuietly(new File(jupyterHomePath));
      jupyterJWTManager.cleanJWT(cid, port);
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
        jp.getSecret(), jp.getCid(), jp.getPort());
    }
    jupyterManager.projectCleanup(project);
  }

  public void updateExpirationDate(Project project, Users user, JupyterSettings jupyterSettings) {
    //Increase hours on expirationDate
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUser);
    if (jupyterProject != null) {
      Date expirationDate = jupyterProject.getExpires();
      Calendar cal = Calendar.getInstance();
      cal.setTime(expirationDate);
      cal.add(Calendar.HOUR_OF_DAY, jupyterSettings.getShutdownLevel());
      expirationDate = cal.getTime();
      jupyterProject.setExpires(expirationDate);
      jupyterProject.setNoLimit(jupyterSettings.isNoLimit());
      jupyterFacade.update(jupyterProject);
    }
  }

  public void versionProgram(Project project, Users user, String sessionKernelId, Path outputPath)
      throws ServiceException {
  
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersController.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      versionProgram(username, sessionKernelId, outputPath, udfso);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public void versionProgram(String hdfsUser, String sessionKernelId, Path outputPath,
    DistributedFileSystemOps udfso) throws ServiceException {
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    String relativeNotebookPath = null;
    try {
      relativeNotebookPath = getNotebookRelativeFilePath(hdfsUser, sessionKernelId, udfso);

      //Get the content of the notebook should work in both spark and python kernels irregardless of contents manager
      if (!Strings.isNullOrEmpty(relativeNotebookPath)) {
        JSONObject notebookContents = new JSONObject(ClientBuilder.newClient()
            .target("http://" + jupyterManager.getJupyterHost() + ":" + jp.getPort() +
                "/hopsworks-api/jupyter/" + jp.getPort() + "/api/contents/" + relativeNotebookPath
                + "?content=1&token=" + jp.getToken())
            .request()
            .method("GET")
            .readEntity(String.class));
        JSONObject notebookJSON = (JSONObject)notebookContents.get("content");
        try {
          udfso.create(outputPath, notebookJSON.toString());
        } catch(IOException e) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_NOTEBOOK_VERSIONING_FAILED,
              Level.FINE, "failed to save notebook content", e.getMessage(), e);
        }
      }
    } catch(Exception e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_NOTEBOOK_VERSIONING_FAILED,
          Level.FINE, "failed to version notebook", e.getMessage(), e);
    }
  }

  public void attachJupyterConfigurationToNotebook(Users user, String hdfsUsername, Project project, String kernelId)
      throws ServiceException {
    DistributedFileSystemOps udfso = null;
    try {
      JupyterSettings jupyterSettings = jupyterSettingsFacade.findByProjectUser(project, user.getEmail());
      udfso = dfs.getDfsOps(hdfsUsername);
      String relativeNotebookPath = getNotebookRelativeFilePath(hdfsUsername, kernelId, udfso);
      JSONObject jupyterSettingsMetadataObj = new JSONObject();
      jupyterSettingsMetadataObj.put(Settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME,
          objectMapper.writeValueAsString(jupyterSettings));
      jupyterSettingsMetadataObj.put(Settings.META_USAGE_TIME, new Date().getTime());
      xAttrsController.addStrXAttr(jupyterSettings.getBaseDir() + "/" + relativeNotebookPath,
          Settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME,
          jupyterSettingsMetadataObj.toString(), udfso);
    } catch (Exception e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ATTACHING_JUPYTER_CONFIG_TO_NOTEBOOK_FAILED,
          Level.FINE, "Failed to attach jupyter configuration for user: " + user+ ", project" + ": " + project + ", " +
          "kernelId: " + kernelId, e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public String getNotebookRelativeFilePath(String hdfsUser, String sessionKernelId, DistributedFileSystemOps udfso)
      throws ServiceException {
    String relativeNotebookPath = null;
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    JSONArray sessionsArray = new JSONArray(ClientBuilder.newClient()
        .target("http://" + jupyterManager.getJupyterHost() + ":" + jp.getPort() + "/hopsworks-api/jupyter/" +
            jp.getPort() + "/api/sessions?token=" + jp.getToken())
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

    return relativeNotebookPath;
  }

  /**
   * Gets the NotebookConversion type depending on the kernel of the notebook
   * @param notebookPath
   * @return NotebookConversion type
   * @throws ServiceException
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public NotebookConversion getNotebookConversionType(String notebookPath, Users user, Project project)
      throws ServiceException {
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    Path p = new Path(notebookPath);
    try {
      udfso = dfs.getDfsOps(projectUsername);
      String notebookString;
      try(FSDataInputStream inStream = udfso.open(p)) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyBytes(inStream, out, 512);
        notebookString = out.toString("UTF-8");
      }
      JSONObject notebookJSON = new JSONObject(notebookString);
      String notebookKernel = (String) notebookJSON
          .getJSONObject("metadata")
          .getJSONObject("kernelspec")
          .get("display_name");
      Optional<NotebookConversion> kernel = NotebookConversion.fromKernel(notebookKernel);
      if(kernel.isPresent()) {
        if(kernel.get() == NotebookConversion.PY_JOB || kernel.get() == NotebookConversion.PY) {
          return kernel.get();
        } else {
          throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.FINE,
              "Unsupported kernel: " + kernel.get().name() + ". Conversion to .py is for PySpark " +
                  "or Python notebooks");
        }
      } else {
        throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.FINE,
            "Unsupported kernel for notebook. Conversion to .py is for PySpark " +
                "or Python notebooks");
      }
    } catch (Exception e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.FINE, "Failed to get " +
          "kernel for notebook.",  e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public enum NotebookConversion {
    PY("PySpark"),
    HTML("Html"),
    PY_JOB("Python");

    private String kernel;
    NotebookConversion(String kernel) {
      this.kernel = kernel;
    }

    public static Optional<NotebookConversion> fromKernel(String kernel) {
      return Arrays.stream(NotebookConversion.values()).filter(k ->
          k.getKernel().equalsIgnoreCase(kernel)).findFirst();
    }

    public String getKernel() {
      return kernel;
    }
  }
}
