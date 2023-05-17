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

import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.KafkaTags;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@DependsOn("ServiceDiscoveryController")
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class HopsKafkaAdminClient {
  
  private static final Logger LOG = Logger.getLogger(HopsKafkaAdminClient.class.getName());
  
  @EJB
  private BaseHadoopClientsService baseHadoopService;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private String brokerFQDN;
  private AdminClient adminClient;

  @PostConstruct
  private void init() {
    try {
      LOG.log(Level.FINE, "Initializing Kafka client");
      brokerFQDN = serviceDiscoveryController.constructServiceAddressWithPort(
          HopsworksService.KAFKA.getNameWithTag(KafkaTags.broker));
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
    props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerFQDN);
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
