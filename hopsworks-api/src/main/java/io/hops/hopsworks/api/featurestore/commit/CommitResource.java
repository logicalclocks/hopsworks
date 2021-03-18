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

package io.hops.hopsworks.api.featurestore.commit;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Api(value = "Feature Group commit Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommitResource {

  @EJB
  private CommitBuilder commitBuilder;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureGroupCommitController featureGroupCommitController;
  @EJB
  private JWTHelper jwtHelper;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;

  public CommitResource setProject(Project project) {
    this.project = project;
    return this;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public void setFeatureGroup(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(this.featurestore, featureGroupId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Commit to Feature Group", response = CommitDTO.class)
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response commitFeatureGroup(@Context UriInfo uriInfo, CommitDTO commitDTO, @Context SecurityContext sc)
      throws FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    FeatureGroupCommit featureGroupCommit =
        featureGroupCommitController.createHudiFeatureGroupCommit(user, featuregroup, commitDTO.getCommitDateString(),
            commitDTO.getCommitTime(), commitDTO.getRowsUpdated(), commitDTO.getRowsInserted(),
            commitDTO.getRowsDeleted(), commitDTO.getValidationId());
    CommitDTO builtCommitDTO = commitBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.COMMITS),
            project, featuregroup, featureGroupCommit);
    return Response.ok().entity(builtCommitDTO).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get Feature Group Commit", response = CommitDTO.class)
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getFeatureGroupCommit(@Context UriInfo uriInfo, @BeanParam Pagination pagination,
                                        @BeanParam CommitBeanParam commitBeanParam, @Context SecurityContext sc) {

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMITS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(commitBeanParam.getSortBySet());
    resourceRequest.setFilter(commitBeanParam.getFilter());

    CommitDTO builtCommitDTO = commitBuilder.build(uriInfo, resourceRequest, project, featuregroup);

    return Response.ok().entity(builtCommitDTO).build();
  }
}
