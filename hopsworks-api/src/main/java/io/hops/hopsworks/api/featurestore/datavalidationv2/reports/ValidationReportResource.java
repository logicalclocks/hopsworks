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

package io.hops.hopsworks.api.featurestore.datavalidationv2.reports;

import io.hops.hopsworks.common.featurestore.datavalidationv2.ValidationReportDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ValidationReportController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.DELETE;
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
@Api(value = "Validation report resource", description = "A service that manages a feature group's expectation suite")
public class ValidationReportResource {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private ValidationReportController validationReportController;
  @EJB
  private ValidationReportBuilder validationReportBuilder;
  @EJB
  private JWTHelper jWTHelper;

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
   * Get a single validation report
   *
   * @param validationReportId id of the validation report to fetch
   * @return JSON-representation of a validation report
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch validation report by id", response = ValidationReportDTO.class)
  @GET
  @Path("/{validationReportId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getById(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("validationReportId")
      Integer validationReportId) throws FeaturestoreException {

    ValidationReport validationReport = validationReportController.getValidationReportById(validationReportId);

    ValidationReportDTO dto = validationReportBuilder.build(
      uriInfo, project, featuregroup, validationReport);

    return Response.ok().entity(dto).build();
  }

  /**
   * Endpoint to fetch a list of validation reports attached to a featuregroup.
   *
   * @param pagination
   * @param validationReportBeanParam
   * @return JSON-array of validation reports
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch all validation report from feature group",
    response = ValidationReportDTO.class, responseContainer = "List")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam ValidationReportBeanParam validationReportBeanParam,
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo) throws FeaturestoreException {

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.VALIDATIONREPORT);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(validationReportBeanParam.getSortBySet());
    resourceRequest.setFilter(validationReportBeanParam.getFilter());

    ValidationReportDTO dtos = validationReportBuilder.build(uriInfo, resourceRequest, project, featuregroup);

    return Response.ok().entity(dtos).build();
  }

  /**
   * Endpoint to create a single validation report
   *
   * @param validationReportDTO json representation of the validation report generated by great-expectation
   * @return JSON information about the created report
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Create a validation report attached to an expectation suite", 
    response = ValidationReportDTO.class)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createValidationReport(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    ValidationReportDTO validationReportDTO) throws FeaturestoreException {

    Users user = jWTHelper.getUserPrincipal(sc);  

    ValidationReport validationReport = validationReportController.createValidationReport(
      user, featuregroup, validationReportDTO);

    ValidationReportDTO dto = validationReportBuilder.build(
      uriInfo, project, featuregroup, validationReport);

    return Response.ok().entity(dto).build();
  }

  /**
   * Endpoint for deleting a validation report with a specified id in a specified featurestore
   *
   * @param validationReportId id of the validation report
   * @throws FeaturestoreException
   */
  @DELETE
  @Path("/{validationReportId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete specific validation report")
  public Response deleteValidationReport(
    @Context
      SecurityContext sc,
    @Context
      HttpServletRequest req,
    @Context
      UriInfo uriInfo,
    @PathParam("validationReportId")
      Integer validationReportId) throws FeaturestoreException {

    Users user = jWTHelper.getUserPrincipal(sc);
    validationReportController.deleteValidationReportById(user, validationReportId);

    return Response.noContent().build();
  }
}