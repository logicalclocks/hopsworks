/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidationv2.results;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Expectation resource")
public class ValidationResultResource {
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private ExpectationSuiteController expectationSuiteController;
  @EJB
  private ValidationResultBuilder validationResultBuilder;

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
  public void setFeatureGroup(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }

  /**
   * Endpoint to fetch a list of validation results corresponding to an expectation.
   *
   * @param pagination
   * @param validationResultBeanParam
   * @return JSON-array of validation reports
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch validation result history of an expectation",
    response = ValidationResultDTO.class, responseContainer = "List")
  @GET
  @Path("/history/{expectationId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAll(
    @BeanParam
      Pagination pagination,
    @BeanParam
      ValidationResultBeanParam validationResultBeanParam,
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationId")
      Integer expectationId) throws FeaturestoreException {

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.VALIDATIONRESULT);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(validationResultBeanParam.getSortBySet());
    resourceRequest.setFilter(validationResultBeanParam.getFilter());

    ValidationResultDTO dtos =
      validationResultBuilder.buildHistory(uriInfo, resourceRequest, project, featuregroup, expectationId);

    return Response.ok().entity(dtos).build();
  }

}