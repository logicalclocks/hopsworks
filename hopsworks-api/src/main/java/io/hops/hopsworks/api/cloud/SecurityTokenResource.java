/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.cloud;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.common.cloud.Credentials;
import io.hops.hopsworks.common.cloud.TemporaryCredentialsHelper;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Api(value = "Cloud SecurityToken Resource")
@Stateless
@Path("/cloud/aws")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SecurityTokenResource {
  private static final Logger LOGGER = Logger.getLogger(SecurityTokenResource.class.getName());
  @EJB
  private TemporaryCredentialsHelper temporaryCredentialsHelper;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectController projectController;
  
  @GET
  @Path("session-token/{projectId}/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.SERVICES}, allowedUserRoles = {"AGENT"})
  public Response getSessionTokenAsAgent(@PathParam("projectId") Integer projectId,
                                         @PathParam("username") String username,
                                         @Secret @QueryParam("roleARN") String roleARN,
                                         @QueryParam("roleSessionName") String roleSessionName,
                                         @DefaultValue("3600") @QueryParam("durationSeconds") int durationSeconds,
                                         @Context HttpServletRequest req,
                                         @Context SecurityContext sc)
    throws CloudException, UserException, ProjectException {
    Users user = userFacade.findByUsername(username);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    Project project = projectController.findProjectById(projectId);
    Credentials credentials = temporaryCredentialsHelper.getTemporaryCredentials(roleARN, roleSessionName,
      durationSeconds, user, project);
    return Response.ok(credentials).build();
  }
}
