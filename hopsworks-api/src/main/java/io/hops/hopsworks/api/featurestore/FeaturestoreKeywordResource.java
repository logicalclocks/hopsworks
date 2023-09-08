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

import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
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
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature store labels resource")
public class FeaturestoreKeywordResource {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;
  private TrainingDataset trainingDataset;
  private FeatureView featureView;

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public void setFeatureView(String name, Integer version) throws FeaturestoreException {
    this.featureView = featureViewController.getByNameVersionAndFeatureStore(name, version, featurestore);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get keywords")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getKeywords(@Context SecurityContext sc,
                              @Context HttpServletRequest req,
                              @Context UriInfo uriInfo)
      throws FeaturestoreException {

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto;
    if (featuregroup != null) {
      List<String> keywords = keywordCtrl.getKeywords(featuregroup);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, keywords);
    } else if (trainingDataset != null) {
      List<String> keywords = keywordCtrl.getKeywords(trainingDataset);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, trainingDataset, keywords);
    } else if (featureView != null) {
      List<String> keywords = keywordCtrl.getKeywords(featureView);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featureView, keywords);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error building keyword object");
    }
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create keywords or replace existing ones")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response replaceKeywords(@Context SecurityContext sc,
                                  @Context HttpServletRequest req,
                                  @Context UriInfo uriInfo, KeywordDTO keywordDTO)
      throws FeaturestoreException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto;
    if (featuregroup != null) {
      List<String> updatedKeywords = keywordCtrl.replaceKeywords(featuregroup,
        new HashSet<>(keywordDTO.getKeywords()));
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, updatedKeywords);
    } else if (trainingDataset != null) {
      List<String> updatedKeywords = keywordCtrl.replaceKeywords(trainingDataset,
        new HashSet<>(keywordDTO.getKeywords()));
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, trainingDataset, updatedKeywords);
    } else if (featureView != null) {
      List<String> updatedKeywords = keywordCtrl.replaceKeywords(featureView,
        new HashSet<>(keywordDTO.getKeywords()));
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featureView, updatedKeywords);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error building keyword object");
    }
    return Response.ok().entity(dto).build();
  }

  @DELETE
  @ApiOperation(value = "Delete a keyword")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteKeywords(@Context SecurityContext sc,
                                 @Context UriInfo uriInfo,
                                 @Context HttpServletRequest req,
                                 @QueryParam("keyword") String keyword)
      throws FeaturestoreException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto;
    if (featuregroup != null) {
      keywordCtrl.deleteKeyword(featuregroup, keyword);
      List<String> updatedKeywords = keywordCtrl.getKeywords(featuregroup);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, updatedKeywords);
    } else if (trainingDataset != null) {
      keywordCtrl.deleteKeyword(trainingDataset, keyword);
      List<String> updatedKeywords = keywordCtrl.getKeywords(trainingDataset);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, trainingDataset, updatedKeywords);
    } else if (featureView != null) {
      keywordCtrl.deleteKeyword(featureView, keyword);
      List<String> updatedKeywords = keywordCtrl.getKeywords(featureView);
      dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featureView, updatedKeywords);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error building keyword object");
    }
    return Response.ok().entity(dto).build();
  }
}
