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

package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/zeppelin/{projectID}/interpreter")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Zeppelin interpreter",
        description = "Zeppelin interpreter")
public class InterpreterService {

  Logger logger = LoggerFactory.getLogger(InterpreterService.class);

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @Inject
  private InterpreterRestApi interpreterRestApi;
  @EJB
  private LivyController livyService;

  @Path("/")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public InterpreterRestApi interpreter(@PathParam("projectID") String projectID, @Context HttpServletRequest httpReq)
          throws AppException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      logger.error("Could not find project in cookies.");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      logger.error("Could not find remote user in request.");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "Could not find remote user.");
    }
    if (!httpReq.isUserInRole("HOPS_ADMIN")) {
      String userRole = projectTeamBean.findCurrentRole(project, user);
      if (userRole == null) {
        logger.error("User with no role in this project.");
        throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "You curently have no role in this project!");
      }
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getProjectConf(project.getName());
    if (zeppelinConf == null) {
      logger.error("Could not connect to web socket.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Could not connect to web socket.");
    }
    interpreterRestApi.setParms(project, user, zeppelinConf);
    return interpreterRestApi;
  }

  @GET
  @Path("/check")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public Response interpreterCheck(@PathParam("projectID") String projectID, @Context HttpServletRequest httpReq) throws
          AppException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      logger.error("Could not find project in cookies.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      logger.error("Could not find remote user in request.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    
    if (!httpReq.isUserInRole("HOPS_ADMIN")) {
      String userRole = projectTeamBean.findCurrentRole(project, user);
      if (userRole == null) {
        logger.error("User with no role in this project.");
        return new JsonResponse(Response.Status.NOT_FOUND, "").build();
      }
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getProjectConf(project.getName());
    if (zeppelinConf == null) {
      logger.error("Zeppelin  not connect to web socket.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    return new JsonResponse(Response.Status.OK, "").build();
  }

  @GET
  @Path("/livy/sessions")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSessions(@PathParam("projectID") String projectID) {
    LivyMsg sessions = livyService.getLivySessions();
    return new JsonResponse(Response.Status.OK, "", sessions).build();
  }

  @GET
  @Path("/livy/sessions/{sessionId}")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSession(@PathParam("projectID") String projectID, @PathParam("sessionId") int sessionId) {
    LivyMsg.Session session = livyService.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId + "' not found.").build();
    }
    return new JsonResponse(Response.Status.OK, "", session).build();
  }

  @DELETE
  @Path("/livy/sessions/delete/{sessionId}")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response deleteSession(@PathParam("projectID") String projectID, @PathParam("sessionId") int sessionId) {
    int res = livyService.deleteLivySession(sessionId);
    if (res == Response.Status.NOT_FOUND.getStatusCode()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId + "' not found.").build();
    }
    return new JsonResponse(Response.Status.OK, "").build();
  }

}
