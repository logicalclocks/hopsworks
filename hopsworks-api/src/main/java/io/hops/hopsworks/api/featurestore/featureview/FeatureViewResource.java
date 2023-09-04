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

package io.hops.hopsworks.api.featurestore.featureview;

import com.google.api.client.util.Sets;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.featureview.ServingKeyDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.ServingKey;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Logged
@RequestScoped
@Api(value = "Feature View Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewResource {

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FeatureViewBuilder featureViewBuilder;

  private Project project;
  private Featurestore featurestore;

  public FeatureViewResource() {
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Create Feature View metadata.", response = FeatureViewDTO.class)
  public Response create(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      FeatureViewDTO featureViewDTO) throws FeaturestoreException, ServiceException, IOException,
                                            FeatureStoreMetadataException, MetadataException, DatasetException {
    if (featureViewDTO == null) {
      throw new IllegalArgumentException("Input JSON for creating a new Feature View cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);

    FeatureView featureView = featureViewBuilder.convertFromDTO(project, featurestore, user, featureViewDTO);
    featureView = featureViewController.createFeatureView(project, user, featureView, featurestore);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.FEATUREVIEW);
    Set<ResourceRequest> expansions = Sets.newHashSet();
    expansions.add(new ResourceRequest(ResourceRequest.Name.QUERY));
    expansions.add(new ResourceRequest(ResourceRequest.Name.FEATURES));
    resourceRequest.setExpansions(expansions);
    featureViewDTO = featureViewBuilder.build(featureView, resourceRequest, project, user, uriInfo);

    return Response.created(featureViewDTO.getHref()).entity(featureViewDTO).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get all Feature View metadata.", response = FeatureViewDTO.class)
  public Response getAll(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
        FeatureViewBeanParam param
  ) throws FeaturestoreException, ServiceException, MetadataException, DatasetException,
           FeatureStoreMetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    List<FeatureView> featureViews = featureViewController.getByFeatureStore(featurestore,
        convertToQueryParam(resourceRequest));

    return Response.ok()
        .entity(featureViewBuilder.build(featureViews, resourceRequest, project, user, uriInfo))
        .build();
  }

  @GET
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get Feature View metadata by name.", response = FeatureViewDTO.class)
  public Response getByName(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
        FeatureViewBeanParam param,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name
  ) throws FeaturestoreException, ServiceException, MetadataException, DatasetException,
           FeatureStoreMetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    List<FeatureView> featureViews = featureViewController.getByNameAndFeatureStore(name, featurestore,
        convertToQueryParam(resourceRequest));

    return Response.ok()
        .entity(featureViewBuilder.build(featureViews, resourceRequest, project, user, uriInfo))
        .build();
  }

  @GET
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get Feature View metadata by name and version.", response = FeatureViewDTO.class)
  public Response getByNameVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
        FeatureViewBeanParam param,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException, ServiceException, MetadataException, DatasetException,
           FeatureStoreMetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    FeatureView featureView = featureViewController.getByNameVersionAndFeatureStore(name, version, featurestore);

    return Response.ok()
        .entity(featureViewBuilder.build(featureView, resourceRequest, project, user, uriInfo))
        .build();
  }

  @GET
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/servingKeys")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get Feature View metadata by name and version.", response = FeatureViewDTO.class)
  public Response getServingKeys(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
          FeatureViewBeanParam param,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    FeatureView featureView = featureViewController.getByNameVersionAndFeatureStore(name, version, featurestore);
    List<ServingKey> servingKeys = featureViewController.getServingKeys(project, user, featureView);

    return Response.ok()
        .entity(servingKeys.stream().map(ServingKeyDTO::new).collect(Collectors.toList()))
        .build();
  }

  @DELETE
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete Feature View metadata by name.")
  public Response deleteName(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name
  ) throws FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    featureViewController.delete(user, project, featurestore, name);

    return Response.ok().build();
  }

  @DELETE
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete Feature View metadata by name and version.")
  public Response deleteNameVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    featureViewController.delete(user, project, featurestore, name, version);

    return Response.ok().build();
  }

  @PUT
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Update Feature View metadata.")
  public Response update(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
          FeatureViewBeanParam param,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String name,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version,
      FeatureViewDTO featureViewDTO) throws FeaturestoreException, ServiceException,
                                            FeatureStoreMetadataException, MetadataException, DatasetException {
    if (featureViewDTO == null) {
      throw new IllegalArgumentException("Input JSON for updating a Feature View cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    FeatureView featureView = featureViewController.update(user, project, featurestore, name, version,
      featureViewDTO.getDescription());

    return Response.ok()
        .entity(featureViewBuilder.build(featureView, resourceRequest, project, user, uriInfo))
        .build();
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  private ResourceRequest makeResourceRequest(FeatureViewBeanParam param) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.FEATUREVIEW);
    resourceRequest.setOffset(param.getPagination().getOffset());
    resourceRequest.setLimit(param.getPagination().getLimit());
    resourceRequest.setSort(param.getParsedSortBy());
    resourceRequest.setFilter(param.getFilters());
    resourceRequest.setExpansions(param.getExpansion().getResources());
    return resourceRequest;
  }

  private QueryParam convertToQueryParam(ResourceRequest resourceRequest) {
    return new QueryParam(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter() == null ? new HashSet<>() : new HashSet<>(resourceRequest.getFilter()),
        resourceRequest.getSort() == null ? new HashSet<>() : new HashSet<>(resourceRequest.getSort())
    );
  }
}
