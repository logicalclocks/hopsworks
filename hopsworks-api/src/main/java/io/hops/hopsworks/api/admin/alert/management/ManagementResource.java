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

package io.hops.hopsworks.api.admin.alert.management;

import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.AlertmanagerStatus;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.Entry;
import io.hops.hopsworks.api.alert.receiver.PostableReceiverDTO;
import io.hops.hopsworks.api.alert.receiver.ReceiverBuilder;
import io.hops.hopsworks.api.alert.route.PostableRouteDTO;
import io.hops.hopsworks.api.alert.route.RouteBuilder;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Logged
@Api(value = "Management Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ManagementResource {
  
  private static final Logger LOGGER = Logger.getLogger(ManagementResource.class.getName());
  
  @EJB
  private AMClient alertManager;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  @EJB
  private RouteBuilder routeBuilder;
  @EJB
  private ReceiverBuilder receiverBuilder;
  
  
  @GET
  @Path("healthy")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response healthy(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      return alertManager.healthy();
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
  }
  
  @GET
  @Path("ready")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response ready(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      return alertManager.ready();
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
  }
  
  @GET
  @Path("status")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response getStatus(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      AlertmanagerStatus alertmanagerStatus = alertManager.getStatus();
      return Response.ok().entity(alertmanagerStatus).build();
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
  }
  
  @GET
  @Path("config")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response getConfig(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      Optional<AlertManagerConfig> alertManagerConfig = alertManagerConfiguration.read();
      return Response.ok().entity(alertManagerConfig.orElse(null)).build();
    } catch (AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    }
  }
  
  @POST
  @Path("config")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateConfig(PostableAlertManagerConfig config,
                               @Context HttpServletRequest req,
                               @Context SecurityContext sc) throws AlertException {
    try {
      AlertManagerConfig alertManagerConfig = toAlertManagerConfig(config);
      alertManagerConfiguration.writeAndReload(alertManagerConfig);
      Optional<AlertManagerConfig> optionalAlertManagerConfig = alertManagerConfiguration.read();
      return Response.ok().entity(optionalAlertManagerConfig.orElse(null)).build();
    } catch (AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
  }
  
  private AlertManagerConfig toAlertManagerConfig(PostableAlertManagerConfig config) throws AlertException {
    return new AlertManagerConfig()
        .withGlobal(config.getGlobal())
        .withTemplates(config.getTemplates())
        .withInhibitRules(config.getInhibitRules())
        .withReceivers(toReceivers(config.getReceivers()))
        .withRoute(config.getRoute().toRoute());
  }
  
  private List<Receiver> toReceivers(List<PostableReceiverDTO> receivers) throws AlertException {
    List<Receiver> receiverList = new ArrayList<>();
    for (PostableReceiverDTO postableReceiverDTO : receivers) {
      receiverList.add(receiverBuilder.build(postableReceiverDTO, false, false));
    }
    return receiverList;
  }
  
  @POST
  @Path("reload")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response reload(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      return alertManager.reload();
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
  }
  
  @POST
  @Path("reload-config")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response reloadConfig(@Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    try {
      alertManagerConfiguration.restoreFromDb();
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    }
    return Response.ok().build();
  }
  
  @PUT
  @Path("global")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateGlobal(Global global, @Context HttpServletRequest req, @Context SecurityContext sc)
    throws AlertException {
    Global dto;
    try {
      alertManagerConfiguration.updateGlobal(global);
      dto = alertManagerConfiguration.getGlobal();
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @Path("templates")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateTemplates(TemplatesDTO templates, @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws AlertException {
    TemplatesDTO dto = new TemplatesDTO();
    try {
      alertManagerConfiguration.updateTemplates(templates.getTemplates());
      List<String> t = alertManagerConfiguration.getTemplates();
      dto.setTemplates(t);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @Path("route")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateRoute(PostableRouteDTO routeDTO,
                              @Context HttpServletRequest req, @Context SecurityContext sc) throws AlertException {
    Route route = routeDTO.toRoute();
    Route dto;
    try {
      alertManagerConfiguration.updateRoute(route);
      dto = alertManagerConfiguration.getGlobalRoute();
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @Path("inhibit")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateInhibitRules(PostableInhibitRulesDTOList postableInhibitRulesDTOList,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc) throws AlertException {
    List<InhibitRule> inhibitRules = toInhibitRules(postableInhibitRulesDTOList);
    InhibitRulesDTO dto = new InhibitRulesDTO();
    try {
      alertManagerConfiguration.updateInhibitRules(inhibitRules);
      List<InhibitRule> i = alertManagerConfiguration.getInhibitRules();
      dto.setInhibitRules(i);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    return Response.ok().entity(dto).build();
  }
  
  private List<InhibitRule> toInhibitRules(PostableInhibitRulesDTOList postableInhibitRulesDTOList) {
    if (postableInhibitRulesDTOList != null) {
      return toInhibitRules(postableInhibitRulesDTOList.getPostableInhibitRulesDTOs());
    }
    return null;
  }
  
  private List<InhibitRule> toInhibitRules(List<PostableInhibitRulesDTO> postableInhibitRulesDTOList) {
    if (postableInhibitRulesDTOList != null && !postableInhibitRulesDTOList.isEmpty()) {
      return postableInhibitRulesDTOList.stream().map(this::toInhibitRule).collect(Collectors.toList());
    }
    return null;
  }
  
  private InhibitRule toInhibitRule(PostableInhibitRulesDTO p) {
    InhibitRule inhibitRule = new InhibitRule();
    inhibitRule.setEqual(p.getEqual());
    if (p.getSourceMatch() != null && !p.getSourceMatch().isEmpty()) {
      inhibitRule.setSourceMatch(p.getSourceMatch().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }
    if (p.getSourceMatchRe() != null && !p.getSourceMatchRe().isEmpty()) {
      inhibitRule.setSourceMatchRe(p.getSourceMatchRe().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }
    if (p.getTargetMatch() != null && !p.getTargetMatch().isEmpty()) {
      inhibitRule.setTargetMatch(p.getTargetMatch().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }
    if (p.getTargetMatchRe() != null && !p.getTargetMatchRe().isEmpty()) {
      inhibitRule.setTargetMatchRe(p.getTargetMatchRe().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }
    return inhibitRule;
  }
  
}