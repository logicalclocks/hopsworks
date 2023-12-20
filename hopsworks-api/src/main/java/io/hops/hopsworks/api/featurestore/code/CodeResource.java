/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.code;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.code.CodeActions;
import io.hops.hopsworks.common.featurestore.code.CodeController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.featurestore.databricks.DatabricksController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@Api(value = "Feature store code Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CodeResource {

  @EJB
  private CodeBuilder codeBuilder;
  @EJB
  private CodeController codeController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private DatabricksController databricksController;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;
  private TrainingDataset trainingDataset;

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setTrainingDatasetId(Integer trainingDatasetId) throws FeaturestoreException {
    this.trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all available codes")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam CodeBeanParam codeBeanParam,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws FeaturestoreException, ServiceException {

    Users user = jWTHelper.getUserPrincipal(sc);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CODE);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(codeBeanParam.getSortBySet());
    resourceRequest.setFilter(codeBeanParam.getFilterSet());
    resourceRequest.setField(codeBeanParam.getFieldSet());

    CodeDTO dto;
    if (featuregroup != null) {
      dto = codeBuilder.build(uriInfo, resourceRequest, project, user, featurestore, featuregroup,
              codeBeanParam.getFormat());
    } else {
      dto = codeBuilder.build(uriInfo, resourceRequest, project, user, featurestore, trainingDataset,
              codeBeanParam.getFormat());
    }

    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("/{codeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get specific available code")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam CodeBeanParam codeBeanParam,
                      @PathParam("codeId") Integer codeId,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws FeaturestoreException, ServiceException {

    Users user = jWTHelper.getUserPrincipal(sc);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CODE);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(codeBeanParam.getSortBySet());
    resourceRequest.setFilter(codeBeanParam.getFilterSet());
    resourceRequest.setField(codeBeanParam.getFieldSet());

    CodeDTO dto;
    if (featuregroup != null) {
      dto = codeBuilder.build(uriInfo, resourceRequest, project, user, featuregroup, codeId,
              codeBeanParam.getFormat());
    } else {
      dto = codeBuilder.build(uriInfo, resourceRequest, project, user, trainingDataset, codeId,
              codeBeanParam.getFormat());
    }

    return Response.ok().entity(dto).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Save new code")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response post(@Context UriInfo uriInfo,
                       @Context HttpServletRequest req,
                       @Context SecurityContext sc,
                       @QueryParam("entityId") String entityId,
                       @QueryParam("type") CodeActions.RunType type,
                       @QueryParam("databricksClusterId") String databricksClusterId,
                       CodeDTO codeDTO)
      throws FeaturestoreException, DatasetException, HopsSecurityException, ServiceException, UserException {

    Users user = jWTHelper.getUserPrincipal(sc);

    String databricksNotebook = null;
    byte[] databricksArchive = null;
    if (type == CodeActions.RunType.DATABRICKS) {
      databricksNotebook = databricksController.getNotebookJupyter(user, databricksClusterId, entityId);
      databricksArchive = databricksController.getNotebookArchive(user, databricksClusterId, entityId);
    }

    CodeDTO dto;
    if (featuregroup != null) {
      FeaturestoreCode featurestoreCode = codeController.registerCode(project, user, codeDTO.getCommitTime(),
          codeDTO.getFeatureGroupCommitId(), codeDTO.getApplicationId(), featuregroup,
          entityId, databricksNotebook, databricksArchive, type);
      dto = codeBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.CODE),
              project, user, featuregroup, featurestoreCode, JupyterController.NotebookConversion.HTML);
    } else {
      FeaturestoreCode featurestoreCode = codeController.registerCode(project, user, codeDTO.getCommitTime(),
              codeDTO.getApplicationId(), trainingDataset, entityId, databricksNotebook, databricksArchive, type);
      dto = codeBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.CODE),
              project, user, trainingDataset, featurestoreCode, JupyterController.NotebookConversion.HTML);
    }

    return Response.ok().entity(dto).build();
  }
}
