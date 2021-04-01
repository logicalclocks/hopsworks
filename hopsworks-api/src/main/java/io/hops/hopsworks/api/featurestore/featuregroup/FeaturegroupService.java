/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordResource;
import io.hops.hopsworks.api.featurestore.commit.CommitResource;
import io.hops.hopsworks.api.featurestore.activities.ActivityResource;
import io.hops.hopsworks.api.featurestore.datavalidation.expectations.fg.FeatureGroupExpectationsResource;
import io.hops.hopsworks.api.featurestore.datavalidation.validations.FeatureGroupValidationsResource;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.featurestore.tag.TagsBuilder;
import io.hops.hopsworks.api.featurestore.tag.TagsDTO;
import io.hops.hopsworks.api.featurestore.tag.TagsExpansionBeanParam;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.ProvArtifactResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionJob;
import io.hops.hopsworks.common.featurestore.tag.AttachTagResult;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagControllerIface;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.ValidationType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A Stateless RESTful service for the featuregroups in a featurestore on Hopsworks.
 * Base URL: project/projectId/featurestores/featurestoreId/featuregroups/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Featuregroup service", description = "A service that manages a feature store's feature groups")
public class FeaturegroupService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private TagsBuilder tagBuilder;
  @Inject
  private FeatureStoreTagControllerIface tagController;
  @Inject
  private FeatureGroupPreviewResource featureGroupPreviewResource;
  @Inject
  private FeatureGroupPartitionResource featureGroupPartitionResource;
  @Inject
  private FeatureGroupDetailsResource featureGroupDetailsResource;
  @Inject
  private StatisticsResource statisticsResource;
  @Inject
  private CommitResource commitResource;
  @Inject
  private ProvArtifactResource provenanceResource;
  @Inject
  private FsJobManagerController fsJobManagerController;
  @Inject
  private IngestionJobBuilder ingestionJobBuilder;
  @Inject
  private FeaturestoreKeywordResource featurestoreKeywordResource;
  @Inject
  private ActivityResource activityResource;
  @Inject
  private FeatureGroupExpectationsResource featureGroupExpectationsResource;
  @Inject
  private FeatureGroupValidationsResource featureGroupValidationsResource;

  private Project project;
  private Featurestore featurestore;
  private static final Logger LOGGER = Logger.getLogger(FeaturegroupService.class.getName());

  /**
   * Set the project of the featurestore (provided by parent resource)
   *
   * @param project the project where the featurestore resides
   */
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Sets the featurestore of the featuregroups (provided by parent resource)
   *
   * @param featurestoreId id of the featurestore
   * @throws FeaturestoreException
   */
  public void setFeaturestoreId(Integer featurestoreId) throws FeaturestoreException {
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    this.featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
  }

  /**
   * Endpoint for getting all featuregroups of a featurestore
   *
   * @return list of JSON featuregroups
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of feature groups for a featurestore",
      response = FeaturegroupDTO.class,
      responseContainer = "List")
  public Response getFeaturegroupsForFeaturestore(
          @BeanParam FeatureGroupBeanParam featureGroupBeanParam,
          @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<FeaturegroupDTO> featuregroups = featuregroupController.
        getFeaturegroupsForFeaturestore(featurestore, project, user, featureGroupBeanParam.getFilterValues());
    GenericEntity<List<FeaturegroupDTO>> featuregroupsGeneric =
        new GenericEntity<List<FeaturegroupDTO>>(featuregroups) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupsGeneric).build();
  }

  /**
   * Endpoint for creating a new featuregroup in a featurestore
   *
   * @param featuregroupDTO JSON payload for the new featuregroup
   * @return JSON information about the created featuregroup
   * @throws HopsSecurityException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create feature group in a featurestore",
      response = FeaturegroupDTO.class)
  public Response createFeaturegroup(@Context SecurityContext sc, FeaturegroupDTO featuregroupDTO)
      throws FeaturestoreException, ServiceException, KafkaException, SchemaException, ProjectException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if(featuregroupDTO == null) {
      throw new IllegalArgumentException("Input JSON for creating a new Feature Group cannot be null");
    }
    try {
      if (featuregroupController.featuregroupExists(featurestore, featuregroupDTO)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_EXISTS, Level.INFO,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId());
      }
      FeaturegroupDTO createdFeaturegroup = featuregroupController.createFeaturegroup(featurestore, featuregroupDTO,
        project, user);
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(createdFeaturegroup) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featuregroupGeneric).build();
    } catch (SQLException | ProvenanceException | IOException | HopsSecurityException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId(), e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the featuregroup
   */
  @Deprecated
  @GET
  @Path("/{featuregroupId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get specific featuregroup from a specific featurestore",
      response = FeaturegroupDTO.class)
  public Response getFeatureGroup(@ApiParam(value = "Id of the featuregroup", required = true)
                                  @PathParam("featuregroupId") Integer featuregroupId, @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    verifyIdProvided(featuregroupId);
    FeaturegroupDTO featuregroupDTO =
        featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId, project, user);
    GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
  }

  /**
   * Retrieve a specific feature group based name. Allow filtering on version.
   *
   * @param name name of the featuregroup
   * @param version queryParam with the desired version
   * @return JSON representation of the featuregroup
   */
  @GET
  // Anything else that is not just number should use this endpoint
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a list of feature groups with a specific name, filter by version",
      response = FeaturegroupDTO.class)
  public Response getFeatureGroup(@ApiParam(value = "Name of the feature group", required = true)
                                  @PathParam("name") String name,
                                  @ApiParam(value = "Filter by a specific version")
                                  @QueryParam("version") Integer version, @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    verifyNameProvided(name);
    List<FeaturegroupDTO> featuregroupDTO;
    if (version == null) {
      featuregroupDTO = featuregroupController.getFeaturegroupWithNameAndFeaturestore(
        featurestore, name, project, user);
    } else {
      featuregroupDTO = Arrays.asList(featuregroupController
          .getFeaturegroupWithNameVersionAndFeaturestore(featurestore, name, version, project, user));
    }
    GenericEntity<List<FeaturegroupDTO>> featuregroupGeneric =
        new GenericEntity<List<FeaturegroupDTO>>(featuregroupDTO) {};
    return Response.ok().entity(featuregroupGeneric).build();
  }

  /**
   * Endpoint for deleting a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the deleted featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featuregroupId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete specific featuregroup from a specific featurestore")
  public Response deleteFeatureGroup(@Context SecurityContext sc,
      @ApiParam(value = "Id of the featuregroup", required = true) @PathParam("featuregroupId") Integer featuregroupId)
      throws FeaturestoreException, ServiceException, SchemaException, KafkaException {
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    //Verify that the user has the data-owner role or is the creator of the featuregroup
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    try {
      featuregroupController.deleteFeaturegroup(featuregroup, project, user);
      return Response.ok().build();
    } catch (SQLException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for deleting the contents of the featuregroup.
   * As HopsHive do not support ACID transactions the way to delete the contents of a table is to drop the table and
   * re-create it, which also will drop the featuregroup metadata due to ON DELETE CASCADE foreign key rule.
   * This method stores the metadata of the featuregroup before deleting it and then re-creates the featuregroup with
   * the same metadata.
   * <p>
   * This endpoint is typically used when the user wants to insert data into a featuregroup with the write-mode
   * 'overwrite' instead of default mode 'append'
   *
   * @param featuregroupId the id of the featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featuregroupId}/clear")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete featuregroup contents")
  public Response deleteFeaturegroupContents(@Context SecurityContext sc,
      @ApiParam(value = "Id of the featuregroup", required = true) @PathParam("featuregroupId") Integer featuregroupId)
      throws FeaturestoreException, ServiceException, KafkaException, SchemaException, ProjectException, UserException {
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    //Verify that the user has the data-owner role or is the creator of the featuregroup
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    try {
      featuregroupController.clearFeaturegroup(featuregroup, project, user);
      return Response.ok().build();
    } catch (SQLException | IOException | ProvenanceException | HopsSecurityException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CLEAR_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for updating the featuregroup metadata without changing the schema.
   * Since the schema is not changed, the data does not need to be dropped.
   *
   * @param featuregroupId id of the featuregroup to update
   * @param featuregroupDTO updated metadata
   * @return JSON representation of the updated featuregroup
   * @throws FeaturestoreException
   */
  @PUT
  @Path("/{featuregroupId: [0-9]+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Update featuregroup contents",
      response = FeaturegroupDTO.class)
  public Response updateFeaturegroup(@Context SecurityContext sc,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId,
      @ApiParam(value = "updateMetadata", example = "true")
      @QueryParam("updateMetadata") @DefaultValue("false") Boolean updateMetadata,
      @ApiParam(value = "enableOnline", example = "true")
      @QueryParam("enableOnline") @DefaultValue("false") Boolean enableOnline,
      @ApiParam(value = "disableOnline", example = "true")
      @QueryParam("disableOnline") @DefaultValue("false") Boolean disableOnline,
      @ApiParam(value = "updateStatsConfig", example = "true")
      @QueryParam("updateStatsConfig") @DefaultValue("false") Boolean updateStatsConfig,
      @ApiParam(value = "validationType", example = "NONE")
      @QueryParam("validationType") ValidationType validationType,
      FeaturegroupDTO featuregroupDTO)
      throws FeaturestoreException, SQLException, ProvenanceException, ServiceException, SchemaException,
      KafkaException, ProjectException, UserException, IOException, HopsSecurityException {
    if(featuregroupDTO == null) {
      throw new IllegalArgumentException("Input JSON for updating Feature Group cannot be null");
    }
    verifyIdProvided(featuregroupId);
    featuregroupDTO.setId(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    FeaturegroupDTO updatedFeaturegroupDTO = null;
    if(updateMetadata) {
      updatedFeaturegroupDTO = featuregroupController.updateFeaturegroupMetadata(project, user, featurestore,
        featuregroupDTO);
    }
    if(enableOnline && featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        !(featuregroup.getCachedFeaturegroup().isOnlineEnabled())) {
      updatedFeaturegroupDTO =
          featuregroupController.enableFeaturegroupOnline(featurestore, featuregroupDTO, project, user);
    }
    if(disableOnline && featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().isOnlineEnabled()){
      updatedFeaturegroupDTO = featuregroupController.disableFeaturegroupOnline(featuregroup, project, user);
    }
    if(updateStatsConfig) {
      updatedFeaturegroupDTO = featuregroupController.updateFeatureGroupStatsConfig(
        featurestore, featuregroupDTO, project, user);
    }
    if(validationType != null) {
      updatedFeaturegroupDTO = featuregroupController.updateValidationType(featuregroup, validationType, project, user);
    }
    if(updatedFeaturegroupDTO != null) {
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(updatedFeaturegroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
    } else {
      GenericEntity<FeaturegroupDTO> featuregroupGeneric = new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
    }
  }
  
  /**
   * Verify that the user id was provided as a path param
   *
   * @param featuregroupId the feature group id to verify
   */
  private void verifyIdProvided(Integer featuregroupId) {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
  }

  /**
   * Verify that the name was provided as a path param
   *
   * @param featureGroupName the feature group name to verify
   */
  private void verifyNameProvided(String featureGroupName) {
    if (Strings.isNullOrEmpty(featureGroupName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NAME_NOT_PROVIDED.getMessage());
    }
  }
  
  @ApiOperation( value = "Create or update one tag for a featuregroup", response = TagsDTO.class)
  @PUT
  @Path("/{featuregroupId}/tags/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the featuregroup", required = true)
                         @PathParam("featuregroupId") Integer featuregroupId,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @ApiParam(value = "Value to set for the tag") String value)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    AttachTagResult result = tagController.upsert(project, user, featurestore, featuregroup, name, value);
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
      featurestore.getId(), ResourceRequest.Name.FEATUREGROUPS.name(), featuregroupId, result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Create or update tags(bulk) for a featuregroup", response = TagsDTO.class)
  @PUT
  @Path("/{featuregroupId}/tags")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response bulkPutTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                              @ApiParam(value = "Id of the featuregroup", required = true)
                              @PathParam("featuregroupId") Integer featuregroupId,
                              TagsDTO tags)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    AttachTagResult result;
    
    if(tags.getItems().size() == 0) {
      result = tagController.upsert(project, user, featurestore, featuregroup, tags.getName(), tags.getValue());
    } else {
      Map<String, String> newTags = new HashMap<>();
      for(TagsDTO tag : tags.getItems()) {
        newTags.put(tag.getName(), tag.getValue());
      }
      result = tagController.upsert(project, user, featurestore, featuregroup, newTags);
    }
  
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
      featurestore.getId(), ResourceRequest.Name.FEATUREGROUPS.name(), featuregroupId, result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Get all tags attached to a featuregroup", response = TagsDTO.class)
  @GET
  @Path("/{featuregroupId}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @ApiParam(value = "Id of the featuregroup", required = true)
                          @PathParam("featuregroupId") Integer featuregroupId,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, FeaturestoreException, FeatureStoreTagException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    Map<String, String> result = tagController.getAll(project, user, featurestore, featuregroup);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
        featurestore.getId(), ResourceRequest.Name.FEATUREGROUPS.name(), featuregroupId, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Get tag attached to a featuregroup", response = TagsDTO.class)
  @GET
  @Path("/{featuregroupId}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the featuregroup", required = true)
                         @PathParam("featuregroupId") Integer featuregroupId,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, FeaturestoreException, FeatureStoreTagException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    Map<String, String> result = tagController.get(project, user, featurestore, featuregroup, name);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
        featurestore.getId(), ResourceRequest.Name.FEATUREGROUPS.name(), featuregroupId, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Delete all attached tags to featuregroup")
  @DELETE
  @Path("/{featuregroupId}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @ApiParam(value = "Id of the featuregroup", required = true)
                             @PathParam("featuregroupId") Integer featuregroupId)
    throws DatasetException, MetadataException, FeaturestoreException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    tagController.deleteAll(project, user, featurestore, featuregroup);

    return Response.noContent().build();
  }

  @ApiOperation( value = "Delete tag attached to featuregroup")
  @DELETE
  @Path("/{featuregroupId}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @ApiParam(value = "Id of the featuregroup", required = true)
                            @PathParam("featuregroupId") Integer featuregroupId,
                            @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name)
    throws DatasetException, MetadataException, FeaturestoreException {
    
    verifyIdProvided(featuregroupId);
    Users user = jWTHelper.getUserPrincipal(sc);
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    tagController.delete(project, user, featurestore, featuregroup, name);

    return Response.noContent().build();
  }

  @POST
  @Path("/{featuregroupId}/ingestion")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Prepares environment for uploading data to ingest into the feature group",
      response = IngestionJobDTO.class)
  public Response ingestionJob(@Context SecurityContext sc,
                               @Context UriInfo uriInfo,
                               @ApiParam(value = "Id of the featuregroup", required = true)
                               @PathParam("featuregroupId") Integer featuregroupId,
                               IngestionJobConf ingestionJobConf)
      throws DatasetException, HopsSecurityException, FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    verifyIdProvided(featuregroupId);

    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    Map<String, String> dataOptions = null;
    if (ingestionJobConf.getDataOptions() != null) {
      dataOptions = ingestionJobConf.getDataOptions().stream()
          .collect(Collectors.toMap(OptionDTO::getName, OptionDTO::getValue));
    }

    Map<String, String> writeOptions = null;
    if (ingestionJobConf.getWriteOptions() != null) {
      dataOptions = ingestionJobConf.getWriteOptions().stream()
          .collect(Collectors.toMap(OptionDTO::getName, OptionDTO::getValue));
    }

    IngestionJob ingestionJob = fsJobManagerController.setupIngestionJob(project, user, featuregroup,
        ingestionJobConf.getSparkJobConfiguration(), ingestionJobConf.getDataFormat(),
        writeOptions, dataOptions);
    IngestionJobDTO ingestionJobDTO = ingestionJobBuilder.build(uriInfo, project, featuregroup, ingestionJob);
    return Response.ok().entity(ingestionJobDTO).build();
  }

  @Path("/{featuregroupId}/details")
  public FeatureGroupDetailsResource getFeatureGroupDetails(
      @ApiParam(value = "Id of the featuregroup") @PathParam("featuregroupId") Integer featuregroupId)
      throws FeaturestoreException {
    FeatureGroupDetailsResource fgDetailsResource = featureGroupDetailsResource.setProject(project);
    fgDetailsResource.setFeaturestore(featurestore);
    fgDetailsResource.setFeatureGroupId(featuregroupId);
    return fgDetailsResource;
  }

  @Path("/{featuregroupId}/preview")
  public FeatureGroupPreviewResource getFeatureGroupPreview(
      @ApiParam(value = "Id of the featuregroup") @PathParam("featuregroupId") Integer featuregroupId)
      throws FeaturestoreException {
    FeatureGroupPreviewResource fgPreviewResource = featureGroupPreviewResource.setProject(project);
    fgPreviewResource.setFeaturestore(featurestore);
    fgPreviewResource.setFeatureGroupId(featuregroupId);
    return fgPreviewResource;
  }

  @Path("/{featuregroupId}/partitions")
  public FeatureGroupPartitionResource getFeatureGroupPartitions(
      @ApiParam(value = "Id of the featuregroup") @PathParam("featuregroupId") Integer featuregroupId)
      throws FeaturestoreException {
    FeatureGroupPartitionResource partitionResource = featureGroupPartitionResource.setProject(project);
    partitionResource.setFeaturestore(featurestore);
    partitionResource.setFeatureGroupId(featuregroupId);
    return partitionResource;
  }

  @Path("/{featureGroupId}/statistics")
  public StatisticsResource statistics(@PathParam("featureGroupId") Integer featureGroupId)
      throws FeaturestoreException {
    this.statisticsResource.setProject(project);
    this.statisticsResource.setFeaturestore(featurestore);
    this.statisticsResource.setFeatureGroupId(featureGroupId);
    return statisticsResource;
  }
  
  @Path("/{featureGroupId}/provenance")
  public ProvArtifactResource provenance(@PathParam("featureGroupId") Integer featureGroupId)
    throws FeaturestoreException {
    Dataset targetEndpoint = featurestoreController.getProjectFeaturestoreDataset(featurestore.getProject());
    this.provenanceResource.setContext(project, targetEndpoint);
    Featuregroup fg = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
    this.provenanceResource.setArtifactId(fg.getName(), fg.getVersion());
    return provenanceResource;
  }
  
  @Path("/{featureGroupId}/expectations")
  public FeatureGroupExpectationsResource expectations(@PathParam("featureGroupId") Integer featureGroupId)
    throws FeaturestoreException {
    this.featureGroupExpectationsResource.setProject(project);
    this.featureGroupExpectationsResource.setFeaturestore(featurestore);
    this.featureGroupExpectationsResource.setFeatureGroup(featureGroupId);
    return featureGroupExpectationsResource;
  }
  
  @Path("/{featureGroupId}/validations")
  public FeatureGroupValidationsResource validationResults(@PathParam("featureGroupId") Integer featureGroupId)
    throws FeaturestoreException {
    this.featureGroupValidationsResource.setProject(project);
    this.featureGroupValidationsResource.setFeaturestore(featurestore);
    this.featureGroupValidationsResource.setFeatureGroupId(featureGroupId);
    return featureGroupValidationsResource;
  }

  @Path("/{featureGroupId}/commits")
  public CommitResource timetravel (
      @ApiParam(value = "Id of the featuregroup") @PathParam("featureGroupId") Integer featureGroupId)
      throws FeaturestoreException {
    this.commitResource.setProject(project);
    this.commitResource.setFeaturestore(featurestore);
    this.commitResource.setFeatureGroup(featureGroupId);
    return commitResource;
  }


  @Path("/{featureGroupId}/keywords")
  public FeaturestoreKeywordResource keywords (
      @ApiParam(value = "Id of the featuregroup") @PathParam("featureGroupId") Integer featureGroupId)
      throws FeaturestoreException {
    this.featurestoreKeywordResource.setProject(project);
    this.featurestoreKeywordResource.setFeaturestore(featurestore);
    this.featurestoreKeywordResource.setFeatureGroupId(featureGroupId);
    return featurestoreKeywordResource;
  }

  @Path("/{featureGroupId}/activity")
  public ActivityResource activity(@ApiParam(value = "Id of the feature group")
                                     @PathParam("featureGroupId") Integer featureGroupId)
      throws FeaturestoreException {
    this.activityResource.setProject(project);
    this.activityResource.setFeaturestore(featurestore);
    this.activityResource.setFeatureGroupId(featureGroupId);
    return this.activityResource;
  }
}