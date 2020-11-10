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
package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.api.experiments.dto.ExperimentDTO;
import io.hops.hopsworks.api.experiments.dto.ExperimentsEndpointDTO;
import io.hops.hopsworks.api.experiments.results.ExperimentResultsResource;
import io.hops.hopsworks.api.experiments.tensorboard.TensorBoardResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentsResource {

  private static final Logger LOGGER = Logger.getLogger(ExperimentsResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ExperimentsBuilder experimentsBuilder;
  @EJB
  private JWTHelper jwtHelper;
  @Inject
  private TensorBoardResource tensorBoardResource;
  @Inject
  private ExperimentResultsResource resultsResource;
  @EJB
  private ExperimentsController experimentsController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;

  private Project project;
  public ExperimentsResource setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }

  @ApiOperation(value = "Get a list of all experiments for this project", response = ExperimentDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
      @BeanParam Pagination pagination,
      @BeanParam ExperimentsBeanParam experimentsBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ExperimentsException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPERIMENTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(experimentsBeanParam.getFilter());
    resourceRequest.setSort(experimentsBeanParam.getSortBySet());
    resourceRequest.setExpansions(experimentsBeanParam.getExpansions().getResources());
    Users user = jwtHelper.getUserPrincipal(sc);
    ExperimentDTO dto = experimentsBuilder.build(uriInfo, resourceRequest, project, user);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation( value = "Get an experiment", response = ExperimentDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get (
    @PathParam("id") String id,
    @BeanParam ExpansionBeanParam expansions,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc)
    throws ExperimentsException, DatasetException, ProvenanceException, MetadataException, GenericException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPERIMENTS);
    resourceRequest.setExpansions(expansions.getResources());
    ProvStateDTO fileState = experimentsController.getExperiment(project, id);
    Users user = jwtHelper.getUserPrincipal(sc);
    if(fileState != null) {
      Map<Long, ExperimentsEndpointDTO> endpoints = new HashMap<>();
      endpoints.put(project.getInode().getId(), experimentsController.getExperimentsEndpoint(project));
      ExperimentDTO dto = experimentsBuilder.build(uriInfo, resourceRequest, project, user, endpoints, fileState);
      if(dto == null) {
        throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.FINE);
      } else {
        return Response.ok().entity(dto).build();
      }
    } else {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_NOT_FOUND, Level.FINE);
    }
  }
  
  @ApiOperation( value = "Create or update an experiment", response = ExperimentDTO.class)
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response post (
    @PathParam("id") String id,
    ExperimentDTO experimentDTO,
    @QueryParam("type") ExperimentOperationType type,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc)
    throws DatasetException, ProvenanceException, PythonException, MetadataException, ProjectException,
    GenericException, ExperimentsException {
    if (experimentDTO == null) {
      throw new IllegalArgumentException("No Experiment configuration was provided");
    }
    Users user = jwtHelper.getUserPrincipal(sc);
    
    Project experimentProject = project;
    switch(type) {
      case INIT: {
        String experimentPath = Settings.HOPS_EXPERIMENTS_DATASET + "/" + id;
        experimentDTO.setEnvironment(environmentController.exportEnv(experimentProject, user, experimentPath));
        try {
          String program = experimentsController.versionProgram(experimentProject, user,
            experimentDTO.getJobName(), experimentDTO.getKernelId(), id);
          experimentDTO.setProgram(program);
        } catch(Exception e) {
          LOGGER.log(Level.SEVERE, "Could not version notebook " + e.getMessage());
        }
      } break;
      case MODEL_UPDATE: {
        Project modelProject = getModelsProjectAndCheckAccess(experimentDTO);
        experimentsController.attachModel(user, experimentProject, id, modelProject, experimentDTO.getModel());
      } break;
      case FULL_UPDATE: {
        //no need to update the summary in any way
      } break;
      default: {
        throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
          "unhandled experiment summary operation type:" + type);
      }
    }
    experimentsController.attachExperiment(user, experimentProject, id, experimentDTO);
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(id);
    switch(type) {
      case INIT:
        return Response.created(builder.build()).entity(experimentDTO).build();
      case MODEL_UPDATE:
      case FULL_UPDATE:
        return Response.ok(builder.build()).entity(experimentDTO).build();
      default: {
        throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
          "unhandled experiment summary operation type:" + type);
      }
    }
  }
  
  private Project getModelsProjectAndCheckAccess(ExperimentDTO experimentDTO)
    throws ProjectException, DatasetException, GenericException {
    Project modelProject;
    if (experimentDTO.getModelProjectName() == null) {
      modelProject = project;
    } else {
      modelProject = projectFacade.findByName(experimentDTO.getModelProjectName());
      if (modelProject == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.INFO,
          "model project not found for experiment");
      }
    }
    Dataset modelDataset = datasetCtrl.getByName(modelProject, Settings.HOPS_MODELS_DATASET);
    if (!accessCtrl.hasAccess(project, modelDataset)) {
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO);
    }
    return modelProject;
  }
  
  public enum ExperimentOperationType {
    INIT,
    FULL_UPDATE,
    MODEL_UPDATE
  }
  
  @ApiOperation( value = "Delete an experiment")
  @DELETE
  @Path("{id}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response delete (
      @PathParam("id") String id,
      @QueryParam("endpointId") Integer parentProjId,
      @Context HttpServletRequest req,
      @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws DatasetException {
    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    if(parentProjId != null) {
      Project parentProject = projectFacade.find(parentProjId);
      experimentsController.delete(hopsworksUser, project, parentProject, id);
    } else {
      experimentsController.delete(hopsworksUser, project, id);
    }
    return Response.noContent().build();
  }

  @ApiOperation(value = "TensorBoard sub-resource", tags = {"TensorBoardResource"})
  @Path("{id}/tensorboard")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public TensorBoardResource tensorboard(@PathParam("id") String id) {
    return this.tensorBoardResource.setProject(project, id);
  }

  @ApiOperation(value = "Results sub-resource", tags = {"ExperimentResultsResource"})
  @Path("{id}/results")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public ExperimentResultsResource results(@PathParam("id") String id) {
    return this.resultsResource.setProject(project, id);
  }
}
