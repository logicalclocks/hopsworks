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
package io.hops.hopsworks.alerting.api.util;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;

import javax.ws.rs.core.UriBuilder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Settings {
  private final static Logger LOGGER = Logger.getLogger(Settings.class.getName());
  public static final String MANAGEMENT_API_HEALTH = "/-/healthy";
  public static final String MANAGEMENT_API_READY = "/-/ready";
  public static final String MANAGEMENT_API_RELOAD = "/-/reload";

  public static final String ALERTS_API_STATUS = "/api/v2/status";
  public static final String ALERTS_API_RECEIVERS = "/api/v2/receivers";
  public static final String ALERTS_API_SILENCES = "/api/v2/silences";
  public static final String ALERTS_API_SILENCE = "/api/v2/silence";
  public static final String ALERTS_API_ALERTS = "/api/v2/alerts";
  public static final String ALERTS_API_ALERTS_GROUPS = "/api/v2/alerts/groups";

  public static final String DEFAULT_SERVICE_DISCOVERY_DOMAIN = "consul";
  public static final String DEFAULT_ALERTMANAGER_FQDN =
    "alertmanager.prometheus.service." + DEFAULT_SERVICE_DISCOVERY_DOMAIN;

  private static final ConcurrentHashMap<String, URI> alertManagerAddresses = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, List<URI>> alertManagerPeers = new ConcurrentHashMap<>();

  public static List<String> getMyIPAddresses() {
    List<String> ipAddresses = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
      while (en.hasMoreElements()) {
        NetworkInterface networkInterface = en.nextElement();
        Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
        while (enumIpAddr.hasMoreElements()) {
          ipAddresses.add(enumIpAddr.nextElement().getHostAddress());
        }
      }
    } catch (SocketException e) {
      LOGGER.log(Level.WARNING, "Failed to get ip address of node. {0}", e.getMessage());
    }
    if (ipAddresses.isEmpty()) {
      LOGGER.log(Level.WARNING, "Failed to get ip address of node. Alert manager can not ensure high availability.");
    }
    return ipAddresses;
  }

  private static List<Service> getAlertManagers(String serviceFQDN) throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = null;
    List<Service> services;
    try {
      client = new Builder(Type.DNS).build();
      String name = Strings.isNullOrEmpty(serviceFQDN) ? DEFAULT_ALERTMANAGER_FQDN : serviceFQDN;
      services = client.getService(ServiceQuery.of(name, Collections.emptySet()))
        .collect(Collectors.toList());
    } finally {
      if (client != null) {
        client.close();
      }
    }
    return services;
  }

  private static Optional<Service> getLocalAM(List<Service> services, List<String> ipAddresses) {
    Optional<Service> service;
    if (!ipAddresses.isEmpty() && services.size() > 1) {
      // should communicate with alert manager on the same node.
      service =
        services.stream().filter(s -> ipAddresses.contains(s.getAddress())).findAny();

    } else {
      service = Optional.of(services.get(0));
    }
    return service;
  }

  private static List<Service> getAMPeers(List<Service> services, List<String> ipAddresses ) {
    List<Service> peers;
    if (!ipAddresses.isEmpty() && services.size() > 1) {
      peers = services.stream().filter(s -> !ipAddresses.contains(s.getAddress())).collect(Collectors.toList());
    } else {
      peers = Collections.emptyList();
    }
    return peers;
  }

  private static URI getURI(String address, String scheme, Integer port) {
    UriBuilder uriBuilder = UriBuilder.fromPath("")
      .scheme(Strings.isNullOrEmpty(scheme) ? "http" : scheme)
      .host(address);
    if (port != null && port > 0) {
      uriBuilder.port(port);
    }
    LOGGER.log(Level.INFO, "");
    return uriBuilder.build();
  }

  public static URI getAlertManagerAddress(String serviceFQDN, String scheme) throws ServiceDiscoveryException {
    serviceFQDN = Strings.isNullOrEmpty(serviceFQDN) ? DEFAULT_ALERTMANAGER_FQDN : serviceFQDN;
    if (alertManagerAddresses.get(serviceFQDN) == null) {
      List<String> ipAddresses = getMyIPAddresses();
      List<Service> services = getAlertManagers(serviceFQDN);
      Optional<Service> optionalService = getLocalAM(services, ipAddresses);
      Service service = optionalService.orElseThrow(() -> new ServiceDiscoveryException("Service not found."));
      alertManagerAddresses.put(serviceFQDN, getURI(service.getAddress(), scheme, service.getPort()));
    }
    return alertManagerAddresses.get(serviceFQDN);
  }

  public static List<URI> getAlertManagerPeers(String serviceFQDN, String scheme) throws ServiceDiscoveryException {
    serviceFQDN = Strings.isNullOrEmpty(serviceFQDN) ? DEFAULT_ALERTMANAGER_FQDN : serviceFQDN;
    if (alertManagerPeers.get(serviceFQDN) == null) {
      List<String> ipAddresses = getMyIPAddresses();
      List<Service> services = getAlertManagers(serviceFQDN);
      List<Service> peers = getAMPeers(services, ipAddresses);
      if (peers != null && !peers.isEmpty()) {
        alertManagerPeers.put(serviceFQDN, peers.stream().map(s -> getURI(s.getAddress(), scheme, s.getPort())).collect(
          Collectors.toList()));
      } else {
        alertManagerPeers.put(serviceFQDN, Collections.emptyList());
      }
    }
    return alertManagerPeers.get(serviceFQDN);
  }

  public static URI getAlertManagerAddress() throws ServiceDiscoveryException {
    return getAlertManagerAddress(null, null);
  }

  public static URI getAlertManagerAddressByFQDN(String serviceFQDN, boolean isHttps)
      throws ServiceDiscoveryException {
    return getAlertManagerAddress(serviceFQDN, isHttps ? "https" : null);
  }

  public static URI getAlertManagerAddressByDN(String serviceDiscoveryDomain, boolean isHttps)
      throws ServiceDiscoveryException {
    String serviceFQDN = DEFAULT_ALERTMANAGER_FQDN;
    if (!Strings.isNullOrEmpty(serviceDiscoveryDomain) &&
      !DEFAULT_SERVICE_DISCOVERY_DOMAIN.equals(serviceDiscoveryDomain)) {
      serviceFQDN = DEFAULT_ALERTMANAGER_FQDN.replace(DEFAULT_SERVICE_DISCOVERY_DOMAIN, serviceDiscoveryDomain);
    }
    return getAlertManagerAddressByFQDN(serviceFQDN, isHttps);
  }

  public static List<URI> getAlertManagerPeers() throws ServiceDiscoveryException {
    return getAlertManagerPeers(null, null);
  }

  public static List<URI> getAlertManagerPeersByFQDN(String serviceFQDN, boolean isHttps)
    throws ServiceDiscoveryException {
    return getAlertManagerPeers(serviceFQDN, isHttps ? "https" : null);
  }

  public static List<URI> getAlertManagerPeersByDN(String serviceDiscoveryDomain, boolean isHttps)
    throws ServiceDiscoveryException {
    String serviceFQDN = DEFAULT_ALERTMANAGER_FQDN;
    if (!Strings.isNullOrEmpty(serviceDiscoveryDomain) &&
      !DEFAULT_SERVICE_DISCOVERY_DOMAIN.equals(serviceDiscoveryDomain)) {
      serviceFQDN = DEFAULT_ALERTMANAGER_FQDN.replace(DEFAULT_SERVICE_DISCOVERY_DOMAIN, serviceDiscoveryDomain);
    }
    return getAlertManagerPeersByFQDN(serviceFQDN, isHttps);
  }

  public static void clearCache() {
    alertManagerAddresses.clear();
  }

}
