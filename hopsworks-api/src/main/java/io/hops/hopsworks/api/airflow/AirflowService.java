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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.airflow.AirflowJWTManager;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AirflowException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.JWTException;
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
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private AirflowJWTManager airflowJWTManager;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  private Integer projectId;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;

  private static enum AirflowOp {
    TO_HDFS,
    FROM_HDFS,
    PURGE_LOCAL,
    RESTART_WEBSERVER
  };
  
  private static final Set<PosixFilePermission> DAGS_PERM = new HashSet<>(8);
  private static final Set<PosixFilePermission> SECRETS_PERM = new HashSet<>();
  static {
    //add owners permission
    DAGS_PERM.add(PosixFilePermission.OWNER_READ);
    DAGS_PERM.add(PosixFilePermission.OWNER_WRITE);
    DAGS_PERM.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    DAGS_PERM.add(PosixFilePermission.GROUP_READ);
    DAGS_PERM.add(PosixFilePermission.GROUP_WRITE);
    DAGS_PERM.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    DAGS_PERM.add(PosixFilePermission.OTHERS_READ);
    DAGS_PERM.add(PosixFilePermission.OTHERS_EXECUTE);
    
    SECRETS_PERM.add(PosixFilePermission.OWNER_READ);
    SECRETS_PERM.add(PosixFilePermission.OWNER_WRITE);
    SECRETS_PERM.add(PosixFilePermission.OWNER_EXECUTE);
    
    SECRETS_PERM.add(PosixFilePermission.GROUP_READ);
    SECRETS_PERM.add(PosixFilePermission.GROUP_EXECUTE);
  }
  
  public AirflowService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  @POST
  @Path("/jwt")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response storeAirflowJWT(@Context SecurityContext sc) throws AirflowException {
    try {
      Users user = jwtHelper.getUserPrincipal(sc);
      String[] audience = new String[]{Audience.JOB};
      String token = jwtHelper.createToken(user, audience, settings.getJWTIssuer());
      DecodedJWT decodedJWT = JWT.decode(token);
      
      LocalDateTime expirationDate = decodedJWT.getExpiresAt().toInstant()
          .atZone(ZoneId.systemDefault()).toLocalDateTime();
      
      airflowJWTManager.storeJWT(user, project, token, expirationDate);
    } catch (NoSuchAlgorithmException | JWTException ex) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.JWT_NOT_CREATED, Level.SEVERE,
          "Could not create JWT for Airflow service", ex.getMessage(), ex);
    }
    return Response.noContent().build();
  }
  
  @GET
  @Path("secretDir")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response secretDir(@Context HttpServletRequest req) throws AirflowException {
    String secret = DigestUtils.sha256Hex(Integer.toString(this.projectId));

    java.nio.file.Path dagsDir = airflowJWTManager.getProjectDagDirectory(project);
  
    java.nio.file.Path secretsDir = airflowJWTManager.getProjectSecretsDirectory(project);
    
    try {
      // Instead of checking and setting the permissions, just set them as it is an idempotent operation
      dagsDir.toFile().mkdirs();
      Files.setPosixFilePermissions(dagsDir, DAGS_PERM);
      
      secretsDir.toFile().mkdirs();
      Files.getFileAttributeView(secretsDir, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
          .setGroup(airflowJWTManager.getAirflowGroup());
      Files.setPosixFilePermissions(secretsDir, SECRETS_PERM);
    } catch (IOException ex) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.AIRFLOW_DIRS_NOT_CREATED, Level.SEVERE,
          "Could not create Airlflow directories", ex.getMessage(), ex);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(secret).build();
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

  public void airflowOperation(AirflowOp op, String projectUsername) {

    try {
      String script = settings.getHopsworksDomainDir() + "/bin/airflowOps.sh";
  
      ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder();
      if (op.equals(AirflowOp.RESTART_WEBSERVER)) {
        pdBuilder.addCommand("sudo");
      }
      pdBuilder.addCommand(script)
          .addCommand(op.toString())
          .addCommand(project.getName())
          .addCommand(projectUsername)
          .redirectErrorStream(true);
  
      ProcessDescriptor processDescriptor = pdBuilder.build();
      LOGGER.log(Level.FINE, "Airflow command " + processDescriptor.toString());
      
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      
      if (!processResult.processExited()) {
        LOGGER.log(Level.WARNING, "Airflow command timed-out");
        return;
      }
      if (processResult.getExitCode() == 0) {
        LOGGER.log(Level.FINEST, "Airflow command: " + processResult.getStdout());
        LOGGER.log(Level.FINE, "Successfully ran Airflow command: " + op);
      } else {
        LOGGER.log(Level.WARNING, "Problem running Airflow command: " + op);
      }
    } catch (Exception ex) {
      Logger.getLogger(AirflowService.class.getName()).log(Level.SEVERE, "Problem with the command: " + op, ex);
    }
  }

}
