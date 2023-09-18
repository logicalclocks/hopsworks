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

import io.hops.hopsworks.api.featurestore.datavalidationv2.expectations.ExpectationResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Expectation suite resource")
public class ExpectationSuiteResource {

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private ExpectationSuiteBuilder expectationSuiteBuilder;
  @EJB
  private ExpectationSuiteController expectationSuiteController;
  @EJB
  private FsJobManagerController fsJobManagerController;
  @EJB
  private JobsBuilder jobsBuilder;
  @Inject
  private ExpectationResource expectationResource;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
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
   *
   * @param expectationSuiteDTO
   *   json representation of an expectation suite to attached to a featuregroup
   * @return JSON information about the created expectation suite
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Create or Update an expectation suite attached to a feature group",
    response = ExpectationSuiteDTO.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {

    if (expectationSuiteController.getExpectationSuite(featuregroup) != null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.EXPECTATION_SUITE_ALREADY_EXISTS,
        Level.WARNING);
    }
    Users user = jWTHelper.getUserPrincipal(sc); 

    ExpectationSuite expectationSuite = expectationSuiteController.createExpectationSuite(
      user, featuregroup, expectationSuiteDTO);

    ExpectationSuiteDTO dto = expectationSuiteBuilder.build(
      uriInfo, project, featuregroup, expectationSuite);

    return Response.created(dto.getHref()).entity(dto).build();
  }

  /**
   * Update expectation suite attached to a featuregroup
   *
   * @param expectationSuiteDTO
   *   json representation of an expectation suite to attached to a featuregroup
   * @return JSON information about the created expectation suite
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Update an expectation suite attached to a feature group",
    response = ExpectationSuiteDTO.class)
  @PUT
  @Path("/{expectationSuiteId: [0-9]+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationSuiteId")
      Integer expectationSuiteId,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {
    
    Users user = jWTHelper.getUserPrincipal(sc); 

    ExpectationSuite expectationSuite = expectationSuiteController.createOrUpdateExpectationSuite(
      user, featuregroup, expectationSuiteDTO);

    ExpectationSuiteDTO dto = expectationSuiteBuilder.build(
      uriInfo, project, featuregroup, expectationSuite);

    return Response.ok().entity(dto).build();
  }

  /**
   * Edit metadata of an expectation suite attached to a featuregroup.
   * Discard any expectations attached to the DTO
   *
   * @param expectationSuiteDTO
   *   json representation of an expectation suite with updated metadata.
   * @return JSON information about the created expectation suite
   * @throws FeaturestoreException
   */
  @Path("/{expectationSuiteId: [0-9]+}/metadata")
  @ApiOperation(value = "Update metadata only of an expectation suite attached to a feature group",
    response = ExpectationSuiteDTO.class)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateMetadataExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationSuiteId")
      Integer expectationSuiteId,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {
    
    Users user = jWTHelper.getUserPrincipal(sc);

    // Expectations list of the DTO is discarded
    boolean verifyInput = true;
    boolean logActivity = true;
    ExpectationSuite expectationSuite = expectationSuiteController.updateMetadataExpectationSuite(
      user, featuregroup, expectationSuiteDTO, logActivity, verifyInput);

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
  @Path("/{expectationSuiteId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteExpectationSuite(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("expectationSuiteId")
      Integer expectationSuiteId) {

    Users user = jWTHelper.getUserPrincipal(sc); 

    expectationSuiteController.deleteExpectationSuite(user, featuregroup);

    return Response.noContent().build();
  }

  @Path("validate")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Setup job and trigger for evaluating expectation suite", response = JobDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response compute(
    @Context
      UriInfo uriInfo,
    @Context
      HttpServletRequest req,
    @Context
      SecurityContext sc)
    throws FeaturestoreException, ServiceException, JobException, ProjectException, GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);

    if (expectationSuiteController.getExpectationSuite(featuregroup) == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.NO_EXPECTATION_SUITE_ATTACHED_TO_THIS_FEATUREGROUP,
        Level.FINE, "Feature Group has no Expectation Suite for data validation attached.");
    }

    Jobs job = fsJobManagerController.setupValidationJob(project, user, featurestore, featuregroup);
    JobDTO jobDTO = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);
    return Response.created(jobDTO.getHref()).entity(jobDTO).build();

  }
  /////////////////////////////////////////
  //// Single Expectation Service
  ////////////////////////////////////////

  @Path("/{expectationSuiteId: [0-9]+}/expectations")
  public ExpectationResource expectationResource(
    @PathParam("expectationSuiteId")
      Integer expectationSuiteId)
    throws FeaturestoreException {
    this.expectationResource.setProject(project);
    this.expectationResource.setFeaturestore(featurestore);
    this.expectationResource.setFeatureGroup(featuregroup.getId());
    this.expectationResource.setExpectationSuite(expectationSuiteId);
    return expectationResource;
  }
}