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

import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;

import org.apache.commons.lang3.StringUtils;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KafkaBrokers {

  @EJB
  protected HopsKafkaAdminClient hopsKafkaAdminClient;

  public enum BrokerProtocol {
    INTERNAL,
    EXTERNAL
  }

  private final Set<String> kafkaBrokers = new HashSet<>();

  @PostConstruct
  @Lock(LockType.WRITE)
  public void setBrokerEndpoints() {
    this.kafkaBrokers.clear();
    this.kafkaBrokers.addAll(hopsKafkaAdminClient.getBrokerEndpoints());
  }

  @Lock(LockType.READ)
  public List<String> getBrokerEndpoints(BrokerProtocol protocol) {
    return kafkaBrokers.stream()
        .filter(bi -> bi.startsWith(protocol.toString()))
        .map(bi -> bi.replaceAll(protocol + ".*://", ""))
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
}