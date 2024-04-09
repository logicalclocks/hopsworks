/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class TestKafkaBrokers {

  private KafkaBrokers kafkaBrokers;
  private Set<String> brokerEndpoints;
  private Set<String> brokerEndpointsStrimzi;

  @Before
  public void setup() {
    kafkaBrokers = Mockito.spy(new KafkaBrokers());
    kafkaBrokers.hopsKafkaAdminClient = Mockito.mock(HopsKafkaAdminClient.class);

    brokerEndpoints = new HashSet<>(Arrays.asList(
      "INTERNAL://10.0.2.15:9091",
      "EXTERNAL://kafka.service.consul:9092",
      "INTERNAL://10.0.2.15:9093"));
    brokerEndpointsStrimzi = new HashSet<>(Arrays.asList(
      "CONTROLPLANE-9090://kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9090",
      "REPLICATION-9091://kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9091",
      "PLAIN-9092://kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9092",
      "INTERNAL-9093://kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9093",
      "EXTERNAL-9094://kafka.service.consul:9094",
      "INTERNAL-9095://kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9095"));
  }

  @Test
  public void testGetBrokerEndpointsInternal() {
    // Arrange
    Mockito.doReturn(brokerEndpoints).when(kafkaBrokers.hopsKafkaAdminClient).getBrokerEndpoints();
    kafkaBrokers.setBrokerEndpoints();

    // Act
    List<String> results = kafkaBrokers.getBrokerEndpoints(KafkaBrokers.BrokerProtocol.INTERNAL);

    // Assert
    Assert.assertEquals(Arrays.asList("10.0.2.15:9093", "10.0.2.15:9091"), results);
  }

  @Test
  public void testGetBrokerEndpointsInternalStrimzi() {
    // Arrange
    Mockito.doReturn(brokerEndpointsStrimzi).when(kafkaBrokers.hopsKafkaAdminClient).getBrokerEndpoints();
    kafkaBrokers.setBrokerEndpoints();

    // Act
    List<String> results = kafkaBrokers.getBrokerEndpoints(KafkaBrokers.BrokerProtocol.INTERNAL);

    // Assert
    Assert.assertEquals(Arrays.asList("kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9095",
      "kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9093"), results);
  }

  @Test
  public void testGetBrokerEndpointsExternal() {
    // Arrange
    Mockito.doReturn(brokerEndpoints).when(kafkaBrokers.hopsKafkaAdminClient).getBrokerEndpoints();
    kafkaBrokers.setBrokerEndpoints();

    // Act
    List<String> results = kafkaBrokers.getBrokerEndpoints(KafkaBrokers.BrokerProtocol.EXTERNAL);

    // Assert
    Assert.assertEquals(Arrays.asList("kafka.service.consul:9092"), results);
  }

  @Test
  public void testGetBrokerEndpointsExternalStrimzi() {
    // Arrange
    Mockito.doReturn(brokerEndpointsStrimzi).when(kafkaBrokers.hopsKafkaAdminClient).getBrokerEndpoints();
    kafkaBrokers.setBrokerEndpoints();

    // Act
    List<String> results = kafkaBrokers.getBrokerEndpoints(KafkaBrokers.BrokerProtocol.EXTERNAL);

    // Assert
    Assert.assertEquals(Arrays.asList("kafka.service.consul:9094"), results);
  }

  @Test
  public void testGetBrokerEndpointsString() {
    // Arrange
    Mockito.doReturn(brokerEndpointsStrimzi).when(kafkaBrokers.hopsKafkaAdminClient).getBrokerEndpoints();
    kafkaBrokers.setBrokerEndpoints();

    // Act
    String results = kafkaBrokers.getBrokerEndpointsString(KafkaBrokers.BrokerProtocol.INTERNAL);

    // Assert
    Assert.assertEquals("kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9095,"
      + "kafka-cluster-kafka-0.kafka-cluster-kafka-brokers.hopsworks.svc:9093", results);
  }
}
