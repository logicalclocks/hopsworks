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
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KafkaBrokers {
  private final static Logger LOGGER = Logger.getLogger(KafkaBrokers.class.getName());
  
  private static final String KAFKA_BROKER_PROTOCOL = "INTERNAL";
  
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  
  private final Set<String> kafkaBrokers;
  
  public KafkaBrokers() {
    this.kafkaBrokers = new HashSet<>();
  }
  
  @PostConstruct
  private void init() {
    try {
      setKafkaBrokers(getBrokerEndpoints());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  public void setKafkaBrokers(Set<String> kafkaBrokers) {
    this.kafkaBrokers.clear();
    this.kafkaBrokers.addAll(kafkaBrokers);
  }

  @Lock(LockType.READ)
  public Set<String> getKafkaBrokers() {
    return this.kafkaBrokers;
  }

  @Lock(LockType.READ)
  public String getKafkaBrokersString() {
    if (!kafkaBrokers.isEmpty()) {
      return StringUtils.join(kafkaBrokers, ",");
    }
    return null;
  }

  @Lock(LockType.READ)
  public Optional<String> getAnyKafkaBroker() {
    return kafkaBrokers.stream()
        .filter(StringUtils::isNoneEmpty)
        .findAny();
  }
  
  @Lock(LockType.READ)
  public Set<String> getBrokerEndpoints() throws IOException, KeeperException, InterruptedException {
    try {
      Service zkService = serviceDiscoveryController
          .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.ZOOKEEPER_CLIENT);
      String zkConnectionString = zkService.getAddress() + ":" + zkService.getPort();
      final ZooKeeper zk = new ZooKeeper(zkConnectionString, Settings.ZOOKEEPER_SESSION_TIMEOUT_MS,
          new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
              // NOOP
            }
          });
      try {
        return zk.getChildren("/brokers/ids", false).stream()
            .map(bi -> getBrokerInfo(zk, bi))
            .filter(StringUtils::isNoneEmpty)
            .map(bi -> bi.split(KafkaConst.DLIMITER))
            .flatMap(Arrays::stream)
            .filter(this::isValidBrokerInfo)
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
  
  private String getBrokerInfo(ZooKeeper zk, String brokerId) {
    try {
      return new String(zk.getData("/brokers/ids/" + brokerId, false, null));
    } catch (KeeperException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Could not get Kafka broker information", ex);
      throw new RuntimeException(ex);
    }
  }
  
  private boolean isValidBrokerInfo(String brokerInfo) {
    return brokerInfo.startsWith(KAFKA_BROKER_PROTOCOL) && brokerInfo.contains(KafkaConst.SLASH_SEPARATOR);
  }
}
