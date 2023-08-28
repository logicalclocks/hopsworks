/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.util;

import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.RandomStringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaServingHelper {
  
  @EJB
  private Settings settings;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;

  
  /**
   * Sets up the kafka topic for logging inference requests for models being served on Hopsworks. This kafka topic
   * for logging is setup automatically when updating or creating a model serving instance if the user have specified
   * that option in the configuration of the serving.
   *
   * @param project the project where the serving resides and where the kafka topic is to be created
   * @param servingWrapper the internal representation of the serving instance
   * @param newDbServing the new serving to save in the database (either a new insertion or an update)
   * @param oldDbServing the old serving in the database (in case of an update)
   * @throws KafkaException
   * @throws ProjectException
   * @throws UserException
   * @throws ServiceException
   * @throws ServingException
   */
  public void setupKafkaServingTopic(Project project, ServingWrapper servingWrapper,
                                     Serving newDbServing, Serving oldDbServing)
      throws KafkaException, ServingException {

    if (servingWrapper.getKafkaTopicDTO() != null &&
        servingWrapper.getKafkaTopicDTO().getName() != null &&
        servingWrapper.getKafkaTopicDTO().getName().equalsIgnoreCase("NONE")) {

      // The User has decided to not log the serving requests
      newDbServing.setKafkaTopic(null);

    } else if (servingWrapper.getKafkaTopicDTO() != null &&
        servingWrapper.getKafkaTopicDTO().getName() != null &&
        servingWrapper.getKafkaTopicDTO().getName().equalsIgnoreCase("CREATE")) {

      // The user is creating a new Kafka Topic
      ProjectTopics topic = setupKafkaTopic(project, servingWrapper);
      newDbServing.setKafkaTopic(topic);

    } else if (servingWrapper.getKafkaTopicDTO() != null &&
        servingWrapper.getKafkaTopicDTO().getName() != null &&
        !(servingWrapper.getKafkaTopicDTO().getName().equalsIgnoreCase("CREATE")
            || servingWrapper.getKafkaTopicDTO().getName().equalsIgnoreCase("NONE"))) {

      if (oldDbServing != null &&
          oldDbServing.getKafkaTopic() != null &&
          oldDbServing.getKafkaTopic().getTopicName()
              .equals(servingWrapper.getKafkaTopicDTO().getName())) {
        // This is an update and the topic name hasn't changed.
        newDbServing.setKafkaTopic(oldDbServing.getKafkaTopic());
      } else {
        // The user has selected a already existing Kafka topic. Check that it matches the schema requirements
        ProjectTopics topic = checkSchemaRequirements(project, servingWrapper);
        newDbServing.setKafkaTopic(topic);
      }
    }
  }
  
  /**
   * Creates the TopicDTO from the topic name of a serving
   *
   * @param serving the serving that is connected to the kafka topic
   * @return the topicDTO of the serving
   */
  public TopicDTO buildTopicDTO(Serving serving) {
    if (serving.getKafkaTopic() == null) {
      return null;
    }
    
    ProjectTopics topic = serving.getKafkaTopic();

    return new TopicDTO(topic.getTopicName(),
      topic.getSubjects().getSubject(),
      topic.getSubjects().getVersion(),
      topic.getSubjects().getSchema().getSchema());
  }

  private ProjectTopics setupKafkaTopic(Project project, ServingWrapper servingWrapper)
      throws KafkaException {
    if (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() == null) {
      // set default value
      servingWrapper.getKafkaTopicDTO().setNumOfReplicas(settings.getKafkaDefaultNumReplicas());
    }

    // Check that the user is not trying to create a topic with negative partitions
    if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() != null
        && servingWrapper.getKafkaTopicDTO().getNumOfPartitions() <= 0) {

      throw new KafkaException(RESTCodes.KafkaErrorCode.BAD_NUM_PARTITION, Level.FINE, "less than 0");

    } else if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() == null) {
      // set default value
      servingWrapper.getKafkaTopicDTO().setNumOfPartitions(settings.getKafkaDefaultNumPartitions());
    }

    // Select schema version according to serving mode
    Integer schemaVersion = Settings.INFERENCE_SCHEMAVERSION;
    if (servingWrapper.getServing().getServingTool() == ServingTool.KSERVE) {
      // Messages for topics with schema version 4 are produced by the inference logger sidecar in Kubernetes
      schemaVersion = 4;
    }
    
    String servingTopicName = getServingTopicName(servingWrapper);

    TopicDTO topicDTO = new TopicDTO(servingTopicName, servingWrapper.getKafkaTopicDTO().getNumOfReplicas(),
        servingWrapper.getKafkaTopicDTO().getNumOfPartitions(), Settings.INFERENCE_SCHEMANAME, schemaVersion);

    return kafkaController.createTopic(project, topicDTO);
  }

  private ProjectTopics checkSchemaRequirements(Project project, ServingWrapper servingWrapper)
      throws KafkaException, ServingException {
    ProjectTopics topic =
      projectTopicsFacade.findTopicByNameAndProject(project, servingWrapper.getKafkaTopicDTO().getName())
        .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,
          "name: " + servingWrapper.getKafkaTopicDTO().getName()));

    if (!topic.getSubjects().getSubject().equalsIgnoreCase(Settings.INFERENCE_SCHEMANAME)) {

      throw new ServingException(RESTCodes.ServingErrorCode.BAD_TOPIC, Level.INFO,
        Settings.INFERENCE_SCHEMANAME + " required");
    }

    return topic;
  }

  private String getServingTopicName(ServingWrapper servingWrapper) {
    return servingWrapper.getServing().getName() + "-inf" + RandomStringUtils.randomNumeric(4);
  }
}
