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
package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.AlertBuilder;
import io.hops.hopsworks.api.alert.AlertDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.project.alert.ProjectAlertsDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.ValidationRuleAlertStatus;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "FeatureGroupAlert Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupAlertResource {

  private static final Logger LOGGER = Logger.getLogger(FeatureGroupAlertResource.class.getName());

  @EJB
  private FeatureGroupAlertBuilder featureGroupAlertBuilder;
  @EJB
  private FeatureGroupAlertFacade featureGroupAlertFacade;
  @EJB
  private AlertController alertController;
  @EJB
  private AlertBuilder alertBuilder;
  @EJB
  private AlertReceiverFacade alertReceiverFacade;

  private Featuregroup featuregroup;

  public void setFeatureGroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all feature group alerts.", response = FeatureGroupAlertDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam FeatureGroupAlertBeanParam featureGroupAlertBeanParam,
                      @Context UriInfo uriInfo,
                      @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureGroupAlertBeanParam.getSortBySet());
    resourceRequest.setFilter(featureGroupAlertBeanParam.getFilter());
    FeatureGroupAlertDTO dto = featureGroupAlertBuilder.buildItems(uriInfo, resourceRequest, this.featuregroup);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find feature group alert by Id.", response = FeatureGroupAlertDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getById(@PathParam("id") Integer id,
                          @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc)
      throws FeaturestoreException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    FeatureGroupAlertDTO dto = featureGroupAlertBuilder.build(uriInfo, resourceRequest, this.featuregroup, id);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("values")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get values for feature group alert.", response = FeatureGroupAlertValues.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAvailableServices(@Context UriInfo uriInfo,
                                       @Context HttpServletRequest req,
                                       @Context SecurityContext sc)  {
    FeatureGroupAlertValues values = new FeatureGroupAlertValues();
    return Response.ok().entity(values).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a feature group alert.", response = FeatureGroupAlertDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createOrUpdate(@PathParam("id") Integer id,
                                 FeatureGroupAlertDTO dto,
                                 @Context UriInfo uriInfo,
                                 @Context SecurityContext sc) throws FeaturestoreException {
    FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(this.featuregroup, id);
    if (featureGroupAlert == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE);
    }
    if (dto == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "No payload.");
    }
    if (dto.getStatus() != null) {
      if (!dto.getStatus().equals(featureGroupAlert.getStatus()) &&
          featureGroupAlertFacade.findByFeatureGroupAndStatus(this.featuregroup, dto.getStatus()) != null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
            "Feature Group Alert with FeatureGroupName=" + this.featuregroup.getName() + " status=" +
                dto.getStatus() + " already exists.");
      }
      featureGroupAlert.setStatus(dto.getStatus());
    }
    if (dto.getSeverity() != null) {
      featureGroupAlert.setSeverity(dto.getSeverity());
    }
    if (!featureGroupAlert.getReceiver().getName().equals(dto.getReceiver())) {
      deleteRoute(featureGroupAlert);
      featureGroupAlert.setReceiver(getReceiver(dto.getReceiver()));
      createRoute(featureGroupAlert);
    }
    featureGroupAlert.setAlertType(alertController.getAlertType(featureGroupAlert.getReceiver()));
    featureGroupAlert = featureGroupAlertFacade.update(featureGroupAlert);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    dto = featureGroupAlertBuilder.build(uriInfo, resourceRequest, featureGroupAlert);
    return Response.ok().entity(dto).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a feature group alert.", response = PostableFeatureGroupAlerts.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response create(PostableFeatureGroupAlerts dto,
                         @QueryParam("bulk") @DefaultValue("false") Boolean bulk,
                         @Context UriInfo uriInfo,
                         @Context SecurityContext sc) throws FeaturestoreException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    FeatureGroupAlertDTO featureGroupAlertDTO = createAlert(dto, bulk, uriInfo, resourceRequest);
    return Response.created(featureGroupAlertDTO.getHref()).entity(featureGroupAlertDTO).build();
  }

  private FeatureGroupAlertDTO createAlert(PostableFeatureGroupAlerts featureGroupAlertDTO, Boolean bulk,
      UriInfo uriInfo, ResourceRequest resourceRequest) throws FeaturestoreException {
    FeatureGroupAlertDTO dto;
    if (bulk) {
      validateBulk(featureGroupAlertDTO);
      dto = new FeatureGroupAlertDTO();
      for (PostableFeatureGroupAlerts pa : featureGroupAlertDTO.getItems()) {
        dto.addItem(createAlert(pa, uriInfo, resourceRequest));
      }
      dto.setCount((long) featureGroupAlertDTO.getItems().size());
    } else {
      dto = createAlert(featureGroupAlertDTO, uriInfo, resourceRequest);
    }
    return dto;
  }

  private void validateBulk(PostableFeatureGroupAlerts featureGroupAlertDTO) throws FeaturestoreException {
    if (featureGroupAlertDTO.getItems() == null || featureGroupAlertDTO.getItems().size() < 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "No payload.");
    }
    Set<ValidationRuleAlertStatus> statusSet = new HashSet<>();
    for (PostableFeatureGroupAlerts dto : featureGroupAlertDTO.getItems()) {
      statusSet.add(dto.getStatus());
    }
    if (statusSet.size() < featureGroupAlertDTO.getItems().size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Duplicate alert.");
    }
  }

  private FeatureGroupAlertDTO createAlert(PostableFeatureGroupAlerts dto, UriInfo uriInfo,
      ResourceRequest resourceRequest) throws FeaturestoreException {
    validate(dto);
    FeatureGroupAlert featureGroupAlert = new FeatureGroupAlert();
    featureGroupAlert.setStatus(dto.getStatus());
    featureGroupAlert.setSeverity(dto.getSeverity());
    featureGroupAlert.setCreated(new Date());
    featureGroupAlert.setFeatureGroup(this.featuregroup);
    featureGroupAlert.setReceiver(getReceiver(dto.getReceiver()));
    featureGroupAlert.setAlertType(alertController.getAlertType(featureGroupAlert.getReceiver()));
    createRoute(featureGroupAlert);
    featureGroupAlertFacade.save(featureGroupAlert);
    featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndStatus(this.featuregroup, dto.getStatus());
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
  public Response getTestById(@PathParam("id") Integer id,
                              @Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws AlertException {
    FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(featuregroup, id);
    List<Alert> alerts;
    try {
      alerts = alertController.testAlert(featuregroup.getFeaturestore().getProject(), featureGroupAlert);
    } catch (AlertManagerUnreachableException | AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    AlertDTO alertDTO =
        alertBuilder.getAlertDTOs(uriInfo, resourceRequest, alerts, featuregroup.getFeaturestore().getProject());
    return Response.ok().entity(alertDTO).build();
  }

  @DELETE
  @Path("{id}")
  @ApiOperation(value = "Delete feature group alert by Id.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(@PathParam("id") Integer id,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws FeaturestoreException {
    FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(this.featuregroup, id);
    if (featureGroupAlert != null) {
      deleteRoute(featureGroupAlert);
      featureGroupAlertFacade.remove(featureGroupAlert);
    }
    return Response.noContent().build();
  }


  private void validate(PostableFeatureGroupAlerts dto) throws FeaturestoreException {
    if (dto == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "No payload.");
    }
    if (dto.getStatus() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Status can not be empty.");
    }
    if (dto.getSeverity() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Severity can not be empty.");
    }
    FeatureGroupAlert featuregroupexpectationalert =
        featureGroupAlertFacade.findByFeatureGroupAndStatus(this.featuregroup, dto.getStatus());
    if (featuregroupexpectationalert != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
          "Feature Group Alert with FeatureGroupName=" + this.featuregroup.getName() + " status=" +
              dto.getStatus() + " already exists.");
    }
  }

  private AlertReceiver getReceiver(String name) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(name)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Receiver can not be empty.");
    }
    Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(name);
    if (alertReceiver.isPresent()) {
      return alertReceiver.get();
    }
    throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Alert receiver not found " + name);
  }

  private void createRoute(FeatureGroupAlert featureGroupAlert) throws FeaturestoreException {
    try {
      alertController.createRoute(featureGroupAlert);
    } catch (AlertManagerClientCreateException | AlertManagerConfigReadException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigUpdateException | AlertManagerNoSuchElementException |
        AlertManagerAccessControlException | AlertManagerUnreachableException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_CREATE_ROUTE, Level.FINE,
          e.getMessage());
    }
  }

  private void deleteRoute(FeatureGroupAlert featureGroupAlert) throws FeaturestoreException {
    try {
      alertController.deleteRoute(featureGroupAlert);
    } catch (AlertManagerUnreachableException | AlertManagerAccessControlException | AlertManagerConfigUpdateException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException | AlertManagerClientCreateException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_DELETE_ROUTE, Level.FINE,
          e.getMessage());
    }
  }
}