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
package io.hops.hopsworks.api.models;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateElastic;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;

import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsResource {

  private static final Logger LOGGER = Logger.getLogger(ModelsResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ModelsBuilder modelsBuilder;
  @EJB
  private ModelsController modelsController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private JWTHelper jwtHelper;


  private Project project;
  public ModelsResource setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }

  @ApiOperation(value = "Get a list of all models for this project", response = ModelDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
      @BeanParam Pagination pagination,
      @BeanParam ModelsBeanParam modelsBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ModelsException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(modelsBeanParam.getFilter());
    resourceRequest.setSort(modelsBeanParam.getSortBySet());
    ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation( value = "Get a model", response = ModelDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get (
      @PathParam("id") String id,
      @Context UriInfo uriInfo,
      @BeanParam ModelsBeanParam modelsBeanParam, @Context SecurityContext sc)
      throws ProvenanceException, ModelsException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    ProvStateElastic fileState = modelsController.getModel(project, id);
    if(fileState != null) {
      ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, project, fileState);
      return Response.ok().entity(dto).build();
    } else {
      throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_NOT_FOUND, Level.FINE);
    }
  }

  @ApiOperation( value = "Create or update a model", response = ModelDTO.class)
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response post (
      @PathParam("id") String id,
      ModelDTO modelDTO,
      @Context HttpServletRequest req,
      @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws DatasetException, ModelsException, JobException, ServiceException,
      PythonException {
    if (modelDTO == null) {
      throw new IllegalArgumentException("Model summary not provided");
    }
    Users user = jwtHelper.getUserPrincipal(sc);
    String realName = user.getFname() + " " + user.getLname();
    modelDTO.setProgram(modelsController.versionProgram(project, user,
        modelDTO.getJobName(), modelDTO.getKernelId(), modelDTO.getName(), modelDTO.getVersion()));
    modelDTO.setEnvironment(environmentController.exportEnv(project, user,
            Settings.HOPS_MODELS_DATASET + "/" + modelDTO.getName() + "/"
                + modelDTO.getVersion()));
    modelsController.attachModel(project, realName, modelDTO);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(id);
    return Response.created(builder.build()).entity(modelDTO).build();
  }
}
