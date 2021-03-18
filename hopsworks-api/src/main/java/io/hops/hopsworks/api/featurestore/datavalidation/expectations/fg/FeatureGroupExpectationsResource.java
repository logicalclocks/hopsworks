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

package io.hops.hopsworks.api.featurestore.datavalidation.expectations.fg;

import io.hops.hopsworks.api.featurestore.datavalidation.expectations.ExpectationDTO;
import io.hops.hopsworks.api.featurestore.datavalidation.expectations.fs.FeatureStoreExpectationsBuilder;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationsController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupExpectation;
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

@Api(value = "Manage feature group expectations.")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupExpectationsResource {

  @EJB
  private FeatureGroupValidationsController featureGroupValidationsController;
  @EJB
  private FeatureStoreExpectationsBuilder featureStoreExpectationsBuilder;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private JWTHelper jWTHelper;
  
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
  
  @ApiOperation(value = "Fetch expectations of the feature group", response = ExpectationDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam FeatureGroupExpectationsBeanParam featureGroupExpectationsBeanParam,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPECTATIONS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureGroupExpectationsBeanParam.getSortBySet());
    resourceRequest.setExpansions(featureGroupExpectationsBeanParam.getExpansions().getResources());
    resourceRequest.setField(featureGroupExpectationsBeanParam.getFieldSet());

    ExpectationDTO dto = featureStoreExpectationsBuilder.build(uriInfo, resourceRequest, project, featuregroup);
    
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
    @BeanParam FeatureGroupExpectationsBeanParam featureGroupExpectationsBeanParam,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws FeaturestoreException {
  
    FeatureGroupExpectation expectation =
      featureGroupValidationsController.getFeatureGroupExpectation(featuregroup, name);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXPECTATIONS);
    resourceRequest.setField(featureGroupExpectationsBeanParam.getFieldSet());

    ExpectationDTO dto = featureStoreExpectationsBuilder.build(uriInfo,resourceRequest, project, featuregroup,
                                                               expectation);
    
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Attach an expectation to a feature group.", response = ExpectationDTO.class)
  @PUT
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response attachExpectation(
    @ApiParam(value = "name of the expectation", required = true) @PathParam("name") String name,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    FeatureGroupExpectation expectation = featureGroupValidationsController.attachExpectation(featuregroup, name,
            project, user);
  
    ExpectationDTO dto = featureStoreExpectationsBuilder.build(uriInfo,
      new ResourceRequest(ResourceRequest.Name.EXPECTATIONS),
      project, featurestore, expectation.getFeatureStoreExpectation());
    
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Detach the expectation with the given name from the feature group.")
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response remove(
    @ApiParam(value = "name of the expectation", required = true) @PathParam("name") String name,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws FeaturestoreException {

    featureGroupValidationsController.detachExpectation(featuregroup, name);

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
