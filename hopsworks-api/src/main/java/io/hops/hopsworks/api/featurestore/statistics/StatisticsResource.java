/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.statistics;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Api(value = "Feature store statistics Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsResource {

  @EJB
  private StatisticsBuilder statisticsBuilder;
  @EJB
  private StatisticsController statisticsController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FsJobManagerController fsJobManagerController;
  @EJB
  private JobsBuilder jobsBuilder;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;
  private TrainingDataset trainingDataset;

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }

  public void setTrainingDatasetId(Integer trainingDatasetId) throws FeaturestoreException {
    this.trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all available statistics")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam StatisticsBeanParam statisticsBeanParam,
                      @Context UriInfo uriInfo,
                      @Context SecurityContext sc) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.STATISTICS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(statisticsBeanParam.getSortBySet());
    resourceRequest.setFilter(statisticsBeanParam.getFilterSet());
    resourceRequest.setField(statisticsBeanParam.getFieldSet());

    StatisticsDTO dto;
    if (featuregroup != null) {
      dto = statisticsBuilder.build(uriInfo, resourceRequest, project, user, featurestore, featuregroup);
    } else {
      dto = statisticsBuilder.build(uriInfo, resourceRequest, project, user, featurestore, trainingDataset);
    }
    return Response.ok().entity(dto).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Register new statistics")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response register(@Context UriInfo uriInfo,
                           @Context SecurityContext sc,
                           StatisticsDTO statisticsDTO)
      throws FeaturestoreException, DatasetException, HopsSecurityException, IOException {

    Users user = jWTHelper.getUserPrincipal(sc);

    FeaturestoreStatistic statistics;
    StatisticsDTO dto;
    if (featuregroup != null) {
      statistics = statisticsController.registerStatistics(project, user, statisticsDTO.getCommitTime(),
          statisticsDTO.getFeatureGroupCommitId(), statisticsDTO.getContent(), featuregroup);
      dto = statisticsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.STATISTICS),
          project, user, featuregroup, statistics);
    } else {
      statistics = statisticsController.registerStatistics(project, user, statisticsDTO.getCommitTime(),
        statisticsDTO.getContent(), trainingDataset);
      dto = statisticsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.STATISTICS),
          project, user, trainingDataset, statistics);
    }
    return Response.ok().entity(dto).build();
  }

  @Path("compute")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Setup job and trigger for computing statistics", response = JobDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response compute(@Context UriInfo uriInfo, @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException, JobException, ProjectException, GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Jobs job = fsJobManagerController.setupStatisticsJob(project, user, featurestore, featuregroup, trainingDataset);
    JobDTO jobDTO = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);
    return Response.created(jobDTO.getHref()).entity(jobDTO).build();
  }
}
