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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
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
import io.hops.hopsworks.common.dao.user.UserFacade;
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
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private static final Logger LOGGER = Logger.getLogger(JupyterService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
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
  private LivyController livyService;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private ElasticController elasticController;

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
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getAllNotebookServersInProject(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws ServiceException {

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND);
    }

    List<JupyterProject> listServers = new ArrayList<>();
    listServers.addAll(servers);

    GenericEntity<List<JupyterProject>> notebookServers
        = new GenericEntity<List<JupyterProject>>(listServers) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        notebookServers).build();
  }

  @GET
  @Path("/livy/sessions")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response livySessions(@Context SecurityContext sc,
      @Context HttpServletRequest req) {
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    List<LivyMsg.Session> sessions = livyService.getLivySessionsForProjectUser(this.project, user,
        ProjectServiceEnum.JUPYTER);
    GenericEntity<List<LivyMsg.Session>> livyActive
        = new GenericEntity<List<LivyMsg.Session>>(sessions) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(livyActive).build();
  }

  /**
   * Get livy session Yarn AppId
   *
   * @return
   */
  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response settings(@Context SecurityContext sc,
      @Context HttpServletRequest req) {

    String loggedinemail = sc.getUserPrincipal().getName();
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(projectId,
        loggedinemail);
    if (js.getProject() == null) {
      js.setProject(project);
    }
    if (settings.isPythonKernelEnabled()) {
      js.setPrivateDir(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.
          getSecret());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        js).build();
  }

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response isRunning(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws ServiceException {

    String hdfsUser = getHdfsUser(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND);
    }
    // Check to make sure the jupyter notebook server is running
    boolean running = jupyterProcessFacade.pingServerJupyterUser(jp.getPid());
    // if the notebook is not running but we have a database entry for it,
    // we should remove the DB entry (and restart the notebook server).
    if (!running) {
      jupyterFacade.removeNotebookServer(hdfsUser);
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_RUNNING);
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
  public Response startNotebookServer(JupyterSettings jupyterSettings,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws ProjectException, HopsSecurityException, ServiceException {

    String hdfsUser = getHdfsUser(sc);
    String loggedinemail = sc.getUserPrincipal().getName();
    Users hopsworksUser = userFacade.findByEmail(loggedinemail);
    String realName = hopsworksUser.getFname() + " " + hopsworksUser.getLname();

    if (project.getPaymentType().equals(PaymentType.PREPAID)) {
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(project.getName());
      if (projectQuota == null || projectQuota.getQuotaRemaining() < 0) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_INSUFFICIENT);
      }
    }

    boolean enabled = project.getConda();
    if (!enabled) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ANACONDA_NOT_ENABLED);
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
        HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), user.getUsername(), settings
            .getHdfsTmpCertDir(),
            dfso, certificateMaterializer, settings, dto.getCertificatesDir());
        // When Livy launches a job it will look in the standard directory for the certificates
        // We materialize them twice but most probably other operations will need them too, so it is OK
        // Remember to remove both when stopping Jupyter server or an exception is thrown
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (IOException | ServiceException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        try {
          certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
          if (dto != null) {
            HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
                settings.getHdfsTmpCertDir(),
                certificateMaterializer, dto.getCertificatesDir(), settings);
          } else {
            throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_LOCATION_UNDEFINED);
          }
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Could not cleanup certificates for " + hdfsUser);
        }
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR);
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }

      String externalIp = Ip.getHost(req.getRequestURL().toString());

      jp = jupyterFacade.saveServer(externalIp, project, configSecret,
          dto.getPort(), user.getId(), dto.getToken(), dto.getPid());

      if (jp == null) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SAVE_SETTINGS_ERROR);
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        jp).build();
  }

  @GET
  @Path("/stopAll")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({"HOPS_ADMIN"})
  public Response stopAll(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws ServiceException {

    jupyterProcessFacade.stopProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response stopNotebookServer(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws ProjectException, ServiceException {
    String hdfsUsername = getHdfsUser(sc);
    stop(hdfsUsername, sc.getUserPrincipal().getName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void stop(String hdfsUser, String loggedinemail) throws ProjectException, ServiceException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.JUPYTER_SERVER_NOT_FOUND, "hdfsUser: " + hdfsUser);
    }

    Users user = userFacade.findByEmail(loggedinemail);

    List<LivyMsg.Session> sessions = livyService.
        getLivySessionsForProjectUser(project, user, ProjectServiceEnum.JUPYTER);

    livyService.deleteAllLivySessions(hdfsUser, ProjectServiceEnum.JUPYTER);

    int retries = 3;
    while(retries > 0 &&
        livyService.getLivySessionsForProjectUser(project, user, ProjectServiceEnum.JUPYTER).size() > 0) {
      LOGGER.log(Level.SEVERE, "Failed previous attempt to delete livy sessions for project " + project.getName() +
            " user " + hdfsUser + ", retrying...");
      livyService.deleteAllLivySessions(hdfsUser, ProjectServiceEnum.JUPYTER);

      try {
        Thread.sleep(1000);
      } catch(InterruptedException ie) {
        LOGGER.log(Level.SEVERE, "Interrupted while sleeping");
      }
      retries--;
    }
    String jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser, jp);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    jupyterProcessFacade.killServerJupyterUser(hdfsUser, jupyterHomePath, jp.getPid(), jp.
        getPort());

    String[] project_user = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER);
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      String certificatesDir = Paths.get(jupyterHomePath, "certificates").toString();
      HopsUtils.cleanupCertificatesForUserCustomDir(project_user[1], project
          .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
      certificateMaterializer.removeCertificatesLocal(project_user[1], project.getName());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not cleanup certificates for " + hdfsUser);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }

    try {
      String experimentsIndex = this.project.getName().toLowerCase()
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

  @GET
  @Path("/convertIPythonNotebook/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response convertIPythonNotebook(@PathParam("path") String path,
      @Context SecurityContext sc) throws ServiceException {
    String hdfsUsername = getHdfsUser(sc);
    String hdfsFilename = "/Projects/" + this.project.getName() + "/" + path;

    String prog = settings.getHopsworksDomainDir() + "/bin/convert-ipython-notebook.sh";
    String[] command = {prog, hdfsFilename, hdfsUsername};
    LOGGER.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.
          getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        // do nothing
      }
      int errCode = process.waitFor();
      if (errCode != 0) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, "error code: " + errCode);
      }
    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, null, ex.getMessage(), ex);

    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private String getHdfsUser(SecurityContext sc) {
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    return  hdfsUsersController.getHdfsUserName(project, user);
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response updateNotebookServer(JupyterSettings jupyterSettings,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(projectId, sc.getUserPrincipal().getName());

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
