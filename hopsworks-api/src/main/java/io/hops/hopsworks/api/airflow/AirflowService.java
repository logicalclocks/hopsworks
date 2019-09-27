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
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.airflow.AirflowManager;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.AirflowException;
import io.hops.hopsworks.restutils.RESTCodes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.hops.hopsworks.jwt.annotation.JWTRequired;
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
  private JWTHelper jwtHelper;
  @EJB
  private AirflowManager airflowJWTManager;

  private Integer projectId;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;
  
  private static final Set<PosixFilePermission> DAGS_PERM = new HashSet<>(8);
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
  }
  
  // Audience for Airflow JWTs
  private static final String[] JWT_AUDIENCE = new String[]{Audience.API};
  
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
    Users user = jwtHelper.getUserPrincipal(sc);
    airflowJWTManager.prepareSecurityMaterial(user, project, JWT_AUDIENCE);
    return Response.noContent().build();
  }
  
  @GET
  @Path("secretDir")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response secretDir() throws AirflowException {
    String secret = DigestUtils.sha256Hex(Integer.toString(this.projectId));

    java.nio.file.Path dagsDir = airflowJWTManager.getProjectDagDirectory(project.getId());
  
    try {
      // Instead of checking and setting the permissions, just set them as it is an idempotent operation
      dagsDir.toFile().mkdirs();
      Files.setPosixFilePermissions(dagsDir, DAGS_PERM);
    } catch (IOException ex) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.AIRFLOW_DIRS_NOT_CREATED, Level.SEVERE,
          "Could not create Airlflow directories", ex.getMessage(), ex);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(secret).build();
  }
}
