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

import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.zookeeper.KeeperException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
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
      throws KafkaException, ProjectException, UserException, ServingException,
    InterruptedException, ExecutionException {

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
        // The user has selected a an already existing Kafka topic. Check that it matches the schema requirements
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

    return new TopicDTO(serving.getKafkaTopic().getTopicName());
  }

  private ProjectTopics setupKafkaTopic(Project project, ServingWrapper servingWrapper) throws
    KafkaException, UserException, ProjectException, InterruptedException, ExecutionException {

    try {
      // Check that the user is not trying to create a topic with  more replicas than brokers.
      if (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() != null &&
          (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() <= 0 ||
              servingWrapper.getKafkaTopicDTO().getNumOfReplicas() > settings.getBrokerEndpoints().size())) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_REPLICATION_ERROR, Level.FINE);

      } else if (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() == null) {
        // set default value
        servingWrapper.getKafkaTopicDTO().setNumOfReplicas(settings.getKafkaDefaultNumReplicas());
      }

    } catch (IOException | KeeperException | InterruptedException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.BROKER_METADATA_ERROR, Level.SEVERE,
          "", e.getMessage(), e);
    }

    // Check that the user is not trying to create a topic with negative partitions
    if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() != null
        && servingWrapper.getKafkaTopicDTO().getNumOfPartitions() <= 0) {

      throw new KafkaException(RESTCodes.KafkaErrorCode.BAD_NUM_PARTITION, Level.FINE, "less than 0");

    } else if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() == null) {
      // set default value
      servingWrapper.getKafkaTopicDTO().setNumOfPartitions(settings.getKafkaDefaultNumPartitions());
    }

    String servingTopicName = getServingTopicName(servingWrapper);

    TopicDTO topicDTO = new TopicDTO(servingTopicName, servingWrapper.getKafkaTopicDTO().getNumOfReplicas(),
        servingWrapper.getKafkaTopicDTO().getNumOfPartitions(), Settings.INFERENCE_SCHEMANAME,
      Settings.INFERENCE_SCHEMAVERSION);

    ProjectTopics pt = kafkaController.createTopicInProject(project, topicDTO);
  
    // Add the ACLs for this topic. By default all users should be able to do everything
    for (ProjectTeam projectTeam : project.getProjectTeamCollection()) {
      AclDTO aclDto = new AclDTO(project.getName(),
        projectTeam.getUser().getEmail(),
        "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
        Settings.KAFKA_ACL_WILDCARD);
      kafkaController.addAclsToTopic(topicDTO.getName(), project.getId(), aclDto);
    }

    return pt;
  }

  private ProjectTopics checkSchemaRequirements(Project project, ServingWrapper servingWrapper)
      throws KafkaException, ServingException {
    ProjectTopics topic =
      projectTopicsFacade.findTopicByNameAndProject(project, servingWrapper.getKafkaTopicDTO().getName())
        .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,
          "name: " + servingWrapper.getKafkaTopicDTO().getName()));

    if (!topic.getSubjects().getSubjectsPK().getSubject().equalsIgnoreCase(Settings.INFERENCE_SCHEMANAME)) {

      throw new ServingException(RESTCodes.ServingErrorCode.BAD_TOPIC, Level.INFO,
        Settings.INFERENCE_SCHEMANAME + " required");
    }

    return topic;
  }

  private String getServingTopicName(ServingWrapper servingWrapper) {
    return servingWrapper.getServing().getName() + "-inf" + RandomStringUtils.randomNumeric(4);
  }
}
