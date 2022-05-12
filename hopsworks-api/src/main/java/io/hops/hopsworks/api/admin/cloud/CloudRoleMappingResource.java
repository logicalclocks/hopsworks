/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import io.hops.hopsworks.api.cloud.TemporaryCredentialsHelper;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.cloud.CloudRoleMappingController;
import io.hops.hopsworks.common.dao.cloud.CloudRoleMappingFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.cloud.ProjectRoles;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@Logged
@Stateless
@Path("/admin/cloud")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Cloud Role Mapping")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudRoleMappingResource {
  
  @EJB
  private CloudRoleMappingBuilder cloudRoleMappingBuilder;
  @EJB
  private ProjectController projectController;
  @EJB
  private CloudRoleMappingFacade cloudRoleMappingFacade;
  @EJB
  private CloudRoleMappingController cloudRoleMappingController;
  @EJB
  private TemporaryCredentialsHelper temporaryCredentialsHelper;
  
  @GET
  @Path("role-mappings/isAvailable")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response check(@Context HttpServletRequest req, @Context UriInfo uriInfo, @Context SecurityContext sc) {
    return Response.ok(new RoleMappingServiceStatus(temporaryCredentialsHelper.checkService())).build();
  }
  
  @GET
  @Path("/role-mappings")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@BeanParam Pagination pagination,
                         @BeanParam CloudRoleMappingBeanParam mappingBeanParam,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(mappingBeanParam.getSortBySet());
    resourceRequest.setFilter(mappingBeanParam.getFilter());
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItems(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("/role-mappings/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllByProject(@PathParam("id") Integer id,
                                  @Context UriInfo uriInfo,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws CloudException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItem(uriInfo, resourceRequest, id);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Path("/role-mappings")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addMapping(@Secret @QueryParam("cloudRole") String cloudRole,
                             @QueryParam("projectId") Integer projectId,
                             @QueryParam("projectRole") ProjectRoles projectRole,
                             @QueryParam("defaultRole") Boolean defaultRole,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc)
      throws ProjectException, CloudException {
    Project project = projectController.findProjectById(projectId);
    CloudRoleMapping cloudRoleMapping =
      cloudRoleMappingController.saveMapping(project, cloudRole, projectRole, defaultRole != null && defaultRole);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItem(uriInfo, resourceRequest, cloudRoleMapping.getId());
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  @PUT
  @Path("/role-mappings/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateMapping(@PathParam("id") Integer id,
                                @Secret @QueryParam("cloudRole") String cloudRole,
                                @QueryParam("projectRole") ProjectRoles projectRole,
                                @QueryParam("defaultRole") Boolean defaultRole,
                                @Context UriInfo uriInfo,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingController.update(id, cloudRole, projectRole, defaultRole);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CLOUD);
    CloudRoleMappingDTO dto = cloudRoleMappingBuilder.buildItem(uriInfo, resourceRequest, cloudRoleMapping.getId());
    return Response.ok(dto).build();
  }
  
  @DELETE
  @Path("/role-mappings/{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteMapping(@PathParam("id") Integer id,
                                @Context UriInfo uriInfo,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.find(id);
    if (cloudRoleMapping != null) {
      cloudRoleMappingFacade.remove(cloudRoleMapping);
    }
    return Response.noContent().build();
  }
  
}
