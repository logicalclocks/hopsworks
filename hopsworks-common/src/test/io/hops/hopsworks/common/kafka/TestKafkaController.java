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

package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.kafka.FeatureStoreKafkaConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SecurityProtocol;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestKafkaController {

  private KafkaController kafkaController;

  @Before
  public void setup() {
    kafkaController = Mockito.spy(new KafkaController());
    kafkaController.kafkaBrokers = Mockito.mock(KafkaBrokers.class);
    kafkaController.hopsKafkaAdminClient = Mockito.mock(HopsKafkaAdminClient.class);
    kafkaController.projectTopicsFacade = Mockito.mock(ProjectTopicsFacade.class);
    kafkaController.storageConnectorController = Mockito.mock(FeaturestoreStorageConnectorController.class);
  }

  @Test
  public void testCheckReplication() throws KafkaException {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());
    List<String> brokers = Collections.nCopies(10, "endpoint");
    Mockito.doReturn(brokers).when(kafkaController.kafkaBrokers).getBrokerEndpoints(Mockito.any());
    TopicDTO topicDTO = new TopicDTO();
    topicDTO.setNumOfReplicas(10);

    // Act
    kafkaController.checkReplication(topicDTO);

    // Assert
    Mockito.verify(kafkaController.kafkaBrokers, Mockito.times(1)).getBrokerEndpoints(Mockito.any());
  }

  @Test(expected = KafkaException.class)
  public void testCheckReplicationTooFewBrokers() throws KafkaException {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());
    List<String> brokers = Collections.nCopies(1, "endpoint");
    Mockito.doReturn(brokers).when(kafkaController.kafkaBrokers).getBrokerEndpoints(Mockito.any());
    TopicDTO topicDTO = new TopicDTO();
    topicDTO.setNumOfReplicas(10);

    // Act
    kafkaController.checkReplication(topicDTO);

    // Assert
  }

  @Test
  public void testRemoveKafkaTopicsInternalKafka() {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());

    // Act
    kafkaController.removeKafkaTopics(Mockito.mock(Project.class));

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(1)).deleteTopics(Mockito.any());
  }

  @Test
  public void testRemoveKafkaTopicsExternalKafka() {
    // Arrange
    Mockito.doReturn(true).when(kafkaController).externalKafka(Mockito.any());

    // Act
    kafkaController.removeKafkaTopics(Mockito.mock(Project.class));

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(0)).deleteTopics(Mockito.any());
  }

  @Test
  public void testRemoveTopicFromProjectInternalKafka() throws KafkaException {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());
    Mockito.doReturn(Optional.ofNullable(new ProjectTopics())).when(kafkaController.projectTopicsFacade)
        .findTopicByNameAndProject(Mockito.any(), Mockito.any());

    // Act
    kafkaController.removeTopicFromProject(Mockito.mock(Project.class), "");

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(1)).deleteTopics(Mockito.any());
  }

  @Test(expected = KafkaException.class)
  public void testRemoveTopicFromProjectInternalKafkaNoTopic() throws KafkaException {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());
    Mockito.doReturn(Optional.empty()).when(kafkaController.projectTopicsFacade)
        .findTopicByNameAndProject(Mockito.any(), Mockito.any());

    // Act
    kafkaController.removeTopicFromProject(Mockito.mock(Project.class), "");

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(1)).deleteTopics(Mockito.any());
  }

  @Test
  public void testRemoveTopicFromProjectExternalKafka() throws KafkaException {
    // Arrange
    Mockito.doReturn(true).when(kafkaController).externalKafka(Mockito.any());

    // Act
    kafkaController.removeTopicFromProject(Mockito.mock(Project.class), "");

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(0)).deleteTopics(Mockito.any());
  }

  @Test(expected = NullPointerException.class)
  public void testCreateTopicInternalKafka() throws KafkaException {
    // Arrange
    Mockito.doReturn(false).when(kafkaController).externalKafka(Mockito.any());

    // Act
    kafkaController.createTopic(Mockito.mock(Project.class), new TopicDTO());

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(1)).listTopics();
  }

  @Test
  public void testCreateTopicExternalKafka() throws KafkaException {
    // Arrange
    Mockito.doReturn(true).when(kafkaController).externalKafka(Mockito.any());

    // Act
    kafkaController.createTopic(Mockito.mock(Project.class), new TopicDTO());

    // Assert
    Mockito.verify(kafkaController.hopsKafkaAdminClient, Mockito.times(0)).listTopics();
  }
}
