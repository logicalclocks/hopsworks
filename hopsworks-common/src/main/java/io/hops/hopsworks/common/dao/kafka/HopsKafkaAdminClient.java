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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.kafka.FeatureStoreKafkaConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
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

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class HopsKafkaAdminClient {

  private static final Logger LOGGER = Logger.getLogger(HopsKafkaAdminClient.class.getName());

  @EJB
  protected ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  protected BaseHadoopClientsService baseHadoopService;
  @EJB
  protected DistributedFsService dfs;

  //region Properties
  public Properties getHopsworksKafkaProperties() {
    try {
      Properties props = new Properties();
      props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
              serviceDiscoveryController.constructServiceFQDNWithPort(
                      HopsworksService.KAFKA.getNameWithTag(KafkaTags.broker)));
      props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KafkaConst.KAFKA_SECURITY_PROTOCOL);
      props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, baseHadoopService.getSuperTrustStorePath());
      props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperTrustStorePassword());
      props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, baseHadoopService.getSuperKeystorePath());
      props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
      props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
      props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
              KafkaConst.KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM);
      return props;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to construct Kafka properties", e);
      return new Properties();
    }
  }

  protected Properties getProjectKafkaProperties(FeatureStoreKafkaConnectorDTO connector) {
    if (Boolean.FALSE.equals(connector.isExternalKafka())) {
      return getHopsworksKafkaProperties();
    } else {
      try {
        Properties props = new Properties();

        // set kafka storage connector options
        for (OptionDTO option : connector.getOptions()) {
          props.setProperty(option.getName(), option.getValue());
        }

        // set connection properties
        props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
            connector.getBootstrapServers());
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
            connector.getSecurityProtocol().name());

        // set ssl configs
        props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
            connector.getSslEndpointIdentificationAlgorithm());

        setPasswordProperty(props, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, connector);
        setPasswordProperty(props, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, connector);
        setPasswordProperty(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG, connector);

        setCertificates(props, connector);

        return props;
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to construct Kafka properties", e);
        return new Properties();
      }
    }
  }

  private void setPasswordProperty(Properties props, String key, FeatureStoreKafkaConnectorDTO connector) {
    String value;
    switch (key) {
      case SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG:
        value = connector.getSslTruststorePassword();
        break;
      case SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG:
        value = connector.getSslKeystorePassword();
        break;
      case SslConfigs.SSL_KEY_PASSWORD_CONFIG:
        value = connector.getSslKeyPassword();
        break;
      default:
        return;
    }

    if (!Strings.isNullOrEmpty(value)) {
      props.setProperty(key, value);
    }
  }

  private void setCertificates(Properties props, FeatureStoreKafkaConnectorDTO connector) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      setCertificateProperty(props, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, connector, dfso);
      setCertificateProperty(props, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, connector, dfso);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  private void setCertificateProperty(Properties props, String key, FeatureStoreKafkaConnectorDTO connector,
                                      DistributedFileSystemOps dfso)
      throws IOException {
    String pathPattern = "/tmp/kafka_sc_%s%s";
    String location;
    String localLocation;
    switch (key) {
      case SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG:
        location = connector.getSslTruststoreLocation();
        localLocation = String.format(pathPattern, connector.getId(), Settings.TRUSTSTORE_SUFFIX);
        break;
      case SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG:
        location = connector.getSslKeystoreLocation();
        localLocation = String.format(pathPattern, connector.getId(), Settings.KEYSTORE_SUFFIX);
        break;
      default:
        return;
    }

    if (!Strings.isNullOrEmpty(location)) {
      // copy file only if it is provided
      dfso.copyToLocal(location, localLocation);
      props.setProperty(key, localLocation);
    }
  }
  //endregion

  //region AdminClient
  public CreateTopicsResult createTopics(Collection<NewTopic> newTopics) {
    try (AdminClient adminClient = AdminClient.create(getHopsworksKafkaProperties())) {
      return adminClient.createTopics(newTopics);
    }
  }

  public DeleteTopicsResult deleteTopics(Collection<String> topics)  {
    try (AdminClient adminClient = AdminClient.create(getHopsworksKafkaProperties())) {
      return adminClient.deleteTopics(topics);
    }
  }

  public ListTopicsResult listTopics()  {
    try (AdminClient adminClient = AdminClient.create(getHopsworksKafkaProperties())) {
      return adminClient.listTopics();
    }
  }

  public DescribeTopicsResult describeTopics(FeatureStoreKafkaConnectorDTO connector, Collection<String> topics) {
    try (AdminClient adminClient = AdminClient.create(getProjectKafkaProperties(connector))) {
      return adminClient.describeTopics(topics);
    }
  }
  //endregion
}