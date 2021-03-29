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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ServiceDiscoveryController {
  
  public enum HopsworksService {
    LIVY("livy"),
    ZOOKEEPER_CLIENT("client.zookeeper"),
    SPARK_HISTORY_SERVER("sparkhistoryserver"),
    HTTPS_RESOURCEMANAGER("https.resourcemanager"),
    HIVE_SERVER_PLAIN("hiveserver2-plain.hive"),
    HIVE_SERVER_TLS("hiveserver2-tls.hive"),
    HIVE_METASTORE("metastore.hive"),
    RPC_NAMENODE("rpc.namenode"),
    SERVING_LOGSTASH("serving.logstash"),
    TF_SERVING_LOGSTASH("tfserving.logstash"),
    SKLEARN_SERVING_LOGSTASH("sklearnserving.logstash"),
    PYTHON_JOBS_LOGSTASH("pythonjobs.logstash"),
    HOPSWORKS_APP("hopsworks.glassfish"),
    JUPYTER_LOGSTASH("jupyter.logstash"),
    REGISTRY("registry"),
    CONSUL_SERVER("consul"),
    ONLINEFS_MYSQL("onlinefs.mysql"),
    RESOURCEMANAGER("resourcemanager"),
    PUSHGATEWAY("pushgateway.prometheus");

    private String name;
    HopsworksService(String name) {
      this.name = name;
    }
    
    public String getServiceName() {
      return this.name;
    }
  }

  private static final Logger LOG = Logger.getLogger(ServiceDiscoveryController.class.getName());
  private static final String CONSUL_SERVICE_TEMPLATE = "%s.service.%s";

  private final Map<Type, ServiceDiscoveryClient> clients = new HashMap<>(1);

  @EJB
  private Settings settings;

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
  public String constructServiceFQDN(HopsworksService service) {
    String serviceName = service.getServiceName();
    if (serviceName.endsWith(".")) {
      serviceName = serviceName.substring(0, serviceName.length() - 1);
    }
    return String.format(CONSUL_SERVICE_TEMPLATE, serviceName, settings.getServiceDiscoveryDomain());
  }

  @Lock(LockType.READ)
  public String constructServiceFQDNWithPort(HopsworksService hopsworksService) throws ServiceDiscoveryException {
    Service service = getAnyAddressOfServiceWithDNS(hopsworksService);
    return service.getName() + ":" + service.getPort();
  }

  @Lock(LockType.READ)
  public String constructServiceAddressWithPort(HopsworksService hopsworksService) throws ServiceDiscoveryException {
    Service service = getAnyAddressOfServiceWithDNS(hopsworksService);
    return service.getAddress() + ":" + service.getPort();
  }

  @Lock(LockType.READ)
  public Stream<Service> getService(Type resolverType, ServiceQuery serviceQuery)
      throws ServiceDiscoveryException {
    ServiceDiscoveryClient client = getClient(resolverType);
    return client.getService(serviceQuery);
  }
  
  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNS(HopsworksService serviceName) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceName), Collections.emptySet());
    Optional<Service> serviceOpt = getService(Type.DNS, serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }
  
  @Lock(LockType.READ)
  public Service getAnyAddressOfServiceWithDNSSRVOnly(HopsworksService serviceName) throws ServiceDiscoveryException {
    ServiceQuery serviceQuery = ServiceQuery.of(constructServiceFQDN(serviceName), Collections.emptySet());
    DnsResolver client = (DnsResolver) getClient(Type.DNS);
    Optional<Service> serviceOpt = client.getServiceSRVOnly(serviceQuery).findAny();
    return serviceOpt.orElseThrow(() -> new ServiceNotFoundException("Could not find service with: " + serviceQuery));
  }
  
  @Lock(LockType.READ)
  public String getConsulServerAddress() throws ServiceDiscoveryException {
    Service consulService =
        getAnyAddressOfServiceWithDNS(HopsworksService.CONSUL_SERVER);
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
