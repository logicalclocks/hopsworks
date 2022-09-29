/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.cloud;

import io.hops.hopsworks.api.admin.cloud.CloudRoleMappingBeanParam;
import io.hops.hopsworks.api.admin.cloud.CloudRoleMappingBuilder;
import io.hops.hopsworks.api.admin.cloud.CloudRoleMappingDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.cloud.CloudRoleMappingController;
import io.hops.hopsworks.common.cloud.Credentials;
import io.hops.hopsworks.common.cloud.TemporaryCredentialsHelper;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Logged
@Api(value = "Cloud RoleMapping Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RoleMappingResource {
  
  private static final Logger LOGGER = Logger.getLogger(RoleMappingResource.class.getName());
  
  @EJB
  private ProjectController projectController;
  @EJB
  private CloudRoleMappingBuilder cloudRoleMappingBuilder;
  @EJB
  private CloudRoleMappingController cloudRoleMappingController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private TemporaryCredentialsHelper temporaryCredentialsHelper;
  
  private Integer projectId;
  private String projectName;

  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  @Logged(logLevel = LogLevel.OFF)
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  @GET
  @Path("role-mappings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getCloudRoleMappings(@BeanParam Pagination pagination,
                                       @BeanParam CloudRoleMappingBeanParam mappingBeanParam,
                                       @Context HttpServletRequest req,
                                       @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws ProjectException {
    Project project = projectController.findProjectById(this.projectId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(mappingBeanParam.getSortBySet());
    resourceRequest.setFilter(mappingBeanParam.getFilter());
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("/role-mappings/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByProjectAndId(@PathParam("id") Integer id,
                                    @Context UriInfo uriInfo,
                                    @Context HttpServletRequest req,
                                    @Context SecurityContext sc)
      throws ProjectException, CloudException {
    Project project = projectController.findProjectById(this.projectId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItem(uriInfo, resourceRequest, id, project);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("/role-mappings/default")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByProjectDefault(@Context UriInfo uriInfo,
                                      @Context HttpServletRequest req,
                                      @Context SecurityContext sc) throws ProjectException, CloudException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = projectController.findProjectById(this.projectId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItemDefault(uriInfo, resourceRequest, user, project);
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @Path("/role-mappings/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateDefault(@PathParam("id") Integer id,
                                @QueryParam("defaultRole") boolean defaultRole,
                                @Context UriInfo uriInfo,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) throws ProjectException, CloudException {
    Project project = projectController.findProjectById(this.projectId);
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingController.setDefault(id, project, defaultRole);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItem(uriInfo, resourceRequest, cloudRoleMapping.getId());
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("aws/session-token")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getSessionToken(@Secret @QueryParam("roleARN") String roleARN,
                                  @QueryParam("roleSessionName") String roleSessionName,
                                  @DefaultValue("3600") @QueryParam("durationSeconds") int durationSeconds,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc)
      throws ProjectException, CloudException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = projectController.findProjectById(this.projectId);
    Credentials credentials = temporaryCredentialsHelper.getTemporaryCredentials(roleARN, roleSessionName,
      durationSeconds, user, project);
    return Response.ok(credentials).build();
  }
}