/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.hosts;

import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.resolvers.DnsResolver;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.multiregion.MultiRegionController;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.Utilities;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ServiceDiscoveryController {

  private static final Logger LOG = Logger.getLogger(ServiceDiscoveryController.class.getName());

  private final Map<Type, ServiceDiscoveryClient> clients = new HashMap<>(1);

  @EJB
  private Settings settings;
  @EJB
  private MultiRegionController multiRegionController;

  @PostConstruct
  public void init() {
    try {
      ServiceDiscoveryClient dnsClient = createClient(Type.DNS);
      clients.put(Type.DNS, dnsClient);
    } catch (ServiceDiscoveryException ex) {
      LOG.log(Level.SEVERE, "Failed to initialize Service Discovery client", ex);
    }
  }

  @PreDestroy
  public void tearDown() {
    for (Map.Entry<Type, ServiceDiscoveryClient> client : clients.entrySet()) {
      client.getValue().close();
    }
  }

  @Lock(LockType.READ)
  public String constructServiceFQDN(String serviceDomain) {
    if (multiRegionController.isEnabled()) {
      return Utilities.constructServiceFQDNWithRegion(serviceDomain,
          multiRegionController.getPrimaryRegionName(),
          settings.getServiceDiscoveryDomain());
    } else {
      return Utilities.constructServiceFQDN(serviceDomain, settings.getServiceDiscoveryDomain());
    }
  }

  @Lock(LockType.READ)
  public String constructServiceFQDN(String serviceDomain, String region) {
    return Utilities.constructServiceFQDNWithRegion(serviceDomain, region, settings.getServiceDiscoveryDomain());
  }

  @Lock(LockType.READ)
  public String constructServiceFQDNWithPort(String serviceDomain) throws ServiceDiscoveryException {
    Service service = getAnyAddressOfServiceWithDNS(serviceDomain);
    return service.getName() + ":" + service.getPort();
  }

  @Lock(LockType.READ)
  public String constructServiceAddressWithPort(String serviceDomain) throws ServiceDiscoveryException {
    Service service = getAnyAddressOfServiceWithDNS(serviceDomain);
    return service.getAddress() + ":" + service.getPort();
  }

  @Lock(LockType.READ)
  public String constructServiceAddressWithPort(String serviceDomain, String region) throws ServiceDiscoveryException {
    Service service = getAnyAddressOfServiceWithDNS(serviceDomain, region);
    return service.getAddress() + ":" + service.getPort();
  }

  @Lock(LockType.READ)
  public Stream<Service> getService(Type resolverType, ServiceQuery serviceQuery)
      throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = getClient(resolverType);
    return client.getService(serviceQuery);
  }
  
  @Lock(LockType.READ)
  public List<Service> getAddressesOfServiceWithDNS(String serviceDomain) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceDomain), Collections.emptySet());
    List<Service> services = getService(Type.DNS, serviceQuery).collect(Collectors.toList());
    if (services.isEmpty()) throw new ServiceNotFoundException("Could not find services with: " + serviceQuery);
    return services;
  }
  
  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNS(String serviceDomain) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceDomain), Collections.emptySet());
    Optional<Service> serviceOpt = getService(Type.DNS, serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }

  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNS(String serviceDomain, String region) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceDomain, region), Collections.emptySet());
    Optional<Service> serviceOpt = getService(Type.DNS, serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }
  
  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNSSRVOnly(String serviceDomain) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceDomain), Collections.emptySet());
    DnsResolver client = (DnsResolver) getClient(Type.DNS);
    Optional<Service> serviceOpt = client.getServiceSRVOnly(serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }

  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNSSRVOnlyWithRegion(String serviceDomain, String region)
      throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceDomain, region), Collections.emptySet());
    DnsResolver client = (DnsResolver) getClient(Type.DNS);
    Optional<Service> serviceOpt = client.getServiceSRVOnly(serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }
  
  @Lock(LockType.READ)
  public String getConsulServerAddress() throws ServiceDiscoveryException {
    Service consulService =
        getAnyAddressOfServiceWithDNS(HopsworksService.CONSUL.getName());
    return consulService.getAddress();
  }
  
  private ServiceDiscoveryClient getClient(Type type) throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = clients.get(type);
    if (client != null) {
      return client;
    }
    throw new ServiceDiscoveryException("Could not find initialized Service Discovery client of type " + type
            + " Was it initialized correctly?");
  }
  
  private ServiceDiscoveryClient createClient(Type type) throws ServiceDiscoveryException {
    switch (type) {
      case DNS:
        return new Builder(type).build();
      case HTTP:
      case CACHING:
        throw new NotImplementedException(type + " resolver is not implemented yet");
      default:
        throw new RuntimeException("Unknown Service Discovery resolver type: " + type);
        
    }
  }
}
