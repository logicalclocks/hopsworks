/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.kafka.KafkaBrokers;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.time.Duration;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@DependsOn("KafkaBrokers")
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class HopsKafkaAdminClient {
  
  private static final Logger LOG = Logger.getLogger(HopsKafkaAdminClient.class.getName());
  
  @EJB
  private BaseHadoopClientsService baseHadoopService;
  @EJB
  private KafkaBrokers kafkaBrokers;
  
  private AdminClient adminClient;
  
  @PostConstruct
  private void init() {
    try {
      LOG.log(Level.FINE, "Initializing Kafka client");
      initClient();
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Kafka is currently unavailable. Will periodically retry to connect");
    }
  }
  
  @AccessTimeout(value = 5000)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void initClient() {
    if (adminClient != null) {
      try {
        LOG.log(Level.FINE, "Will attempt to close current kafka client");
        adminClient.close(Duration.ofSeconds(3));
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Could not close adminClient, will continue with initialization", e);
      }
    }
    Properties props = new Properties();
    Set<String> brokers = kafkaBrokers.getKafkaBrokers();
    //Keep only INTERNAL protocol brokers
    brokers.removeIf(seed -> seed.split(KafkaConst.COLON_SEPARATOR)[0]
      .equalsIgnoreCase(KafkaConst.KAFKA_BROKER_EXTERNAL_PROTOCOL));
    String brokerAddress = brokers.iterator().next().split("://")[1];
    props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
    props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KafkaConst.KAFKA_SECURITY_PROTOCOL);
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, baseHadoopService.getSuperTrustStorePath());
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperTrustStorePassword());
    props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, baseHadoopService.getSuperKeystorePath());
    props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
    props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
    props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
      KafkaConst.KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM);
    LOG.log(Level.FINE, "Will attempt to initialize current kafka client");
    adminClient = AdminClient.create(props);
  }
  
  public ListTopicsResult listTopics() {
    try {
      return adminClient.listTopics();
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Kafka cluster is unavailable", e);
      initClient();
      return adminClient.listTopics();
    }
  }
  
  public CreateTopicsResult createTopics(Collection<NewTopic> newTopics) {
    try {
      return adminClient.createTopics(newTopics);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Kafka cluster is unavailable", e);
      initClient();
      return adminClient.createTopics(newTopics);
    }
  }
  
  public DeleteTopicsResult deleteTopics(Collection<String> topics) {
    try {
      return adminClient.deleteTopics(topics);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Kafka cluster is unavailable", e);
      initClient();
      return adminClient.deleteTopics(topics);
    }
  }
  
  public DescribeTopicsResult describeTopics(Collection<String> topics) {
    try {
      return adminClient.describeTopics(topics);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Kafka cluster is unavailable", e);
      initClient();
      return adminClient.describeTopics(topics);
    }
  }
  
}
