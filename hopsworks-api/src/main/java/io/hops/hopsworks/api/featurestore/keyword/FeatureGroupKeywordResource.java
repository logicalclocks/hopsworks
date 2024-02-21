/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.keyword;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordBuilder;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
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

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature Group Keywords Resource")
public class FeatureGroupKeywordResource {
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
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
                              @Context UriInfo uriInfo) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    List<String> keywords = keywordCtrl.getKeywords(featuregroup);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, keywords);
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
    List<String> updatedKeywords = keywordCtrl.replaceKeywords(featuregroup, new HashSet<>(keywordDTO.getKeywords()));
    dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, updatedKeywords);
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
    keywordCtrl.deleteKeyword(featuregroup, keyword);
    List<String> updatedKeywords = keywordCtrl.getKeywords(featuregroup);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, featuregroup, updatedKeywords);
    return Response.ok().entity(dto).build();
  }
}