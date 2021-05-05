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

package io.hops.hopsworks.alerting.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.api.alert.dto.AlertGroup;
import io.hops.hopsworks.alerting.api.alert.dto.AlertmanagerStatus;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.api.alert.dto.PostableSilence;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.alerting.api.alert.dto.Silence;
import io.hops.hopsworks.alerting.api.alert.dto.SilenceID;
import io.hops.hopsworks.alerting.api.util.Settings;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlertManagerClient implements Closeable {
  private static final Logger LOGGER = Logger.getLogger(AlertManagerClient.class.getName());
  private Client client;
  private WebTarget webTarget;

  public enum RequestMethod {
    GET, POST, PUT, DELETE;
  }

  public AlertManagerClient(Client client, URI target) {
    this.client = client;
    this.webTarget = client.target(target);
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.close();
    }
  }

  private void checkResponse(Response response) throws AlertManagerResponseException {
    Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
    if (statusFamily.equals(Response.Status.Family.CLIENT_ERROR) ||
        statusFamily.equals(Response.Status.Family.SERVER_ERROR)) {
      String output = response.hasEntity() ? ": " + response.readEntity(String.class) : "";
      throw new AlertManagerResponseException(response.getStatusInfo().getReasonPhrase() + output);
    }
  }

  private Response sendRequest(Invocation.Builder invocationBuilder, RequestMethod method, Entity<String> entity)
      throws AlertManagerServerException, AlertManagerResponseException {
    Response response = null;
    try {
      switch (method) {
        case GET:
          response = invocationBuilder.get();
          break;
        case POST:
          response = invocationBuilder.post(entity);
          break;
        case DELETE:
          response = invocationBuilder.delete();
          break;
      }
    } catch (Exception e) {
      throw new AlertManagerServerException(e.getMessage());
    }
    checkResponse(response);
    return response;
  }

  private <T> Entity<String> toEntity(T pojo) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return Entity.json(objectMapper.writeValueAsString(pojo));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to write json");
    }
  }

  private <T> Entity<String> toEntity(List<T> pojo) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return Entity.json(objectMapper.writeValueAsString(pojo));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to write json");
    }
  }

  private <T> T getResponse(Response response, Class<T> type) {
    if (response.hasEntity()) {
      ObjectMapper objectMapper = new ObjectMapper();
      T content;
      try {
        content = objectMapper.readValue(response.readEntity(String.class), type);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read response");
      }
      return content;
    } else {
      throw new IllegalStateException("Failed to read response");
    }
  }

  private <T> List<T> getResponseList(Response response, Class<T> clazz) {
    if (response.hasEntity()) {
      //because GenericType<List<T>> creates dependency conflict.
      ObjectMapper objectMapper = new ObjectMapper();
      List<T> content;
      TypeFactory t = objectMapper.getTypeFactory();
      try {
        content = objectMapper
            .readValue(response.readEntity(String.class), t.constructCollectionType(ArrayList.class, clazz));
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read response");
      }
      return content;
    } else {
      throw new IllegalStateException("Failed to read response");
    }
  }

  private WebTarget setQueryParams(WebTarget webTarget, Boolean active, Boolean silenced, Boolean inhibited,
      Boolean unprocessed, Set<String> filters, String receiver) {
    if (active != null) {
      webTarget = webTarget.queryParam("active", active);
    }
    if (silenced != null) {
      webTarget = webTarget.queryParam("silenced", silenced);
    }
    if (inhibited != null) {
      webTarget = webTarget.queryParam("inhibited", inhibited);
    }
    if (unprocessed != null) {
      webTarget = webTarget.queryParam("unprocessed", unprocessed);
    }
    webTarget = setFilters(webTarget, filters);
    if (!Strings.isNullOrEmpty(receiver)) {
      webTarget = webTarget.queryParam("receiver", receiver);
    }
    return webTarget;
  }

  private WebTarget setFilters(WebTarget webTarget, Set<String> filters) {
    if (filters != null && !filters.isEmpty()) {
      for (String filter : filters) {
        webTarget = webTarget.queryParam("filter", filter);
      }
    }
    return webTarget;
  }

  public Response healthy() throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.MANAGEMENT_API_HEALTH);
    LOGGER.log(Level.FINE, "Sending request healthy to: {0}", wt.toString());
    return sendRequest(wt.request(), RequestMethod.GET, null);
  }

  public Response ready() throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.MANAGEMENT_API_READY);
    LOGGER.log(Level.FINE, "Sending request ready to: {0}", wt.toString());
    return sendRequest(wt.request(), RequestMethod.GET, null);
  }

  public Response reload() throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.MANAGEMENT_API_RELOAD);
    LOGGER.log(Level.FINE, "Sending request reload to: {0}", wt.toString());
    return sendRequest(wt.request(), RequestMethod.POST, Entity.json(""));
  }

  public AlertmanagerStatus getStatus() throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_STATUS);
    LOGGER.log(Level.FINE, "Sending request getStatus to: {0}", wt.toString());
    AlertmanagerStatus alertmanagerStatus =
        getResponse(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null),
            AlertmanagerStatus.class);
    return alertmanagerStatus;
  }

  public List<ReceiverName> getReceivers() throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_RECEIVERS);
    LOGGER.log(Level.FINE, "Sending request getReceivers to: {0}", wt.toString());
    List<ReceiverName> receiver =
        getResponseList(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null),
            ReceiverName.class);
    return receiver;
  }

  public List<Silence> getSilences() throws AlertManagerResponseException, AlertManagerServerException {
    return getSilences(null);
  }

  public List<Silence> getSilences(Set<String> filters)
      throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_SILENCES);
    wt = setFilters(wt, filters);
    LOGGER.log(Level.FINE, "Sending request getSilences to: {0}", wt.toString());
    List<Silence> silences =
        getResponseList(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null), Silence.class);
    return silences;
  }

  public Silence getSilence(String uuid) throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_SILENCE).path(uuid);
    LOGGER.log(Level.FINE, "Sending request getSilence to: {0}", wt.toString());
    Silence silence =
        getResponse(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null), Silence.class);
    return silence;
  }

  public SilenceID postSilences(PostableSilence postableSilence)
      throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_SILENCES);
    LOGGER.log(Level.FINE, "Sending request postSilences to: {0}", wt.toString());
    SilenceID silenceID =
        getResponse(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.POST, toEntity(postableSilence)),
        SilenceID.class);
    return silenceID;
  }

  public Response deleteSilence(String uuid) throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_SILENCE).path(uuid);
    LOGGER.log(Level.FINE, "Sending request deleteSilence to: {0}", wt.toString());
    return sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.DELETE, null);
  }

  public List<Alert> getAlerts() throws AlertManagerResponseException, AlertManagerServerException {
    return getAlerts(null, null, null, null, null, null);
  }

  public List<Alert> getAlerts(Boolean active, Boolean silenced, Boolean inhibited, Boolean unprocessed,
      Set<String> filters, String receiver)
      throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_ALERTS);
    wt = setQueryParams(wt, active, silenced, inhibited, unprocessed, filters, receiver);
    LOGGER.log(Level.FINE, "Sending request getAlerts to: {0}", wt.toString());
    List<Alert> alerts =
        getResponseList(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null), Alert.class);
    return alerts;
  }

  public Response postAlerts(List<PostableAlert> postableAlerts)
      throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_ALERTS);
    LOGGER.log(Level.FINE, "Sending request postAlerts to: {0}", wt.toString());
    return sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.POST, toEntity(postableAlerts));
  }

  public List<AlertGroup> getAlertGroups(Boolean active, Boolean silenced, Boolean inhibited, Set<String> filters,
      String receiver) throws AlertManagerResponseException, AlertManagerServerException {
    WebTarget wt = webTarget.path(Settings.ALERTS_API_ALERTS_GROUPS);
    wt = setQueryParams(wt, active, silenced, inhibited, null, filters, receiver);
    LOGGER.log(Level.FINE, "Sending request getAlertGroups to: {0}", wt.toString());
    return getResponseList(sendRequest(wt.request(MediaType.APPLICATION_JSON), RequestMethod.GET, null),
        AlertGroup.class);
  }

  public static class Builder {
    private Client client;
    private String serviceFQDN;
    private String serviceDN;
    private boolean isHttps;

    public Builder(Client client) {
      this.client = client;
    }

    public Builder enableHttps() {
      this.isHttps = true;
      return this;
    }

    public Builder withServiceFQDN(String serviceFQDN) {
      if (!Strings.isNullOrEmpty(this.serviceDN)) {
        throw new IllegalArgumentException("Can not set serviceFQDN if serviceDN is set.");
      }
      this.serviceFQDN = serviceFQDN;
      return this;
    }

    public Builder withServiceDN(String domain) {
      if (!Strings.isNullOrEmpty(this.serviceFQDN)) {
        throw new IllegalArgumentException("Can not set serviceDN if serviceFQDN is set.");
      }
      this.serviceDN = domain;
      return this;
    }

    public AlertManagerClient build() throws ServiceDiscoveryException {
      URI target;
      if (Strings.isNullOrEmpty(this.serviceFQDN) && Strings.isNullOrEmpty(this.serviceDN)) {
        target = Settings.getAlertManagerAddress();
      } else if (Strings.isNullOrEmpty(this.serviceFQDN)) {
        target = Settings.getAlertManagerAddressByDN(this.serviceDN, this.isHttps);
      } else {
        target = Settings.getAlertManagerAddressByFQDN(this.serviceFQDN, this.isHttps);
      }
      return new AlertManagerClient(this.client, target);
    }
  }
}
