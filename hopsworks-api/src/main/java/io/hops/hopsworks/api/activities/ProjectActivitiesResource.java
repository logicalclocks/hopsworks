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
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.api.Pagination;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.ActivitiesException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectActivitiesResource {

  private static final Logger LOGGER = Logger.getLogger(ProjectActivitiesResource.class.getName());
  @EJB
  private ActivitiesBuilder activitiesBuilder;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;
  private String projectName;

  public ProjectActivitiesResource() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  private Project getProjectById() throws ProjectException {
    Project project = projectFacade.find(this.projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    return project;
  }

  private Project getProjectByName() throws ProjectException {
    Project project = projectFacade.findByName(this.projectName);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectName: " + 
          projectName);
    }
    return project;
  }

  private Project getProject() throws ProjectException {
    return this.projectId != null ? getProjectById() : getProjectByName();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllByProject(
      @BeanParam Pagination pagination,
      @BeanParam ActivitiesBeanParam activitiesBeanParam,
      @QueryParam("expand") String expand,
      @Context UriInfo uriInfo) throws ProjectException {
    Project project = getProject(); //test if project exist 
    ResourceProperties resourceProperties = new ResourceProperties(ResourceProperties.Name.ACTIVITIES, pagination, 
        activitiesBeanParam.getSort(), activitiesBeanParam.getFilter(), expand);
    ActivitiesDTO activitiesDTO = activitiesBuilder.buildItems(uriInfo, resourceProperties, project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(activitiesDTO).build();
  }

  @GET
  @Path("{activityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllById(
      @PathParam("activityId") Integer activityId,
      @QueryParam("expand") String expand,
      @Context UriInfo uriInfo) throws ProjectException, ActivitiesException {
    Project project = getProject(); //test if project exist
    ResourceProperties resourceProperties = new ResourceProperties(ResourceProperties.Name.ACTIVITIES, expand);
    ActivitiesDTO activitiesDTO = activitiesBuilder.build(uriInfo, resourceProperties, project, activityId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(activitiesDTO).build();
  }

}
