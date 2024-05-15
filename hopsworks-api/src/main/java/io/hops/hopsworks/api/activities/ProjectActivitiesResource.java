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
 */
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.project.ProjectSubResource;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.ActivitiesException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Api(value = "Project Activities Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectActivitiesResource extends ProjectSubResource {

  private static final Logger LOGGER = Logger.getLogger(ProjectActivitiesResource.class.getName());
  @EJB
  private ActivitiesBuilder activitiesBuilder;
  @EJB
  private ProjectController projectController;

  public ProjectActivitiesResource() {
  }
  
  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Finds activities in project.", response = ActivitiesDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response findAllByProject(
      @BeanParam Pagination pagination,
      @BeanParam ActivitiesBeanParam activitiesBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ProjectException {
    Project project = getProject(); //test if project exist
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ACTIVITIES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(activitiesBeanParam.getSortBySet());
    resourceRequest.setFilter(activitiesBeanParam.getFilter());
    if (activitiesBeanParam.getExpansions() != null) {
      resourceRequest.setExpansions(activitiesBeanParam.getExpansions().getResources());
    }
  
    ActivitiesDTO activitiesDTO = activitiesBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(activitiesDTO).build();
  }

  @GET
  @Path("{activityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Finds an activity in project.", response = ActivitiesDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response findAllById(
      @PathParam("activityId") Integer activityId,
      @BeanParam ExpansionBeanParam expansions,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ProjectException, ActivitiesException {
    Project project = getProject(); //test if project exist
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ACTIVITIES);
    resourceRequest.setExpansions(expansions.getResources());
    ActivitiesDTO activitiesDTO = activitiesBuilder.build(uriInfo, resourceRequest, project, activityId);
    return Response.ok().entity(activitiesDTO).build();
  }

}
