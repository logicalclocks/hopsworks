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

package io.hops.hopsworks.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import io.hops.hopsworks.alert.dao.AlertManagerConfigFacade;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.ConfigUtil;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alert.util.JsonObjectHelper;
import io.hops.hopsworks.alert.util.VariablesFacade;
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
import io.hops.hopsworks.alerting.config.ConfigUpdater;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AlertManagerConfiguration {
  private final static Logger LOGGER = Logger.getLogger(AlertManagerConfiguration.class.getName());

  private AlertManagerConfigController alertManagerConfigController;
  private Exception initException;
  private ITopic<String> configUpdatedTopic;

  @Inject
  private HazelcastInstance hazelcastInstance;
  @EJB
  private VariablesFacade variablesFacade;
  @EJB
  private AlertManagerConfigFacade alertManagerConfigFacade;
  @EJB
  private AMConfigUpdater amConfigUpdater;
  @EJB
  private AMClient amClient;
  @EJB
  private transient AlertReceiverFacade alertReceiverFacade;

  public AlertManagerConfiguration() {
  }

  // For test
  @VisibleForTesting
  public AlertManagerConfiguration(AlertManagerConfigController alertManagerConfigController,
      AlertManagerConfigFacade alertManagerConfigFacade, AMClient amClient, AMConfigUpdater amConfigUpdater) {
    this.alertManagerConfigController = alertManagerConfigController;
    this.alertManagerConfigFacade = alertManagerConfigFacade;
    this.amClient = amClient;
    this.amConfigUpdater = amConfigUpdater;
  }

  @PostConstruct
  public void init() {
    tryBuildAlertManagerConfigCtrl();
    // hazelcastInstance == null if Hazelcast is Disabled
    if (hazelcastInstance != null) {
      configUpdatedTopic = hazelcastInstance.getTopic(Constants.AM_CONFIG_UPDATED_TOPIC_NAME);
    }
  }

  private void tryBuildAlertManagerConfigCtrl() {
    Optional<String> alertManagerConfFile =
        variablesFacade.getVariableValue(VariablesFacade.ALERT_MANAGER_CONFIG_FILE_PATH_VARIABLE);
    String configFile = alertManagerConfFile.orElse(null);
    try {
      alertManagerConfigController = new AlertManagerConfigController.Builder()
          .withConfigPath(configFile)
          .build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to init Alertmanager config controller. " + e.getMessage());
      initException = e;
    }
  }

  private void doSanityCheck() throws AlertManagerConfigCtrlCreateException {
    if (alertManagerConfigController == null) {
      if (this.initException != null) {
        throw new AlertManagerConfigCtrlCreateException(this.initException);
      }
      throw new AlertManagerConfigCtrlCreateException("Failed to instantiate AlertManagerConfigController");
    }
  }


  @Lock(LockType.READ)
  public Optional<AlertManagerConfig> read() throws AlertManagerConfigReadException {
    //First read from database
    ObjectMapper objectMapper = new ObjectMapper();
    return alertManagerConfigFacade.read(objectMapper);
  }

  private void broadcast(String message) {
    //Notify other nodes if configUpdatedTopic is created ==> Hazelcast is enabled
    if (configUpdatedTopic != null) {
      configUpdatedTopic.publishAsync("Update configuration. " + message);
    }
  }

  private void updateAlertManagerConfig(AlertManagerConfig alertManagerConfig)
      throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    if (alertManagerConfig != null) {
      doSanityCheck();
      try {
        alertManagerConfigController.writeAndReload(alertManagerConfig, amClient.getClient());
      } catch (AlertManagerServerException e) {
        LOGGER.log(Level.SEVERE, "AlertManager server unreachable. {0}", e.getMessage());
        throw new AlertManagerConfigUpdateException(e);
      }
    }
  }

  @Lock(LockType.READ)
  public void restoreFromDb() throws AlertManagerConfigUpdateException {
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = alertManagerConfigFacade.getLatest();
    if (optionalAlertManagerConfigEntity.isPresent()) {
      ObjectMapper objectMapper = new ObjectMapper();
      JSONObject jsonAlertManagerConfigBackup = optionalAlertManagerConfigEntity.get().getContent();
      try {
        AlertManagerConfig alertManagerConfigBackup =
          objectMapper.readValue(jsonAlertManagerConfigBackup.toString(), AlertManagerConfig.class);
        updateAlertManagerConfig(alertManagerConfigBackup);
      } catch (Exception e) {
        throw new AlertManagerConfigUpdateException(
          "Failed to revert alert manger config from database. " + e.getMessage(), e);
      }
    }
  }

  public void writeAndReload(AlertManagerConfig alertManagerConfig) throws AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerConfigReadException {
    if (alertManagerConfig != null) {
      amConfigUpdater.writeAndReload(alertManagerConfigController, amClient.getClient(), alertManagerConfig,
        alertManagerConfigFacade);
      // broadcast to all nodes
      broadcast("Alert Manager Config updated");
    }
  }

  public void restoreFromBackup() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException,
      IOException, AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = alertManagerConfigFacade.getLatest();
    ObjectMapper objectMapper = new ObjectMapper();

    JSONObject jsonAlertManagerConfig = alertManagerConfig != null ?
        new JSONObject(objectMapper.writeValueAsString(alertManagerConfig)) : null;

    JSONObject jsonAlertManagerConfigBackup =
        optionalAlertManagerConfigEntity.map(AlertManagerConfigEntity::getContent).orElse(null);

    AlertManagerConfig alertManagerConfigBackup = jsonAlertManagerConfigBackup != null ?
        objectMapper.readValue(jsonAlertManagerConfigBackup.toString(), AlertManagerConfig.class) : null;

    if (jsonAlertManagerConfigBackup != null) {
      if (jsonAlertManagerConfig == null || !JsonObjectHelper.similar(jsonAlertManagerConfig,
        jsonAlertManagerConfigBackup)) {
        updateAlertManagerConfig(alertManagerConfigBackup);
        LOGGER.log(Level.FINE, "Alert manager config restored from backup.");
      }
    } else if (jsonAlertManagerConfig != null) {
      alertManagerConfigFacade.saveToDatabase(jsonAlertManagerConfig);
      LOGGER.log(Level.INFO, "Alert manager config backup saved.");
    }
  }

  @Lock(LockType.READ)
  public Global getGlobal() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    return alertManagerConfig.map(AlertManagerConfig::getGlobal).orElse(null);
  }

  public void updateGlobal(Global global) throws AlertManagerConfigCtrlCreateException,
      AlertManagerClientCreateException, AlertManagerUnreachableException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    doSanityCheck();
    amConfigUpdater.updateGlobal(alertManagerConfigController, amClient.getClient(), global, alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config global updated");
  }

  @Lock(LockType.READ)
  public List<String> getTemplates() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    return alertManagerConfig.map(AlertManagerConfig::getTemplates).orElse(null);
  }

  public void updateTemplates(List<String> templates) throws AlertManagerConfigCtrlCreateException,
      AlertManagerClientCreateException, AlertManagerUnreachableException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    doSanityCheck();
    amConfigUpdater.updateTemplates(alertManagerConfigController, amClient.getClient(), templates,
      alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config templates updated");
  }

  @Lock(LockType.READ)
  public Route getGlobalRoute() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    return alertManagerConfig.map(AlertManagerConfig::getRoute).orElse(null);
  }

  public void updateRoute(Route route) throws AlertManagerConfigCtrlCreateException, AlertManagerClientCreateException,
      AlertManagerUnreachableException, AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    doSanityCheck();
    amConfigUpdater.updateGlobalRoute(alertManagerConfigController, amClient.getClient(), route,
      alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config route updated");
  }

  @Lock(LockType.READ)
  public List<InhibitRule> getInhibitRules() throws AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    return alertManagerConfig.map(AlertManagerConfig::getInhibitRules).orElse(null);
  }

  public void updateInhibitRules(List<InhibitRule> inhibitRules) throws AlertManagerConfigCtrlCreateException,
      AlertManagerClientCreateException, AlertManagerUnreachableException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    doSanityCheck();
    amConfigUpdater.updateInhibitRules(alertManagerConfigController, amClient.getClient(), inhibitRules,
      alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config inhibitRules updated");
  }

  @Lock(LockType.READ)
  public Receiver getReceiver(String name, Project project)
      throws AlertManagerConfigCtrlCreateException, AlertManagerNoSuchElementException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, true);
    return getReceiver(name);
  }

  @Lock(LockType.READ)
  public Receiver getReceiver(String name) throws AlertManagerConfigCtrlCreateException,
      AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    if (alertManagerConfig.isPresent()) {
      int index = ConfigUpdater.getIndexOfReceiver(alertManagerConfig.get(), name);
      return alertManagerConfig.get().getReceivers().get(index);
    }
    return null;
  }

  public void addReceiver(Receiver receiver, Project project) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    ConfigUtil.fixReceiverName(receiver, project);
    addReceiver(receiver);
  }

  public void addReceiver(Receiver receiver) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    amConfigUpdater.addReceiver(alertManagerConfigController, amClient.getClient(), receiver,
      alertManagerConfigFacade, alertReceiverFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config receiver added");
  }

  private void checkPermission(String name, Project project, boolean includeGlobal)
      throws AlertManagerAccessControlException {
    if (Strings.isNullOrEmpty(name) ||
        !(name.startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER, project.getName())) ||
            (includeGlobal && name.startsWith(Constants.GLOBAL_RECEIVER_NAME_PREFIX)))) {
      throw new AlertManagerAccessControlException(
          "You do not have permission to access this receiver. Receiver=" + name);
    }
  }

  public void updateReceiver(String name, Receiver receiver, Project project) throws AlertManagerNoSuchElementException,
      AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerAccessControlException,
      AlertManagerConfigReadException {
    checkPermission(name, project, false);
    ConfigUtil.fixReceiverName(receiver, project);
    updateReceiver(name, receiver);
  }

  public void updateReceiver(String name, Receiver receiver) throws AlertManagerNoSuchElementException,
      AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    amConfigUpdater.updateReceiver(alertManagerConfigController, amClient.getClient(), name, receiver,
      alertManagerConfigFacade, alertReceiverFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config receiver updated");
  }

  public void removeReceiver(String name, Project project, boolean cascade) throws AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    removeReceiver(name, cascade);
  }

  public void removeReceiver(String name, boolean cascade)
      throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    boolean remover = amConfigUpdater.removeReceiver(alertManagerConfigController, amClient.getClient(), name,
      cascade, alertManagerConfigFacade, alertReceiverFacade);
    if (remover) {
      // broadcast to all nodes
      broadcast("Alert Manager Config receiver removed");
    }
  }

  @Lock(LockType.READ)
  public List<Route> getRoutes(Project project) throws AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException {
    List<Route> projectRoutes = new ArrayList<>();
    List<Route> routeList = getRoutes();
    for (Route route : routeList) {
      if (ConfigUtil.isRouteInProject(route, project)) {
        projectRoutes.add(route);
      } else if (ConfigUtil.isRouteGlobal(route)) {
        projectRoutes.add(route);
      }
    }
    return projectRoutes;
  }

  @Lock(LockType.READ)
  public List<Route> getRoutes() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    return alertManagerConfig.map(AlertManagerConfig::getRoute).orElse(new Route()).getRoutes();
  }

  @Lock(LockType.READ)
  public Route getRoute(Route route, Project project)
      throws AlertManagerAccessControlException, AlertManagerNoSuchElementException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    if (project != null) {
      return getRoute(route.getReceiver(), route.getMatch(), route.getMatchRe(), project);
    } else {
      return getRoute(route.getReceiver(), route.getMatch(), route.getMatchRe());
    }
  }

  @Lock(LockType.READ)
  public Route getRoute(String receiver, Map<String, String> match, Map<String, String> matchRe, Project project)
      throws AlertManagerConfigCtrlCreateException, AlertManagerNoSuchElementException,
      AlertManagerConfigReadException {
    doSanityCheck();
    Route route = new Route(receiver).withMatch(match).withMatchRe(matchRe);
    List<Route> routes = getRoutes(project);
    int index = routes.indexOf(route);
    if (index < 0) {
      throw new AlertManagerNoSuchElementException(
          "A route with the given receiver name was not found. Receiver Name=" + route.getReceiver());
    }
    return routes.get(index);
  }

  @Lock(LockType.READ)
  public Route getRoute(String receiver, Map<String, String> match, Map<String, String> matchRe)
      throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    Route route = new Route(receiver).withMatch(match).withMatchRe(matchRe);
    return getRoute(route);
  }

  @Lock(LockType.READ)
  public Route getRoute(Route route) throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    Optional<AlertManagerConfig> alertManagerConfig = read();
    if (alertManagerConfig.isPresent()) {
      int index = ConfigUpdater.getIndexOfRoute(alertManagerConfig.get(), route);
      return alertManagerConfig.get().getRoute().getRoutes().get(index);
    }
    return null;
  }

  public void addRoute(Route route, Project project)
      throws AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException, AlertManagerNoSuchElementException {
    ConfigUtil.fixRoute(route, project);
    if (!Strings.isNullOrEmpty(route.getReceiver()) && !route.getReceiver()
        .startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER, project.getName()))) {
      throw new AlertManagerAccessControlException(
          "You do not have permission to add a route with receiver=" + route.getReceiver());
    }
    addRoute(route);
  }

  public void addRoute(Route route)
      throws AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException, AlertManagerConfigReadException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerNoSuchElementException {
    doSanityCheck();
    amConfigUpdater.addRoute(alertManagerConfigController, amClient.getClient(), route, alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config route added");
  }

  private void checkPermission(Route route, Project project) throws AlertManagerAccessControlException {
    if (!ConfigUtil.isRouteInProject(route, project)) {
      throw new AlertManagerAccessControlException("You do not have permission to change this route. " + route);
    }
  }

  public void updateRoute(Route routeToUpdate, Route route, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    if ((route.getMatch() == null || route.getMatch().isEmpty()) &&
        (route.getMatchRe() == null || route.getMatchRe().isEmpty())) {
      throw new AlertManagerNoSuchElementException("Need to set match or matchRe to find a route.");
    }
    checkPermission(routeToUpdate, project);
    ConfigUtil.fixRoute(route, project);
    updateRoute(routeToUpdate, route);
  }

  public void updateRoute(Route routeToUpdate, Route route)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
    AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    amConfigUpdater.updateRoute(alertManagerConfigController, amClient.getClient(), routeToUpdate, route,
      alertManagerConfigFacade);
    // broadcast to all nodes
    broadcast("Alert Manager Config route updated");
  }

  public void removeRoute(Route route, Project project)
      throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    List<Route> routes = getRoutes(project);
    if (!routes.isEmpty() && routes.contains(route)) {
      removeRoute(route);
    }
  }

  public void removeRoute(Route route) throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    boolean remover = amConfigUpdater.removeRoute(alertManagerConfigController, amClient.getClient(), route,
      alertManagerConfigFacade);
    if (remover) {
      // broadcast to all nodes
      broadcast("Alert Manager Config route removed");
    }
  }

  public void cleanProject(Project project) throws AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    boolean cleaned = amConfigUpdater.cleanProject(alertManagerConfigController, amClient.getClient(), project,
      alertManagerConfigFacade, alertReceiverFacade);
    if (cleaned) {
      // broadcast to all nodes
      broadcast("Alert Manager Config project cleanup for: " + project.getName());
    }
  }

}
