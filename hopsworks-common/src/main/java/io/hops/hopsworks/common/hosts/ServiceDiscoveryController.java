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
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.NotImplementedException;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ServiceDiscoveryController {
  
  public enum HopsworksService {
    LIVY("livy"),
    ZOOKEEPER_CLIENT("client.zookeeper"),
    SPARK_HISTORY_SERVER("sparkhistoryserver"),
    HTTP_RESOURCEMANAGER("http.resourcemanager"),
    HIVE_SERVER_PLAIN("hiveserver2-plain.hive"),
    HIVE_SERVER_TLS("hiveserver2-tls.hive"),
    RPC_NAMENODE("rpc.namenode");
    
    private String name;
    HopsworksService(String name) {
      this.name = name;
    }
    
    public String getServiceName() {
      return this.name;
    }
  }
  
  private static final String CONSUL_SERVICE_TEMPLATE = "%s.service.%s";
  
  @EJB
  private Settings settings;
  
  @Lock(LockType.READ)
  public String constructServiceFQDN(HopsworksService service) {
    String serviceName = service.getServiceName();
    if (serviceName.endsWith(".")) {
      serviceName = serviceName.substring(0, serviceName.length() - 1);
    }
    return String.format(CONSUL_SERVICE_TEMPLATE, serviceName, settings.getServiceDiscoveryDomain());
  }
  
  @Lock(LockType.READ)
  public Stream<Service> getService(Type resolverType, ServiceQuery serviceQuery)
      throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = getClient(resolverType);
    try {
      return client.getService(serviceQuery);
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }
  
  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNS(HopsworksService serviceName) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceName), Collections.emptySet());
    Optional<Service> serviceOpt = getService(Type.DNS, serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }
  
  private ServiceDiscoveryClient getClient(Type type) throws ServiceDiscoveryException {
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
