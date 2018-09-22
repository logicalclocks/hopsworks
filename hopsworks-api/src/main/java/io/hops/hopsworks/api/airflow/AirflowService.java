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
package io.hops.hopsworks.api.airflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AirflowService {

  private final static Logger LOGGER = Logger.getLogger(AirflowService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;

  private Integer projectId;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;

  public AirflowService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Path("copyFromAirflowToHdfs")
  @Produces(MediaType.TEXT_PLAIN)
  public Response copyFromAirflowToHdfs(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    copyHdfsAirflow(false, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("copyToAirflowFromHdfs")
  @Produces(MediaType.TEXT_PLAIN)
  public Response copyToAirflowFromHdfs(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    copyHdfsAirflow(true, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();

  }

  public boolean copyHdfsAirflow(boolean toHdfs, String projectUsername) {

    try {
      String script = settings.getHopsworksDomainDir() + "/bin/copyHdfsAirflow.sh";
      String copyCommand = toHdfs ? "toHdfs" : "fromHdfs";

      String[] command = {script, copyCommand, project.getName(), projectUsername};

      ProcessBuilder ps = new ProcessBuilder(command);
      ps.redirectErrorStream(true);
      Process pr = ps.start();
      BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        sb.append(line);
      }
      pr.waitFor();
      in.close();
    } catch (Exception ex) {
      Logger.getLogger(AirflowService.class.getName()).log(Level.SEVERE, null, ex);
    }
    return true;
  }

}
