/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.GenericEntity;
import org.apache.commons.codec.digest.DigestUtils;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private final static Logger LOGGER = Logger.getLogger(JupyterService.class.getName());

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
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getAllNotebookServersInProject(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new AppException(
          Response.Status.NOT_FOUND.getStatusCode(),
          "Could not find any Jupyter notebook servers for this project.");
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
      @Context HttpServletRequest req) throws AppException {
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
   * @throws AppException
   */
  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response settings(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

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
      @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(
          Response.Status.NOT_FOUND.getStatusCode(),
          "Could not find any Jupyter notebook server for this project.");
    }
    // Check to make sure the jupyter notebook server is running
    boolean running = jupyterProcessFacade.pingServerJupyterUser(jp.getPid());
    // if the notebook is not running but we have a database entry for it,
    // we should remove the DB entry (and restart the notebook server).
    if (!running) {
      jupyterFacade.removeNotebookServer(hdfsUser);
      throw new AppException(
          Response.Status.NOT_FOUND.getStatusCode(),
          "Found Jupyter notebook server for you, but it wasn't running.");
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
      @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
          Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "Could not find your username. Report a bug.");
    }

    boolean enabled = project.getConda();
    if (!enabled) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "First enable Anaconda. Click on 'Settings -> Python'");
    }

    if (project.getPaymentType().equals(PaymentType.PREPAID)) {
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(project.getName());
      if (projectQuota == null || projectQuota.getQuotaRemaining() < 0) {
        throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "This project is out of credits.");
      }
    }

    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);

    if (jp == null) {
      HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

      String configSecret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
      JupyterDTO dto;
      DistributedFileSystemOps dfso = dfsService.getDfsOps();
      String[] project_user = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER);

      try {
        jupyterSettingsFacade.update(jupyterSettings);
        dto = jupyterProcessFacade.startServerAsJupyterUser(project, configSecret, hdfsUser, jupyterSettings);
        if (dto == null) {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
        }
        HopsUtils.materializeCertificatesForUser(project.getName(), project_user[1], settings.getHdfsTmpCertDir(),
            dfso, certificateMaterializer, settings);
      } catch (InterruptedException | IOException ex) {
        Logger.getLogger(JupyterService.class.getName()).log(Level.SEVERE, null, ex);
        try {
          HopsUtils.cleanupCertificatesForUser(project_user[1], project.getName(), settings.getHdfsTmpCertDir(),
              certificateMaterializer);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Could not cleanup certificates for " + hdfsUser);
        }
        throw new AppException(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Problem starting a Jupyter notebook server.");
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }

      if (dto == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "Incomplete request!");
      }

      String externalIp = Ip.getHost(req.getRequestURL().toString());

      jp = jupyterFacade.saveServer(externalIp, project, configSecret,
          dto.getPort(), user.getId(), dto.getToken(), dto.getPid());

      if (jp == null) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(),
            "Could not save Jupyter Settings.");
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
      @Context HttpServletRequest req) throws AppException {

    jupyterProcessFacade.stopProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stopDataOwner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response stopDataOwner(@PathParam("hdfsUsername") String hdfsUsername,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response stopNotebookServer(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    String hdfsUsername = getHdfsUser(sc);
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void stop(String hdfsUser) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not find Jupyter entry for user: " + hdfsUser);
    }
    livyService.deleteAllLivySessions(hdfsUser, ProjectServiceEnum.JUPYTER);
    String jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser, jp);

    // stop the server, remove the user in this project's local dirs
    // This method also removes the corresponding row for the Notebook process in the JupyterProject table.
    jupyterProcessFacade.killServerJupyterUser(hdfsUser, jupyterHomePath, jp.getPid(), jp.
        getPort());

    String[] project_user = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER);
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      HopsUtils.cleanupCertificatesForUser(project_user[1], project
          .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not cleanup certificates for " + hdfsUser);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }
  }

  @GET
  @Path("/convertIPythonNotebook/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response convertIPythonNotebook(@PathParam("path") String path,
      @Context SecurityContext sc) throws AppException {
    String hdfsUsername = getHdfsUser(sc);
    String hdfsFilename = "/Projects/" + this.project.getName() + "/" + path;

    String prog = settings.getHopsworksDomainDir() + "/bin/convert-ipython-notebook.sh";
    String[] command = {prog, hdfsFilename, hdfsUsername};
    LOGGER.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.
          getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        // do nothing
      }
      int errCode = process.waitFor();
      if (errCode != 0) {
        throw new AppException(Response.Status.EXPECTATION_FAILED.getStatusCode(),
            "Problem converting ipython notebook to python program. " + line);
      }
    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class
          .getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Problem converting ipython notebook to python program.");

    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    return hdfsUsername;
  }

}
