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

package io.hops.hopsworks.api.featurestore;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.keyword.KeywordControllerIface;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
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
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature store labels resource")
public class FeaturestoreKeywordResource {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private JWTHelper jwtHelper;
  @Inject
  private KeywordControllerIface keywordControllerIface;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;

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
  @ApiOperation(value = "Get keywords")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getKeywords(@Context SecurityContext sc, @Context UriInfo uriInfo)
      throws FeaturestoreException, MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    List<String> keywords = keywordControllerIface.getAll(project, user, featuregroup, trainingDataset);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project,
        featuregroup, trainingDataset, keywords);
    return Response.ok().entity(dto).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Replace existing keywords")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response replaceKeywords(@Context SecurityContext sc, @Context UriInfo uriInfo, KeywordDTO keywordDTO)
      throws FeaturestoreException, MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    List<String> updatedKeywords =
        keywordControllerIface.replaceKeywords(project, user, featuregroup, trainingDataset, keywordDTO.getKeywords());

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project,
        featuregroup, trainingDataset, updatedKeywords);
    return Response.ok().entity(dto).build();
  }

  @DELETE
  @ApiOperation(value = "Delete a keyword")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteKeywords(@Context SecurityContext sc, @Context UriInfo uriInfo,
                                @QueryParam("keyword") String keyword)
      throws FeaturestoreException, MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    List<String> updatedKeywords =
        keywordControllerIface.deleteKeywords(project, user, featuregroup, trainingDataset, Arrays.asList(keyword));

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project,
        featuregroup, trainingDataset, updatedKeywords);
    return Response.ok().entity(dto).build();
  }

}
