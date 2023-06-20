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
package io.hops.hopsworks.alerting.config;

import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;

import java.util.ArrayList;
import java.util.List;

public class ConfigUpdater {

  //private constructor to hide the implicit public one.
  private ConfigUpdater() {
  }

  public static AlertManagerConfig updateGlobal(AlertManagerConfig alertManagerConfig, Global global) {
    ConfigValidator.validate(global);
    alertManagerConfig.setGlobal(global);
    return alertManagerConfig;
  }

  public static AlertManagerConfig updateTemplates(AlertManagerConfig alertManagerConfig, List<String> templates) {
    ConfigValidator.validate(templates);
    alertManagerConfig.setTemplates(templates);
    return alertManagerConfig;
  }

  public static AlertManagerConfig updateGlobalRoute(AlertManagerConfig alertManagerConfig, Route route) {
    ConfigValidator.validateGlobalRoute(route);
    alertManagerConfig.setRoute(route);
    return alertManagerConfig;
  }

  public static AlertManagerConfig updateInhibitRules(AlertManagerConfig alertManagerConfig,
    List<InhibitRule> inhibitRules) {
    ConfigValidator.validateInhibitRules(inhibitRules);
    alertManagerConfig.setInhibitRules(inhibitRules);
    return alertManagerConfig;
  }

  public static AlertManagerConfig addReceiver(AlertManagerConfig alertManagerConfig, Receiver receiver)
    throws AlertManagerDuplicateEntryException {
    ConfigValidator.validate(receiver);
    if (alertManagerConfig.getReceivers() == null) {
      alertManagerConfig.setReceivers(new ArrayList<>());
    }
    if (alertManagerConfig.getReceivers().contains(receiver)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same name already exists.");
    }
    alertManagerConfig.getReceivers().add(receiver);
    return alertManagerConfig;
  }

  public static int getIndexOfReceiver(AlertManagerConfig alertManagerConfig, String name)
    throws AlertManagerNoSuchElementException {
    Receiver receiverToUpdate = new Receiver(name);
    int index = alertManagerConfig.getReceivers() == null ? -1 :
      alertManagerConfig.getReceivers().indexOf(receiverToUpdate);
    if (index < 0) {
      throw new AlertManagerNoSuchElementException("A receiver with the given name was not found. Name=" + name);
    }
    return index;
  }

  public static int getIndexOfRoute(AlertManagerConfig alertManagerConfig, Route route)
    throws AlertManagerNoSuchElementException {
    int index = alertManagerConfig.getRoute() == null || alertManagerConfig.getRoute().getRoutes() == null ? -1 :
      alertManagerConfig.getRoute().getRoutes().indexOf(route);
    if (index < 0) {
      throw new AlertManagerNoSuchElementException(
        "A route with the given receiver name was not found. Receiver Name=" + route.getReceiver());
    }
    return index;
  }

  public static AlertManagerConfig updateReceiver(AlertManagerConfig alertManagerConfig, String name, Receiver receiver)
    throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException {
    ConfigValidator.validate(receiver);
    int index = getIndexOfReceiver(alertManagerConfig, name);
    if (!name.equals(receiver.getName()) && alertManagerConfig.getReceivers().contains(receiver)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same name already exists.");
    }
    alertManagerConfig.getReceivers().remove(index);
    alertManagerConfig.getReceivers().add(receiver);
    return alertManagerConfig;
  }

  private static AlertManagerConfig removeRoutes(String receiver, AlertManagerConfig alertManagerConfig) {
    List<Route> routesToDelete = new ArrayList<>();
    for (Route route : alertManagerConfig.getRoute().getRoutes()) {
      if (route.getReceiver().equals(receiver)) {
        routesToDelete.add(route);
      }
    }
    if (routesToDelete.isEmpty()) {
      alertManagerConfig.getRoute().getRoutes().removeAll(routesToDelete);
    }
    return alertManagerConfig;
  }

  public static AlertManagerConfig removeReceiver(AlertManagerConfig alertManagerConfig, String name, boolean cascade) {
    int index = alertManagerConfig.getReceivers() == null ? -1 :
      alertManagerConfig.getReceivers().indexOf(new Receiver(name));
    if (index > -1) {
      alertManagerConfig.getReceivers().remove(index);
      if (cascade) {
        return removeRoutes(name, alertManagerConfig);
      }
      return alertManagerConfig;
    }
    return null;
  }

  public static AlertManagerConfig addRoute(AlertManagerConfig alertManagerConfig, Route route)
      throws AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException {
    ConfigValidator.validate(route);
    getIndexOfReceiver(alertManagerConfig, route.getReceiver());//check if receiver exists
    if (alertManagerConfig.getRoute() == null) {
      alertManagerConfig.setRoute(new Route());
    }
    if (alertManagerConfig.getRoute().getRoutes() == null) {
      alertManagerConfig.getRoute().setRoutes(new ArrayList<>());
    }
    if (alertManagerConfig.getRoute().getRoutes().contains(route)) {
      throw new AlertManagerDuplicateEntryException("A route with the same match and receiver name already exists.");
    }
    alertManagerConfig.getRoute().getRoutes().add(route);
    return alertManagerConfig;
  }

  public static AlertManagerConfig updateRoute(AlertManagerConfig alertManagerConfig, Route routeToUpdate, Route route)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException {
    ConfigValidator.validate(route);
    getIndexOfReceiver(alertManagerConfig, route.getReceiver());//check if receiver exists
    int index = getIndexOfRoute(alertManagerConfig, routeToUpdate);
    if (!routeToUpdate.equals(route) && alertManagerConfig.getRoute().getRoutes().contains(route)) {
      throw new AlertManagerDuplicateEntryException("A route with the same mather and receiver name already exists.");
    }
    alertManagerConfig.getRoute().getRoutes().remove(index);
    alertManagerConfig.getRoute().getRoutes().add(route);
    return alertManagerConfig;
  }

  public static AlertManagerConfig removeRoute(AlertManagerConfig alertManagerConfig, Route route) {
    int index = alertManagerConfig.getRoute().getRoutes() == null ? -1 :
      alertManagerConfig.getRoute().getRoutes().indexOf(route);
    if (index > -1) {
      alertManagerConfig.getRoute().getRoutes().remove(index);
      return alertManagerConfig;
    }
    return null;
  }

  /**
   * @param name
   * @param emailConfig
   * @return updated AlertManagerConfig
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException
   *   if emailConfig is missing to
   */
  public static AlertManagerConfig addEmailToReceiver(AlertManagerConfig alertManagerConfig, String name,
      EmailConfig emailConfig) throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException {
    ConfigValidator.validate(emailConfig);
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    if (receiverToUpdate.getEmailConfigs() == null) {
      receiverToUpdate.setEmailConfigs(new ArrayList<>());
    }
    if (receiverToUpdate.getEmailConfigs().contains(emailConfig)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same email already exists.");
    }
    receiverToUpdate.getEmailConfigs().add(emailConfig);
    return alertManagerConfig;
  }

  /**
   * @param name
   * @param emailConfig
   * @return updated AlertManagerConfig or null if it was not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public static AlertManagerConfig removeEmailFromReceiver(AlertManagerConfig alertManagerConfig, String name,
      EmailConfig emailConfig) throws AlertManagerNoSuchElementException {
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    index = receiverToUpdate.getEmailConfigs() == null ? -1 : receiverToUpdate.getEmailConfigs().indexOf(emailConfig);
    if (index > -1) {
      receiverToUpdate.getEmailConfigs().remove(index);
      return alertManagerConfig;
    }
    return null;
  }

  /**
   * @param name
   * @param slackConfig
   * @return updated AlertManagerConfig
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException
   *   if slackConfig is missing apiUrl or channel
   */
  public static AlertManagerConfig addSlackToReceiver(AlertManagerConfig alertManagerConfig, String name,
      SlackConfig slackConfig) throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException {
    ConfigValidator.validate(slackConfig);
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    if (receiverToUpdate.getSlackConfigs() == null) {
      receiverToUpdate.setSlackConfigs(new ArrayList<>());
    }
    if (receiverToUpdate.getSlackConfigs().contains(slackConfig)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same api url and channel already exists.");
    }
    receiverToUpdate.getSlackConfigs().add(slackConfig);
    return alertManagerConfig;
  }

  /**
   * @param name
   * @param slackConfig
   * @return updated AlertManagerConfig or null if it was not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public static AlertManagerConfig removeSlackFromReceiver(AlertManagerConfig alertManagerConfig, String name,
      SlackConfig slackConfig) throws AlertManagerNoSuchElementException {
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    index = receiverToUpdate.getSlackConfigs() == null ? -1 : receiverToUpdate.getSlackConfigs().indexOf(slackConfig);
    if (index > -1) {
      receiverToUpdate.getSlackConfigs().remove(index);
      return alertManagerConfig;
    }
    return null;
  }

  /**
   * @param name
   * @param pagerdutyConfig
   * @return updated AlertManagerConfig
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException
   *   if pagerdutyConfig is missing both RoutingKey and ServiceKey
   */
  public static AlertManagerConfig addPagerdutyToReceiver(AlertManagerConfig alertManagerConfig, String name,
      PagerdutyConfig pagerdutyConfig) throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException {
    ConfigValidator.validate(pagerdutyConfig);
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    if (receiverToUpdate.getPagerdutyConfigs() == null) {
      receiverToUpdate.setPagerdutyConfigs(new ArrayList<>());
    }
    if (receiverToUpdate.getPagerdutyConfigs().contains(pagerdutyConfig)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same api url and channel already exists.");
    }
    receiverToUpdate.getPagerdutyConfigs().add(pagerdutyConfig);
    return alertManagerConfig;
  }

  /**
   * @param name
   * @param pagerdutyConfig
   * @return updated AlertManagerConfig or null if it is not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public static AlertManagerConfig removePagerdutyFromReceiver(AlertManagerConfig alertManagerConfig, String name,
      PagerdutyConfig pagerdutyConfig) throws AlertManagerNoSuchElementException {
    int index = getIndexOfReceiver(alertManagerConfig, name);
    Receiver receiverToUpdate = alertManagerConfig.getReceivers().get(index);
    index = receiverToUpdate.getPagerdutyConfigs() == null ? -1 :
      receiverToUpdate.getPagerdutyConfigs().indexOf(pagerdutyConfig);
    if (index > -1) {
      receiverToUpdate.getPagerdutyConfigs().remove(index);
      return alertManagerConfig;
    }
    return null;
  }

}
