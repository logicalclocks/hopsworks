/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.AlertBuilder;
import io.hops.hopsworks.api.alert.AlertDTO;
import io.hops.hopsworks.api.alert.FeatureStoreAlertController;
import io.hops.hopsworks.api.alert.FeatureStoreAlertValidation;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewAlertBuilder;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewAlertDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.project.alert.ProjectAlertsDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewAlertFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "FeatureStoreAlert Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public abstract class FeatureStoreAlertResource extends FeaturestoreSubResource {
  
  private static final Logger LOGGER = Logger.getLogger(FeatureStoreAlertResource.class.getName());
  @EJB
  private FeatureGroupAlertBuilder featureGroupAlertBuilder;
  @EJB
  private FeatureGroupAlertFacade featureGroupAlertFacade;
  @EJB
  private AlertController alertController;
  @EJB
  private AlertBuilder alertBuilder;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FeatureViewAlertBuilder featureViewAlertBuilder;
  @EJB
  protected FeatureViewAlertFacade featureViewAlertFacade;
  @EJB
  protected FeatureStoreAlertController featureStoreAlertController;
  @EJB
  protected FeatureStoreAlertValidation featureStoreAlertValidation;
  
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private ProjectController projectController;
  @EJB
  private FeaturegroupController featuregroupController;

  private String featureViewName;
  private Integer featureViewVersion;
  private Integer featureGroupId;
  
  public void setFeatureView(String name, Integer version) {
    this.featureViewName = name;
    this.featureViewVersion = version;
  }
  
  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }
  
  protected FeatureView getFeatureView(Featurestore featurestore) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(featureViewName) || featureViewVersion == null) {
      return null;
    }
    return featureViewController.getByNameVersionAndFeatureStore(this.featureViewName, this.featureViewVersion,
      featurestore);
  }
  
  protected Featuregroup getFeatureGroup(Featurestore featurestore) throws FeaturestoreException {
    if (featureGroupId != null) {
      return featuregroupController.getFeaturegroupById(featurestore, this.featureGroupId);
    }
    return null;
  }

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }
  @Override
  protected FeaturestoreController getFeaturestoreController() {
    return featurestoreController;
  }

  protected abstract ResourceRequest.Name getEntityType();
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all feature store alerts.", response = RestDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(
    @BeanParam
    Pagination pagination,
    @BeanParam
    FeatureGroupAlertBeanParam featureGroupAlertBeanParam,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) throws FeaturestoreException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureGroupAlertBeanParam.getSortBySet());
    resourceRequest.setFilter(featureGroupAlertBeanParam.getFilter());
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validateEntityType(getEntityType(), featuregroup, featureView);
    FeatureGroupAlertDTO dto;
    FeatureViewAlertDTO featureViewAlertDto;
    if (getEntityType().equals(ResourceRequest.Name.FEATUREGROUPS)) {
      dto = featureGroupAlertBuilder.buildItems(uriInfo, resourceRequest, featuregroup);
      return Response.ok().entity(dto).build();
    } else {
      featureViewAlertDto = featureViewAlertBuilder.buildMany(uriInfo, resourceRequest,
        featureStoreAlertController.retrieveManyAlerts(resourceRequest, featureView));
      return Response.ok().entity(featureViewAlertDto).build();
    }
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find feature store alert by Id.", response = RestDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getById(
    @PathParam("id")
    Integer id,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc)
    throws FeaturestoreException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validateEntityType(getEntityType(), featuregroup, featureView);
    if (getEntityType().equals(ResourceRequest.Name.FEATUREGROUPS)) {
      FeatureGroupAlertDTO dto = featureGroupAlertBuilder.build(uriInfo, resourceRequest, featuregroup, id);
      return Response.ok().entity(dto).build();
    } else {
      FeatureViewAlertDTO dto = featureViewAlertBuilder.buildFeatureViewAlertDto(uriInfo, resourceRequest,
        featureStoreAlertController.retrieveSingleAlert(id, featureView));
      return Response.ok().entity(dto).build();
    }
  }
  
  @GET
  @Path("values")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get values for feature store alert.", response = FeatureGroupAlertValues.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAvailableServices(
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) {
    FeatureGroupAlertValues values = new FeatureGroupAlertValues();
    return Response.ok().entity(values).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a feature store alert.", response = PostableFeatureStoreAlerts.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response create(PostableFeatureStoreAlerts dto,
    @QueryParam("bulk")
    @DefaultValue("false")
    Boolean bulk,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) throws FeaturestoreException, AlertException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validateEntityType(getEntityType(), featuregroup, featureView);
    if (getEntityType().equals(ResourceRequest.Name.FEATUREGROUPS)) {
      FeatureGroupAlertDTO featureGroupAlertDTO = createAlert(dto, bulk, uriInfo, resourceRequest);
      return Response.created(featureGroupAlertDTO.getHref()).entity(featureGroupAlertDTO).build();
    } else {
      featureStoreAlertValidation.validateFeatureViewRequest(dto, getEntityType());
      FeatureViewAlertDTO fvDTO = createFeatureViewAlert(dto, bulk, uriInfo, resourceRequest);
      return Response.created(fvDTO.getHref()).entity(fvDTO).build();
    }
  }
  
  
  
  private FeatureGroupAlertDTO createAlert(PostableFeatureStoreAlerts featureGroupAlertDTO, Boolean bulk,
    UriInfo uriInfo, ResourceRequest resourceRequest) throws FeaturestoreException, ProjectException {
    FeatureGroupAlertDTO dto;
    if (bulk) {
      featureStoreAlertValidation.validateBulk(featureGroupAlertDTO);
      dto = new FeatureGroupAlertDTO();
      for (PostableFeatureStoreAlerts pa : featureGroupAlertDTO.getItems()) {
        dto.addItem(createAlert(pa, uriInfo, resourceRequest));
      }
      dto.setCount((long) featureGroupAlertDTO.getItems().size());
    } else {
      dto = createAlert(featureGroupAlertDTO, uriInfo, resourceRequest);
    }
    return dto;
  }
  
  private FeatureGroupAlertDTO createAlert(PostableFeatureStoreAlerts dto, UriInfo uriInfo,
    ResourceRequest resourceRequest) throws FeaturestoreException, ProjectException {
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validate(dto, featuregroup, featureView);
    FeatureGroupAlert featureGroupAlert =
      featureStoreAlertController.persistFeatureGroupEntityValues(dto, featuregroup);
    featureStoreAlertController.createRoute(project,featureGroupAlert);
    return featureGroupAlertBuilder.buildItems(uriInfo, resourceRequest, featureGroupAlert);
  }
  
  @POST
  @Path("{id}/test")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Test alert by Id.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTestById(
    @PathParam("id")
    Integer id,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) throws AlertException, ProjectException {
    List<Alert> alerts;
    Project project = getProject();
    FeatureGroupAlert featureGroupAlert=null;
    FeatureViewAlert featureViewAlert=null;
    try {
      if (getEntityType().equals(ResourceRequest.Name.FEATUREGROUPS)) {
        featureGroupAlert = featureGroupAlertFacade.find(id);
        if (featureGroupAlert == null) {
          throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
            "Alert not found " + id);
        }
        alerts = alertController.testAlert(project, featureGroupAlert);
      } else {
        featureViewAlert = featureViewAlertFacade.find(id);
        if (featureViewAlert == null) {
          throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
            "Alert not found " + id);
        }
        alerts = alertController.testAlert(project, featureViewAlert);
      }
    } catch (AlertManagerUnreachableException | AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.SEVERE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.SEVERE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.SEVERE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    AlertDTO alertDTO =
      alertBuilder.getAlertDTOs(uriInfo, resourceRequest, alerts, project);
    return Response.ok().entity(alertDTO).build();
  }
  
  @DELETE
  @Path("{id}")
  @ApiOperation(value = "Delete feature store alert by Id.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(
    @PathParam("id")
    Integer id,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) throws FeaturestoreException, ProjectException {
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validateEntityType(getEntityType(), featuregroup, featureView);
    if (getEntityType().equals(ResourceRequest.Name.FEATUREGROUPS)) {
      FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(featuregroup, id);
      if (featureGroupAlert != null) {
        featureStoreAlertController.deleteRoute(featureGroupAlert, project);
        featureGroupAlertFacade.remove(featureGroupAlert);
      }
      return Response.noContent().build();
    } else {
      FeatureViewAlert featureViewAlert = featureViewAlertFacade.findByFeatureViewAndId(featureView, id);
      if (featureViewAlert != null) {
        featureStoreAlertController.deleteRoute(featureViewAlert, project);
        featureViewAlertFacade.remove(featureViewAlert);
      }
      return Response.noContent().build();
    }
  }
  
  private FeatureViewAlertDTO createFeatureViewAlert(PostableFeatureStoreAlerts paDTO, Boolean bulk,
    UriInfo uriInfo, ResourceRequest resourceRequest) throws FeaturestoreException, ProjectException {
    FeatureViewAlertDTO dto;
    if (bulk) {
      featureStoreAlertValidation.validateBulk(paDTO);
      dto = new FeatureViewAlertDTO();
      for (PostableFeatureStoreAlerts pa : paDTO.getItems()) {
        dto.addItem(createFeatureViewAlert(pa, uriInfo, resourceRequest));
      }
      dto.setCount((long) paDTO.getItems().size());
    } else {
      dto = createFeatureViewAlert(paDTO, uriInfo, resourceRequest);
    }
    return dto;
  }
  
  private FeatureViewAlertDTO createFeatureViewAlert(PostableFeatureStoreAlerts dto, UriInfo uriInfo,
    ResourceRequest resourceRequest) throws FeaturestoreException, ProjectException {
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Featuregroup featuregroup = getFeatureGroup(featurestore);
    FeatureView featureView = getFeatureView(featurestore);
    featureStoreAlertValidation.validate(dto, featuregroup, featureView);
    FeatureViewAlert featureViewAlert;
    FeatureViewAlertDTO fvDTO;
    featureViewAlert = featureStoreAlertController.persistFeatureViewEntityValues(dto, featureView);
    fvDTO = featureViewAlertBuilder.buildFeatureViewAlertDto(uriInfo, resourceRequest, featureViewAlert);
    featureStoreAlertController.createRoute(project,featureViewAlert);
    return fvDTO;
  }
}