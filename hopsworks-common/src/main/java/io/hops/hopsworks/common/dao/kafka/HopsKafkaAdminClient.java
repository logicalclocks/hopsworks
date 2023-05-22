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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.restutils.RESTCodes;
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
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class HopsKafkaAdminClient {
  
  private static final Logger LOG = Logger.getLogger(HopsKafkaAdminClient.class.getName());
  
  @EJB
  private BaseHadoopClientsService baseHadoopService;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private AdminClient initClient() throws ServiceDiscoveryException {
    Properties props = new Properties();
    props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        serviceDiscoveryController.constructServiceFQDNWithPort(
            (HopsworksService.KAFKA.getNameWithTag(KafkaTags.broker))));
    props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KafkaConst.KAFKA_SECURITY_PROTOCOL);
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, baseHadoopService.getSuperTrustStorePath());
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperTrustStorePassword());
    props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, baseHadoopService.getSuperKeystorePath());
    props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
    props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, baseHadoopService.getSuperKeystorePassword());
    props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
      KafkaConst.KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM);
    LOG.log(Level.FINE, "Will attempt to initialize current kafka client");
    return AdminClient.create(props);
  }
  
  public ListTopicsResult listTopics() throws KafkaException {
    try (AdminClient adminClient = initClient()) {
      return adminClient.listTopics();
    } catch (Exception e) {
      throw new KafkaException(
          RESTCodes.KafkaErrorCode.BROKER_METADATA_ERROR, Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }
  
  public CreateTopicsResult createTopics(Collection<NewTopic> newTopics) throws KafkaException {
    try (AdminClient adminClient = initClient()) {
      return adminClient.createTopics(newTopics);
    } catch (Exception e) {
      throw new KafkaException(
          RESTCodes.KafkaErrorCode.TOPIC_CREATION_FAILED, Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }
  
  public DeleteTopicsResult deleteTopics(Collection<String> topics) throws KafkaException {
    try (AdminClient adminClient = initClient()){
      return adminClient.deleteTopics(topics);
    } catch (Exception e) {
      throw new KafkaException(
          RESTCodes.KafkaErrorCode.TOPIC_DELETION_FAILED, Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }
  
  public DescribeTopicsResult describeTopics(Collection<String> topics) throws KafkaException {
    try (AdminClient adminClient = initClient()){
      return adminClient.describeTopics(topics);
    } catch (Exception e) {
      throw new KafkaException(
          RESTCodes.KafkaErrorCode.BROKER_METADATA_ERROR, Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }
}
