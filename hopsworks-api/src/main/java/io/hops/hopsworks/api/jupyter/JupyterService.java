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

import io.hops.hopsworks.api.filter.NoCacheResponse;

import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.core.GenericEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private static final Logger LOGGER = Logger.getLogger(JupyterService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;
  @EJB
  private LivyController livyController;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private ElasticController elasticController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private JupyterController jupyterController;

  private Integer projectId;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;

  public JupyterService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Launches a Jupyter notebook server for this project-specific user
   *
   * @return
   * @throws io.hops.hopsworks.common.exception.ServiceException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAllNotebookServersInProject() throws ServiceException {

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND, Level.FINE);
    }

    List<JupyterProject> listServers = new ArrayList<>();
    listServers.addAll(servers);

    GenericEntity<List<JupyterProject>> notebookServers = new GenericEntity<List<JupyterProject>>(listServers) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(notebookServers).build();
  }

  @GET
  @Path("/livy/sessions")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response livySessions(@Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<LivyMsg.Session> sessions = livyController.getLivySessionsForProjectUser(this.project, user,
        ProjectServiceEnum.JUPYTER);
    GenericEntity<List<LivyMsg.Session>> livyActive = new GenericEntity<List<LivyMsg.Session>>(sessions) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(livyActive).build();
  }

  @DELETE
  @Path("/livy/sessions/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopLivySession(@PathParam("appId") String appId, @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    stopSession(hdfsUsername, user, appId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  /**
   * Get livy session Yarn AppId
   *
   * @param sc
   * @return
   */
  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response settings(@Context SecurityContext sc) {

    Users user = jWTHelper.getUserPrincipal(sc);
    String loggedinemail = user.getEmail();
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(projectId,loggedinemail);
    if (js.getProject() == null) {
      js.setProject(project);
    }
    if (settings.isPythonKernelEnabled()) {
      js.setPrivateDir(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(js).build();
  }

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response isRunning(@Context HttpServletRequest req, @Context SecurityContext sc) throws ServiceException {

    String hdfsUser = getHdfsUser(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND, Level.FINE);
    }
    // Check to make sure the jupyter notebook server is running
    boolean running = jupyterProcessFacade.pingServerJupyterUser(jp.getPid());
    // if the notebook is not running but we have a database entry for it,
    // we should remove the DB entry (and restart the notebook server).
    if (!running) {
      jupyterFacade.removeNotebookServer(hdfsUser, jp.getPort());
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_RUNNING, Level.FINE);
    }
    String externalIp = Ip.getHost(req.getRequestURL().toString());
    settings.setHopsworksExternalIp(externalIp);
    Integer port = req.getLocalPort();
    String endpoint = externalIp + ":" + port;
    if (endpoint.compareToIgnoreCase(jp.getHostIp()) != 0) {
      // update the host_ip to whatever the client saw as the remote host:port
      jp.setHostIp(endpoint);
      jupyterFacade.update(jp);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jp).build();
  }

  @POST
  @Path("/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response startNotebookServer(JupyterSettings jupyterSettings, @Context HttpServletRequest req, 
      @Context SecurityContext sc) throws ProjectException, HopsSecurityException, ServiceException {

    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, hopsworksUser);
    String realName = hopsworksUser.getFname() + " " + hopsworksUser.getLname();

    if (project.getPaymentType().equals(PaymentType.PREPAID)) {
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(project.getName());
      if (projectQuota == null || projectQuota.getQuotaRemaining() < 0) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_INSUFFICIENT, Level.FINE);
      }
    }

    boolean enabled = project.getConda();
    if (!enabled) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ANACONDA_NOT_ENABLED, Level.FINE);
    }

    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);

    if (jp == null) {
      HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

      String configSecret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
      JupyterDTO dto = null;
      DistributedFileSystemOps dfso = dfsService.getDfsOps();
  
      try {
        jupyterSettingsFacade.update(jupyterSettings);
        dto = jupyterProcessFacade.startServerAsJupyterUser(project, configSecret, hdfsUser, realName, jupyterSettings);
        HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), user.getUsername(),
            settings.getHdfsTmpCertDir(), dfso, certificateMaterializer, settings, dto.getCertificatesDir());
        // When Livy launches a job it will look in the standard directory for the certificates
        // We materialize them twice but most probably other operations will need them too, so it is OK
        // Remember to remove both when stopping Jupyter server or an exception is thrown
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (ServiceException ex) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE, ex.getMessage(),
            null, ex);
      } catch (IOException ex) {
        certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
        HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
            settings.getHdfsTmpCertDir(), certificateMaterializer, dto.getCertificatesDir(), settings);
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_MATERIALIZATION_ERROR, Level.SEVERE);
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }

      String externalIp = Ip.getHost(req.getRequestURL().toString());

      try {
        jp = jupyterFacade.saveServer(externalIp, project, configSecret,
          dto.getPort(), user.getId(), dto.getToken(), dto.getPid());
      } catch(Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to save Jupyter notebook settings", e);
        stopAllSessions(hdfsUser, hopsworksUser, configSecret, dto.getPid(), dto.getPort());
      }

      if (jp == null) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SAVE_SETTINGS_ERROR, Level.SEVERE);
      }
    } else {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVER_ALREADY_RUNNING, Level.FINE);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        jp).build();
  }

  @GET
  @Path("/stopAll")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
  public Response stopAll() throws ServiceException {
    jupyterProcessFacade.stopProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stopDataOwner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopDataOwner(@PathParam("hdfsUsername") String hdfsUsername, @Context SecurityContext sc) throws
      ServiceException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUsername);
    stopAllSessions(hdfsUsername, user, jp.getSecret(), jp.getPid(), jp.getPort());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopNotebookServer(@Context SecurityContext sc) throws ProjectException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUsername);
    if (jp == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.JUPYTER_SERVER_NOT_FOUND, Level.FINE,
        "hdfsUser: " + hdfsUsername);
    }
    stopAllSessions(hdfsUsername, user, jp.getSecret(), jp.getPid(), jp.getPort());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void stopAllSessions(String hdfsUser, Users user, String secret, long pid, int port) throws ServiceException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway

    List<LivyMsg.Session> sessions = livyController.
        getLivySessionsForProjectUser(project, user, ProjectServiceEnum.JUPYTER);

    livyController.deleteAllLivySessions(hdfsUser, ProjectServiceEnum.JUPYTER);

    int retries = 3;
    while(retries > 0 &&
        livyController.getLivySessionsForProjectUser(project, user, ProjectServiceEnum.JUPYTER).size() > 0) {
      LOGGER.log(Level.SEVERE, "Failed previous attempt to delete livy sessions for project " + project.getName() +
            " user " + hdfsUser + ", retrying...");
      livyController.deleteAllLivySessions(hdfsUser, ProjectServiceEnum.JUPYTER);

      try {
        Thread.sleep(1000);
      } catch(InterruptedException ie) {
        LOGGER.log(Level.SEVERE, "Interrupted while sleeping");
      }
      retries--;
    }
    String jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser, project, secret);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    jupyterProcessFacade.killServerJupyterUser(hdfsUser, jupyterHomePath, pid, port);

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
      updateRunningExperimentAsKilled(session);
    }
  }

  private void stopSession(String hdfsUser, Users user, String appId) {

    List<LivyMsg.Session> sessions = livyController.getLivySessionsForProjectUser(project, user,
      ProjectServiceEnum.JUPYTER);

    for(LivyMsg.Session session: sessions) {
      if(session.getAppId().equalsIgnoreCase(appId)) {
        livyController.deleteLivySession(session.getId());
        updateRunningExperimentAsKilled(session);
        break;
      }
    }
  }

  private void updateRunningExperimentAsKilled(LivyMsg.Session session) {
    try {
      String experimentsIndex = this.project.getName().toLowerCase()
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

  @GET
  @Path("/convertIPythonNotebook/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response convertIPythonNotebook(@PathParam("path") String path,
      @Context SecurityContext sc) throws ServiceException {
    String ipynbPath = settings.getProjectPath(this.project.getName()) + "/" + path;
    int extensionIndex = ipynbPath.lastIndexOf(".ipynb");
    StringBuilder pathBuilder = new StringBuilder(ipynbPath.substring(0, extensionIndex)).append(".py");
    String pyAppPath = pathBuilder.toString();
    String hdfsUsername = getHdfsUser(sc);
    jupyterController.convertIPythonNotebook(hdfsUsername, ipynbPath, project, pyAppPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private String getHdfsUser(SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    return  hdfsUsersController.getHdfsUserName(project, user);
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateNotebookServer(JupyterSettings jupyterSettings, @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(projectId, user.getEmail());

    js.setShutdownLevel(jupyterSettings.getShutdownLevel());
    jupyterSettingsFacade.update(js);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(js).build();
  }

  private JSONObject getApplicationFromRM(String appId) {

    Response response = null;
    String rmUrl = "http://" + settings.getRmIp() + ":" + settings.getRmPort() + "/ws/v1/cluster/apps/" + appId;
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(rmUrl);
    try {
      response = target.request().get();
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Unable to get information about app " + appId);
    } finally {
      client.close();
    }
    return new JSONObject(response.readEntity(String.class));

  }

}
