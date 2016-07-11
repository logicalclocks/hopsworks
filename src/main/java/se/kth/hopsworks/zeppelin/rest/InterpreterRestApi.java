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
package se.kth.hopsworks.zeppelin.rest;

import java.io.IOException;
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
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.Interpreter.RegisteredInterpreter;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.rest.message.NewInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

import com.google.gson.Gson;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Produces;
import org.apache.zeppelin.dep.Repository;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.repository.RemoteRepository;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstate;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstateFacade;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
import se.kth.hopsworks.zeppelin.util.LivyMsg;
import se.kth.hopsworks.zeppelin.util.TicketContainer;

/**
 * Interpreter Rest API
 * <p>
 */
@RequestScoped
public class InterpreterRestApi {

  Logger logger = LoggerFactory.getLogger(InterpreterRestApi.class);
  Project project;
  Users user;
  ZeppelinConfig zeppelinConf;
  String roleInProject;

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

  Gson gson = new Gson();

  public InterpreterRestApi() {
  }

  public void setParms(Project project, Users user, String userRole,
          ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.user = user;
    this.zeppelinConf = zeppelinConf;
    this.roleInProject = userRole;
  }

  /**
   * List all interpreter settings
   * <p/>
   * @return
   */
  @GET
  @Path("setting")
  public Response listSettings() {
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = zeppelinConf.getReplFactory().get();
    return new JsonResponse(Status.OK, "", interpreterSettings).build();
  }

  /**
   * Add new interpreter setting
   *
   * @param message
   * @return
   * @throws InterpreterException
   */
  @POST
  @Path("setting")
  public Response newSettings(String message) {
    try {
      NewInterpreterSettingRequest request = gson.fromJson(message,
              NewInterpreterSettingRequest.class);
      Properties p = new Properties();
      p.putAll(request.getProperties());
      // Option is deprecated from API, always use remote = true
      InterpreterSetting interpreterSetting = zeppelinConf.getReplFactory().
              add(request.getName(),
                      request.getGroup(),
                      request.getDependencies(),
                      request.getOption(),
                      p);
      logger.info("new setting created with {}", interpreterSetting.id());
      return new JsonResponse(Status.CREATED, "", interpreterSetting).build();
    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while creating ", e);
      return new JsonResponse(
              Status.NOT_FOUND,
              e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
    } catch (IOException | RepositoryException e) {
      logger.error("Exception in InterpreterRestApi while creating ", e);
      return new JsonResponse(
              Status.INTERNAL_SERVER_ERROR,
              e.getMessage(),
              ExceptionUtils.getStackTrace(e)).build();
    }
  }

  @PUT
  @Path("setting/{settingId}")
  public Response updateSetting(String message,
          @PathParam("settingId") String settingId) {
    logger.info("Update interpreterSetting {}", settingId);
    try {
      UpdateInterpreterSettingRequest request = gson.fromJson(message,
              UpdateInterpreterSettingRequest.class);
      // Option is deprecated from API, always use remote = true
      zeppelinConf.getReplFactory().setPropertyAndRestart(settingId,
              request.getOption(),
              request.getProperties(),
              request.getDependencies());
    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse(
              Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e)).
              build();
    } catch (IOException | RepositoryException e) {
      logger.error("Exception in InterpreterRestApi while updateSetting ", e);
      return new JsonResponse(
              Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.
              getStackTrace(e)).build();
    }
    InterpreterSetting setting = zeppelinConf.getReplFactory().get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse(Status.OK, "", setting).build();
  }

  /**
   * Remove interpreter setting
   *
   * @param settingId
   * @return
   * @throws java.io.IOException
   */
  @DELETE
  @Path("setting/{settingId}")
  public Response removeSetting(@PathParam("settingId") String settingId) throws
          IOException {
    logger.info("Remove interpreterSetting {}", settingId);
    zeppelinConf.getReplFactory().remove(settingId);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * Restart interpreter setting
   *
   * @param settingId
   * @return
   */
  @PUT
  @Path("setting/restart/{settingId}")
  public Response restartSetting(@PathParam("settingId") String settingId) {
    logger.info("Restart interpreterSetting {}", settingId);
    InterpreterSetting setting = zeppelinConf.getReplFactory().get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    try {
      zeppelinConf.getReplFactory().restart(settingId);
    } catch (InterpreterException e) {
      logger.error("Exception in InterpreterRestApi while restartSetting ", e);
      return new JsonResponse(
              Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e)).
              build();
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
   * Stop livy session
   *
   * @param sessionId
   * @param settingId
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @DELETE
  @Path("/livy/sessions/delete/{settingId}/{sessionId}")
  public Response stopSession(@PathParam("settingId") String settingId,
          @PathParam("sessionId") int sessionId) throws AppException {
    logger.info("Restart interpreterSetting {}", settingId);
    InterpreterSetting setting = zeppelinConf.getReplFactory().get(settingId);
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
              "You can't stop this session.");
    }

    int res = zeppelinResource.deleteLivySession(sessionId);
    if (res == Response.Status.NOT_FOUND.getStatusCode()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }

    InterpreterDTO interpreter = new InterpreterDTO(setting,
            !zeppelinResource.isLivySessionAlive(sessionId));
    return new JsonResponse(Status.OK, "Deleted ", interpreter).build();
  }

  /**
   * List all available interpreters by group
   * <p/>
   * @return
   */
  @GET
  public Response listInterpreter() {
    Map<String, RegisteredInterpreter> m = Interpreter.registeredInterpreters;
    return new JsonResponse(Status.OK, "", m).build();
  }

  /**
   * List of dependency resolving repositories
   *
   * @return
   */
  @GET
  @Path("repository")
  public Response listRepositories() {
    List<RemoteRepository> interpreterRepositories = null;
    interpreterRepositories = zeppelinConf.getReplFactory().getRepositories();
    return new JsonResponse(Status.OK, "", interpreterRepositories).build();
  }

  /**
   * Add new repository
   *
   * @param message
   * @return
   */
  @POST
  @Path("repository")
  public Response addRepository(String message) {
    try {
      Repository request = gson.fromJson(message, Repository.class);
      zeppelinConf.getReplFactory().addRepository(
              request.getId(),
              request.getUrl(),
              request.isSnapshot(),
              request.getAuthentication());
      logger.info("New repository {} added", request.getId());
    } catch (Exception e) {
      logger.
              error("Exception in InterpreterRestApi while adding repository ",
                      e);
      return new JsonResponse(
              Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.
              getStackTrace(e)).build();
    }
    return new JsonResponse(Status.CREATED).build();
  }

  /**
   * Delete repository
   *
   * @param repoId
   * @return
   */
  @DELETE
  @Path("repository/{repoId}")
  public Response removeRepository(@PathParam("repoId") String repoId) {
    logger.info("Remove repository {}", repoId);
    try {
      zeppelinConf.getReplFactory().removeRepository(repoId);
    } catch (Exception e) {
      logger.error("Exception in InterpreterRestApi while removing repository ",
              e);
      return new JsonResponse(
              Status.INTERNAL_SERVER_ERROR, e.getMessage(), ExceptionUtils.
              getStackTrace(e)).build();
    }
    return new JsonResponse(Status.OK).build();
  }

  /**
   * list interpreters with status(running or not).
   * <p/>
   * @return nothing if successful.
   * @throws se.kth.hopsworks.rest.AppException
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
    interpreterSettings = zeppelinConf.getReplFactory().get();
    List<ProjectTeam> projectTeam;
    InterpreterDTO interpreterDTO;
    List<YarnApplicationstate> yarnAppStates;
    int id;
    String hdfsUsername;
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO = new InterpreterDTO(interpreter, !zeppelinResource.
              isInterpreterRunning(interpreter, project));
      interpreterDTOs.put(interpreter.getGroup(), interpreterDTO);
      if (interpreter.getGroup().contains("livy")) {
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
              interpreterDTO.getSessions().add(new LivyMsg.Session(id, member.
                      getUser().getEmail()));
            }
          }
        }
      }
    }
    return interpreterDTOs;
  }

  @GET
  @Path("restart")
  public Response restart() {
    zeppelinConfFactory.removeFromCache(this.project.getName());
    zeppelinConfFactory.removeFromCache(this.project.getName(), this.user.
            getEmail());
    TicketContainer.instance.invalidate(this.user.getEmail());
    return new JsonResponse(Status.OK, "Cache cleared.").build();
  }

}
