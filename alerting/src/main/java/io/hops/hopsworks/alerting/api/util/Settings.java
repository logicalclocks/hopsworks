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
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Settings {

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

  private static Optional<Service> getAlertManagerService(String serviceFQDN) throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = null;
    Optional<Service> services;
    try {
      client = new Builder(Type.DNS).build();
      String name = Strings.isNullOrEmpty(serviceFQDN)? DEFAULT_ALERTMANAGER_FQDN : serviceFQDN;
      services = client.getService(ServiceQuery.of(name, Collections.emptySet())).findFirst();
    } finally {
      if (client != null) {
        client.close();
      }
    }
    return services;
  }

  public static URI getAlertManagerAddress(String serviceFQDN, String scheme) throws ServiceDiscoveryException {
    serviceFQDN = Strings.isNullOrEmpty(serviceFQDN)? DEFAULT_ALERTMANAGER_FQDN : serviceFQDN;
    if (alertManagerAddresses.get(serviceFQDN) == null) {
      Optional<Service> optionalService = getAlertManagerService(serviceFQDN);
      Service service = optionalService.orElseThrow(() -> new ServiceDiscoveryException("Service not found."));
      UriBuilder uriBuilder = UriBuilder.fromPath("")
        .scheme(Strings.isNullOrEmpty(scheme)? "http" : scheme)
        .host(service.getName());
      if (service.getPort() != null && service.getPort() > 0) {
        uriBuilder.port(service.getPort());
      }
      URI alertManagerAddress = uriBuilder.build();
      alertManagerAddresses.put(serviceFQDN, alertManagerAddress);
    }
    return alertManagerAddresses.get(serviceFQDN);
  }

  public static URI getAlertManagerAddress() throws ServiceDiscoveryException {
    return getAlertManagerAddress(null, null);
  }

  public static URI getAlertManagerAddressByFQDN(String serviceFQDN, boolean isHttps) throws ServiceDiscoveryException {
    return getAlertManagerAddress(serviceFQDN, isHttps? "https" : null);
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

  public static void clearCache() {
    alertManagerAddresses.clear();
  }

}
