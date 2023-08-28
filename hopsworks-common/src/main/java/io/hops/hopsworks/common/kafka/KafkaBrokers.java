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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KafkaBrokers {
  private final static Logger LOGGER = Logger.getLogger(KafkaBrokers.class.getName());

  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  public enum BrokerProtocol {
    INTERNAL,
    EXTERNAL
  }

  private final Set<String> kafkaBrokers = new HashSet<>();

  @PostConstruct
  @Lock(LockType.WRITE)
  public void setBrokerEndpoints() {
    try {
      this.kafkaBrokers.clear();
      this.kafkaBrokers.addAll(getBrokerEndpoints());
    } catch (IOException | KeeperException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  @Lock(LockType.READ)
  public List<String> getBrokerEndpoints(BrokerProtocol protocol) {
    return kafkaBrokers.stream()
        .filter(bi -> bi.startsWith(protocol.toString()))
        .map(bi -> bi.replaceAll(protocol + "://", ""))
        .collect(Collectors.toList());
  }

  @Lock(LockType.READ)
  public String getBrokerEndpointsString(BrokerProtocol protocol) {
    List<String> kafkaProtocolBrokers = getBrokerEndpoints(protocol);
    if (!kafkaProtocolBrokers.isEmpty()) {
      return StringUtils.join(kafkaProtocolBrokers, ",");
    }
    return null;
  }

  private Set<String> getBrokerEndpoints()
      throws IOException, KeeperException, InterruptedException {
    try {
      String zkConnectionString = getZookeeperConnectionString();
      try (ZooKeeper zk = new ZooKeeper(zkConnectionString, Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, watchedEvent -> {
      })) {
        return zk.getChildren("/brokers/ids", false).stream()
            .map(bi -> getBrokerInfo(zk, bi))
            .filter(StringUtils::isNoneEmpty)
            .map(bi -> bi.split(KafkaConst.DLIMITER))
            .flatMap(Arrays::stream)
            .filter(this::isValidBrokerInfo)
            .collect(Collectors.toSet());
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

  private String getZookeeperConnectionString() throws ServiceDiscoveryException {
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

  private boolean isValidBrokerInfo(String brokerInfo) {
    String[] brokerProtocolNames = Arrays
        .stream(BrokerProtocol.values())
        .map(Enum::name)
        .toArray(String[]::new);
    return StringUtils.startsWithAny(brokerInfo, brokerProtocolNames)
        && brokerInfo.contains(KafkaConst.SLASH_SEPARATOR);
  }
}