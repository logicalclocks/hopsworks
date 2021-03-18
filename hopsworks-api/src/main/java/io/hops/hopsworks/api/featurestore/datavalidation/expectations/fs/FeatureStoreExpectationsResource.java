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

package io.hops.hopsworks.api.featurestore.datavalidation.expectations.fs;

import io.hops.hopsworks.api.featurestore.datavalidation.expectations.ExpectationDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.datavalidation.Expectation;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureStoreExpectation;
import io.hops.hopsworks.persistence.entity.project.Project;
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
import javax.ws.rs.DELETE;
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

@Api(value = "Manage data validation rules of a feature group.")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreExpectationsResource {

  @EJB
  private FeatureGroupValidationsController featureGroupValidationsController;
  @EJB
  private FeatureStoreExpectationsBuilder featureStoreExpectationsBuilder;
  @EJB
  private FeaturestoreController featurestoreController;

  private Project project;
  private Featurestore featurestore;

  public void setProject(Project project) {
    this.project = project;
  }
  
  /**
   * Sets the featurestore of the featuregroups (provided by parent resource)
   *
   * @param featurestoreId id of the featurestore
   * @throws FeaturestoreException FeaturestoreException
   */
  public void setFeaturestoreId(Integer featurestoreId) throws FeaturestoreException {
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    this.featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
  }
  
  @ApiOperation(value = "Fetch all data validation rule attached", response = ExpectationDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
      @BeanParam Pagination pagination,
      @BeanParam FeatureStoreExpectationsBeanParam featureStoreExpectationsBeanParam,
      @Context SecurityContext sc,
      @Context UriInfo uriInfo) {

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPECTATIONS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureStoreExpectationsBeanParam.getSortBySet());
    resourceRequest.setExpansions(featureStoreExpectationsBeanParam.getExpansions().getResources());
    resourceRequest.setField(featureStoreExpectationsBeanParam.getFieldSet());

    ExpectationDTO dto = featureStoreExpectationsBuilder.build(uriInfo, resourceRequest, project, featurestore);

    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Fetch a specific data validation rule", response = ExpectationDTO.class)
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(
      @ApiParam(value = "name of the expectation", required = true) @PathParam("name") String name,
      @BeanParam FeatureStoreExpectationsBeanParam featureStoreExpectationsBeanParam,
      @Context SecurityContext sc,
      @Context UriInfo uriInfo) throws FeaturestoreException {
  
    FeatureStoreExpectation  featureStoreExpectation = featureGroupValidationsController.getFeatureStoreExpectation(
        featurestore, name);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPECTATIONS);
    resourceRequest.setExpansions(featureStoreExpectationsBeanParam.getExpansions().getResources());
    resourceRequest.setField(featureStoreExpectationsBeanParam.getFieldSet());

    ExpectationDTO dto = featureStoreExpectationsBuilder.build(uriInfo, resourceRequest,
            project, featurestore, featureStoreExpectation);

    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Create feature store expectation.", response = ExpectationDTO.class)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createExpectation(
     Expectation expectation,
     @Context SecurityContext sc,
     @Context UriInfo uriInfo) throws FeaturestoreException {

    // Workaround to ignore the list initialized from a json like {legalValues: null}
    if (expectation.getRules() != null && !expectation.getRules().isEmpty()){
      expectation.getRules().stream().filter(rule -> rule.getLegalValues() != null
              && rule.getLegalValues().size() == 1
              && rule.getLegalValues().get(0) == null).forEach(rule -> rule.setLegalValues(null));
    }
    FeatureStoreExpectation featureStoreExpectation =
      featureGroupValidationsController.createOrUpdateExpectation(featurestore,
        new io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Expectation(
          expectation.getName(), expectation.getDescription(), expectation.getFeatures(), expectation.getRules()));


    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPECTATIONS);
    ExpectationDTO dto =
      featureStoreExpectationsBuilder.build(uriInfo, resourceRequest, project, featurestore, featureStoreExpectation);

    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Delete the expectation with the given name from the feature store.")
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
      @ApiParam(value = "name of the rule", required = true) @PathParam("name") String name,
      @Context SecurityContext sc,
      @Context UriInfo uriInfo) throws FeaturestoreException {

    featureGroupValidationsController.deleteExpectation(featurestore, name);

    return Response.noContent().build();
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
}
