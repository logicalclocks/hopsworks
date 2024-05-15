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

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.featuregroup.FeatureGroupSubResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
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
import javax.servlet.http.HttpServletRequest;
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
public class CommitResource extends FeatureGroupSubResource {

  @EJB
  private CommitBuilder commitBuilder;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureGroupCommitController featureGroupCommitController;
  @EJB
  private JWTHelper jwtHelper;

  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private ProjectController projectController;

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @Override
  protected FeaturestoreController getFeaturestoreController() {
    return featurestoreController;
  }

  @Override
  protected FeaturegroupController getFeaturegroupController() {
    return featuregroupController;
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Commit to Feature Group", response = CommitDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response commitFeatureGroup(@Context UriInfo uriInfo, CommitDTO commitDTO,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc) throws ProjectException, FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featuregroup featuregroup = getFeaturegroup(project);
    FeatureGroupCommit featureGroupCommit =
        featureGroupCommitController.createHudiFeatureGroupCommit(user, featuregroup,
            commitDTO.getCommitTime(), commitDTO.getRowsUpdated(), commitDTO.getRowsInserted(),
            commitDTO.getRowsDeleted(), commitDTO.getValidationId(), commitDTO.getLastActiveCommitTime());
    CommitDTO builtCommitDTO = commitBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.COMMITS),
            project, featuregroup, featureGroupCommit);
    return Response.ok().entity(builtCommitDTO).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get Feature Group Commit", response = CommitDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getFeatureGroupCommit(@Context UriInfo uriInfo,
                                        @BeanParam Pagination pagination,
                                        @BeanParam CommitBeanParam commitBeanParam,
                                        @Context HttpServletRequest req,
                                        @Context SecurityContext sc) throws ProjectException, FeaturestoreException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMITS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(commitBeanParam.getSortBySet());
    resourceRequest.setFilter(commitBeanParam.getFilter());
    Project project = getProject();
    CommitDTO builtCommitDTO = commitBuilder.build(uriInfo, resourceRequest, project, getFeaturegroup(project));

    return Response.ok().entity(builtCommitDTO).build();
  }
}
