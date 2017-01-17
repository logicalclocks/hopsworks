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

import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Notebook;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 * Configurations Rest API Endpoint
 */
@Path("/zeppelin/{projectID}/configurations")
@Stateless
@Produces("application/json")
@Api(value = "Zeppelin configurations",
        description = "Zeppelin configurations")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class ConfigurationsRestApi {

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;

  private Notebook notebook;

  public ConfigurationsRestApi() {
  }

  public ConfigurationsRestApi(Notebook notebook) {
    this.notebook = notebook;
  }

  @GET
  @Path("all")
  public Response getAll(@PathParam("projectID") String projectID,
          @Context HttpServletRequest httpReq) throws
          AppException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());

    String userRole = projectTeamBean.findCurrentRole(project, user);

    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName(), user.getEmail());
    if (zeppelinConf == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }
    ZeppelinConfiguration conf = zeppelinConf.getNotebook().getConf();

    Map<String, String> configurations = conf.dumpConfigurations(conf,
            new ZeppelinConfiguration.ConfigurationKeyPredicate() {
          @Override
          public boolean apply(String key) {
            return !key.contains("password") && !key.equals(
                    ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_AZURE_CONNECTION_STRING.
                    getVarName());
          }
        });
    return new JsonResponse(Status.OK, "", configurations).build();
  }

  @GET
  @Path("prefix/{prefix}")
  public Response getByPrefix(@PathParam("prefix") final String prefix,
          @Context HttpServletRequest httpReq) throws AppException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    if (project == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());

    String userRole = projectTeamBean.findCurrentRole(project, user);

    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName(), user.getEmail());
    if (zeppelinConf == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }
    ZeppelinConfiguration conf = zeppelinConf.getNotebook().getConf();

    Map<String, String> configurations = conf.dumpConfigurations(conf,
            new ZeppelinConfiguration.ConfigurationKeyPredicate() {
          @Override
          public boolean apply(String key) {
            return !key.contains("password") && !key.equals(
                    ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_AZURE_CONNECTION_STRING.
                    getVarName()) && key.startsWith(prefix);
          }
        });
    return new JsonResponse(Status.OK, "", configurations).build();
  }

}
