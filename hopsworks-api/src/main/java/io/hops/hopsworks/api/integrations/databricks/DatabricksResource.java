/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.databricks;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.featurestore.databricks.DatabricksController;
import io.hops.hopsworks.featurestore.databricks.client.DbCluster;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.integrations.DatabricksInstance;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Logged
@RequestScoped
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Databricks integration resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatabricksResource {

  @EJB
  private DatabricksController databricksController;
  @EJB
  private DatabricksClusterBuilder databricksClusterBuilder;
  @EJB
  private DatabricksInstanceBuilder databricksInstanceBuilder;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  private Project project;

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "List registered Databricks instances", response = DatabricksInstanceDTO.class)
  public Response listInstances(@Context UriInfo uriInfo,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
    return Response.ok().entity(databricksInstanceBuilder.build(uriInfo, project, resourceRequest, user)).build();
  }

  @POST
  @Path("{instance}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Register a new Databricks instance", response = DatabricksInstanceDTO.class)
  public Response registerInstance(@Context UriInfo uriInfo,
                                   @Context HttpServletRequest req,
                                   @Context SecurityContext sc,
                                   @PathParam("instance") String instanceUrl,
                                   DatabricksApiKeyDTO databricksApiKeyDTO)
      throws FeaturestoreException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    DatabricksInstance dbInstance =
        databricksController.registerInstance(user, instanceUrl, databricksApiKeyDTO.getApiKey());

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
    return Response.ok().entity(databricksInstanceBuilder.build(uriInfo, project, resourceRequest, dbInstance)).build();
  }

  @DELETE
  @Path("{instance}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a Databricks instance")
  public Response deleteInstance(@Context UriInfo uriInfo,
                                 @Context HttpServletRequest req,
                                 @Context SecurityContext sc,
                                 @PathParam("instance") String instanceUrl)
      throws FeaturestoreException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    databricksController.deleteInstance(user, instanceUrl);

    return Response.ok().build();
  }

  @GET
  @Path("{instance}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "List cluster of a Databricks instance", response = DatabricksClusterDTO.class)
  public Response listClusters(@Context UriInfo uriInfo,
                               @Context HttpServletRequest req,
                               @Context SecurityContext sc,
                               @PathParam("instance") String dbInstance)
      throws FeaturestoreException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<DbCluster> dbClusters = databricksController.listClusters(user, dbInstance);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
    Set<ResourceRequest> expansionSet = new HashSet<>();
    expansionSet.add(new ResourceRequest(ResourceRequest.Name.USERS));
    resourceRequest.setExpansions(expansionSet);
    DatabricksClusterDTO dto =
        databricksClusterBuilder.build(uriInfo, project, resourceRequest, dbInstance, dbClusters);

    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("{instance}/clusters/{clusterId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get details about a Databricks cluster", response = DatabricksClusterDTO.class)
  public Response getCluster(@Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc,
                             @PathParam("instance") String dbInstance,
                             @PathParam("clusterId") String clusterId)
      throws FeaturestoreException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    DbCluster dbClusters = databricksController.getCluster(user, dbInstance, clusterId);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
    DatabricksClusterDTO dto =
        databricksClusterBuilder.build(uriInfo, project, resourceRequest, dbInstance, dbClusters);

    return Response.ok().entity(dto).build();
  }

  @POST
  @Path("{instance}/clusters/{clusterId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Configure a Databricks cluster", response = DatabricksClusterDTO.class)
  public Response configureCluster(@Context UriInfo uriInfo,
                                   @Context HttpServletRequest req,
                                   @Context SecurityContext sc,
                                   @PathParam("instance") String dbInstance,
                                   @PathParam("clusterId") String clusterId,
                                   @ApiParam(value = "Username of the user for whom to configure the cluster.")
                                   @QueryParam("username") String username)
      throws IOException, UserException, ServiceDiscoveryException, FeaturestoreException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Users targetUser = null;

    if (!Strings.isNullOrEmpty(username)) {
      targetUser = userFacade.findByUsername(username);
      if (targetUser == null) {
        throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE,
            "Cannot find user with username: " + username);
      }
      if (projectTeamFacade.findCurrentRole(project, targetUser) == null) {
        throw new UserException(RESTCodes.UserErrorCode.ROLE_NOT_FOUND, Level.FINE,
            "Cannot find role for user: " + username + " in project: " + project.getName());
      }
    } else {
      targetUser = user;
    }

    if (!targetUser.equals(user) &&
        !projectTeamFacade.findCurrentRole(project, user).equalsIgnoreCase("Data Owner")) {
      throw new IllegalArgumentException("Only Data owners are allowed to configure clusters for team members");
    }

    DbCluster dbClusters = databricksController.configureCluster(dbInstance, clusterId, user, targetUser, project);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
    DatabricksClusterDTO dto =
        databricksClusterBuilder.build(uriInfo, project, resourceRequest, dbInstance, dbClusters);

    return Response.ok().entity(dto).build();
  }
}
