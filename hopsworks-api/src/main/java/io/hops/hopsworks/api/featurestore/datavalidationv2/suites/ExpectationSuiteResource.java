/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidationv2.suites;

import io.hops.hopsworks.common.featurestore.datavalidationv2.ExpectationSuiteDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ExpectationSuiteController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Expectation suite resource", description = "A service that manages a feature group's expectation suite")
public class ExpectationSuiteResource {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private ExpectationSuiteBuilder expectationSuiteBuilder;
  @EJB
  private ExpectationSuiteController expectationSuiteController;

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
   * Endpoint to get expectation suite attached to a featuregroup
   *
   * @return JSON-representation of an expectation suite
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch expectation suite attach to the feature group", response = ExpectationSuiteDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo) throws FeaturestoreException {
    ExpectationSuite expectationSuite = expectationSuiteController.getExpectationSuite(featuregroup);
    return Response.ok()
      .entity(expectationSuiteBuilder.build(uriInfo, project, featuregroup, expectationSuite))
      .build();
  }

  /**
   * Create expectation suite attached to a featuregroup
   * @param expectationSuiteDTO json representation of an expectation suite to attached to a featuregroup
   * @return JSON information about the created expectation suite
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Create an expectation suite attached to a feature group", response = ExpectationSuiteDTO.class)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {

    ExpectationSuite expectationSuite = expectationSuiteController.createExpectationSuite(
      featuregroup, expectationSuiteDTO);

    ExpectationSuiteDTO dto = expectationSuiteBuilder.build(
      uriInfo, project, featuregroup, expectationSuite);

    return Response.ok().entity(dto).build();
  }

  /**
   * Delete an expectation suite attached to a featuregroup
   *
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Delete an expectation suite attached to a feature group", response = ExpectationSuiteDTO.class)
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo) throws FeaturestoreException {

    expectationSuiteController.deleteExpectationSuite(featuregroup);

    return Response.noContent().build();
  }
}