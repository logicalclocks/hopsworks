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
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;

/**
 * Interpreter Rest API
 *
 */
@Path("/interpreter")
@Produces("application/json")
public class InterpreterRestApi {

  Logger logger = LoggerFactory.getLogger(InterpreterRestApi.class);

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
   * @return
   */
  @GET
  @Path("start/{settingId}")
  public Response start() {
    List<InterpreterSetting> interpreterSettings;
    interpreterSettings = interpreterFactory.get();
    return new JsonResponse(Status.OK, "", interpreterSettings).build();
  }
  
   /**
   * stop an interpreter
   * <p>
   * @param settingId
   * @return nothing if successful. 
   */
  @GET
  @Path("stop/{settingId}")
  public Response stop(@PathParam("settingId") String settingId) {
    logger.info("Stoping interpreterSetting {}", settingId);
    try {
     interpreterFactory.restart(settingId);
    } catch (InterpreterException e) {
      return new JsonResponse(Status.BAD_REQUEST, e.getMessage(), e).build();
    }
    return new JsonResponse(Status.OK).build();
  }
  
   /**
   * list running interpreters
   * <p>
   * @return nothing if successful. 
   * @throws se.kth.hopsworks.rest.AppException 
   */
  @GET
  @Path("running")
  public Response getRunning() throws AppException {
    ZeppelinConfiguration conf = this.zeppelin.getConf();
    URI filesystemRoot;
    String pidPath = conf.getInterpreterSettingPath();
    try {
      filesystemRoot = new URI(conf.getInterpreterSettingPath());
    } catch (URISyntaxException e1) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e1.getMessage());
    }

    if (filesystemRoot.getScheme() == null) { // it is local path
      try {
        filesystemRoot = new URI(new File(
                conf.getRelativeDir(filesystemRoot.getPath())).getAbsolutePath());
      } catch (URISyntaxException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e.getMessage());
      }
    }
    return new JsonResponse(Status.OK).build();
  }
}
