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

import com.google.common.annotations.VisibleForTesting;
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
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AlertManagerClient implements Closeable {
  private static final Logger LOGGER = Logger.getLogger(AlertManagerClient.class.getName());
  private final ClientWrapper clientWrapper;
  private final List<ClientWrapper> peerClients;

  public AlertManagerClient(Client client, URI target, List<URI> peers) {
    this.clientWrapper = new ClientWrapper(client, target);
    this.peerClients =
      peers.stream().map(p -> new ClientWrapper(ClientBuilder.newClient(), p)).collect(Collectors.toList());
  }

  @VisibleForTesting
  public AlertManagerClient(ClientWrapper clientWrapper, List<ClientWrapper> peerClients) {
    this.clientWrapper = clientWrapper;
    this.peerClients = peerClients;
  }

  @Override
  public void close() {
    if (this.clientWrapper != null) {
      this.clientWrapper.close();
    }
    for (ClientWrapper client : this.peerClients) {
      client.close();
    }
  }

  public Response healthy() throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.healthy();
  }

  public Response ready() throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.ready();
  }

  public Response reload() throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.reload();
  }

  public AlertmanagerStatus getStatus() throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.getStatus();
  }

  public List<ReceiverName> getReceivers() throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.getReceivers();
  }

  public List<Silence> getSilences(Set<String> filters) throws AlertManagerResponseException,
      AlertManagerServerException {
    return this.clientWrapper.getSilences(filters);
  }

  public Silence getSilence(String uuid) throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.getSilence(uuid);
  }

  public SilenceID postSilences(PostableSilence postableSilence) throws AlertManagerResponseException,
      AlertManagerServerException {
    return this.clientWrapper.postSilences(postableSilence);
  }

  public Response deleteSilence(String uuid) throws AlertManagerResponseException, AlertManagerServerException {
    return this.clientWrapper.deleteSilence(uuid);
  }

  public List<Alert> getAlerts() throws AlertManagerResponseException, AlertManagerServerException {
    return getAlerts(null, null, null, null, null, null);
  }

  public List<Alert> getAlerts(Boolean active, Boolean silenced, Boolean inhibited, Boolean unprocessed,
      Set<String> filters, String receiver) throws AlertManagerResponseException, AlertManagerServerException {
    List<Alert> localAlerts = null;
    AlertManagerResponseException alertManagerResponseException = null;
    AlertManagerServerException alertManagerServerException = null;
    try {
      localAlerts = this.clientWrapper.getAlerts(active, silenced, inhibited, unprocessed, filters, receiver);
    } catch (AlertManagerResponseException e) {
      alertManagerResponseException = e;
      LOGGER.log(Level.WARNING, "Could not get alert from {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    } catch (AlertManagerServerException e) {
      alertManagerServerException = e;
      LOGGER.log(Level.WARNING, "Could not get alert from {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    }
    for (ClientWrapper client : this.peerClients) {
      try {
        if (localAlerts == null) {
          localAlerts = client.getAlerts(active, silenced, inhibited, unprocessed, filters, receiver);
        } else {
          localAlerts.addAll(client.getAlerts(active, silenced, inhibited, unprocessed, filters, receiver));
        }
      } catch (AlertManagerResponseException e) {
        alertManagerResponseException = e;
        LOGGER.log(Level.WARNING, "Could not get alert from {0}. {1}", new Object[]{client.toString(), e.getMessage()});
      } catch (AlertManagerServerException e) {
        alertManagerServerException = e;
        LOGGER.log(Level.WARNING, "Could not get alert from {0}. {1}", new Object[]{client.toString(), e.getMessage()});
      }
    }
    //If there is a response so throw response exception
    if (localAlerts == null && alertManagerResponseException != null) {
      throw alertManagerResponseException;
    }
    if (localAlerts == null && alertManagerServerException != null) {
      throw alertManagerServerException;
    }
    return localAlerts;
  }

  public Response postAlerts(List<PostableAlert> postableAlerts)
      throws AlertManagerResponseException, AlertManagerServerException {
    Response response = null;
    AlertManagerResponseException alertManagerResponseException = null;
    AlertManagerServerException alertManagerServerException = null;
    try {
      response = this.clientWrapper.postAlerts(postableAlerts);
    } catch (AlertManagerResponseException e) {
      alertManagerResponseException = e;
      LOGGER.log(Level.WARNING, "Could not post alert to {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    } catch (AlertManagerServerException e) {
      alertManagerServerException = e;
      LOGGER.log(Level.WARNING, "Could not post alert to {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    }
    for (ClientWrapper client : this.peerClients) {
      try {
        response = client.postAlerts(postableAlerts);
      } catch (AlertManagerResponseException e) {
        alertManagerResponseException = e;
        LOGGER.log(Level.WARNING, "Could not post alert to {0}. {1}", new Object[]{client.toString(), e.getMessage()});
      } catch (AlertManagerServerException e) {
        alertManagerServerException = e;
        LOGGER.log(Level.WARNING, "Could not post alert to {0}. {1}", new Object[]{client.toString(), e.getMessage()});
      }
    }
    //If there is a response so throw response exception
    if (response == null && alertManagerResponseException != null) {
      throw alertManagerResponseException;
    }
    if (response == null && alertManagerServerException != null) {
      throw alertManagerServerException;
    }
    return response;
  }

  public List<AlertGroup> getAlertGroups(Boolean active, Boolean silenced, Boolean inhibited, Set<String> filters,
    String receiver) throws AlertManagerResponseException, AlertManagerServerException {
    List<AlertGroup> localAlertGroups = null;
    AlertManagerResponseException alertManagerResponseException = null;
    AlertManagerServerException alertManagerServerException = null;
    try {
      localAlertGroups = this.clientWrapper.getAlertGroups(active, silenced, inhibited, filters, receiver);
    } catch (AlertManagerResponseException e) {
      alertManagerResponseException = e;
      LOGGER.log(Level.WARNING, "Could not get Alert Groups from {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    } catch (AlertManagerServerException e) {
      alertManagerServerException = e;
      LOGGER.log(Level.WARNING, "Could not get Alert Groups from {0}. {1}",
        new Object[]{this.clientWrapper.toString(), e.getMessage()});
    }
    for (ClientWrapper client : this.peerClients) {
      try {
        if (localAlertGroups == null) {
          localAlertGroups = client.getAlertGroups(active, silenced, inhibited, filters, receiver);
        } else {
          localAlertGroups.addAll(client.getAlertGroups(active, silenced, inhibited, filters, receiver));
        }
      } catch (AlertManagerResponseException e) {
        alertManagerResponseException = e;
        LOGGER.log(Level.WARNING, "Could not get Alert Groups from {0}. {1}",
          new Object[]{client.toString(), e.getMessage()});
      } catch (AlertManagerServerException e) {
        alertManagerServerException = e;
        LOGGER.log(Level.WARNING, "Could not get Alert Groups from {0}. {1}",
          new Object[]{client.toString(), e.getMessage()});
      }
    }
    //If there is a response so throw response exception
    if (localAlertGroups == null && alertManagerResponseException != null) {
      throw alertManagerResponseException;
    }
    if (localAlertGroups == null && alertManagerServerException != null) {
      throw alertManagerServerException;
    }
    return localAlertGroups;
  }

  public static class Builder {
    private final Client client;
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
      List<URI> peers;
      if (Strings.isNullOrEmpty(this.serviceFQDN) && Strings.isNullOrEmpty(this.serviceDN)) {
        target = Settings.getAlertManagerAddress();
        peers = Settings.getAlertManagerPeers();
      } else if (Strings.isNullOrEmpty(this.serviceFQDN)) {
        target = Settings.getAlertManagerAddressByDN(this.serviceDN, this.isHttps);
        peers = Settings.getAlertManagerPeersByDN(this.serviceDN, this.isHttps);
      } else {
        target = Settings.getAlertManagerAddressByFQDN(this.serviceFQDN, this.isHttps);
        peers = Settings.getAlertManagerPeersByFQDN(this.serviceFQDN, this.isHttps);
      }
      return new AlertManagerClient(this.client, target, peers);
    }
  }
}
