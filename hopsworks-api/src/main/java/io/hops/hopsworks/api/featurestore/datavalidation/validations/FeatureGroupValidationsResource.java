/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidation.validations;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationsController;
import io.hops.hopsworks.common.featurestore.featuregroup.ExpectationResult;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.ValidationResult;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Api(value = "Manage feature group data validation results.")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupValidationsResource {
  
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureGroupValidationBuilder featureGroupValidationBuilder;
  @EJB
  private FeatureGroupValidationsController featureGroupValidationsController;
  @EJB
  private JWTHelper jwtHelper;
  
  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }
  
  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }
  
  @ApiOperation(value = "Fetch the results of all data validations of a feature group",
    response = FeatureGroupValidationDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(@BeanParam Pagination pagination,
                         @BeanParam FeatureGroupValidationsBeanParam featureGroupValidationsBeanParam,
                         @Context SecurityContext sc,
                         @Context UriInfo uriInfo) throws FeaturestoreException {
  
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.VALIDATIONS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureGroupValidationsBeanParam.getSortBySet());
    resourceRequest.setFilter(featureGroupValidationsBeanParam.getFilter());
  
    FeatureGroupValidationDTO dto = featureGroupValidationBuilder
      .build(uriInfo, resourceRequest, user, project, featuregroup);
    
    return Response.ok(dto).build();
  }
  
  @ApiOperation(
    value = "Retrieve a data validation instance of a feature group, identified by validation id",
    response = FeatureGroupValidationDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(
    @ApiParam(value = "Time when a particular validation started", required = true)
    @PathParam("id") Integer id,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws FeaturestoreException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    FeatureGroupValidationDTO dto = featureGroupValidationBuilder
      .build(uriInfo, new ResourceRequest(ResourceRequest.Name.VALIDATIONS), user, project, featuregroup, id);
  
    return Response.ok(dto).build();
  }
  
  @ApiOperation(value = "Post the data validation results of the feature group",
    response = FeatureGroupValidationDTO.class)
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putValidationResults(FeatureGroupValidations featureGroupValidations,
                                       @Context SecurityContext sc,
                                       @Context UriInfo uriInfo) throws FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);

    // Workaround to ignore the list initialized from a json like {legalValues: null}
    for (ExpectationResult expectationResult : featureGroupValidations.getExpectationResults()) {
      if (expectationResult.getExpectation().getRules() != null
              && !expectationResult.getExpectation().getRules().isEmpty()) {
        expectationResult.getExpectation().getRules().stream().filter(rule -> rule.getLegalValues() != null
                && rule.getLegalValues().size() == 1
                && rule.getLegalValues().get(0) == null).forEach(rule -> rule.setLegalValues(null));
      }
      for (ValidationResult validationResult : expectationResult.getResults()) {
        if (validationResult.getRule() != null && validationResult.getRule().getLegalValues() != null
                && validationResult.getRule().getLegalValues().size() == 1
                && validationResult.getRule().getLegalValues().get(0) == null) {
          validationResult.getRule().setLegalValues(null);
        }
      }
    }
    FeatureGroupValidation featureGroupValidation =
      featureGroupValidationsController.putFeatureGroupValidationResults(user, featurestore.getProject(),
        featuregroup, featureGroupValidations.getExpectationResults(), featureGroupValidations.getValidationTime());
  
    FeatureGroupValidationDTO dto = featureGroupValidationBuilder
      .build(uriInfo, new ResourceRequest(ResourceRequest.Name.VALIDATIONS), user, project, featuregroup,
        featureGroupValidation);
  
    return Response.ok().entity(dto).build();
  }
  
  public enum Engine {
    DEEQU("deequ");
    
    private final String name;
    
    Engine(String name) {
      this.name = name;
    }
    
    public static Engine fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    public String getName() {
      return name;
    }
    
    @Override
    public String toString() {
      return name;
    }
  }

  public enum ValidationTimeType {
    VALIDATION_TIME,
    COMMIT_TIME
  }
}
