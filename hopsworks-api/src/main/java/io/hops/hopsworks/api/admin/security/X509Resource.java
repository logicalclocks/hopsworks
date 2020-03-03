/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.api.admin.security;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.AccessCredentialsDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;

@RequestScoped
@Api(value = "API for platform admins or agents to access x509 credentials of users")
public class X509Resource {
  
  @EJB
  private ProjectController projectController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  
  @GET
  @TransactionAttribute(TransactionAttributeType.NEVER)
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.SERVICES, Audience.API}, allowedUserRoles = {"AGENT", "HOPS_ADMIN"})
  @ApiOperation(value = "Get keystore, truststore and password of a project user",
    response = AccessCredentialsDTO.class)
  public Response getx509(@QueryParam("username") String projectUsername, @Context SecurityContext sc)
    throws ProjectException, UserException, HopsSecurityException {
    try {
      String projectName = hdfsUsersController.getProjectName(projectUsername);
      String username = hdfsUsersController.getUserName(projectUsername);
  
      ProjectDTO projectDTO = projectController.getProjectByName(projectName);
      Users user = userFacade.findByUsername(username);
      if (user == null) {
        throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
      }
  
      try {
        AccessCredentialsDTO credentialsDTO = projectController.credentials(projectDTO.getProjectId(), user);
        return Response.ok(credentialsDTO).build();
      } catch (DatasetException ex) {
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND, Level.FINE);
      }
    } catch (ArrayIndexOutOfBoundsException ex) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE,
          "Invalid project user format for username: " + projectUsername);
    }
  }
}
