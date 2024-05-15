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

package io.hops.hopsworks.api.featurestore.datavalidationv2.expectations;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.datavalidationv2.suites.ExpectationSuiteSubResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Expectation resource")
public class ExpectationResource extends ExpectationSuiteSubResource {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private ExpectationSuiteController expectationSuiteController;
  @EJB
  private ExpectationBuilder expectationBuilder;
  @EJB
  private ExpectationController expectationController;
  @EJB
  private JWTHelper jWTHelper;
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

  @Override
  protected ExpectationSuiteController getExpectationSuiteController() {
    return expectationSuiteController;
  }

  /**
   * Endpoint to get an Expectation
   * @param expectationId id of the expectation to fetch
   * @return JSON-representation of an expectation
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch expectation attached to the feature group", response = ExpectationDTO.class)
  @GET
  @Path("/{expectationId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationId")
      Integer expectationId) throws FeaturestoreException, ProjectException {
    Expectation expectation = expectationController.getExpectationById(expectationId);
    Project project = getProject();
    Featuregroup featuregroup = getFeaturegroup(project);
    return Response.ok()
      .entity(expectationBuilder.build(uriInfo, project, featuregroup, getExpectationSuite(), expectation))
      .build();
  }

  /**
   * Append expectation to the expectation suite attached to a featuregroup
   *
   * @param expectationDTO
   *   json representation of an expectation to attached to a featuregroup
   * @return JSON information about the created expectation
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Create an expectation attached to a feature group", response = ExpectationDTO.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createExpectation(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    ExpectationDTO expectationDTO) throws FeaturestoreException, ProjectException {

    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featuregroup featuregroup = getFeaturegroup(project);
    ExpectationSuite expectationSuite = getExpectationSuite();
    boolean logActivity = true;
    boolean verifyInput = true;
    Expectation expectation = expectationController.createOrUpdateExpectation(
      user, expectationSuite, expectationDTO, logActivity, verifyInput);

    ExpectationDTO dto = expectationBuilder.build(uriInfo, project, featuregroup, expectationSuite, expectation);

    return Response.created(dto.getHref()).entity(dto).build();
  }

  /**
   * Update expectation to the expectation suite attached to a featuregroup
   *
   * @param expectationId id of the expectation to update
   * @param expectationDTO
   *   json representation of an expectation to attached to a featuregroup
   * @return JSON information about the created expectation
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Update an expectation attached to a feature group", response = ExpectationDTO.class)
  @PUT
  @Path("/{expectationId: [0-9]+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateExpectation(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationId")
      Integer expectationId,
    ExpectationDTO expectationDTO) throws FeaturestoreException, ProjectException {

    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featuregroup featuregroup = getFeaturegroup(project);
    ExpectationSuite expectationSuite = getExpectationSuite();
    boolean logActivity = true;
    boolean verifyInput = true;
    Expectation expectation = expectationController.createOrUpdateExpectation(
      user, expectationSuite, expectationDTO, logActivity, verifyInput);

    ExpectationDTO dto = expectationBuilder.build(uriInfo, project, featuregroup, expectationSuite, expectation);

    return Response.ok().entity(dto).build();
  }

  /**
   * Delete an expectation in the suite attached to a featuregroup
   *
   * @param expectationId id of the expectation to delete
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Delete an expectation attached to a feature group", response = ExpectationDTO.class)
  @DELETE
  @Path("/{expectationId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteExpectation(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationId")
      Integer expectationId) {
    
    Users user = jWTHelper.getUserPrincipal(sc);

    expectationController.deleteExpectation(user, expectationId, true);

    return Response.noContent().build();
  }

  /**
   * Endpoint to fetch a list of validation reports attached to a featuregroup.
   *
   * @return JSON-array of validation reports
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch all expectation from an Expectation Suite",
    response = ExpectationDTO.class, responseContainer = "List")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getExpectationByExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo) throws FeaturestoreException, ProjectException {
    Project project = getProject();
    ExpectationDTO dtos = expectationBuilder.build(uriInfo, project, getFeaturegroup(project), getExpectationSuite());

    return Response.ok().entity(dtos).build();
  }
}