/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.featurestore.storageconnectors.kafka.FeatureStoreKafkaConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SecurityProtocol;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.mockito.Mockito;

import java.util.Properties;

public class TestHopsKafkaAdminClient {

  private HopsKafkaAdminClient hopsKafkaAdminClient;

  @Before
  public void setup() {
    hopsKafkaAdminClient = Mockito.spy(new HopsKafkaAdminClient());
    hopsKafkaAdminClient.serviceDiscoveryController = Mockito.mock(ServiceDiscoveryController.class);
    hopsKafkaAdminClient.baseHadoopService = Mockito.mock(BaseHadoopClientsService.class);
    hopsKafkaAdminClient.dfs = Mockito.mock(DistributedFsService.class);
  }

  @Test
  public void testGetHopsworksKafkaProperties() throws ServiceDiscoveryException {
    // Arrange
    Properties propertiesExpected = new Properties();
    propertiesExpected.setProperty("ssl.endpoint.identification.algorithm", "");
    propertiesExpected.setProperty("bootstrap.servers", "testBrokers");
    propertiesExpected.setProperty("ssl.truststore.location", "testGetSuperTrustStorePath");
    propertiesExpected.setProperty("ssl.truststore.password", "testGetSuperTrustStorePassword");
    propertiesExpected.setProperty("ssl.keystore.location", "testGetSuperKeystorePath");
    propertiesExpected.setProperty("ssl.keystore.password", "testGetSuperKeystorePassword");
    propertiesExpected.setProperty("ssl.key.password", "testGetSuperKeystorePassword");
    propertiesExpected.setProperty("security.protocol", "SSL");

    Mockito.doReturn("testBrokers").when(hopsKafkaAdminClient.serviceDiscoveryController).constructServiceFQDNWithPort(Mockito.any());
    Mockito.doReturn("testGetSuperTrustStorePath").when(hopsKafkaAdminClient.baseHadoopService).getSuperTrustStorePath();
    Mockito.doReturn("testGetSuperTrustStorePassword").when(hopsKafkaAdminClient.baseHadoopService).getSuperTrustStorePassword();
    Mockito.doReturn("testGetSuperKeystorePath").when(hopsKafkaAdminClient.baseHadoopService).getSuperKeystorePath();
    Mockito.doReturn("testGetSuperKeystorePassword").when(hopsKafkaAdminClient.baseHadoopService).getSuperKeystorePassword();

    // Act
    Properties properties = hopsKafkaAdminClient.getHopsworksKafkaProperties();

    // Assert
    Assert.equals(propertiesExpected, properties);
  }

  @Test
  public void getProjectKafkaPropertiesInternal() throws ServiceDiscoveryException {
    // Arrange
    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO = new FeatureStoreKafkaConnectorDTO();
    kafkaConnectorDTO.setExternalKafka(Boolean.FALSE);

    Properties propertiesExpected = new Properties();
    propertiesExpected.setProperty("ssl.endpoint.identification.algorithm", "");
    propertiesExpected.setProperty("bootstrap.servers", "testBrokers");
    propertiesExpected.setProperty("ssl.truststore.location", "testGetSuperTrustStorePath");
    propertiesExpected.setProperty("ssl.truststore.password", "testGetSuperTrustStorePassword");
    propertiesExpected.setProperty("ssl.keystore.location", "testGetSuperKeystorePath");
    propertiesExpected.setProperty("ssl.keystore.password", "testGetSuperKeystorePassword");
    propertiesExpected.setProperty("ssl.key.password", "testGetSuperKeystorePassword");
    propertiesExpected.setProperty("security.protocol", "SSL");

    Mockito.doReturn("testBrokers").when(hopsKafkaAdminClient.serviceDiscoveryController).constructServiceFQDNWithPort(Mockito.any());
    Mockito.doReturn("testGetSuperTrustStorePath").when(hopsKafkaAdminClient.baseHadoopService).getSuperTrustStorePath();
    Mockito.doReturn("testGetSuperTrustStorePassword").when(hopsKafkaAdminClient.baseHadoopService).getSuperTrustStorePassword();
    Mockito.doReturn("testGetSuperKeystorePath").when(hopsKafkaAdminClient.baseHadoopService).getSuperKeystorePath();
    Mockito.doReturn("testGetSuperKeystorePassword").when(hopsKafkaAdminClient.baseHadoopService).getSuperKeystorePassword();

    // Act
    Properties properties = hopsKafkaAdminClient.getProjectKafkaProperties(kafkaConnectorDTO);

    // Assert
    Assert.equals(propertiesExpected, properties);
  }

  @Test
  public void getProjectKafkaPropertiesExternalMinimal() {
    // Arrange
    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO = new FeatureStoreKafkaConnectorDTO();
    kafkaConnectorDTO.setId(1);
    kafkaConnectorDTO.setName("kafka_connector");
    kafkaConnectorDTO.setDescription("Connector used for exchanging information within hopsworks");
    kafkaConnectorDTO.setFeaturestoreId(122);
    kafkaConnectorDTO.setStorageConnectorType(FeaturestoreConnectorType.KAFKA);
    kafkaConnectorDTO.setBootstrapServers("testBrokers");
    kafkaConnectorDTO.setSecurityProtocol(SecurityProtocol.SSL);
    kafkaConnectorDTO.setSslEndpointIdentificationAlgorithm("");
    kafkaConnectorDTO.setExternalKafka(Boolean.TRUE);

    Properties propertiesExpected = new Properties();
    propertiesExpected.setProperty("ssl.endpoint.identification.algorithm", "");
    propertiesExpected.setProperty("bootstrap.servers", "testBrokers");
    propertiesExpected.setProperty("security.protocol", "SSL");

    // Act
    Properties properties = hopsKafkaAdminClient.getProjectKafkaProperties(kafkaConnectorDTO);

    // Assert
    Assert.equals(propertiesExpected, properties);
  }

  @Test
  public void getProjectKafkaPropertiesExternalWithCertificates() {
    // Arrange
    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO = new FeatureStoreKafkaConnectorDTO();
    kafkaConnectorDTO.setId(1);
    kafkaConnectorDTO.setName("kafka_connector");
    kafkaConnectorDTO.setDescription("Connector used for exchanging information within hopsworks");
    kafkaConnectorDTO.setFeaturestoreId(122);
    kafkaConnectorDTO.setStorageConnectorType(FeaturestoreConnectorType.KAFKA);
    kafkaConnectorDTO.setBootstrapServers("testBrokers");
    kafkaConnectorDTO.setSslTruststoreLocation("sslTruststoreLocation");
    kafkaConnectorDTO.setSslTruststorePassword("sslTruststorePassword");
    kafkaConnectorDTO.setSslKeystoreLocation("sslKeystoreLocation");
    kafkaConnectorDTO.setSslKeystorePassword("sslKeystorePassword");
    kafkaConnectorDTO.setSslKeyPassword("sslKeyPassword");
    kafkaConnectorDTO.setSecurityProtocol(SecurityProtocol.SSL);
    kafkaConnectorDTO.setSslEndpointIdentificationAlgorithm("");
    kafkaConnectorDTO.setExternalKafka(Boolean.TRUE);

    Properties propertiesExpected = new Properties();
    propertiesExpected.setProperty("ssl.endpoint.identification.algorithm", "");
    propertiesExpected.setProperty("bootstrap.servers", "testBrokers");
    propertiesExpected.setProperty("security.protocol", "SSL");
    propertiesExpected.setProperty("ssl.truststore.location", "/tmp/kafka_sc_1__tstore.jks");
    propertiesExpected.setProperty("ssl.truststore.password", "sslTruststorePassword");
    propertiesExpected.setProperty("ssl.keystore.location", "/tmp/kafka_sc_1__kstore.jks");
    propertiesExpected.setProperty("ssl.keystore.password", "sslKeystorePassword");
    propertiesExpected.setProperty("ssl.key.password", "sslKeyPassword");

    Mockito.doReturn(Mockito.mock(DistributedFileSystemOps.class)).when(hopsKafkaAdminClient.dfs).getDfsOps();

    // Act
    Properties properties = hopsKafkaAdminClient.getProjectKafkaProperties(kafkaConnectorDTO);

    // Assert
    Assert.equals(propertiesExpected, properties);
  }
}
