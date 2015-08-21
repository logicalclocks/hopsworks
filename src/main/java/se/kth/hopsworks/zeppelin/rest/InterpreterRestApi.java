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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.interpreter.Interpreter.RegisteredInterpreter;
import se.kth.hopsworks.zeppelin.rest.message.NewInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Level;
import javax.ejb.EJB;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.quartz.SchedulerException;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;

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
   * <p>
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
   * <p>
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
    interpreterFactory.add(request.getName(), request.getGroup(), request.
            getOption(), p);
    return new JsonResponse(Status.CREATED, "").build();
  }

  @PUT
  @Path("setting/{settingId}")
  public Response updateSetting(String message,
          @PathParam("settingId") String settingId) {
    logger.info("Update interpreterSetting {}", settingId);

    try {
      UpdateInterpreterSettingRequest p = gson.fromJson(message,
              UpdateInterpreterSettingRequest.class);
      interpreterFactory.setPropertyAndRestart(settingId, p.getOption(), p.
              getProperties());
    } catch (InterpreterException e) {
      return new JsonResponse(Status.NOT_FOUND, e.getMessage(), e).build();
    } catch (IOException e) {
      return new JsonResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage(), e).
              build();
    }
    InterpreterSetting setting = interpreterFactory.get(settingId);
    if (setting == null) {
      return new JsonResponse(Status.NOT_FOUND, "", settingId).build();
    }
    return new JsonResponse(Status.OK, "", setting).build();
  }

  @DELETE
  @Path("setting/{settingId}")
  public Response removeSetting(@PathParam("settingId") String settingId) throws
          IOException {
    logger.info("Remove interpreterSetting {}", settingId);
    interpreterFactory.remove(settingId);
    return new JsonResponse(Status.OK).build();
  }

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
   * <p>
   * @return
   */
  @GET
  public Response listInterpreter() {
    Map<String, RegisteredInterpreter> m = Interpreter.registeredInterpreters;
    return new JsonResponse(Status.OK, "", m).build();
  }

  /**
   * Start an interpreter
   * <p>
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
      notebookRepo = setupNotebookRepo(project);
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
   * <p>
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
    //wait until the pid file is removed
    while (!isNotRunning(interpreterSetting)) {} //too risky!
    InterpreterDTO interpreter = new InterpreterDTO(interpreterSetting, true);
    return new JsonResponse(Status.OK, "", interpreter).build();
  }

  /**
   * list interpreters with status(running or not).
   * <p>
   * @return nothing if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("interpretersWithStatus")
  public Response getinterpretersWithStatus() throws AppException {
    Map<String, InterpreterDTO> interpreters = runningInterpreters();
    return new JsonResponse(Status.OK, "", interpreters).build();
  }

  private Map<String, InterpreterDTO> runningInterpreters() throws AppException {
    Map<String, InterpreterDTO> interpreterDTO = new HashMap<>();
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = interpreterFactory.get();
    for (InterpreterSetting interpreter : interpreterSettings) {
      interpreterDTO.put(interpreter.getGroup(), new InterpreterDTO(interpreter,
              isNotRunning(
                      interpreter)));
    }
    return interpreterDTO;
  }

  private boolean isNotRunning(InterpreterSetting interpreter) throws
          AppException {
    ZeppelinConfiguration conf = this.zeppelin.getConf();
    String binPath = conf.getRelativeDir("bin");
    FileObject[] pidFiles = getPidFiles();
    boolean notRunning;
    notRunning = true;
    for (FileObject file : pidFiles) {
      if (file.getName().toString().contains(interpreter.getGroup())) {
        notRunning = !isProccessAlive(binPath + "/alive.sh", readPid(file));
        break;
      }
    }
    return notRunning;
  }

  private FileObject[] getPidFiles() throws AppException {
    ZeppelinConfiguration conf = this.zeppelin.getConf();
    URI filesystemRoot;
    FileSystemManager fsManager;
    String runPath = conf.getRelativeDir("run");//the string run should be a constant.
    try {
      filesystemRoot = new URI(runPath);
    } catch (URISyntaxException e1) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e1.getMessage());
    }

    if (filesystemRoot.getScheme() == null) { // it is local path
      try {
        filesystemRoot = new URI(new File(runPath).getAbsolutePath());
      } catch (URISyntaxException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                e.getMessage());
      }
    }
    FileObject[] pidFiles = null;
    try {
      fsManager = VFS.getManager();
      pidFiles = fsManager.resolveFile(filesystemRoot.toString() + "/").
              getChildren();
    } catch (FileSystemException ex) {
      logger.error("Failed to load pid files", ex);
    }
    return pidFiles;
  }

  private NotebookRepo setupNotebookRepo(Project project) throws AppException {
    ZeppelinConfiguration conf = zeppelin.getConf();
    Class<?> notebookStorageClass;
    NotebookRepo repo;
    try {
      notebookStorageClass = Class.forName(conf.getString(
              ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE));
      Constructor<?> constructor = notebookStorageClass.getConstructor(
              ZeppelinConfiguration.class, Project.class);
      repo = (NotebookRepo) constructor.newInstance(conf, project);

    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
            InstantiationException | IllegalAccessException |
            IllegalArgumentException | InvocationTargetException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not instantiate notebook" + ex.getMessage());
    }

    return repo;
  }

  private boolean isProccessAlive(String bashPath, String pid) {
    ProcessBuilder pb = new ProcessBuilder(bashPath, pid);
    int exitValue;
    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
    } catch (IOException | InterruptedException ex) {
      return false;
    }
    return exitValue == 0;
  }

  private String readPid(FileObject file) {
    //pid value can only be extended up to a theoretical maximum of 
    //32768 for 32 bit systems or 4194304 for 64 bit:
    byte[] pid = new byte[8];
    try {
      file.getContent().getInputStream().read(pid);
    } catch (FileSystemException ex) {
      return null;
    } catch (IOException ex) {
      return null;
    }
    String s;
    try {
      s = new String(pid, "UTF-8").trim();
    } catch (UnsupportedEncodingException ex) {
      return null;
    }
    return s;
  }
}
