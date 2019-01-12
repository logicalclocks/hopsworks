/*
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
 */
package io.hops.hopsworks.api.airflow;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
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
import org.apache.commons.codec.digest.DigestUtils;

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

  private static enum AirflowOp {
    TO_HDFS,
    FROM_HDFS,
    PURGE_LOCAL,
    RESTART_WEBSERVER
  };

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
  @Path("secretDir")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response secretDir(@Context HttpServletRequest req) {
    String secret = DigestUtils.sha256Hex(Integer.toString(this.projectId));

    String baseDir = settings.getAirflowDir() + "/dags/hopsworks/";
    String destDir = baseDir + secret;
    Set<PosixFilePermission> xOnly = new HashSet<>();
    xOnly.add(PosixFilePermission.OWNER_WRITE);
    xOnly.add(PosixFilePermission.OWNER_READ);
    xOnly.add(PosixFilePermission.OWNER_EXECUTE);
    xOnly.add(PosixFilePermission.GROUP_WRITE);
    xOnly.add(PosixFilePermission.GROUP_EXECUTE);

    Set<PosixFilePermission> perms = new HashSet<>();
    //add owners permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_WRITE);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    perms.add(PosixFilePermission.OTHERS_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);

    Response.Status response = Response.Status.OK;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    
    try {
      // Instead of checking and setting the permissions, just set them as it is an idempotent operation
      new File(baseDir).mkdirs();      
//      Files.setPosixFilePermissions(Paths.get(baseDir), xOnly);

      // Instead of checking and setting the permissions, just set them as it is an idempotent operation
      new File(destDir).mkdirs();
      Files.setPosixFilePermissions(Paths.get(destDir), perms);
      json.setData(secret);
    } catch (IOException ex) {
      Logger.getLogger(AirflowService.class.getName()).
          log(Level.SEVERE, null, "Could not set permissions on file " + ex);
      response = Response.Status.INTERNAL_SERVER_ERROR;
    }

    return noCacheResponse.getNoCacheResponseBuilder(response).entity(json).build();
  }

  @GET
  @Path("purgeAirflowDagsLocal")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response purgeAirflowDagsLocal(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    airflowOperation(AirflowOp.PURGE_LOCAL, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("restartWebserver")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response restartAirflowWebserver(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    airflowOperation(AirflowOp.RESTART_WEBSERVER, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("copyFromAirflowToHdfs")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response copyFromAirflowToHdfs(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    airflowOperation(AirflowOp.TO_HDFS, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("copyToAirflowFromHdfs")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response copyToAirflowFromHdfs(@Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    airflowOperation(AirflowOp.FROM_HDFS, projectUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();

  }

  public boolean airflowOperation(AirflowOp op, String projectUsername) {

    try {
      Logger.getLogger(AirflowService.class.getName()).log(Level.INFO, "Attempting to restart airflow webserver");
      String script = settings.getHopsworksDomainDir() + "/bin/airflowOps.sh";
      String copyCommand = op.toString();

      String[] command;
      if (op == op.RESTART_WEBSERVER) {
        String[] c = {"sudo", script, copyCommand, project.getName(), projectUsername};
        command = c;
      } else {
        String[] c = {script, copyCommand, project.getName(), projectUsername};
        command = c;
      }
      Logger.getLogger(AirflowService.class.getName()).log(Level.INFO, "Command: {0}", command);
      ProcessBuilder ps = new ProcessBuilder(command);
      ps.redirectErrorStream(true);
      Process pr = ps.start();
      BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        sb.append(line);
      }
      Logger.getLogger(AirflowService.class.getName()).log(Level.INFO, sb.toString());
      int res = pr.waitFor();
      in.close();
      if (res == 0) {
        Logger.getLogger(AirflowService.class.getName()).log(Level.INFO, "Successfully ran command: {0}", op);
      } else {
        Logger.getLogger(AirflowService.class.getName()).log(Level.WARNING, "Problem running the command: {0}", op);
      }
    } catch (Exception ex) {
      Logger.getLogger(AirflowService.class.getName()).log(Level.SEVERE, "Problem with the command: " + op, ex);
    }
    return true;
  }

}
