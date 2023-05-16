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

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alert.util.VariablesFacade;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.api.alert.dto.AlertGroup;
import io.hops.hopsworks.alerting.api.alert.dto.AlertmanagerStatus;
import io.hops.hopsworks.alerting.api.alert.dto.Matcher;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.api.alert.dto.PostableSilence;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.alerting.api.alert.dto.Silence;
import io.hops.hopsworks.alerting.api.alert.dto.SilenceID;
import io.hops.hopsworks.alerting.api.util.Settings;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
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
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AMClient {
  private final static Logger LOGGER = Logger.getLogger(AMClient.class.getName());

  private AlertManagerClient client;
  private Exception initException;
  private int count = 0;
  private int serverErrorCount = 0;

  @EJB
  private VariablesFacade variablesFacade;
  @Resource
  TimerService timerService;

  @PostConstruct
  public void init() {
    tryBuildClient();
  }

  void registerServerError() {
    serverErrorCount++;
    if (serverErrorCount > Constants.NUM_SERVER_ERRORS) {
      count = 0;
      serverErrorCount = 0;
      client.close();
      client = null;
      Settings.clearCache();
      tryBuildClient();
    }
  }

  public AMClient() {
  }

  //For test
  public AMClient(AlertManagerClient client) {
    this.client = client;
  }

  void registerSuccess(){
    serverErrorCount = 0;
  }

  public int getServerErrorCount() {
    return serverErrorCount;
  }

  void tryBuildClient() {
    String domain = variablesFacade.getVariableValue(VariablesFacade.SERVICE_DISCOVERY_DOMAIN_VARIABLE).orElse("");
    try {
      client = new AlertManagerClient.Builder(ClientBuilder.newClient()).withServiceDN(domain).build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to init Alertmanager client. " + e.getMessage());
      initException = e;
      createRetryTimer();
    }
  }

  void createRetryTimer() {
    long duration = Constants.RETRY_SECONDS * 1000;
    if (count > Constants.NUM_RETRIES) {
      duration *= Constants.NUM_RETRIES;
    } else {
      count++;
    }
    TimerConfig config = new TimerConfig();
    config.setInfo("Retry client");
    config.setPersistent(false);
    timerService.createSingleActionTimer(duration, config);
  }

  @PreDestroy
  public void preDestroy() {
    if (client != null) {
      client.close();
    }
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void performTimeout(Timer timer) {
    tryBuildClient();
  }

  private void doClientSanityCheck() throws AlertManagerClientCreateException {
    if (client == null) {
      if (this.initException != null) {
        throw new AlertManagerClientCreateException(this.initException);
      }
      throw new AlertManagerClientCreateException("Failed to instantiate AlertManagerClient");
    }
  }

  public AlertManagerClient getClient() {
    return client;
  }

  public Response healthy()
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Response response = client.healthy();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public Response ready()
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Response response = client.ready();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public Response reload()
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Response response = client.reload();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public AlertmanagerStatus getStatus()
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      AlertmanagerStatus response = client.getStatus();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public List<ReceiverName> getReceivers(Project project, boolean includeGlobal)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    List<ReceiverName> receivers = getReceivers();
    if (receivers == null || receivers.isEmpty()) {
      return Collections.emptyList();
    }
    return receivers.stream()
        .filter(receiver -> receiver.getName()
            .startsWith(Constants.RECEIVER_NAME_PREFIX.replace(Constants.PROJECT_PLACE_HOLDER, project.getName())) ||
            (includeGlobal && receiver.getName().startsWith(Constants.GLOBAL_RECEIVER_NAME_PREFIX)))
        .collect(Collectors.toList());
  }

  public List<ReceiverName> getReceivers()
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      List<ReceiverName> response = client.getReceivers();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public List<Silence> getSilences(Set<String> filters, Project project)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    filters = getFilters(filters, project);
    return  getSilences(filters);
  }

  public List<Silence> getSilences(Set<String> filters)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      List<Silence> response = client.getSilences(filters);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public Optional<Silence> getSilence(String uuid, Project project)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    Silence silence = getSilence(uuid);
    if (silence == null) {
      return Optional.empty();
    }
    for (Matcher matcher : silence.getMatchers()) {
      if (matcher.getName().equals(Constants.LABEL_PROJECT)) {
        if (matcher.getValue().startsWith(project.getName())) {
          return Optional.of(silence);
        }
      }
    }
    return Optional.empty();
  }

  public Silence getSilence(String uuid)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Silence response = client.getSilence(uuid);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  /**
   * Post a new silence or update an existing one
   * @param postableSilence
   * @param project
   * @param user
   * @return
   * @throws AlertManagerClientCreateException
   * @throws AlertManagerResponseException
   * @throws AlertManagerUnreachableException
   * @throws AlertManagerAccessControlException
   */
  public SilenceID postSilences(PostableSilence postableSilence, Project project, Users user)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    postableSilence.setCreatedBy(Constants.SILENCE_CREATED_BY_FORMAT
        .replace(Constants.USER_PLACE_HOLDER, user.getUsername())
        .replace(Constants.PROJECT_PLACE_HOLDER, project.getName()));
    if (Strings.isNullOrEmpty(postableSilence.getComment())) {
      throw new IllegalArgumentException("Comment is required.");
    }
    if (postableSilence.getMatchers() == null || postableSilence.getMatchers().isEmpty()) {
      throw new IllegalArgumentException("Matchers can not be empty.");
    }
    boolean containsProject = false;
    for (Matcher matcher : postableSilence.getMatchers()) {
      if (matcher.getName().equals(Constants.LABEL_PROJECT)) {
        containsProject = true;
        if (!matcher.getValue().startsWith(project.getName())) {
          throw new AlertManagerAccessControlException(
              "You do not have permission to access silence with " + matcher.getName() + "=" + matcher.getValue());
        }
      }
    }
    if (!containsProject) {
      postableSilence.getMatchers().add(new Matcher(false, Constants.LABEL_PROJECT, project.getName()));
    }
    return postSilences(postableSilence);
  }

  /**
   * Post a new silence or update an existing one
   * @param postableSilence
   * @return
   * @throws AlertManagerResponseException
   * @throws AlertManagerClientCreateException
   * @throws AlertManagerUnreachableException
   */
  public SilenceID postSilences(PostableSilence postableSilence)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      SilenceID response = client.postSilences(postableSilence);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public Response deleteSilence(String uuid, Project project)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    Optional<Silence> silence = getSilence(uuid, project);
    if (silence.isPresent()) {
      return deleteSilence(uuid);
    }
    return Response.ok().build();
  }

  public Response deleteSilence(String uuid)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Response response = client.deleteSilence(uuid);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public List<Alert> getAlerts(Project project)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    List<Alert> alerts = getAlerts();
    List<Alert> projectAlerts = new ArrayList<>();
    for (Alert alert : alerts) {
      if (alert.getLabels().get(Constants.LABEL_PROJECT) != null &&
          alert.getLabels().get(Constants.LABEL_PROJECT).equals(project.getName())) {
        projectAlerts.add(alert);
      }
    }
    return projectAlerts;
  }

  public List<Alert> getAlerts()
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      List<Alert> response = client.getAlerts();
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public List<Alert> getAlerts(Boolean active, Boolean silenced, Boolean inhibited, Boolean unprocessed,
      Set<String> filters, String receiver, Project project)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    filters = getFilters(filters, project);
    return getAlerts(active, silenced, inhibited, unprocessed, filters, receiver);
  }

  public List<Alert> getAlerts(Boolean active, Boolean silenced, Boolean inhibited, Boolean unprocessed,
      Set<String> filters, String receiver)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      List<Alert> response = client.getAlerts(active, silenced, inhibited, unprocessed, filters, receiver);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public Response postAlert(PostableAlert postableAlert, Project project)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    postableAlerts.add(postableAlert);
    return postAlerts(postableAlerts, project);
  }

  public Response postAlerts(List<PostableAlert> postableAlerts, Project project)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    for (PostableAlert postableAlert : postableAlerts) {
      if (postableAlert.getLabels() == null) {
        throw new IllegalArgumentException("Labels can not be empty.");
      }
      if (postableAlert.getLabels().get(Constants.LABEL_PROJECT) != null) {
        if (!postableAlert.getLabels().get(Constants.LABEL_PROJECT).startsWith(project.getName())) {
          throw new AlertManagerAccessControlException(
              "You do not have permission to create alerts for " + Constants.LABEL_PROJECT + "=" +
                  postableAlert.getLabels().get(Constants.LABEL_PROJECT));
        }
      } else {
        postableAlert.getLabels().put(Constants.LABEL_PROJECT, project.getName());
      }
      if (postableAlert.getLabels().get(Constants.ALERT_TYPE_LABEL) != null) {
        AlertType type = AlertType.fromValue(postableAlert.getLabels().get(Constants.ALERT_TYPE_LABEL));
        if (type == null) {
          throw new IllegalArgumentException("Value for label type not recognized.");
        }
        if (AlertType.SYSTEM_ALERT.equals(type)) {
          throw new AlertManagerAccessControlException(
              "You do not have permission to create alerts for " + Constants.ALERT_TYPE_LABEL + "=" +
                  AlertType.SYSTEM_ALERT.getValue());
        }
      } else {
        postableAlert.getLabels().put(Constants.ALERT_TYPE_LABEL, AlertType.PROJECT_ALERT.getValue());
      }
    }
    return postAlerts(postableAlerts);
  }

  @Asynchronous
  public void asyncPostAlerts(List<PostableAlert> postableAlerts) {
    try {
      postAlerts(postableAlerts);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not send alert", e);
    }
  }

  public Response postAlerts(List<PostableAlert> postableAlerts)
      throws AlertManagerClientCreateException, AlertManagerResponseException, AlertManagerUnreachableException {
    doClientSanityCheck();
    try {
      Response response = client.postAlerts(postableAlerts);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  public List<AlertGroup> getAlertGroups(Boolean active, Boolean silenced, Boolean inhibited, Set<String> filters,
      String receiver, Project project)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException,
      AlertManagerAccessControlException {
    filters = getFilters(filters, project);
    return getAlertGroups(active, silenced, inhibited, filters, receiver);
  }

  public List<AlertGroup> getAlertGroups(Boolean active, Boolean silenced, Boolean inhibited, Set<String> filters,
      String receiver)
      throws AlertManagerResponseException, AlertManagerClientCreateException, AlertManagerUnreachableException{
    doClientSanityCheck();
    try {
      List<AlertGroup> response = client.getAlertGroups(active, silenced, inhibited, filters, receiver);
      registerSuccess();
      return response;
    } catch (AlertManagerServerException e) {
      registerServerError();
      throw new AlertManagerUnreachableException("Alertmanager not reachable." + e.getMessage(), e);
    }
  }

  private Set<String> getFilters(Set<String> filters, Project project) throws AlertManagerAccessControlException {
    if (filters == null) {
      filters = new HashSet<>();
    }
    boolean containsProject = false;
    String projectFilter = Constants.FILTER_BY_PROJECT_FORMAT.replace(Constants.PROJECT_PLACE_HOLDER,
        project.getName());
    for (String filter : filters) {
      if (filter.startsWith(Constants.FILTER_BY_PROJECT_LABEL)) {
        containsProject = true;
        if (!filter.equals(projectFilter)) {
          throw new AlertManagerAccessControlException("You do not have permission to create alerts for " + filter);
        }
      }
    }
    if (!containsProject) {
      filters.add(projectFilter);
    }
    return filters;
  }
}
