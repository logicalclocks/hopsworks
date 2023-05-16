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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.alert.dao.AlertManagerConfigFacade;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.ConfigUtil;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class FixReceiversTimer implements Serializable {
  private final static Logger LOGGER = Logger.getLogger(FixReceiversTimer.class.getName());
  private static final long serialVersionUID = 3302197432816982668L;

  @EJB
  private transient AlertManagerConfigFacade alertManagerConfigFacade;
  @EJB
  private transient AlertReceiverFacade alertReceiverFacade;
  @EJB
  private transient AlertManagerConfiguration alertManagerConfiguration;

  public FixReceiversTimer() {
  }

  //For test
  public FixReceiversTimer(AlertManagerConfigFacade alertManagerConfigFacade,
    AlertReceiverFacade alertReceiverFacade, AlertManagerConfiguration alertManagerConfiguration) {
    this.alertManagerConfigFacade = alertManagerConfigFacade;
    this.alertReceiverFacade = alertReceiverFacade;
    this.alertManagerConfiguration = alertManagerConfiguration;
  }

  @Schedule(hour = "*", info = "Fix Alert Manager config receivers from backup.")
  public void fixReceiversTimer() {
    try {
      runFixConfig();
      LOGGER.log(Level.FINE, "Fix Alert Manager config receivers from backup.");
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Failed to fix Alert Manager config from backup. " + e.getMessage());
    }
  }

  public Optional<AlertManagerConfig> read(ObjectMapper objectMapper) throws AlertManagerConfigReadException {
    //First read from database
    return alertManagerConfigFacade.read(objectMapper);
  }

  private boolean fixReceivers(AlertManagerConfig alertManagerConfig, ObjectMapper objectMapper,
    List<AlertReceiver> alertReceivers) throws IOException {
    List<Route> routes = getRoutes(alertManagerConfig);
    List<Receiver> receivers = alertManagerConfig.getReceivers() == null ? new ArrayList<>() :
      alertManagerConfig.getReceivers();
    List<Receiver> receiversToAdd = new ArrayList<>();
    for (AlertReceiver alertReceiver : alertReceivers) {
      Receiver receiver = receiverToAdd(receivers, alertReceiver, objectMapper);
      if (receiver != null && !receiversToAdd.contains(receiver)) {
        receiversToAdd.add(receiver);
      }
    }
    List<Receiver> receiversToRemove = new ArrayList<>();
    for (Receiver receiver : receivers) {
      Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(receiver.getName());
      if (!alertReceiver.isPresent() && isNotSystemReceiver(alertManagerConfig, receiver)) {
        receiversToRemove.add(receiver);
      }
    }
    if (!receiversToAdd.isEmpty() || !receiversToRemove.isEmpty()) {
      if (!receiversToAdd.isEmpty()) {
        receivers.addAll(receiversToAdd);
        LOGGER.log(Level.INFO, "Alert manager config updated. Added {0} receivers.", receiversToAdd.size());
      }
      if (!receiversToRemove.isEmpty()) {
        List<Route> routesToRemove = new ArrayList<>();
        for (Route route : routes) {
          if (receiversToRemove.contains(new Receiver(route.getReceiver()))) {
            routesToRemove.add(route);
          }
        }
        if (!routesToRemove.isEmpty()) {
          alertManagerConfig.getRoute().getRoutes().removeAll(routesToRemove);
          LOGGER.log(Level.INFO, "Alert manager config updated. Removed {0} routes.", routesToRemove.size());
        }
        receivers.removeAll(receiversToRemove);
        LOGGER.log(Level.INFO, "Alert manager config updated. Removed {0} receivers.", receiversToRemove.size());
      }
      alertManagerConfig.setReceivers(receivers);
      return true;
    }
    return false;
  }

  private boolean isNotSystemReceiver(AlertManagerConfig alertManagerConfig, Receiver receiver) {
    if (alertManagerConfig.getRoute() == null) {
      return true;
    }
    if (alertManagerConfig.getRoute().getReceiver().equals(receiver.getName())) {
      return false;
    }
    List<Route> routes = alertManagerConfig.getRoute().getRoutes();
    for (Route route : routes) {
      Map<String, String> match = route.getMatch() != null ? route.getMatch() : route.getMatchRe();
      String alertType = match != null && match.get(Constants.ALERT_TYPE_LABEL) != null ?
        match.get(Constants.ALERT_TYPE_LABEL) : null;
      if (alertType != null && alertType.equals(AlertType.SYSTEM_ALERT.getValue()) &&
        route.getReceiver().equals(receiver.getName())) {
        return false;
      }
    }
    return true;
  }

  private boolean fixRoutes(AlertManagerConfig alertManagerConfig, List<AlertReceiver> alertReceivers) {
    List<Route> routes = getRoutes(alertManagerConfig);
    List<Route> routesToAdd = new ArrayList<>();
    for (AlertReceiver alertReceiver : alertReceivers) {
      if (!alertReceiver.getName().equals(AlertType.DEFAULT.getValue())) {
        routesToAdd.addAll(getRoutesToAdd(alertReceiver, routes));
      }
    }
    if (!routesToAdd.isEmpty()) {
      alertManagerConfig.getRoute().getRoutes().addAll(routesToAdd);
      LOGGER.log(Level.INFO, "Alert manager config updated. Added {0} routes.", routesToAdd.size());
      return true;
    }
    return false;
  }

  private List<Route> getRoutesToAdd(AlertReceiver alertReceiver, List<Route> routes) {
    Collection<JobAlert> jobAlerts = alertReceiver.getJobAlertCollection();
    Collection<FeatureGroupAlert> featureGroupAlerts = alertReceiver.getFeatureGroupAlertCollection();
    Collection<ProjectServiceAlert> projectServiceAlerts = alertReceiver.getProjectServiceAlertCollection();
    List<Route> routesToAdd = new ArrayList<>();
    for (JobAlert jobAlert : jobAlerts) {
      Route route = jobAlert.getAlertType().isGlobal() ? ConfigUtil.getRoute(jobAlert.getAlertType()) :
        ConfigUtil.getRoute(jobAlert);
      if (!routes.contains(route) && !routesToAdd.contains(route)) {
        routesToAdd.add(route);
      }
    }
    for (FeatureGroupAlert featureGroupAlert : featureGroupAlerts) {
      Route route =
        featureGroupAlert.getAlertType().isGlobal() ? ConfigUtil.getRoute(featureGroupAlert.getAlertType()) :
          ConfigUtil.getRoute(featureGroupAlert);
      if (!routes.contains(route) && !routesToAdd.contains(route)) {
        routesToAdd.add(route);
      }
    }
    for (ProjectServiceAlert projectServiceAlert : projectServiceAlerts) {
      Route route =
        projectServiceAlert.getAlertType().isGlobal() ? ConfigUtil.getRoute(projectServiceAlert.getAlertType()) :
          ConfigUtil.getRoute(projectServiceAlert);
      if (!routes.contains(route) && !routesToAdd.contains(route)) {
        routesToAdd.add(route);
      }
    }
    return routesToAdd;
  }

  private Receiver receiverToAdd(List<Receiver> receivers, AlertReceiver alertReceiver, ObjectMapper objectMapper)
    throws IOException {
    Receiver receiverToAdd = null;
    if (!receivers.contains(new Receiver(alertReceiver.getName()))) {
      receiverToAdd = objectMapper.readValue(alertReceiver.getConfig().toString(), Receiver.class);
    }
    return receiverToAdd;
  }

  private List<Route> getRoutes(AlertManagerConfig alertManagerConfig) {
    if (alertManagerConfig.getRoute() == null) {
      alertManagerConfig.setRoute(new Route().withRoutes(new ArrayList<>()));
    } else if (alertManagerConfig.getRoute().getRoutes() == null) {
      alertManagerConfig.getRoute().setRoutes(new ArrayList<>());
    }
    return alertManagerConfig.getRoute().getRoutes();
  }

  public void runFixConfig() throws AlertManagerConfigReadException, IOException, AlertManagerUnreachableException,
    AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerClientCreateException {
    ObjectMapper objectMapper = new ObjectMapper();
    Optional<AlertManagerConfig> alertManagerConfig = read(objectMapper);
    if (alertManagerConfig.isPresent()) {
      List<AlertReceiver> alertReceivers = alertReceiverFacade.findAll();
      AlertManagerConfig aMConfig = alertManagerConfig.get();
      boolean updatedReceivers = fixReceivers(aMConfig, objectMapper, alertReceivers);
      boolean updatedRoutes = fixRoutes(aMConfig, alertReceivers);
      if (updatedReceivers || updatedRoutes) {
        alertManagerConfiguration.writeAndReload(aMConfig);
      }
    }
  }
}
