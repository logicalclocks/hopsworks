/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import fish.payara.cluster.Clustered;
import io.hops.hopsworks.alert.dao.AlertManagerConfigFacade;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.util.ConfigUtil;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
import io.hops.hopsworks.alerting.config.ConfigUpdater;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.json.JSONObject;

import javax.ejb.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This Clustered Singleton is used as a write lock on Alert manager configuration.
 * In HA, we need to synchronise Alert manager configuration updates, so we do not overwrite each other's changes.
 */
@Clustered
@Singleton
public class AMConfigUpdater implements Serializable {

  //needed b/c Hazelcast assigns serialVersionUID on deployment and will throw HazelcastSerializationException
  //if the UID is different from the one in cache (local class incompatible: stream classdesc serialVersionUID = xxx,
  //local class serialVersionUID = xxx)
  private static final long serialVersionUID = 7243440946325768353L;
  
  @VisibleForTesting
  public AMConfigUpdater() {
    // here for testing.
  }
  
  public void updateAlertManagerConfig(AlertManagerConfigController alertManagerConfigController,
      AlertManagerClient client, AlertManagerConfig alertManagerConfig) throws AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    try {
      alertManagerConfigController.writeAndReload(alertManagerConfig, client);
    } catch (AlertManagerServerException e) {
      throw new AlertManagerConfigUpdateException("AlertManager server unreachable.", e);
    }
  }
  
  public void restoreFromDb(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigUpdateException {
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = alertManagerConfigFacade.getLatest();
    if (optionalAlertManagerConfigEntity.isPresent()) {
      ObjectMapper objectMapper = new ObjectMapper();
      JSONObject jsonAlertManagerConfigBackup = optionalAlertManagerConfigEntity.get().getContent();
      try {
        AlertManagerConfig alertManagerConfigBackup =
          objectMapper.readValue(jsonAlertManagerConfigBackup.toString(), AlertManagerConfig.class);
        updateAlertManagerConfig(alertManagerConfigController, client, alertManagerConfigBackup);
      } catch (Exception e) {
        throw new AlertManagerConfigUpdateException(
          "Failed to revert alert manger config from database. " + e.getMessage(), e);
      }
    }
  }
  
  private void saveToDatabase(AlertManagerConfig alertManagerConfig, AlertManagerConfigFacade alertManagerConfigFacade)
      throws AlertManagerConfigUpdateException {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JSONObject jsonObject = new JSONObject(objectMapper.writeValueAsString(alertManagerConfig));
      alertManagerConfigFacade.saveToDatabase(jsonObject);
    } catch (JsonProcessingException e) {
      throw new AlertManagerConfigUpdateException(
        "Can not save config to database. Failed to parse config to json. " + e.getMessage(), e);
    }
  }
  
  private void saveConfigAndReceiver(JSONObject alertManagerConfig, String oldName, String newName,
      JSONObject alertReceiver, AlertManagerConfigFacade alertManagerConfigFacade,
      AlertReceiverFacade alertReceiverFacade) {
    alertManagerConfigFacade.saveToDatabase(alertManagerConfig);
    if (alertReceiver != null) {
      alertReceiverFacade.saveReceiverToDatabase(oldName, newName, alertReceiver);
    } else {
      alertReceiverFacade.removeReceiverFromDatabase(oldName);
    }
  }
  
  private void saveToDatabase(AlertManagerConfig alertManagerConfig, String receiverName, Receiver receiver,
      AlertManagerConfigFacade alertManagerConfigFacade, AlertReceiverFacade alertReceiverFacade)
      throws AlertManagerConfigUpdateException {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JSONObject alertManagerConfigJson = new JSONObject(objectMapper.writeValueAsString(alertManagerConfig));
      JSONObject receiverJson = null;
      String newReceiverName = null;
      if (receiver != null) {
        receiverJson = new JSONObject(objectMapper.writeValueAsString(receiver));
        newReceiverName = receiver.getName();
      }
      saveConfigAndReceiver(alertManagerConfigJson, receiverName, newReceiverName, receiverJson,
        alertManagerConfigFacade, alertReceiverFacade);
    } catch (JsonProcessingException e) {
      throw new AlertManagerConfigUpdateException(
        "Can not save config to database. Failed to parse config to json. " + e.getMessage(), e);
    }
  }
  
  public void writeAndReload(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      AlertManagerConfig alertManagerConfig, AlertManagerConfigFacade alertManagerConfigFacade)
      throws AlertManagerConfigUpdateException,
    AlertManagerConfigReadException {
    if (alertManagerConfig != null) {
      updateAlertManagerConfig(alertManagerConfigController, client, alertManagerConfig);
      try {
        saveToDatabase(alertManagerConfig, alertManagerConfigFacade);
      } catch (Exception e) {
        restoreFromDb(alertManagerConfigController, client, alertManagerConfigFacade);
        throw e;
      }
    }
  }
  
  public void writeAndReload(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      AlertManagerConfig alertManagerConfig, String name, Receiver receiver,
      AlertManagerConfigFacade alertManagerConfigFacade, AlertReceiverFacade alertReceiverFacade)
      throws AlertManagerConfigUpdateException, AlertManagerConfigReadException {
    if (alertManagerConfig != null) {
      updateAlertManagerConfig(alertManagerConfigController, client, alertManagerConfig);
      try {
        saveToDatabase(alertManagerConfig, name, receiver, alertManagerConfigFacade, alertReceiverFacade);
      } catch (Exception e) {
        restoreFromDb(alertManagerConfigController, client, alertManagerConfigFacade);
        throw e;
      }
    }
  }
  
  private AlertManagerConfig read(AlertManagerConfigController alertManagerConfigController,
      AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigReadException {
    //First read from database
    ObjectMapper objectMapper = new ObjectMapper();
    Optional<AlertManagerConfig> alertManagerConfig = alertManagerConfigFacade.read(objectMapper);
    //if database is empty use the config
    return alertManagerConfig.orElse(alertManagerConfigController.read());
  }
  
  public void updateGlobal(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Global global, AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateGlobal(alertManagerConfig, global);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public void updateTemplates(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      List<String> templates, AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateTemplates(alertManagerConfig, templates);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public void updateGlobalRoute(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Route route, AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateGlobalRoute(alertManagerConfig, route);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public void updateInhibitRules(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      List<InhibitRule> inhibitRules, AlertManagerConfigFacade alertManagerConfigFacade)
      throws AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateInhibitRules(alertManagerConfig, inhibitRules);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public void addReceiver(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Receiver receiver, AlertManagerConfigFacade alertManagerConfigFacade, AlertReceiverFacade alertReceiverFacade)
      throws AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException,
    AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, receiver.getName(), receiver,
      alertManagerConfigFacade, alertReceiverFacade);
  }
  
  public void updateReceiver(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      String name, Receiver receiver, AlertManagerConfigFacade alertManagerConfigFacade,
      AlertReceiverFacade alertReceiverFacade) throws AlertManagerNoSuchElementException,
      AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateReceiver(alertManagerConfig, name, receiver);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, name, receiver, alertManagerConfigFacade,
      alertReceiverFacade);
  }
  
  public boolean removeReceiver(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      String name, boolean cascade, AlertManagerConfigFacade alertManagerConfigFacade,
      AlertReceiverFacade alertReceiverFacade) throws AlertManagerConfigUpdateException,
      AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    alertManagerConfig = ConfigUpdater.removeReceiver(alertManagerConfig, name, cascade);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, name, null, alertManagerConfigFacade,
      alertReceiverFacade);
    return alertManagerConfig != null;
  }
  
  public void addRoute(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Route route, AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException,
    AlertManagerConfigReadException, AlertManagerNoSuchElementException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.addRoute(alertManagerConfig, route);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public void updateRoute(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Route routeToUpdate, Route route, AlertManagerConfigFacade alertManagerConfigFacade)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException,
      AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    ConfigUpdater.updateRoute(alertManagerConfig, routeToUpdate, route);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
  }
  
  public boolean removeRoute(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Route route, AlertManagerConfigFacade alertManagerConfigFacade) throws AlertManagerConfigUpdateException,
      AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    alertManagerConfig = ConfigUpdater.removeRoute(alertManagerConfig, route);
    writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
    return alertManagerConfig != null;
  }
  
  public boolean cleanProject(AlertManagerConfigController alertManagerConfigController, AlertManagerClient client,
      Project project, AlertManagerConfigFacade alertManagerConfigFacade, AlertReceiverFacade alertReceiverFacade)
      throws AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    AlertManagerConfig alertManagerConfig = read(alertManagerConfigController, alertManagerConfigFacade);
    List<Route> routes = alertManagerConfig.getRoute() == null || alertManagerConfig.getRoute().getRoutes() == null ?
      Collections.emptyList() : alertManagerConfig.getRoute().getRoutes();
    List<Receiver> receivers = alertManagerConfig.getReceivers() == null ? Collections.emptyList() :
      alertManagerConfig.getReceivers();
    
    List<Receiver> receiversToRemove = new ArrayList<>();
    for (Receiver receiver : receivers) {
      if (receiver.getName().startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER,
        project.getName()))) {
        receiversToRemove.add(receiver);
      }
    }
    List<Route> routesToRemove = new ArrayList<>();
    for (Route route : routes) {
      if (ConfigUtil.isRouteInProject(route, project) ||
        receiversToRemove.contains(new Receiver(route.getReceiver()))) {
        routesToRemove.add(route);
      }
    }
    
    if (!routesToRemove.isEmpty() || !receiversToRemove.isEmpty()) {
      if (!routesToRemove.isEmpty()) {
        alertManagerConfig.getRoute().getRoutes().removeAll(routesToRemove);
      }
      if (!receiversToRemove.isEmpty()) {
        alertManagerConfig.getReceivers().removeAll(receiversToRemove);
        for (Receiver receiver : receiversToRemove) {
          alertReceiverFacade.removeReceiverFromDatabase(receiver.getName());
        }
      }
      writeAndReload(alertManagerConfigController, client, alertManagerConfig, alertManagerConfigFacade);
      return true;
    }
    return false;
  }
}
