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

package io.hops.hopsworks.common.kafka;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.ZooKeeperTags;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KafkaBrokers {
  private final static Logger LOGGER = Logger.getLogger(KafkaBrokers.class.getName());
  
  public static final String KAFKA_BROKER_PROTOCOL_INTERNAL = "INTERNAL";
  public static final String KAFKA_BROKER_PROTOCOL_EXTERNAL = "EXTERNAL";
  
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  
  private final Set<String> internalKafkaBrokers;
  
  public KafkaBrokers() {
    this.internalKafkaBrokers = new HashSet<>();
  }
  
  @PostConstruct
  private void init() {
    try {
      setInternalKafkaBrokers(getBrokerEndpoints(KAFKA_BROKER_PROTOCOL_INTERNAL));
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  public void setInternalKafkaBrokers(Set<String> internalKafkaBrokers) {
    this.internalKafkaBrokers.clear();
    this.internalKafkaBrokers.addAll(internalKafkaBrokers);
  }

  @Lock(LockType.READ)
  public String getKafkaBrokersString() {
    if (!internalKafkaBrokers.isEmpty()) {
      return StringUtils.join(internalKafkaBrokers, ",");
    }
    return null;
  }

  @Lock(LockType.READ)
  public Set<String> getBrokerEndpoints(String protocol) throws IOException, KeeperException, InterruptedException {
    try {
      String zkConnectionString = getZookeeperConnectionString();
      final ZooKeeper zk = new ZooKeeper(zkConnectionString, Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, watchedEvent -> {});
      try {
        return zk.getChildren("/brokers/ids", false).stream()
            .map(bi -> getBrokerInfo(zk, bi))
            .filter(StringUtils::isNoneEmpty)
            .map(bi -> bi.split(KafkaConst.DLIMITER))
            .flatMap(Arrays::stream)
            .filter(bi -> isValidBrokerInfo(bi, protocol))
            .collect(Collectors.toSet());
      } finally {
        zk.close();
      }
    } catch (ServiceDiscoveryException ex) {
      throw new IOException(ex);
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof KeeperException) {
        throw (KeeperException) ex.getCause();
      }
      if (ex.getCause() instanceof InterruptedException) {
        throw (InterruptedException) ex.getCause();
      }
      throw ex;
    }
  }
  
  public String getZookeeperConnectionString() throws ServiceDiscoveryException {
    return serviceDiscoveryController.getService(
      Type.DNS, ServiceQuery.of(
        serviceDiscoveryController.constructServiceFQDN(
            HopsworksService.ZOOKEEPER.getNameWithTag(ZooKeeperTags.client)),
        Collections.emptySet()))
      .map(zkServer -> zkServer.getAddress() + ":" + zkServer.getPort())
      .collect(Collectors.joining(","));
  }
  
  private String getBrokerInfo(ZooKeeper zk, String brokerId) {
    try {
      return new String(zk.getData("/brokers/ids/" + brokerId, false, null));
    } catch (KeeperException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Could not get Kafka broker information", ex);
      throw new RuntimeException(ex);
    }
  }
  
  private boolean isValidBrokerInfo(String brokerInfo, String protocol) {
    return brokerInfo.startsWith(protocol) && brokerInfo.contains(KafkaConst.SLASH_SEPARATOR);
  }
}
