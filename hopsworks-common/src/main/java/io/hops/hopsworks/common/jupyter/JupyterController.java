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
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.system.job.SystemJobStatus;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterController {

  private static final Logger LOGGER = Logger.getLogger(JupyterController.class.getName());

  @EJB
  private DistributedFsService dfs;
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
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private JupyterJWTManager jupyterJWTManager;
  @EJB
  private XAttrsController xAttrsController;
  @Inject
  private NoteBookConverter noteBookConverter;

  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JaxbAnnotationModule());
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public SystemJobStatus convertIPythonNotebook(Project project, Users user, String notebookPath, String pyPath,
      NotebookConversion notebookConversion)  throws ServiceException {
    return noteBookConverter.convertIPythonNotebook(project, user, notebookPath, pyPath, notebookConversion);
  }

  public void shutdown(Project project, Users user, String secret, String cid, int port) throws ServiceException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    int retries = 3;
    while(retries > 0 &&
      livyController.getLivySessionsForProjectUser(project, user).size() > 0) {
      LOGGER.log(Level.SEVERE, "Failed previous attempt to delete livy sessions for project " + project.getName() +
        " user " + user.getUsername() + ", retrying...");
      livyController.deleteAllLivySessions(project, user);

      try {
        Thread.sleep(200);
      } catch(InterruptedException ie) {
        LOGGER.log(Level.SEVERE, "Interrupted while sleeping");
      }
      retries--;
    }
    String jupyterHomePath = jupyterManager.getJupyterHome(project, user, secret);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    try {
      // If we fail to start Jupyter server, then we call this shutdown to clean up
      // Do some sanity check before using jupyter settings
      jupyterManager.stopJupyterServer(project, user, jupyterHomePath, cid, port);
    } finally {
      DistributedFileSystemOps dfso = dfsService.getDfsOps();
      try {
        String certificatesDir = Paths.get(jupyterHomePath, "certificates").toString();
        HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
            settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }
      FileUtils.deleteQuietly(new File(jupyterHomePath));
      jupyterJWTManager.cleanJWT(cid, port);
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
      shutdown(project, jp.getUser(), jp.getSecret(), jp.getCid(), jp.getPort());
    }
    jupyterManager.projectCleanup(project);
  }

  public void updateExpirationDate(Project project, Users user, JupyterSettings jupyterSettings) {
    //Increase hours on expirationDate
    jupyterFacade.findByProjectUser(project, user)
        .ifPresent(jupyterProject -> {
          Date expirationDate = jupyterProject.getExpires();
          Calendar cal = Calendar.getInstance();
          cal.setTime(expirationDate);
          cal.add(Calendar.HOUR_OF_DAY, jupyterSettings.getShutdownLevel());
          expirationDate = cal.getTime();
          jupyterProject.setExpires(expirationDate);
          jupyterProject.setNoLimit(jupyterSettings.isNoLimit());
          jupyterFacade.update(jupyterProject);
        });
  }

  public void versionProgram(Project project, Users user, String sessionKernelId, Path outputPath)
      throws ServiceException {
  
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersController.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      versionProgram(project, user, sessionKernelId, outputPath, udfso);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public void versionProgram(Project project, Users user, String sessionKernelId, Path outputPath,
                             DistributedFileSystemOps udfso) throws ServiceException {
    JupyterProject jp = jupyterFacade.findByProjectUser(project, user)
        .orElseThrow(() -> new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_RUNNING, Level.FINE));

    try {
      String relativeNotebookPath = getNotebookRelativeFilePath(jp, sessionKernelId);

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

  public void attachJupyterConfigurationToNotebook(Project project, Users user, String kernelId)
      throws ServiceException {
    JupyterProject jupyterProject = jupyterFacade.findByProjectUser(project, user)
        .orElseThrow(() -> new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_RUNNING, Level.FINE));

    DistributedFileSystemOps udfso = null;
    try {
      JupyterSettings jupyterSettings = jupyterSettingsFacade.findByProjectUser(project, user.getEmail());
      udfso = dfs.getDfsOps(project, user);
      String relativeNotebookPath = getNotebookRelativeFilePath(jupyterProject, kernelId);
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

  public String getNotebookRelativeFilePath(JupyterProject jp, String sessionKernelId) throws ServiceException {
    String relativeNotebookPath = null;
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
      // display_name can be Python 3 or just Python.
      String[] parts = notebookKernel.trim().split(" ");
      Optional<NotebookConversion> kernel = NotebookConversion.fromKernel(parts[0]);
      if(kernel.isPresent()) {
        if(NotebookConversion.PY_JOB.equals(kernel.get()) || NotebookConversion.PY.equals(kernel.get())) {
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
}
