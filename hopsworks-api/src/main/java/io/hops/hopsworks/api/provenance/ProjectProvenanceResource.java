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
package io.hops.hopsworks.api.provenance;

import io.hops.hopsworks.api.dataset.DatasetAccessType;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.ops.ProvLinksBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvLinksBuilder;
import io.hops.hopsworks.api.provenance.ops.ProvOpsBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvOpsBuilder;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBuilder;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBeanParams;
import io.hops.hopsworks.api.provenance.state.ProvStateBeanParams;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.provenance.core.dto.ProvDatasetDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.common.provenance.state.ProvStateBuilder;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Project Provenance Service", description = "Project Provenance Service")
public class ProjectProvenanceResource {
  private static final Logger logger = Logger.getLogger(ProjectProvenanceResource.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private HopsFSProvenanceController fsProvenanceCtrl;
  @Inject
  private ProvStateBuilder stateBuilder;
  @Inject
  private ProvOpsBuilder opsBuilder;
  @Inject
  private ProvLinksBuilder linksBuilder;
  @Inject
  private ProvUsageBuilder usageBuilder;
  @EJB
  private FeaturestoreController fsCtrl;
  @EJB
  private DatasetController datasetCtrl;
  
  private Project project;
  
  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the Provenance Type of PROJECT/DATASET", response = ProvTypeDTO.class)
  public Response getProvenanceStatus(
    @QueryParam("type") @DefaultValue("PROJECT") TypeOf typeOf,
    @Context SecurityContext sc)
    throws ProvenanceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    switch(typeOf) {
      case PROJECT:
        ProvTypeDTO status = fsProvenanceCtrl.getProjectProvType(user, project);
        return Response.ok().entity(status).build();
      case DATASETS:
        GenericEntity<List<ProvDatasetDTO>> result
          = new GenericEntity<List<ProvDatasetDTO>>(fsProvenanceCtrl.getDatasetsProvType(user, project)) {};
        return Response.ok().entity(result).build();
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
          "return type: " + typeOf + " is not managed");
    }
  }
  
  public enum TypeOf {
    PROJECT,
    DATASETS
  }
  
  @GET
  @Path("/states")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "State Provenance query endpoint", response = ProvStateDTO.class)
  public Response getFileStates(
    @BeanParam
      ProvStateBeanParams params,
    @BeanParam Pagination pagination,
    @Context HttpServletRequest req) throws ProvenanceException {
    ProvStateDTO result = stateBuilder.build(project, params, pagination);
    return Response.ok().entity(result).build();
  }
  
  @GET
  @Path("ops")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Operations Provenance query endpoint", response = ProvOpsDTO.class)
  public Response getFileOps(
    @BeanParam ProvOpsBeanParams params,
    @BeanParam Pagination pagination,
    @Context HttpServletRequest req,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws ProvenanceException, GenericException {
    ProvOpsDTO result = opsBuilder.build(project, params, pagination);
    return Response.ok().entity(result).build();
  }
  
  @GET
  @Path("links")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Links Provenance query endpoint - " +
    "link feature groups/training datasets/experiments/models through their application ids",
    response = ProvLinksDTO.class)
  public Response getLinks(
    @BeanParam ProvLinksBeanParams params,
    @BeanParam Pagination pagination,
    @Context HttpServletRequest req) throws ProvenanceException, GenericException {
    ProvLinksDTO result = linksBuilder.build(project, params, pagination);
    return Response.ok().entity(result).build();
  }
  
  @GET
  @Path("usage")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Artifact usage", response = ProvArtifactUsageParentDTO.class)
  public Response usage(
    @QueryParam("artifact_id") String artifactId,
    @QueryParam("endpoint_id") Integer endpointId,
    @QueryParam("artifact_type") DatasetAccessType accessType,
    @BeanParam
      ProvUsageBeanParams params,
    @Context UriInfo uriInfo) throws ProvenanceException, GenericException {
    if(artifactId == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "artifactId id cannot be null");
    }
    Project targetProject = project;
    if(endpointId != null) {
      targetProject = projectFacade.findById(endpointId).orElseThrow(
        () -> new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "target project not found")
      );
    }
    Dataset targetEndpoint;
    if (accessType != null) {
      try {
        switch (accessType) {
          case FEATUREGROUPS:
            targetEndpoint = fsCtrl.getProjectFeaturestoreDataset(targetProject);
            break;
          case TRAININGDATASETS:
            String tdName = project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
            targetEndpoint = datasetCtrl.getByName(targetProject, tdName);
            break;
          case MODELS:
            targetEndpoint = datasetCtrl.getByName(targetProject, Settings.HOPS_MODELS_DATASET);
            break;
          case EXPERIMENTS:
            targetEndpoint = datasetCtrl.getByName(targetProject, Settings.HOPS_EXPERIMENTS_DATASET);
            break;
          default:
            throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
              "access type not supports:" + accessType);
        }
      } catch (FeaturestoreException | DatasetException e) {
        throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.FINE,
          "cannot access the dataset of the artifact");
      }
    } else {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.FINE, "access type not defined");
    }
    ProvArtifactUsageParentDTO status
      = usageBuilder.buildAccessible(uriInfo, project, targetEndpoint, artifactId, params.getUsageType());
    return Response.ok().entity(status).build();
  }
}
