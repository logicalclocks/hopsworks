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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.Interpreter.RegisteredInterpreter;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterOption;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.rest.message.NewInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

/**
 * Interpreter Rest API
 *
 */
@Path("/interpreter")
@Produces("application/json")
public class InterpreterRestApi {

  Logger logger = LoggerFactory.getLogger(InterpreterRestApi.class);
  @EJB
  private ProjectController projectController;
  @EJB
  private ZeppelinResource zeppelinResource;
  private final InterpreterFactory interpreterFactory;
  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;

  Gson gson = new Gson();

  public InterpreterRestApi() {
    this.interpreterFactory = zeppelin.getReplFactory();
  }

  public InterpreterRestApi(InterpreterFactory interpreterFactory) {
    this.interpreterFactory = interpreterFactory;
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
    interpreterSettings = interpreterFactory.get();
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
    InterpreterGroup interpreterGroup = interpreterFactory.
            add(request.getName(),
                    request.getGroup(), new InterpreterOption(true), p);
    InterpreterSetting setting = interpreterFactory.
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
      interpreterFactory.setPropertyAndRestart(settingId,
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
    InterpreterSetting setting = interpreterFactory.get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse(Status.OK, "", setting).build();
  }

  /**
   * Remove interpreter setting
   * @param settingId
   * @return 
   * @throws java.io.IOException 
   */
  @DELETE
  @Path("setting/{settingId}")
  public Response removeSetting(@PathParam("settingId") String settingId) throws
          IOException {
    logger.info("Remove interpreterSetting {}", settingId);
    interpreterFactory.remove(settingId);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * Restart interpreter setting
   * @param settingId
   * @return 
   */
  @PUT
  @Path("setting/restart/{settingId}")
  public Response restartSetting(@PathParam("settingId") String settingId) {
    logger.info("Restart interpreterSetting {}", settingId);
    try {
      interpreterFactory.restart(settingId);
    } catch (InterpreterException e) {
      return new JsonResponse(Status.NOT_FOUND, e.getMessage(), e).build();
    }
    InterpreterSetting setting = interpreterFactory.get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse(Status.OK, "", setting).build();
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
   * Start an interpreter
   * <p/>
   * @param id
   * @param settingId
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("{id}/start/{settingId}")
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response start(@PathParam("id") Integer id,
          @PathParam("settingId") String settingId) throws
          AppException {
    logger.info("Starting interpreterSetting {}", settingId);
    InterpreterSetting interpreterSetting = interpreterFactory.get(settingId);
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    NotebookRepo notebookRepo;
    Notebook newNotebook;
    Note note;
    try {
      notebookRepo = zeppelinResource.setupNotebookRepo(project);
      newNotebook = new Notebook(notebookRepo);
      note = newNotebook.createNote();
    } catch (IOException | SchedulerException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create notebook" + ex.getMessage());
    }
    Paragraph p = note.addParagraph(); // it's an empty note. so add one paragraph
    if (interpreterSetting.getGroup().equalsIgnoreCase("spark")) {
      p.setText(" ");
    } else {
      p.setText("%" + interpreterSetting.getGroup() + " ");
    }

    note.run(p.getId());
    //wait until starting job terminates
    while (!note.getParagraph(p.getId()).isTerminated()) {
    }
    newNotebook.removeNote(note.id());

    InterpreterDTO interpreterDTO
            = new InterpreterDTO(interpreterSetting, false);
    return new JsonResponse(Status.OK, "", interpreterDTO).build();
  }

  /**
   * stop an interpreter
   * <p/>
   * @param settingId
   * @return nothing if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("stop/{settingId}")
  public Response stop(@PathParam("settingId") String settingId) throws
          AppException {
    logger.info("Stoping interpreterSetting {}", settingId);
    try {
      interpreterFactory.restart(settingId);
    } catch (InterpreterException e) {
      return new JsonResponse(Status.BAD_REQUEST, e.getMessage(), e).build();
    }

    InterpreterSetting interpreterSetting = interpreterFactory.get(settingId);
    //wait until the peocess is stoped.
    while (zeppelinResource.isInterpreterRunning(interpreterSetting)) {
      logger.info("Infinite loop???");
    }

    InterpreterDTO interpreter = new InterpreterDTO(interpreterSetting, true);
    return new JsonResponse(Status.OK, "", interpreter).build();
  }

  /**
   * list interpreters with status(running or not).
   * <p/>
   * @return nothing if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("interpretersWithStatus")
  public Response getinterpretersWithStatus() throws AppException {
    Map<String, InterpreterDTO> interpreters = null;
    interpreters = interpreters();
    return new JsonResponse(Status.OK, "", interpreters).build();
  }

  private Map<String, InterpreterDTO> interpreters() throws AppException {
    Map<String, InterpreterDTO> interpreterDTO = new HashMap<>();
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = interpreterFactory.get();
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO.put(interpreter.getGroup(), new InterpreterDTO(interpreter,
              !zeppelinResource.isInterpreterRunning(interpreter)));
    }
    return interpreterDTO;
  }

}
