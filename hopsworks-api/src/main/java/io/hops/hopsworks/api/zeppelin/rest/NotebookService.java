/*
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
 *
 */

package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.swagger.annotations.Api;
import java.io.IOException;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/zeppelin/{projectID}/notebook")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Zeppelin notebook",
        description = "Zeppelin notebook")
public class NotebookService {

  Logger logger = LoggerFactory.getLogger(NotebookService.class);

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private HdfsUsersController hdfsController;
  @Inject
  private NotebookRestApi notebookRestApi;

  @Path("/")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public NotebookRestApi interpreter(@PathParam("projectID") String projectID,
          @Context HttpServletRequest httpReq) throws
          AppException, IOException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find remote user.");
    }
    String userRole = projectTeamBean.findCurrentRole(project, user);
    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }

    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getProjectConf(project.getName());
    if (zeppelinConf == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }
    //try if we can list notebooks. Will throw exception if notebook dir is not there.
    if (zeppelinConf.getNotebook() == null) {
      zeppelinConf.getNotebookRepo().list(AuthenticationInfo.ANONYMOUS);
    }

    notebookRestApi.setParms(project, hdfsController.getHdfsUserName(
            project, user), zeppelinConf);
    return notebookRestApi;
  }

}
