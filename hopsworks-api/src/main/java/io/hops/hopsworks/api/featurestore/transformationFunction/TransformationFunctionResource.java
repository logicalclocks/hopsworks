/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.transformationFunction;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionDTO;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Api(value = "Feature Store Transformation Function Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TransformationFunctionResource {

  @EJB
  private TransformationFunctionBuilder transformationFunctionBuilder;
  @EJB
  private TransformationFunctionController transformationFunctionController;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;
  private Featurestore featurestore;

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get transformation function DTO",
      response = TransformationFunctionDTO.class)
  public Response get(
      @BeanParam Pagination pagination,
      @BeanParam TransformationFunctionsBeanParam transformationFunctionsBeanParam,
      @Context SecurityContext sc,
      @Context UriInfo uriInfo,
      @QueryParam("name") String name,
      @QueryParam("version") Integer version) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TRANSFORMATIONFUNCTIONS);
    resourceRequest.setSort(transformationFunctionsBeanParam.getSortBySet());
    resourceRequest.setFilter(transformationFunctionsBeanParam.getFilter());
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(transformationFunctionsBeanParam.getSortBySet());
    TransformationFunctionDTO transformationFunctionDTO =
        transformationFunctionBuilder.build(uriInfo, resourceRequest, user, project, featurestore, name, version);
    return Response.ok().entity(transformationFunctionDTO).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Register transformation function in to a featurestore",
      response = TransformationFunctionDTO.class)
  public Response attach(@Context UriInfo uriInfo,
                         @Context SecurityContext sc,
                         TransformationFunctionDTO transformationFunctionDTO)
      throws IOException, FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);

    TransformationFunction transformationFunction =
        transformationFunctionController.register(user, project, featurestore,
            transformationFunctionDTO);
    TransformationFunctionDTO newTransformationFunctionDTO =
        transformationFunctionBuilder.build(uriInfo, new ResourceRequest(
            ResourceRequest.Name.TRANSFORMATIONFUNCTIONS), user, project, featurestore, transformationFunction);
    return Response.ok().entity(newTransformationFunctionDTO).build();
  }

  /**
   * Endpoint for deleting a transformation function, this will delete both the metadata and the data storage
   *
   * @param transformationFunctionId the id of the transformationFunction
   * @return JSON representation of the deleted transformation function
   * @throws FeaturestoreException
   */
  @DELETE
  @Path("/{transformationFunctionId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete a transformation function with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response delete(@Context SecurityContext sc,
                         @Context HttpServletRequest req,
                         @ApiParam(value = "Id of the transformation function dataset", required = true)
                         @PathParam("transformationFunctionId") Integer transformationFunctionId)
      throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    transformationFunctionController.delete(project, featurestore, user, transformationFunctionId);
    return Response.ok().build();
  }
}
