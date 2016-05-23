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

import com.google.gson.Gson;
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
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterOption;
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

/**
 * Interpreter Rest API
 * <p>
 */
@RequestScoped
public class InterpreterRestApi {

  Logger logger = LoggerFactory.getLogger(InterpreterRestApi.class);
  Project project;
  ZeppelinConfig zeppelinConf;
  String roleInProject;
  
  @EJB
  private ZeppelinResource zeppelinResource;

  Gson gson = new Gson();

  public InterpreterRestApi() {
  }
  
  public void setParms(Project project, String userRole, ZeppelinConfig zeppelinConf) {
    this.project = project;
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
   * <p/>
   * @param message
   * @return
   * @throws IOException
   * @throws InterpreterException
   */
  @POST
  @Path("setting")
  public Response newSettings(String message) throws InterpreterException,
          IOException {
    NewInterpreterSettingRequest request = gson.fromJson(message,
            NewInterpreterSettingRequest.class);
    Properties p = new Properties();
    p.putAll(request.getProperties());
    // Option is deprecated from API, always use remote = true
    InterpreterGroup interpreterGroup = zeppelinConf.getReplFactory().
            add(request.getName(),
                    request.getGroup(), new InterpreterOption(true), p);
    InterpreterSetting setting = zeppelinConf.getReplFactory().
            get(interpreterGroup.getId());
    logger.info("new setting created with " + setting.id());
    return new JsonResponse(Status.CREATED, "", setting).build();
  }

  @PUT
  @Path("setting/{settingId}")
  public Response updateSetting(String message,
          @PathParam("settingId") String settingId) {
    logger.info("Update interpreterSetting {}", settingId);
    try {
      UpdateInterpreterSettingRequest p = gson.fromJson(message,
              UpdateInterpreterSettingRequest.class);
      // Option is deprecated from API, always use remote = true
      zeppelinConf.getReplFactory().setPropertyAndRestart(settingId,
              new InterpreterOption(true), p.getProperties());
    } catch (InterpreterException e) {
      return new JsonResponse(
              Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e)).
              build();
    } catch (IOException e) {
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
    try {
      zeppelinConf.getReplFactory().restart(settingId);
    } catch (InterpreterException e) {
      return new JsonResponse(Status.NOT_FOUND, e.getMessage(), e).build();
    }
    InterpreterSetting setting = zeppelinConf.getReplFactory().get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    int timeout = zeppelinConf.getConf().getInt(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_CONNECT_TIMEOUT);
    long startTime = System.currentTimeMillis();
    long endTime;
    while (zeppelinResource.isInterpreterRunning(setting, project)) {
      endTime = System.currentTimeMillis();
      if ((endTime - startTime) > (timeout*2)) {
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
    Map<String, InterpreterDTO> interpreterDTO = new HashMap<>();
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = zeppelinConf.getReplFactory().get();
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO.put(interpreter.getGroup(), new InterpreterDTO(interpreter,
              !zeppelinResource.isInterpreterRunning(interpreter, project)));
    }
    return interpreterDTO;
  }

}
