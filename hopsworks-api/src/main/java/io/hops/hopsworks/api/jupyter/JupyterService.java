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
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jobs.executions.MonitoringUrlBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.jupyter.JupyterJWTManager;
import io.hops.hopsworks.common.jupyter.JupyterManager;
import io.hops.hopsworks.common.jupyter.NotebookConversion;
import io.hops.hopsworks.common.jupyter.NotebookDTO;
import io.hops.hopsworks.common.jupyter.RemoteFSDriverType;
import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.system.job.SystemJobStatus;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterMode;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.PaymentType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private static final Logger LOGGER = Logger.getLogger(JupyterService.class.getName());
  private static final String[] JUPYTER_JWT_AUD = new String[]{Audience.API};

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @Inject
  private JupyterManager jupyterManager;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private LivyController livyController;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private JupyterJWTManager jupyterJWTManager;
  @EJB
  private SparkController sparkController;
  @EJB
  UsersController usersController;
  @EJB
  private MonitoringUrlBuilder monitoringUrlBuilder;
  @EJB
  private NotebookBuilder notebookBuilder;

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
   * @throws ServiceException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAllNotebookServersInProject(@Context SecurityContext sc) throws ServiceException {

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND, Level.FINE);
    }

    List<JupyterProject> listServers = new ArrayList<>(servers);

    GenericEntity<List<JupyterProject>> notebookServers = new GenericEntity<List<JupyterProject>>(listServers) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(notebookServers).build();
  }

  @GET
  @Path("/livy/sessions")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response livySessions(@Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<LivyMsg.Session> sessions = livyController.getLivySessionsForProjectUser(this.project, user);
    List<LivySessionDTO> livySessionDTOS = new ArrayList<>();
    for (LivyMsg.Session session : sessions) {
      LivySessionDTO dto = new LivySessionDTO(session);
      dto.setMonitoring(monitoringUrlBuilder.build(session.getAppId(), project));
      livySessionDTOS.add(dto);
    }
    GenericEntity<List<LivySessionDTO>> livyActive = new GenericEntity<List<LivySessionDTO>>(livySessionDTOS) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(livyActive).build();
  }

  @DELETE
  @Path("/livy/sessions/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopLivySession(@PathParam("appId") String appId, @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    jupyterController.stopSession(project, user, appId);
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response settings(@Context SecurityContext sc,
                           @Context HttpServletRequest req) {

    Users user = jWTHelper.getUserPrincipal(sc);
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(project, user.getEmail());
    RemoteFSDriverType driver = settings.getJupyterRemoteFsManager();
    // if we are using hopsfsmount the base dir is the project path
    String baseDir = js.getBaseDir();
    String projectPath = Utils.getProjectPath(project.getName());
    if (driver == RemoteFSDriverType.HDFS_CONTENTS_MANAGER && projectPath.equals(baseDir)) {
      // we should put back the default base dir, the jupyter dataset
      baseDir = Utils.getProjectPath(project.getName()) + Settings.ServiceDataset.JUPYTER.getName();
    } else if (driver == RemoteFSDriverType.HOPSFS_MOUNT) {
      baseDir = projectPath;
    }
    js.setBaseDir(baseDir);
    js.setPrivateDir(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret());
    js.setMode(JupyterMode.JUPYTER_LAB);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(js).build();
  }

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response isRunning(@Context HttpServletRequest req, @Context SecurityContext sc) throws ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    JupyterProject jp = jupyterFacade.findByProjectUser(project, user)
        .orElseThrow(() -> new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND, Level.FINE));

    // Check to make sure the jupyter notebook server is running
    boolean running = jupyterManager.ping(jp);
    // if the notebook is not running but we have a database entry for it,
    // we should remove the DB entry (and restart the notebook server).
    if (!running) {
      jupyterFacade.remove(jp);
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_RUNNING, Level.FINE);
    }

    //set minutes left until notebook server is killed
    Duration durationLeft = Duration.between(new Date().toInstant(), jp.getExpires().toInstant());
    jp.setMinutesUntilExpiration(durationLeft.toMinutes());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jp).build();
  }

  @POST
  @Path("/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startNotebookServer(JupyterSettings jupyterSettings, @Context HttpServletRequest req,
                                      @Context SecurityContext sc, @Context UriInfo uriInfo) throws ProjectException,
      HopsSecurityException, ServiceException, GenericException, JobException {

    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    //The JupyterSettings bean is serialized without the Project and User when attaching it to the notebook as xattr.
    // .We need to put the user object when we are launching Jupyter from the notebook. The Project object is set
    // from in the front-end
    if (jupyterSettings.getUsers() == null) {
      jupyterSettings.setUsers(hopsworksUser);
    }
    if (project.getPaymentType().equals(PaymentType.PREPAID)) {
      yarnProjectsQuotaFacade.findByProjectName(project.getName())
          .filter(q -> q.getQuotaRemaining() > 0)
          .orElseThrow(() -> new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_ERROR, Level.FINE));
    }
    if (project.getPythonEnvironment() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ANACONDA_NOT_ENABLED, Level.FINE);
    }

    if (jupyterFacade.findByProjectUser(project, hopsworksUser).isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVER_ALREADY_RUNNING, Level.FINE);
    }

    if (jupyterSettings.getMode() == null) {
      // set default mode for jupyter if mode is null
      jupyterSettings.setMode(JupyterMode.JUPYTER_LAB);
    }

    String configSecret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
    JupyterDTO dto = null;
    String allowOriginHost = uriInfo.getBaseUri().getHost();
    int allowOriginPort = uriInfo.getBaseUri().getPort();
    String allowOriginPortStr = allowOriginPort != -1 ? ":" + allowOriginPort : "";
    String allowOrigin = settings.getJupyterOriginScheme() + "://" + allowOriginHost + allowOriginPortStr;
    try {
      RemoteFSDriverType remoteFSDriverType = settings.getJupyterRemoteFsManager();
      if (remoteFSDriverType == RemoteFSDriverType.HOPSFS_MOUNT) {
        jupyterSettings.setBaseDir(Utils.getProjectPath(project.getName()));
      }
      jupyterSettingsFacade.update(jupyterSettings);

      //Inspect dependencies
      sparkController
          .inspectDependencies(project, hopsworksUser, (SparkJobConfiguration) jupyterSettings.getJobConfig(), false);
      dto = jupyterManager.startJupyterServer(project, hopsworksUser, configSecret, jupyterSettings, allowOrigin);
      jupyterJWTManager.materializeJWT(hopsworksUser, project, jupyterSettings, dto.getCid(), dto.getPort(),
          JUPYTER_JWT_AUD);
      jupyterManager.waitForStartup(project, hopsworksUser);
    } catch (ServiceException | TimeoutException ex) {
      if (dto != null) {
        jupyterController.shutdown(project, hopsworksUser, configSecret, dto.getCid(), dto.getPort());
      }
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE, ex.getMessage(),
          null, ex);
    } catch (IOException ex) {
      if (dto != null) {
        jupyterController.shutdown(project, hopsworksUser, configSecret, dto.getCid(), dto.getPort());
      }
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_MATERIALIZATION_ERROR, Level.SEVERE,
          ex.getMessage(), null, ex);
    }

    try {
      Date expirationDate = new Date();
      Calendar cal = Calendar.getInstance();
      cal.setTime(expirationDate);
      cal.add(Calendar.HOUR_OF_DAY, jupyterSettings.getShutdownLevel());
      expirationDate = cal.getTime();

      JupyterProject jupyterProject = jupyterFacade.saveServer(project, hopsworksUser, configSecret, dto.getPort(),
          dto.getToken(), dto.getCid(), expirationDate, jupyterSettings.isNoLimit());

      //set minutes left until notebook server is killed
      Duration durationLeft = Duration.between(new Date().toInstant(), jupyterProject.getExpires().toInstant());
      jupyterProject.setMinutesUntilExpiration(durationLeft.toMinutes());

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jupyterProject).build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to save Jupyter notebook settings", e);
      jupyterController.shutdown(project, hopsworksUser, configSecret, dto.getCid(), dto.getPort());
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SAVE_SETTINGS_ERROR, Level.SEVERE);
    }
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopNotebookServer(@Context HttpServletRequest req,
                                     @Context SecurityContext sc) throws ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    JupyterProject jp = jupyterFacade.findByProjectUser(project, user)
        .orElseThrow(() -> new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_SERVERS_NOT_FOUND, Level.FINE));

    jupyterController.shutdown(project, user, jp.getSecret(), jp.getCid(), jp.getPort());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/convertIPythonNotebook/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response convertIPythonNotebook(@PathParam("path") String path,
                                         @Context HttpServletRequest req,
                                         @Context SecurityContext sc) throws ServiceException {
    if (path.startsWith("/")) {
      path = path.replaceFirst("/", "");
    }
    String ipynbPath = Utils.getProjectPath(this.project.getName()) + path;
    int extensionIndex = ipynbPath.lastIndexOf(".ipynb");
    StringBuilder pathBuilder = new StringBuilder(ipynbPath.substring(0, extensionIndex)).append(".py");
    String pyAppPath = pathBuilder.toString();
    Users user = jWTHelper.getUserPrincipal(sc);
    NotebookConversion conversionType = jupyterController.getNotebookConversionType(ipynbPath, user, this.project);
    SystemJobStatus status =
      jupyterController.convertIPythonNotebook(project, user, ipynbPath, pyAppPath, conversionType);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(status).build();
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateNotebookServer(JupyterSettings jupyterSettings,
                                       @Context HttpServletRequest req,
                                       @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    jupyterSettingsFacade.update(jupyterSettings);
    jupyterController.updateExpirationDate(project, user, jupyterSettings);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jupyterSettings).build();
  }

  @GET
  @Path("recent")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response recent(@QueryParam("count") @DefaultValue("5") @Min(0) @Max(100) int count,
                         @Context UriInfo uriInfo, @Context SecurityContext sc, @Context HttpServletRequest req)
      throws OpenSearchException {
    NotebookDTO dto = notebookBuilder.build(uriInfo, project, count);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Attach a jupyter configuration to the notebook.", notes = "The notebook is passed as the " +
      "kernelId from jupyter.")
  @PUT
  @Path("/attachConfiguration/{hdfsUsername}/{kernelId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response attachJupyterConfigurationToNotebook(@PathParam("hdfsUsername") String hdfsUsername,
                                                       @PathParam("kernelId") String kernelId,
                                                       @Context HttpServletRequest req,
                                                       @Context SecurityContext sc) throws ServiceException {
    Optional<Users> user = usersController.findByUsername(hdfsUsersController.getUserName(hdfsUsername));
    if (!user.isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode
          .WRONG_HDFS_USERNAME_PROVIDED_FOR_ATTACHING_JUPYTER_CONFIGURATION_TO_NOTEBOOK, Level.FINE,
          "HDFS username provided does not exist.");
    }
    jupyterController.attachJupyterConfigurationToNotebook(project, user.get(), kernelId);
    return Response.ok().build();
  }
}