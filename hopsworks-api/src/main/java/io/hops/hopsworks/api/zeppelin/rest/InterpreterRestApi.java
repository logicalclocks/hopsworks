/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.zeppelin.rest;

import com.google.gson.Gson;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.rest.message.NewInterpreterSettingRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.RestartInterpreterRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImpl;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImplFactory;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ZeppelinException;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.dep.Repository;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterPropertyType;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.interpreter.InterpreterSettingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.repository.RemoteRepository;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Interpreter Rest API
 */
@RequestScoped
@Produces("application/json")
public class InterpreterRestApi {

  Logger logger = LoggerFactory.getLogger(InterpreterRestApi.class);
  private InterpreterSettingManager interpreterSettingManager;
  private Project project;
  private ZeppelinConfig zeppelinConf;
  private Users user;

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private ProjectTeamFacade teambean;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private YarnApplicationstateFacade appStateBean;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private LivyController livyService;
  @EJB
  private Settings settings;
  @EJB
  private NotebookServerImplFactory notebookServerImplFactory;

  Gson gson = new Gson();

  public InterpreterRestApi() {
  }

  public void setParms(Project project, Users user, ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.user = user;
    this.zeppelinConf = zeppelinConf;
    this.interpreterSettingManager = zeppelinConf.getInterpreterSettingManager();
  }

  /**
   * List all interpreter settings
   */
  @GET
  @Path("setting")
  public Response listSettings() {
    return new JsonResponse<>(Status.OK, "", interpreterSettingManager.get()).build();
  }

  /**
   * Get a setting
   */
  @GET
  @Path("setting/{settingId}")
  public Response getSetting(@PathParam("settingId") String settingId) {
    try {
      InterpreterSetting setting = this.interpreterSettingManager.get(settingId);
      if (setting == null) {
        return new JsonResponse<>(Status.NOT_FOUND).build();
      } else {
        return new JsonResponse<>(Status.OK, "", setting).build();
      }
    } catch (NullPointerException e) {
      logger.error("Exception in InterpreterRestApi while creating ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e)).build();
    }
  }

  /**
   * Add new interpreter setting
   *
   * @param message NewInterpreterSettingRequest
   */
  @POST
  @Path("setting")
  public Response newSettings(String message) {
    try {
      NewInterpreterSettingRequest request = NewInterpreterSettingRequest.fromJson(message);
      if (request == null) {
        return new JsonResponse<>(Status.BAD_REQUEST).build();
      }

      InterpreterSetting interpreterSetting = interpreterSettingManager
          .createNewSetting(request.getName(), request.getGroup(), request.getDependencies(),
              request.getOption(), request.getProperties());
      zeppelinResource.persistToDB(this.project);
      logger.info("new setting created with {}", interpreterSetting.getId());
      return new JsonResponse<>(Status.OK, "", interpreterSetting).build();
    } catch (InterpreterException | IOException e) {
      logger.error("Exception in InterpreterRestApi while creating ", e);
      return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
          .build();
    }
  }

  @PUT
  @Path("setting/{settingId}")
  public Response updateSetting(String message, @PathParam("settingId") String settingId) {
    logger.info("Update interpreterSetting {}", settingId);

    try {
      UpdateInterpreterSettingRequest request = UpdateInterpreterSettingRequest.fromJson(message);
      interpreterSettingManager.setPropertyAndRestart(settingId, request.getOption(), request.getProperties(),
          request.getDependencies());
    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
          .build();
    } catch (IOException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e)).build();
    }
    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    //Persist json to the database
    zeppelinResource.persistToDB(this.project);
    if (setting == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse<>(Status.OK, "", setting).build();
  }

  /**
   * Remove interpreter setting
   */
  @DELETE
  @Path("setting/{settingId}")
  public Response removeSetting(@PathParam("settingId") String settingId) throws IOException {
    logger.info("Remove interpreterSetting {}", settingId);
    interpreterSettingManager.remove(settingId);
    zeppelinResource.persistToDB(this.project);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * Restart interpreter setting
   */
  @PUT
  @Path("setting/restart/{settingId}")
  public Response restartSetting(String message, @PathParam("settingId") String settingId) {
    logger.info("Restart interpreterSetting {}, msg={}", settingId, message);

    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    try {
      RestartInterpreterRequest request = RestartInterpreterRequest.fromJson(message);

      String noteId = request == null ? null : request.getNoteId();
      if (null == noteId) {
        interpreterSettingManager.close(setting);
      } else {
        interpreterSettingManager.restart(settingId, noteId, SecurityUtils.getPrincipal());
      }

      cleanUserCertificates(project);

      zeppelinConf.getNotebookServer().clearParagraphRuntimeInfo(setting);

    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while restartSetting ", e);
      return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e)).build();
    }
    if (setting == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
    }
    int timeout = zeppelinConf.getConf().getInt(ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_CONNECT_TIMEOUT);
    long startTime = System.currentTimeMillis();
    long endTime;
    while (zeppelinResource.isInterpreterRunning(setting, project)) {
      endTime = System.currentTimeMillis();
      if ((endTime - startTime) > (timeout * 2)) {
        break;
      }
    }
    if (zeppelinResource.isInterpreterRunning(setting, project)) {
      zeppelinResource.forceKillInterpreter(setting, project);
    }
    InterpreterDTO interpreter = new InterpreterDTO(setting, !zeppelinResource.isInterpreterRunning(setting, project));
    return new JsonResponse(Status.OK, "", interpreter).build();
  }
  
  private void cleanUserCertificates(Project project) {
    if (!areRunningInterpretersForProject(project)) {
      HopsUtils.cleanupCertificatesForProject(project.getName(), settings.getHdfsTmpCertDir(),
        certificateMaterializer, settings);
      certificateMaterializer.closedInterpreter(project.getId());
    }
    
  }

  private boolean areRunningInterpretersForProject(Project project) {
    Map<String, InterpreterDTO> interpreters = interpreters(project);
    boolean running = false;
    for (Map.Entry<String, InterpreterDTO> interpreter : interpreters.entrySet()) {
      if (!interpreter.getValue().isNotRunning()) {
        running = true;
        break;
      }
    }
    
    return running;
  }
  
  /**
   * Get livy session Yarn AppId
   *
   * @param sessionId
   * @return
   */
  @GET
  @Path("/livy/sessions/appId/{sessionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLivySessionAppId(@PathParam("sessionId") int sessionId) throws ZeppelinException {
    LivyMsg.Session session = livyService.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId + "' not found.").build();
    }
    String projName = hdfsUsersController.getProjectName(session.getProxyUser());

    if (!this.project.getName().equals(projName)) {
      throw new ZeppelinException(RESTCodes.ZeppelinErrorCode.STOP_SESSIONS_FORBIDDEN, Level.FINE);
    }

    List<YarnApplicationstate> appStates = appStateBean.findByAppname("livy-session-" + sessionId);
    if (appStates == null || appStates.isEmpty()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId + "' not running.").build();
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(appStates.get(0).getApplicationid()).
        build();

  }

  /**
   * Get spark interpreter Yarn AppId
   *
   * @return
   */
  @GET
  @Path("/spark/appId")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getSparkSessionAppId() {
    List<YarnApplicationstate> appStates = appStateBean.findByAppname(this.project.getName() + "-Zeppelin");
    if (appStates == null || appStates.isEmpty()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Zeppelin not running for project " + this.project.getName()).
          build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(appStates.get(0).getApplicationid()).
        build();
  }

  /**
   * Stop livy session
   *
   * @param sessionId
   * @param settingId
   * @return
   */
  @DELETE
  @Path("/livy/sessions/delete/{settingId}/{sessionId}")
  public Response stopSession(@PathParam("settingId") String settingId, @PathParam("sessionId") int sessionId)
    throws ZeppelinException {
    logger.info("Restart interpreterSetting {}", settingId);
    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    LivyMsg.Session session = livyService.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId + "' not found.").build();
    }
    String projName = hdfsUsersController.getProjectName(session.getProxyUser());
    if (!this.project.getName().equals(projName)) {
      throw new ZeppelinException(RESTCodes.ZeppelinErrorCode.STOP_SESSIONS_FORBIDDEN, Level.FINE);
    }
    List<LivyMsg.Session> sessions = livyService.getLivySessionsForProjectUser(this.project, this.user,
        ProjectServiceEnum.ZEPPELIN);
    try {
      livyService.deleteLivySession(sessionId);
      if (sessions.size() > 0) {
        ZeppelinConfig zConf = zeppelinConfFactory.getProjectConf(this.project.getName());
        if (zConf.getReplFactory() != null) {
          zConf.getInterpreterSettingManager().restart(settingId);
        }
      }
    } catch (InterpreterException e) {
      throw new ZeppelinException(RESTCodes.ZeppelinErrorCode.INTERPRETER_CLOSE_ERROR, Level.SEVERE);
    }

    int timeout = zeppelinConf.getConf().getInt(ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_CONNECT_TIMEOUT);
    long startTime = System.currentTimeMillis();
    long endTime;
    while (livyService.isLivySessionAlive(sessionId)) {
      endTime = System.currentTimeMillis();
      if ((endTime - startTime) > (timeout * 2)) {
        break;
      }
    }
    int res = livyService.deleteLivySession(sessionId);
    cleanUserCertificates(project);

    if (res != Response.Status.NOT_FOUND.getStatusCode() && res != Response.Status.OK.getStatusCode()) {
      return new JsonResponse(Status.EXPECTATION_FAILED, "Could not stop session '" + sessionId + "'").build();
    }

    InterpreterDTO interpreter = new InterpreterDTO(setting, !zeppelinResource.isInterpreterRunning(setting, project),
        livyService.getLivySessions(this.project, ProjectServiceEnum.ZEPPELIN));
    return new JsonResponse(Status.OK, "Deleted ", interpreter).build();
  }

  /**
   * List all available interpreters by group
   */
  @GET
  public Response listInterpreter() {
    Map<String, InterpreterSetting> m = interpreterSettingManager.getAvailableInterpreterSettings();
    return new JsonResponse<>(Status.OK, "", m).build();
  }

  /**
   * List of dependency resolving repositories
   */
  @GET
  @Path("repository")
  public Response listRepositories() {
    List<RemoteRepository> interpreterRepositories = interpreterSettingManager.getRepositories();
    return new JsonResponse<>(Status.OK, "", interpreterRepositories).build();
  }

  /**
   * Add new repository
   *
   * @param message Repository
   */
  @POST
  @Path("repository")
  public Response addRepository(String message) {
    try {
      Repository request = Repository.fromJson(message);
      interpreterSettingManager.addRepository(request.getId(), request.getUrl(),
          request.isSnapshot(), request.getAuthentication(), request.getProxy());
      zeppelinResource.persistToDB(this.project);
      logger.info("New repository {} added", request.getId());
    } catch (Exception e) {
      logger.error("Exception in InterpreterRestApi while adding repository ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
          ExceptionUtils.getStackTrace(e)).build();
    }
    return new JsonResponse(Status.OK).build();
  }

  /**
   * get metadata values
   */
  @GET
  @Path("metadata/{settingId}")
  public Response getMetaInfo(@Context HttpServletRequest req, @PathParam("settingId") String settingId) {
    InterpreterSetting interpreterSetting = interpreterSettingManager.get(settingId);
    if (interpreterSetting == null) {
      return new JsonResponse<>(Status.NOT_FOUND).build();
    }
    Map<String, String> infos = interpreterSetting.getInfos();
    return new JsonResponse<>(Status.OK, "metadata", infos).build();
  }

  /**
   * Delete repository
   *
   * @param repoId ID of repository
   * @return 
   */
  @DELETE
  @Path("repository/{repoId}")
  public Response removeRepository(@PathParam("repoId") String repoId) {
    logger.info("Remove repository {}", repoId);
    try {
      interpreterSettingManager.removeRepository(repoId);
      zeppelinResource.persistToDB(this.project);
    } catch (Exception e) {
      logger.error("Exception in InterpreterRestApi while removing repository ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e)).build();
    }
    return new JsonResponse(Status.OK).build();
  }

  /**
   * Get available types for property
   * @return 
   */
  @GET
  @Path("property/types")
  public Response listInterpreterPropertyTypes() {
    return new JsonResponse<>(Status.OK, InterpreterPropertyType.getTypes()).build();
  }

  /**
   * list interpreters with status(running or not).
   * <p/>
   * @return nothing if successful.
   */
  @GET
  @Path("interpretersWithStatus")
  public Response getinterpretersWithStatus() {
    Map<String, InterpreterDTO> interpreters = interpreters(project);
    return new JsonResponse(Status.OK, "", interpreters).build();
  }

  private Map<String, InterpreterDTO> interpreters(Project project) {
    Map<String, InterpreterDTO> interpreterDTOs = new HashMap<>();
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = interpreterSettingManager.get();
    InterpreterDTO interpreterDTO;
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO = new InterpreterDTO(interpreter, !zeppelinResource.isInterpreterRunning(interpreter, project));
      interpreterDTOs.put(interpreter.getName(), interpreterDTO);
      if (interpreter.getName().contains("livy")) {
        interpreterDTO.setSessions(livyService.getLivySessions(project, ProjectServiceEnum.ZEPPELIN));
      }
      if (interpreter.getName().equalsIgnoreCase(settings.getZeppelinDefaultInterpreter())) {
        interpreterDTO.setDefaultInterpreter(true);
      }
    }
    return interpreterDTOs;
  }

  /**
   * Restarts zeppelin by cleaning the cache for the user
   * and project
   *
   * @return
   */
  @GET
  @Path("restart")
  public Response restart() throws ZeppelinException {
    Long timeSinceLastRestart;
    Long lastRestartTime = zeppelinConfFactory.getLastRestartTime(this.project.getName());
    if (lastRestartTime != null) {
      timeSinceLastRestart = System.currentTimeMillis() - lastRestartTime;
      if (timeSinceLastRestart < 60000 * 1) {
        throw new ZeppelinException(RESTCodes.ZeppelinErrorCode.RESTART_ERROR, Level.FINE);
      }
    }
    Map<String, InterpreterDTO> interpreterDTOMap = interpreters(this.project);
    InterpreterDTO interpreterDTO;
    for (String key : interpreterDTOMap.keySet()) {
      interpreterDTO = interpreterDTOMap.get(key);
      if (!interpreterDTO.isNotRunning()) {
        if (interpreterDTO.getName().equalsIgnoreCase("livy")) {
          for (LivyMsg.Session session : interpreterDTO.getSessions()) {
            stopSession(interpreterDTO.getId(), session.getId());
          }
        } else {
          restartSetting(null, interpreterDTO.getId());
        }
      }
    }

    NotebookServerImpl notebookServerImpl = notebookServerImplFactory.getNotebookServerImpl(this.project.getName());
    if (notebookServerImpl != null) {
      notebookServerImpl.closeConnections(notebookServerImplFactory);
    }

    List<ProjectTeam> projectTeam = teambean.findMembersByProject(this.project);
    for (ProjectTeam member : projectTeam) {
      TicketContainer.instance.invalidate(member.getUser().getEmail());
    }

    return new JsonResponse(Status.OK, "Cache cleared.").build();
  }
}
