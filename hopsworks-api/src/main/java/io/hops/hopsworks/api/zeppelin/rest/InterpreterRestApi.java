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

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.zeppelin.dep.Repository;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.sonatype.aether.repository.RemoteRepository;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.zeppelin.rest.message.NewInterpreterSettingRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.RestartInterpreterRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfFacade;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.administration.JobAdministration;
import io.hops.hopsworks.common.util.Settings;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.zeppelin.interpreter.InterpreterSettingManager;

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
  private String roleInProject;
  private Users user;

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private ProjectTeamFacade teambean;
  @EJB
  private HdfsUsersController hdfsUserBean;
  @EJB
  private YarnApplicationstateFacade appStateBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  @EJB
  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  Gson gson = new Gson();
  private final EnumSet<YarnApplicationState> PREDICATE = EnumSet.of(YarnApplicationState.RUNNING);

  public InterpreterRestApi() {
  }

  public void setParms(Project project, Users user, String userRole,
          ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.user = user;
    this.zeppelinConf = zeppelinConf;
    this.roleInProject = userRole;
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
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
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
      NewInterpreterSettingRequest request = gson.fromJson(message,
              NewInterpreterSettingRequest.class);
      if (request == null) {
        return new JsonResponse<>(Status.BAD_REQUEST).build();
      }
      Properties p = new Properties();
      p.putAll(request.getProperties());
      InterpreterSetting interpreterSetting = interpreterSettingManager
              .createNewSetting(request.getName(), request.getGroup(), request.getDependencies(),
                      request.getOption(), p);
      persistToDB();
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
  public Response updateSetting(String message,
          @PathParam("settingId") String settingId) {
    logger.info("Update interpreterSetting {}", settingId);

    try {
      UpdateInterpreterSettingRequest request = gson.fromJson(message, UpdateInterpreterSettingRequest.class);
      interpreterSettingManager
              .setPropertyAndRestart(settingId, request.getOption(), request.getProperties(),
                      request.getDependencies());
    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
              .build();
    } catch (IOException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
    }
    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    //Persist json to the database
    persistToDB();
    if (setting == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse<>(Status.OK, "", setting).build();
  }

  private void persistToDB() {
    try {
      String s = readConfigFile(new File(zeppelinConf.getConfDirPath()
              + ZeppelinConfig.INTERPRETER_JSON));
      zeppelinInterpreterConfFacade.create(project.getName(), s);
    } catch (IOException ex) {
      java.util.logging.Logger.getLogger(InterpreterRestApi.class.getName()).
              log(Level.SEVERE, null, ex);
    }
  }

  private String readConfigFile(File path) throws IOException {
    // write contents to file as text, not binary data
    if (!path.exists()) {
      throw new IOException("Problem creating file: " + path);
    }
    return new String(Files.readAllBytes(path.toPath()));
  }

  /**
   * Remove interpreter setting
   */
  @DELETE
  @Path("setting/{settingId}")
  public Response removeSetting(@PathParam("settingId") String settingId) throws IOException {
    logger.info("Remove interpreterSetting {}", settingId);
    interpreterSettingManager.remove(settingId);
    persistToDB();
    return new JsonResponse(Status.OK).build();
  }

  /**
   * Restart interpreter setting
   */
  @PUT
  @Path("setting/restart/{settingId}")
  public Response restartSetting(String message,
          @PathParam("settingId") String settingId) {
    logger.info("Restart interpreterSetting {}, msg={}", settingId, message);

    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    try {
      RestartInterpreterRequest request = gson.fromJson(message, RestartInterpreterRequest.class);

      String noteId = request == null ? null : request.getNoteId();
      if (null == noteId) {
        interpreterSettingManager.close(setting);
      } else {
        interpreterSettingManager.restart(settingId, noteId, SecurityUtils.getPrincipal());
      }
      zeppelinConf.getNotebookServer().clearParagraphRuntimeInfo(setting);

    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while restartSetting ", e);
      return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
              .build();
    }
    if (setting == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
    }
    int timeout = zeppelinConf.getConf().getInt(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_CONNECT_TIMEOUT);
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
    InterpreterDTO interpreter = new InterpreterDTO(setting,
            !zeppelinResource.isInterpreterRunning(setting, project));
    return new JsonResponse(Status.OK, "", interpreter).build();
  }

  /**
   * Get livy session Yarn AppId
   *
   * @param sessionId
   * @param settingId
   * @return
   * @throws AppException
   */
  @GET
  @Path("/livy/sessions/appId/{sessionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLivySessionAppId(@PathParam("sessionId") int sessionId)
          throws AppException {
    LivyMsg.Session session = zeppelinResource.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }
    String projName = hdfsUserBean.getProjectName(session.getProxyUser());
    String username = hdfsUserBean.getUserName(session.getProxyUser());
    if (!this.project.getName().equals(projName)) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
              "You can't stop sessions in another project.");
    }
    if (!this.user.getUsername().equals(username) && this.roleInProject.equals(
            AllowedRoles.DATA_SCIENTIST)) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
              "You are not authorized to stop this session.");
    }

    List<YarnApplicationstate> appStates = appStateBean.findByAppname("livy-session-" + sessionId);
    if (appStates == null || appStates.isEmpty()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not running.").build();
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(appStates.get(0).getApplicationid()).build();

  }

  /**
   * Get spark interpreter Yarn AppId
   *
   * @param sessionId
   * @param settingId
   * @return
   * @throws AppException
   */
  @GET
  @Path("/spark/appId")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getSparkSessionAppId()
          throws AppException {
    List<YarnApplicationstate> appStates = appStateBean.findByAppname(this.project.getName() + "-Zeppelin");
    if (appStates == null || appStates.isEmpty()) {
      return new JsonResponse(Response.Status.NOT_FOUND,
              "Zeppelin not running for project " + this.project.getName()).build();
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(appStates.get(0).getApplicationid()).build();

  }

  /**
   * Stop livy session
   *
   * @param sessionId
   * @param settingId
   * @return
   * @throws AppException
   */
  @DELETE
  @Path("/livy/sessions/delete/{settingId}/{sessionId}")
  public Response stopSession(@PathParam("settingId") String settingId,
          @PathParam("sessionId") int sessionId) throws AppException {
    logger.info("Restart interpreterSetting {}", settingId);
    InterpreterSetting setting = interpreterSettingManager.get(settingId);
    LivyMsg.Session session = zeppelinResource.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }
    String projName = hdfsUserBean.getProjectName(session.getProxyUser());
    String username = hdfsUserBean.getUserName(session.getProxyUser());
    if (!this.project.getName().equals(projName)) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
              "You can't stop sessions in another project.");
    }
    if (!this.user.getUsername().equals(username) && this.roleInProject.equals(
            AllowedRoles.DATA_SCIENTIST)) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
              "You are not authorized to stop this session.");
    }
//    List<YarnApplicationstate> yarnAppStates;
//    yarnAppStates = appStateBean.
//            findByAppuserAndAppState(session.getProxyUser(),
//                    "RUNNING");
    List<JobAdministration.YarnApplicationReport> yarnAppStates;
    yarnAppStates = fetchJobs(session.getProxyUser());
    try {
      zeppelinResource.deleteLivySession(sessionId);
      if (this.user.getUsername().equals(username) && yarnAppStates.size() == 1) {
        interpreterSettingManager.restart(settingId);
      } else if (yarnAppStates.size() == 1) {
        Users u = userFacade.findByUsername(username);
        if (u == null) {
          throw new AppException(Status.BAD_REQUEST.getStatusCode(),
                  "The owner of the session was not found.");
        }
        ZeppelinConfig zConf = zeppelinConfFactory.getZeppelinConfig(
                this.project.getName(), u.getEmail());
        if (zConf.getReplFactory() != null) {
          zConf.getInterpreterSettingManager().restart(settingId);
        }
      }
    } catch (InterpreterException e) {
      logger.warn("Could not close interpreter.", e);
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
              "Could not close interpreter. Make sure it is not running.");
    }

    int timeout = zeppelinConf.getConf().getInt(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_CONNECT_TIMEOUT);
    long startTime = System.currentTimeMillis();
    long endTime;
    while (zeppelinResource.isLivySessionAlive(sessionId)) {
      endTime = System.currentTimeMillis();
      if ((endTime - startTime) > (timeout * 2)) {
        break;
      }
    }
    int res = zeppelinResource.deleteLivySession(sessionId);
    if (res != Response.Status.NOT_FOUND.getStatusCode() && res
            != Response.Status.OK.getStatusCode()) {
      return new JsonResponse(Status.EXPECTATION_FAILED,
              "Could not stop session '" + sessionId
              + "'").build();
    }

//    InterpreterDTO interpreter = new InterpreterDTO(setting,
//            !zeppelinResource.isInterpreterRunning(setting, project),
//            getRunningLivySessions(this.project));
    InterpreterDTO interpreter = new InterpreterDTO(setting,
            !zeppelinResource.isInterpreterRunning(setting, project),
            getRunningLivySessionsfromYarnClient(this.project));
    return new JsonResponse(Status.OK, "Deleted ", interpreter).build();
  }

  /**
   * List all available interpreters by group
   */
  @GET
  public Response listInterpreter(String message) {
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
      Repository request = gson.fromJson(message, Repository.class);
      interpreterSettingManager.addRepository(request.getId(), request.getUrl(),
              request.isSnapshot(), request.getAuthentication(), request.getProxy());
      persistToDB();
      logger.info("New repository {} added", request.getId());
    } catch (Exception e) {
      logger.error("Exception in InterpreterRestApi while adding repository ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
    }
    return new JsonResponse(Status.OK).build();
  }

  /**
   * get the metainfo property value
   */
  @GET
  @Path("getmetainfos/{settingId}")
  public Response getMetaInfo(@Context HttpServletRequest req,
          @PathParam("settingId") String settingId) {
    String propName = req.getParameter("propName");
    if (propName == null) {
      return new JsonResponse<>(Status.BAD_REQUEST).build();
    }
    String propValue = null;
    InterpreterSetting interpreterSetting = interpreterSettingManager.get(settingId);
    Map<String, String> infos = interpreterSetting.getInfos();
    if (infos != null) {
      propValue = infos.get(propName);
    }
    Map<String, String> respMap = new HashMap<>();
    respMap.put(propName, propValue);
    logger.debug("Get meta info");
    logger.debug("Interpretersetting Id: {}, property Name:{}, property value: {}", settingId,
            propName, propValue);
    return new JsonResponse<>(Status.OK, respMap).build();
  }

  /**
   * Delete repository
   *
   * @param repoId ID of repository
   */
  @DELETE
  @Path("repository/{repoId}")
  public Response removeRepository(@PathParam("repoId") String repoId) {
    logger.info("Remove repository {}", repoId);
    try {
      interpreterSettingManager.removeRepository(repoId);
      persistToDB();
    } catch (Exception e) {
      logger.error("Exception in InterpreterRestApi while removing repository ", e);
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
    }
    return new JsonResponse(Status.OK).build();
  }

  /**
   * list interpreters with status(running or not).
   * <p/>
   * @return nothing if successful.
   * @throws AppException
   */
  @GET
  @Path("interpretersWithStatus")
  public Response getinterpretersWithStatus()
          throws AppException {
    Map<String, InterpreterDTO> interpreters = interpreters(project);
    return new JsonResponse(Status.OK, "", interpreters).build();
  }

  private Map<String, InterpreterDTO> interpreters(Project project) throws
          AppException {
    Map<String, InterpreterDTO> interpreterDTOs = new HashMap<>();
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = interpreterSettingManager.get();
    InterpreterDTO interpreterDTO;
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO = new InterpreterDTO(interpreter, !zeppelinResource.
              isInterpreterRunning(interpreter, project));
      interpreterDTOs.put(interpreter.getName(), interpreterDTO);
      if (interpreter.getName().contains("livy")) {
        //interpreterDTO.setSessions(getRunningLivySessions(project));
        interpreterDTO.
                setSessions(getRunningLivySessionsfromYarnClient(project));
      }
    }
    return interpreterDTOs;
  }

  private List<LivyMsg.Session> getRunningLivySessions(Project project) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    List<ProjectTeam> projectTeam;
    List<YarnApplicationstate> yarnAppStates;
    String hdfsUsername;
    int id;
    projectTeam = teambean.findMembersByProject(project);
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = hdfsUserBean.getHdfsUserName(project, member.getUser());
      yarnAppStates = appStateBean.findByAppuserAndAppState(hdfsUsername,
              "RUNNING");
      for (YarnApplicationstate state : yarnAppStates) {
        try {
          id = Integer.parseInt(state.getAppname().substring(
                  "livy-session-".length()));
        } catch (NumberFormatException e) {
          continue;
        }
        if (state.getAppname().startsWith("livy-session-")) {
          sessions.add(new LivyMsg.Session(id, member.
                  getUser().getEmail()));
        }
      }
    }
    return sessions;
  }

  /**
   * Restarts zeppelin by cleaning the cache for the user
   * and project
   *
   * @return
   * @throws AppException
   */
  @GET
  @Path("restart")
  public Response restart() throws AppException {
    Long timeSinceLastRestart;
    Long lastRestartTime = zeppelinConfFactory.getLastRestartTime(this.project.
            getName());
    if (lastRestartTime != null) {
      timeSinceLastRestart = System.currentTimeMillis() - lastRestartTime;
      if (timeSinceLastRestart < 60000 * 1) {
        throw new AppException(Status.BAD_REQUEST.getStatusCode(),
                "This service has been restarted recently. "
                + "Please wait a few minutes before trying again.");
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
        } else if (interpreterDTO.getName().equalsIgnoreCase("spark")) {
          restartSetting(null, interpreterDTO.getId());
        }
      }
    }

    zeppelinConfFactory.removeFromCache(this.project.getName());
    List<ProjectTeam> projectTeam;
    projectTeam = teambean.findMembersByProject(this.project);
    for (ProjectTeam member : projectTeam) {
      zeppelinConfFactory.removeFromCache(this.project.getName(), member.
              getUser().getEmail());
      TicketContainer.instance.invalidate(member.getUser().getEmail());
    }

    return new JsonResponse(Status.OK, "Cache cleared.").build();
  }

  //temporary until YarnApplicationstate table fixed
  private List<LivyMsg.Session> getRunningLivySessionsfromYarnClient(
          Project project) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    List<ProjectTeam> projectTeam;
    List<JobAdministration.YarnApplicationReport> yarnAppReport;
    yarnAppReport = fetchJobs();
    String hdfsUsername;
    int id;
    projectTeam = teambean.findMembersByProject(project);
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = hdfsUserBean.getHdfsUserName(project, member.getUser());
      for (JobAdministration.YarnApplicationReport report : yarnAppReport) {
        if (hdfsUsername.equals(report.getUser()) && report.getName().
                startsWith("livy-session-")) {
          try {
            id = Integer.parseInt(report.getName().substring(
                    "livy-session-".length()));
          } catch (NumberFormatException e) {
            continue;
          }
          sessions.add(new LivyMsg.Session(id, member.
                  getUser().getEmail()));
        }
      }
    }
    return sessions;
  }

  private List<JobAdministration.YarnApplicationReport> fetchJobs() {
    JobAdministration jobAdmin = new JobAdministration();
    List<JobAdministration.YarnApplicationReport> reports = new ArrayList<>();
    YarnClient client = YarnClient.createYarnClient();
    Configuration conf = settings.getConfiguration();
    client.init(conf);
    client.start();
    try {
      //Create our custom YarnApplicationReport Pojo
      for (ApplicationReport appReport : client.getApplications(PREDICATE)) {
        reports.add(jobAdmin.new YarnApplicationReport(appReport.
                getApplicationId().
                toString(),
                appReport.getName(), appReport.getUser(), appReport.
                getStartTime(), appReport.getFinishTime(), appReport.
                getApplicationId().getClusterTimestamp(),
                appReport.getApplicationId().getId(), appReport.
                getYarnApplicationState().name()));
      }
    } catch (YarnException | IOException ex) {
      logger.error("", ex);
    }
    return reports;
  }

  private List<JobAdministration.YarnApplicationReport> fetchJobs(
          String username) {
    JobAdministration jobAdmin = new JobAdministration();
    List<JobAdministration.YarnApplicationReport> reports = new ArrayList<>();
    YarnClient client = YarnClient.createYarnClient();
    Configuration conf = settings.getConfiguration();
    client.init(conf);
    client.start();
    try {
      //Create our custom YarnApplicationReport Pojo
      for (ApplicationReport appReport : client.getApplications(PREDICATE)) {
        if (username.equals(appReport.getUser())) {
          reports.add(jobAdmin.new YarnApplicationReport(appReport.
                  getApplicationId().
                  toString(),
                  appReport.getName(), appReport.getUser(), appReport.
                  getStartTime(), appReport.getFinishTime(), appReport.
                  getApplicationId().getClusterTimestamp(),
                  appReport.getApplicationId().getId(), appReport.
                  getYarnApplicationState().name()));
        }
      }
    } catch (YarnException | IOException ex) {
      logger.error("", ex);
    }
    return reports;
  }

}
