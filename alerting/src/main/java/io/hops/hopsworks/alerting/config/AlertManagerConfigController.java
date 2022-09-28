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

package io.hops.hopsworks.alerting.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigFileNotFoundException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;

import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AlertManagerConfigController {
  private static final Logger LOGGER = Logger.getLogger(AlertManagerConfigController.class.getName());
  private static final String CONFIG_FILE_PATH = "/srv/hops/alertmanager/alertmanager/alertmanager.yml";

  private final File configFile;
  private AlertManagerClient client;

  private AlertManagerConfigController(File configFile, AlertManagerClient client) {
    this.configFile = configFile;
    this.client = client;
  }

  public void closeClient() {
    this.client.close();
  }

  /**
   * Read Alertmanager config from configFile
   * @return
   */
  public AlertManagerConfig read() throws AlertManagerConfigReadException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    try {
      return objectMapper.readValue(configFile, AlertManagerConfig.class);
    } catch (IOException e) {
      throw new AlertManagerConfigReadException("Failed to read configuration file. Error " + e.getMessage());
    }
  }

  /**
   * Writes alertManagerConfig to configFile in YAML format.
   * Do not use if you are not sure the yaml is well-formed.
   * User writeAndReload instead that will rolls back if it loading the yaml fails
   * @param alertManagerConfig
   * @throws IOException
   */
  public void write(AlertManagerConfig alertManagerConfig) throws AlertManagerConfigUpdateException {
    YAMLFactory yamlFactory = new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    try {
      objectMapper.writeValue(configFile, alertManagerConfig);
    } catch (IOException e) {
      throw new AlertManagerConfigUpdateException("Failed to update configuration file. Error " + e.getMessage());
    }
  }

  /**
   * Writes alertManagerConfig to configFile in YAML format.
   * Rolls back if it fails to reload the file to the alertmanager.
   * @param alertManagerConfig
   * @throws IOException
   * @throws AlertManagerConfigUpdateException
   */
  public void writeAndReload(AlertManagerConfig alertManagerConfig) throws AlertManagerConfigUpdateException,
      AlertManagerServerException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfigTmp = read();
    write(alertManagerConfig);
    try {
      client.reload();
    } catch (AlertManagerResponseException e) {
      write(alertManagerConfigTmp);
      throw new AlertManagerConfigUpdateException("Failed to update AlertManagerConfig. " + e.getMessage(), e);
    } catch (AlertManagerServerException se) {
      write(alertManagerConfigTmp);
      throw se;
    }
  }

  public Global getGlobal() throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    return alertManagerConfig.getGlobal();
  }

  /**
   *
   * @param global
   * @return updated AlertManagerConfig
   * @throws AlertManagerConfigReadException
   * @throws AlertManagerServerException
   */
  public AlertManagerConfig updateGlobal(Global global) throws AlertManagerConfigReadException {
    validate(global);
    AlertManagerConfig alertManagerConfig = read();
    alertManagerConfig.setGlobal(global);
    return alertManagerConfig;
  }

  private void validate(Global global) {
    if (global == null) {
      throw new IllegalArgumentException("Global config can not be null.");
    }
  }

  /**
   *
   * @return
   * @throws AlertManagerConfigReadException
   */
  public List<String> getTemplates() throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    return alertManagerConfig.getTemplates();
  }

  /**
   *
   * @param templates
   * @return updated AlertManagerConfig
   * @throws AlertManagerConfigReadException
   * @throws AlertManagerServerException
   */
  public AlertManagerConfig updateTemplates(List<String> templates) throws AlertManagerConfigReadException {
    validate(templates);
    AlertManagerConfig alertManagerConfig = read();
    alertManagerConfig.setTemplates(templates);
    return alertManagerConfig;
  }

  private void validate(List<String> templates) {
    if (templates == null || templates.isEmpty()) {
      throw new IllegalArgumentException("Templates config can not be null or empty.");
    }
  }

  /**
   *
   * @return
   * @throws AlertManagerConfigReadException
   */
  public Route getGlobalRoute() throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    return alertManagerConfig.getRoute();
  }

  /**
   *
   * @param route
   * @return updated AlertManagerConfig
   * @throws AlertManagerConfigReadException
   * @throws AlertManagerServerException
   */
  public AlertManagerConfig updateGlobalRoute(Route route) throws AlertManagerConfigReadException {
    validateGlobalRoute(route);
    AlertManagerConfig alertManagerConfig = read();
    alertManagerConfig.setRoute(route);
    return alertManagerConfig;
  }

  private void validateGlobalRoute(Route route) {
    if (route == null) {
      throw new IllegalArgumentException("Route config can not be null.");
    }
  }

  /**
   *
   * @return
   * @throws AlertManagerConfigReadException
   */
  public List<InhibitRule> getInhibitRules() throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    return alertManagerConfig.getInhibitRules();
  }

  /**
   *
   * @param inhibitRules
   * @return updated AlertManagerConfig
   * @throws AlertManagerConfigReadException
   * @throws AlertManagerServerException
   */
  public AlertManagerConfig updateInhibitRules(List<InhibitRule> inhibitRules) throws AlertManagerConfigReadException {
    validateInhibitRules(inhibitRules);
    AlertManagerConfig alertManagerConfig = read();
    alertManagerConfig.setInhibitRules(inhibitRules);
    return alertManagerConfig;
  }

  private void validateInhibitRules(List<InhibitRule> inhibitRules) {
    if (inhibitRules == null || inhibitRules.isEmpty()) {
      throw new IllegalArgumentException("Inhibit Rules config can not be null or empty.");
    }
  }

  /**
   *
   * @param name
   * @return
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public Receiver getReceiver(String name) throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    int index = getIndexOfReceiver(alertManagerConfig, name);
    return alertManagerConfig.getReceivers().get(index);
  }

  /**
   *
   * @param receiver
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if receiver is missing configuration
   */
  public AlertManagerConfig addReceiver(Receiver receiver) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigReadException {
    validate(receiver);
    AlertManagerConfig alertManagerConfig = read();
    if (alertManagerConfig.getReceivers() == null) {
      alertManagerConfig.setReceivers(new ArrayList<>());
    }
    if (alertManagerConfig.getReceivers().contains(receiver)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same name already exists.");
    }
    alertManagerConfig.getReceivers().add(receiver);
    return alertManagerConfig;
  }

  /**
   *
   * @param name
   * @param receiver
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if receiver is missing configuration
   */
  public AlertManagerConfig updateReceiver(String name, Receiver receiver) throws AlertManagerNoSuchElementException,
      AlertManagerDuplicateEntryException, AlertManagerConfigReadException {
    validate(receiver);
    AlertManagerConfig alertManagerConfig = read();
    int index = getIndexOfReceiver(alertManagerConfig, name);
    if (!name.equals(receiver.getName()) && alertManagerConfig.getReceivers().contains(receiver)) {
      throw new AlertManagerDuplicateEntryException("A receiver with the same name already exists.");
    }
    alertManagerConfig.getReceivers().remove(index);
    alertManagerConfig.getReceivers().add(receiver);
    return alertManagerConfig;
  }

  /**
   *
   * @param name
   * @param emailConfig
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if emailConfig is missing to
   */
  public AlertManagerConfig addEmailToReceiver(String name, EmailConfig emailConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigReadException {
    validate(emailConfig);
    AlertManagerConfig alertManagerConfig = read();
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
   *
   * @param name
   * @param emailConfig
   * @return updated AlertManagerConfig or null if it was not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerServerException
   * @throws AlertManagerConfigReadException
   */
  public AlertManagerConfig removeEmailFromReceiver(String name, EmailConfig emailConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
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
   *
   * @param name
   * @param slackConfig
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if slackConfig is missing apiUrl or channel
   */
  public AlertManagerConfig addSlackToReceiver(String name, SlackConfig slackConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigReadException {
    validate(slackConfig);
    AlertManagerConfig alertManagerConfig = read();
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
   *
   * @param name
   * @param slackConfig
   * @return updated AlertManagerConfig or null if it was not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerServerException
   * @throws AlertManagerConfigReadException
   */
  public AlertManagerConfig removeSlackFromReceiver(String name, SlackConfig slackConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
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
   *
   * @param name
   * @param pagerdutyConfig
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if pagerdutyConfig is missing both RoutingKey and ServiceKey
   */
  public AlertManagerConfig addPagerdutyToReceiver(String name, PagerdutyConfig pagerdutyConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigReadException {
    validate(pagerdutyConfig);
    AlertManagerConfig alertManagerConfig = read();
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
   *
   * @param name
   * @param pagerdutyConfig
   * @return updated AlertManagerConfig or null if it is not updated
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerServerException
   * @throws AlertManagerConfigReadException
   */
  public AlertManagerConfig removePagerdutyFromReceiver(String name, PagerdutyConfig pagerdutyConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
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

  /**
   *
   * @param name
   * @return updated AlertManagerConfig or null if it is not updated
   * @throws AlertManagerServerException
   * @throws AlertManagerConfigReadException
   */
  public AlertManagerConfig removeReceiver(String name, boolean cascade) throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
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

  private AlertManagerConfig removeRoutes(String receiver, AlertManagerConfig alertManagerConfig) {
    List<Route> routesToDelete = new ArrayList<>();
    for(Route route : alertManagerConfig.getRoute().getRoutes()) {
      if (route.getReceiver().equals(receiver)) {
        routesToDelete.add(route);
      }
    }
    if (routesToDelete.size() > 0) {
      alertManagerConfig.getRoute().getRoutes().removeAll(routesToDelete);
    }
    return alertManagerConfig;
  }

  /**
   *
   * @return
   * @throws AlertManagerConfigReadException
   */
  public List<Route> getRoutes() throws AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    return alertManagerConfig.getRoute() == null || alertManagerConfig.getRoute().getRoutes() == null?
        Collections.emptyList() : alertManagerConfig.getRoute().getRoutes();
  }

  /**
   *
   * @param receiver
   * @param match
   * @param matchRe
   * @return
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public Route getRoute(String receiver, Map<String, String> match, Map<String, String> matchRe)
      throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    Route route = new Route(receiver).withMatch(match).withMatchRe(matchRe);
    return getRoute(route);
  }

  /**
   *
   * @param route
   * @return
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerConfigReadException
   */
  public Route getRoute(Route route) throws AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    int index = getIndexOfRoute(alertManagerConfig, route);
    return alertManagerConfig.getRoute().getRoutes().get(index);
  }

  /**
   *
   * @param route
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if route is missing a receiver or match
   */
  public AlertManagerConfig addRoute(Route route)
      throws AlertManagerDuplicateEntryException, AlertManagerConfigReadException,
      AlertManagerNoSuchElementException {
    validate(route);
    AlertManagerConfig alertManagerConfig = read();
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

  /**
   *
   * @param routeToUpdate
   * @param route
   * @return updated AlertManagerConfig
   * @throws IOException
   * @throws AlertManagerNoSuchElementException
   * @throws AlertManagerDuplicateEntryException
   * @throws AlertManagerConfigUpdateException
   * @throws IllegalArgumentException if route is missing a receiver or match
   */
  public AlertManagerConfig updateRoute(Route routeToUpdate, Route route)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException, AlertManagerConfigReadException {
    validate(route);
    AlertManagerConfig alertManagerConfig = read();
    getIndexOfReceiver(alertManagerConfig, route.getReceiver());//check if receiver exists
    int index = getIndexOfRoute(alertManagerConfig, routeToUpdate);
    if (!routeToUpdate.equals(route) && alertManagerConfig.getRoute().getRoutes().contains(route)) {
      throw new AlertManagerDuplicateEntryException("A route with the same mather and receiver name already exists.");
    }
    alertManagerConfig.getRoute().getRoutes().remove(index);
    alertManagerConfig.getRoute().getRoutes().add(route);
    return alertManagerConfig;
  }

  /**
   *
   * @param route
   * @return updated AlertManagerConfig or null if it is not updated
   * @throws AlertManagerConfigUpdateException
   * @throws AlertManagerServerException
   * @throws AlertManagerConfigReadException
   */
  public AlertManagerConfig removeRoute(Route route)
      throws AlertManagerConfigUpdateException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = read();
    int index = alertManagerConfig.getRoute().getRoutes() == null ? -1 :
        alertManagerConfig.getRoute().getRoutes().indexOf(route);
    if (index > -1) {
      alertManagerConfig.getRoute().getRoutes().remove(index);
      return alertManagerConfig;
    }
    return null;
  }

  private int getIndexOfReceiver(AlertManagerConfig alertManagerConfig, String name)
      throws AlertManagerNoSuchElementException {
    Receiver receiverToUpdate = new Receiver(name);
    int index = alertManagerConfig.getReceivers() == null ? -1 :
        alertManagerConfig.getReceivers().indexOf(receiverToUpdate);
    if (index < 0) {
      throw new AlertManagerNoSuchElementException("A receiver with the given name was not found. Name=" + name);
    }
    return index;
  }

  private int getIndexOfRoute(AlertManagerConfig alertManagerConfig, Route route)
      throws AlertManagerNoSuchElementException {
    int index = alertManagerConfig.getRoute() == null || alertManagerConfig.getRoute().getRoutes() == null ? -1 :
        alertManagerConfig.getRoute().getRoutes().indexOf(route);
    if (index < 0) {
      throw new AlertManagerNoSuchElementException(
          "A route with the given receiver name was not found. Receiver Name=" + route.getReceiver());
    }
    return index;
  }

  private void validate(Receiver receiver) {
    if (Strings.isNullOrEmpty(receiver.getName())) {
      throw new IllegalArgumentException("Receiver name can not be empty.");
    }
    if ((receiver.getEmailConfigs() == null || receiver.getEmailConfigs().isEmpty()) &&
        (receiver.getSlackConfigs() == null || receiver.getSlackConfigs().isEmpty()) &&
        (receiver.getOpsgenieConfigs() == null || receiver.getOpsgenieConfigs().isEmpty()) &&
        (receiver.getPagerdutyConfigs() == null || receiver.getPagerdutyConfigs().isEmpty()) &&
        (receiver.getPushoverConfigs() == null || receiver.getPushoverConfigs().isEmpty()) &&
        (receiver.getVictoropsConfigs() == null || receiver.getVictoropsConfigs().isEmpty()) &&
        (receiver.getWebhookConfigs() == null || receiver.getWebhookConfigs().isEmpty()) &&
        (receiver.getWechatConfigs() == null || receiver.getWechatConfigs().isEmpty())) {
      throw new IllegalArgumentException("Receiver needs at least one configuration.");
    }
  }

  private void validate(EmailConfig emailConfig) {
    if (Strings.isNullOrEmpty(emailConfig.getTo())) {
      throw new IllegalArgumentException("EmailConfig to can not be empty.");
    }
  }

  private void validate(SlackConfig slackConfig) {
    if (Strings.isNullOrEmpty(slackConfig.getApiUrl()) || Strings.isNullOrEmpty(slackConfig.getChannel())) {
      throw new IllegalArgumentException("SlackConfig api url and channel can not be empty.");
    }
  }

  private void validate(PagerdutyConfig pagerdutyConfig) {
    if (!Strings.isNullOrEmpty(pagerdutyConfig.getRoutingKey()) &&
        !Strings.isNullOrEmpty(pagerdutyConfig.getServiceKey())) {
      throw new IllegalArgumentException("PagerdutyConfig RoutingKey or ServiceKey must be set.");
    }
  }

  private void validate(Route route) {
    if (Strings.isNullOrEmpty(route.getReceiver())) {
      throw new IllegalArgumentException("Route receiver can not be empty.");
    }
    if ((route.getMatch() == null || route.getMatch().isEmpty()) &&
        (route.getMatchRe() == null || route.getMatchRe().isEmpty())) {
      throw new IllegalArgumentException("Route needs to set match or match regex.");
    }
  }

  public static class Builder {
    private String configPath;
    private AlertManagerClient client;

    public Builder() {
    }

    public Builder withConfigPath(String configPath) {
      this.configPath = configPath;
      return this;
    }

    public Builder withClient(AlertManagerClient client) {
      this.client = client;
      return this;
    }

    private File getConfigFile() throws AlertManagerConfigFileNotFoundException {
      File configFile;
      if (Strings.isNullOrEmpty(this.configPath)) {
        configFile = new File(CONFIG_FILE_PATH);
      } else {
        configFile = new File(this.configPath);
      }
      if (!configFile.exists() || !configFile.isFile()) {
        throw new AlertManagerConfigFileNotFoundException(
            "Alertmanager configuration file not found. Path: " + configFile.getPath());
      } else if (!configFile.canWrite()) {
        throw new AlertManagerConfigFileNotFoundException(
            "Failed to access Alertmanager configuration file. Write permission denied on: " + configFile.getPath());
      }
      return configFile;
    }

    public AlertManagerConfigController build()
        throws ServiceDiscoveryException, AlertManagerConfigFileNotFoundException {
      File configFile = getConfigFile();
      if (this.client == null) {
        this.client = new AlertManagerClient.Builder(ClientBuilder.newClient()).build();
      }
      return new AlertManagerConfigController(configFile, this.client);
    }
  }
}
