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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.hops.hopsworks.alert.dao.AlertManagerConfigFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alert.util.VariablesFacade;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.api.util.Settings;
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
  @Resource
  TimerService timerService;
  private AlertManagerConfigController alertManagerConfigController;
  private AlertManagerClient client;
  private Exception initException;
  private int clientCount = 0;
  private int configCount = 0;
  private int serverErrorCount = 0;
  @EJB
  private VariablesFacade variablesFacade;
  @EJB
  private AlertManagerConfigFacade alertManagerConfigFacade;

  @PostConstruct
  public void init() {
    tryBuildClient();
    tryBuildAlertManagerConfigCtrl();
  }

  private void tryBuildClient() {
    String domain = variablesFacade.getVariableValue(VariablesFacade.SERVICE_DISCOVERY_DOMAIN_VARIABLE).orElse("");
    try {
      client = new AlertManagerClient.Builder(ClientBuilder.newClient()).withServiceDN(domain).build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to init Alertmanager client. " + e.getMessage());
      initException = e;
      createRetryTimer(Constants.TimerType.CLIENT);
    }
  }

  private void tryBuildAlertManagerConfigCtrl() {
    Optional<String> alertManagerConfFile =
        variablesFacade.getVariableValue(VariablesFacade.ALERT_MANAGER_CONFIG_FILE_PATH_VARIABLE);
    String configFile = alertManagerConfFile.orElse(null);
    try {
      alertManagerConfigController = new AlertManagerConfigController.Builder()
          .withClient(getClient())
          .withConfigPath(configFile)
          .build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to init Alertmanager config controller. " + e.getMessage());
      initException = e;
      createRetryTimer(Constants.TimerType.CONFIG);
    }
  }

  private void doClientSanityCheck() throws AlertManagerClientCreateException {
    if (client == null) {
      if (this.initException != null) {
        throw new AlertManagerClientCreateException(this.initException);
      }
      throw new AlertManagerClientCreateException("Failed to instantiate AlertManagerClient");
    }
  }

  private AlertManagerClient getClient() throws AlertManagerClientCreateException {
    doClientSanityCheck();
    return this.client;
  }

  private void registerServerError() {
    serverErrorCount++;
    if (serverErrorCount > Constants.NUM_SERVER_ERRORS) {
      clientCount = 0;
      serverErrorCount = 0;
      client.close();
      client = null;
      Settings.clearCache();
      tryBuildClient();
    }
  }

  private void registerSuccess() {
    serverErrorCount = 0;
  }

  public int getServerErrorCount() {
    return serverErrorCount;
  }

  private void createRetryTimer(Constants.TimerType type) {
    long duration = Constants.RETRY_SECONDS * 1000;
    switch (type) {
      case CLIENT:
        if (clientCount > Constants.NUM_RETRIES) {
          duration *= Constants.NUM_RETRIES;
        } else {
          clientCount++;
        }
        break;
      case CONFIG:
        if (configCount > Constants.NUM_RETRIES) {
          duration *= Constants.NUM_RETRIES;
        } else {
          configCount++;
        }
    }
    TimerConfig config = new TimerConfig();
    config.setInfo(type.name());
    timerService.createSingleActionTimer(duration, config);
  }

  private void doSanityCheck() throws AlertManagerConfigCtrlCreateException {
    if (alertManagerConfigController == null) {
      if (this.initException != null) {
        throw new AlertManagerConfigCtrlCreateException(this.initException);
      }
      throw new AlertManagerConfigCtrlCreateException("Failed to instantiate AlertManagerConfigController");
    }
  }

  private void registerServerError(AlertManagerServerException e) throws AlertManagerUnreachableException {
    registerServerError();
    throw new AlertManagerUnreachableException("Alertmanager not reachable.", e);
  }

  @PreDestroy
  public void preDestroy() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void performTimeout(Timer timer) {
    if (Constants.TimerType.CONFIG.name().equals(timer.getInfo())) {
      tryBuildAlertManagerConfigCtrl();
    } else if (Constants.TimerType.CLIENT.name().equals(timer.getInfo())) {
      tryBuildClient();
    }
  }

  @Lock(LockType.READ)
  public AlertManagerConfig read() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.read();
  }

  public void writeAndReload(AlertManagerConfig alertManagerConfig) throws AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    if (alertManagerConfig != null) {
      doSanityCheck();
      doClientSanityCheck();
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        JSONObject jsonObject = new JSONObject(objectMapper.writeValueAsString(alertManagerConfig));
        alertManagerConfigController.writeAndReload(alertManagerConfig);
        saveToDatabase(jsonObject);
        registerSuccess();
      } catch (AlertManagerServerException e) {
        registerServerError(e);
      } catch (JsonProcessingException e) {
        throw new AlertManagerConfigUpdateException(
            "Can not save config to database. Failed to parse config to json. " + e.getMessage(), e);
      }
    }
  }

  private void saveToDatabase(JSONObject jsonObject) {
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = alertManagerConfigFacade.getLatest();
    AlertManagerConfigEntity alertManagerConfigEntity;
    if (!optionalAlertManagerConfigEntity.isPresent()) {
      alertManagerConfigEntity = new AlertManagerConfigEntity();
      alertManagerConfigEntity.setContent(jsonObject);
      alertManagerConfigEntity.setCreated(new Date());
      alertManagerConfigFacade.save(alertManagerConfigEntity);
    } else {
      alertManagerConfigEntity = optionalAlertManagerConfigEntity.get();
      alertManagerConfigEntity.setContent(jsonObject);
      alertManagerConfigEntity.setCreated(new Date());
      alertManagerConfigFacade.update(alertManagerConfigEntity);
    }
  }

  public void restoreFromBackup() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException,
      IOException, AlertManagerUnreachableException, AlertManagerConfigUpdateException,
      AlertManagerClientCreateException {
    AlertManagerConfig alertManagerConfig = read();
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = alertManagerConfigFacade.getLatest();
    ObjectMapper objectMapper = new ObjectMapper();

    JSONObject jsonAlertManagerConfig = alertManagerConfig != null?
        new JSONObject(objectMapper.writeValueAsString(alertManagerConfig)) : null;

    JSONObject jsonAlertManagerConfigBackup =
        optionalAlertManagerConfigEntity.map(AlertManagerConfigEntity::getContent).orElse(null);

    AlertManagerConfig alertManagerConfigBackup = jsonAlertManagerConfigBackup != null?
        objectMapper.readValue(jsonAlertManagerConfigBackup.toString(), AlertManagerConfig.class) : null;

    if (jsonAlertManagerConfig != null && jsonAlertManagerConfigBackup != null) {
      if (!jsonAlertManagerConfig.similar(jsonAlertManagerConfigBackup)) {
        boolean updated = merge(alertManagerConfig, alertManagerConfigBackup);
        if (updated) {
          writeAndReload(alertManagerConfig);
          LOGGER.log(Level.INFO, "Fixed Alert manager config from backup.");
        } else {// Not similar but not updated then save new backup
          saveToDatabase(jsonAlertManagerConfig);
          LOGGER.log(Level.INFO, "Alert manager config backup saved.");
        }
      } else {
        LOGGER.log(Level.INFO, "Alert manager config is up to date with backup.");
      }
    } else if (jsonAlertManagerConfig == null && jsonAlertManagerConfigBackup != null) {
      writeAndReload(alertManagerConfigBackup);
      LOGGER.log(Level.INFO, "Replace Alert manager config with backup.");
    } else if (jsonAlertManagerConfig != null) {
      saveToDatabase(jsonAlertManagerConfig);
      LOGGER.log(Level.INFO, "Alert manager config backup saved.");
    }
  }

  private boolean merge(AlertManagerConfig alertManagerConfig1, AlertManagerConfig alertManagerConfig2) {
    boolean updated = false;
    if (alertManagerConfig1.getGlobal() == null && alertManagerConfig2.getGlobal() != null) {
      alertManagerConfig1.setGlobal(alertManagerConfig2.getGlobal());
      updated = true;
    }
    if (alertManagerConfig1.getTemplates() == null && alertManagerConfig2.getTemplates() != null) {
      alertManagerConfig1.setTemplates(alertManagerConfig2.getTemplates());
      updated = true;
    }
    if (alertManagerConfig1.getRoute() == null && alertManagerConfig2.getRoute() != null) {
      alertManagerConfig1.setRoute(alertManagerConfig2.getRoute());
      updated = true;
    } else if (alertManagerConfig2.getRoute() != null && alertManagerConfig2.getRoute().getRoutes() != null &&
        !alertManagerConfig2.getRoute().getRoutes().isEmpty()) {
      if (alertManagerConfig1.getRoute().getRoutes() == null || alertManagerConfig1.getRoute().getRoutes().isEmpty()) {
        alertManagerConfig1.getRoute().setRoutes(alertManagerConfig2.getRoute().getRoutes());
        updated = true;
      } else {
        for (Route route : alertManagerConfig2.getRoute().getRoutes()) {
          if (!alertManagerConfig1.getRoute().getRoutes().contains(route)) {
            alertManagerConfig1.getRoute().getRoutes().add(route);
            updated = true;
          }
        }
      }
    }
    if ((alertManagerConfig1.getInhibitRules() == null || alertManagerConfig1.getInhibitRules().isEmpty()) &&
        alertManagerConfig2.getInhibitRules() != null) {
      alertManagerConfig1.setInhibitRules(alertManagerConfig2.getInhibitRules());
      updated = true;
    }
    if ((alertManagerConfig1.getReceivers() == null || alertManagerConfig1.getReceivers().isEmpty()) &&
        alertManagerConfig2.getReceivers() != null) {
      alertManagerConfig1.setReceivers(alertManagerConfig2.getReceivers());
      updated = true;
    } else if (alertManagerConfig2.getReceivers() != null && !alertManagerConfig2.getReceivers().isEmpty()) {
      for (Receiver receiver : alertManagerConfig2.getReceivers()) {
        if (!alertManagerConfig1.getReceivers().contains(receiver)) {
          alertManagerConfig1.getReceivers().add(receiver);
          updated = true;
        }
      }
    }
    return updated;
  }

  public Global getGlobal() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getGlobal();
  }

  public void updateGlobal(Global global)
      throws AlertManagerConfigCtrlCreateException, AlertManagerClientCreateException,
      AlertManagerUnreachableException, AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateGlobal(global);
    writeAndReload(alertManagerConfig);
  }

  public List<String> getTemplates() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getTemplates();
  }

  public void updateTemplates(List<String> templates) throws AlertManagerConfigCtrlCreateException,
      AlertManagerClientCreateException, AlertManagerUnreachableException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateTemplates(templates);
    writeAndReload(alertManagerConfig);
  }

  public Route getGlobalRoute() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getGlobalRoute();
  }

  public void updateRoute(Route route) throws AlertManagerConfigCtrlCreateException, AlertManagerClientCreateException,
      AlertManagerUnreachableException, AlertManagerConfigReadException, AlertManagerConfigUpdateException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateGlobalRoute(route);
    writeAndReload(alertManagerConfig);
  }

  public List<InhibitRule> getInhibitRules() throws AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getInhibitRules();
  }

  public void updateInhibitRules(List<InhibitRule> inhibitRules) throws AlertManagerConfigCtrlCreateException,
      AlertManagerClientCreateException, AlertManagerUnreachableException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateInhibitRules(inhibitRules);
    writeAndReload(alertManagerConfig);
  }

  private void fixReceiverName(Receiver receiver, Project project) {
    if (receiver.getName() != null && !receiver.getName()
        .startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER, project.getName()))) {
      receiver.setName(Constants.RECEIVER_NAME_FORMAT.replace(Constants.PROJECT_PLACE_HOLDER, project.getName())
          .replace(Constants.RECEIVER_NAME_PLACE_HOLDER, receiver.getName()));
    }
  }

  public Receiver getReceiver(String name, Project project)
      throws AlertManagerConfigCtrlCreateException, AlertManagerNoSuchElementException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    doSanityCheck();
    checkPermission(name, project, true);
    return alertManagerConfigController.getReceiver(name);
  }

  public Receiver getReceiver(String name) throws AlertManagerConfigCtrlCreateException,
      AlertManagerNoSuchElementException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getReceiver(name);
  }

  public void addReceiver(Receiver receiver, Project project) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    fixReceiverName(receiver, project);
    addReceiver(receiver);
  }

  public void addReceiver(Receiver receiver) throws AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addReceiver(receiver);
    writeAndReload(alertManagerConfig);
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

  public void updateReceiver(String name, Receiver receiver, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    fixReceiverName(receiver, project);
    updateReceiver(name, receiver);
  }

  public void updateReceiver(String name, Receiver receiver)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateReceiver(name, receiver);
    writeAndReload(alertManagerConfig);
  }

  public void addEmailToReceiver(String name, EmailConfig emailConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    addEmailToReceiver(name, emailConfig);
  }

  public void addEmailToReceiver(String name, EmailConfig emailConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addEmailToReceiver(name, emailConfig);
    writeAndReload(alertManagerConfig);
  }

  public void removeEmailFromReceiver(String name, EmailConfig emailConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    removeEmailFromReceiver(name, emailConfig);
  }

  public void removeEmailFromReceiver(String name, EmailConfig emailConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.removeEmailFromReceiver(name, emailConfig);
    writeAndReload(alertManagerConfig);
  }

  public void addSlackToReceiver(String name, SlackConfig slackConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    addSlackToReceiver(name, slackConfig);
  }

  public void addSlackToReceiver(String name, SlackConfig slackConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addSlackToReceiver(name, slackConfig);
    writeAndReload(alertManagerConfig);
  }

  public void removeSlackFromReceiver(String name, SlackConfig slackConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    removeSlackFromReceiver(name, slackConfig);
  }

  public void removeSlackFromReceiver(String name, SlackConfig slackConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.removeSlackFromReceiver(name, slackConfig);
    writeAndReload(alertManagerConfig);
  }

  public void addPagerdutyToReceiver(String name, PagerdutyConfig pagerdutyConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    addPagerdutyToReceiver(name, pagerdutyConfig);
  }

  public void addPagerdutyToReceiver(String name, PagerdutyConfig pagerdutyConfig)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addPagerdutyToReceiver(name, pagerdutyConfig);
    writeAndReload(alertManagerConfig);
  }

  public void removePagerdutyFromReceiver(String name, PagerdutyConfig pagerdutyConfig, Project project)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
    checkPermission(name, project, false);
    removePagerdutyFromReceiver(name, pagerdutyConfig);
  }

  public void removePagerdutyFromReceiver(String name, PagerdutyConfig pagerdutyConfig)
      throws AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig =
        alertManagerConfigController.removePagerdutyFromReceiver(name, pagerdutyConfig);
    writeAndReload(alertManagerConfig);
  }

  public void removeReceiver(String name, Project project)
      throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerAccessControlException,
      AlertManagerConfigReadException {
    checkPermission(name, project, false);
    removeReceiver(name);
  }

  public void removeReceiver(String name)
      throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.removeReceiver(name);
    writeAndReload(alertManagerConfig);
  }

  private void fixRoute(Route route, Project project) {
    if (route.getMatch() != null && !route.getMatch().isEmpty()) {
      route.getMatch().put(Constants.ALERT_TYPE_LABEL, AlertType.PROJECT_ALERT.getValue());
      route.getMatch().put(Constants.LABEL_PROJECT, project.getName());
    }
    if (route.getMatchRe() != null && !route.getMatchRe().isEmpty()) {
      route.getMatchRe().put(Constants.ALERT_TYPE_LABEL, AlertType.PROJECT_ALERT.getValue());
      route.getMatchRe().put(Constants.LABEL_PROJECT, project.getName());
    }
  }

  private boolean isRouteInProject(Route route, Project project) {
    return !Strings.isNullOrEmpty(route.getReceiver()) && route.getReceiver()
        .startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER, project.getName())) &&
        ((route.getMatch() != null && route.getMatch().get(Constants.LABEL_PROJECT) != null &&
            route.getMatch().get(Constants.LABEL_PROJECT).equals(project.getName())) ||
            (route.getMatchRe() != null && route.getMatchRe().get(Constants.LABEL_PROJECT) != null &&
                route.getMatchRe().get(Constants.LABEL_PROJECT).equals(project.getName())));
  }

  private boolean isRouteGlobal(Route route) {
    return (route.getMatch() != null && route.getMatch().get(Constants.ALERT_TYPE_LABEL) != null &&
        AlertType.fromValue(route.getMatch().get(Constants.ALERT_TYPE_LABEL)).isGlobal()) ||
        (route.getMatchRe() != null && route.getMatchRe().get(Constants.ALERT_TYPE_LABEL) != null &&
            AlertType.fromValue(route.getMatchRe().get(Constants.ALERT_TYPE_LABEL)).isGlobal());
  }

  public List<Route> getRoutes(Project project) throws AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException {
    List<Route> projectRoutes = new ArrayList<>();
    List<Route> routeList = getRoutes();
    for (Route route : routeList) {
      if (isRouteInProject(route, project)) {
        projectRoutes.add(route);
      } else if (isRouteGlobal(route)) {
        projectRoutes.add(route);
      }
    }
    return projectRoutes;
  }

  public List<Route> getRoutes() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getRoutes();
  }

  public Route getRoute(Route route, Project project)
      throws AlertManagerAccessControlException, AlertManagerNoSuchElementException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    if (project != null) {
      return getRoute(route.getReceiver(), route.getMatch(), route.getMatchRe(), project);
    } else {
      return getRoute(route.getReceiver(), route.getMatch(), route.getMatchRe());
    }
  }

  public Route getRoute(String receiver, Map<String, String> match, Map<String, String> matchRe, Project project)
      throws AlertManagerConfigCtrlCreateException, AlertManagerNoSuchElementException,
      AlertManagerAccessControlException, AlertManagerConfigReadException {
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

  public Route getRoute(String receiver, Map<String, String> match, Map<String, String> matchRe)
      throws AlertManagerConfigCtrlCreateException, AlertManagerNoSuchElementException,
      AlertManagerConfigReadException {
    doSanityCheck();
    return alertManagerConfigController.getRoute(receiver, match, matchRe);
  }

  public void addRoute(Route route, Project project)
      throws AlertManagerDuplicateEntryException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException, AlertManagerClientCreateException,
      AlertManagerAccessControlException, AlertManagerConfigReadException, AlertManagerNoSuchElementException {
    fixRoute(route, project);
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
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addRoute(route);
    writeAndReload(alertManagerConfig);
  }

  private void checkPermission(Route route, Project project) throws AlertManagerAccessControlException {
    if (!isRouteInProject(route, project)) {
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
    fixRoute(route, project);
    updateRoute(routeToUpdate, route);
  }

  public void updateRoute(Route routeToUpdate, Route route)
      throws AlertManagerNoSuchElementException, AlertManagerDuplicateEntryException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerUnreachableException,
      AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.updateRoute(routeToUpdate, route);
    writeAndReload(alertManagerConfig);
  }

  public void removeRoute(Route route, Project project)
      throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerAccessControlException,
      AlertManagerConfigReadException {
    List<Route> routes = getRoutes(project);
    if (!routes.isEmpty() && routes.contains(route)) {
      removeRoute(route);
    }
  }

  public void removeRoute(Route route) throws AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerUnreachableException, AlertManagerClientCreateException, AlertManagerConfigReadException {
    doSanityCheck();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.removeRoute(route);
    writeAndReload(alertManagerConfig);
  }
}
