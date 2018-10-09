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

package io.hops.hopsworks.common.serving;

import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.SchemaTopics;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.serving.tf.TfServingException;
import io.hops.hopsworks.common.serving.tf.TfServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang.RandomStringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KafkaServingHelper {

  private final static String SCHEMANAME = "inferenceschema";
  private final static int SCHEMAVERSION = 1;

  private final static Logger LOGGER = Logger.getLogger(KafkaServingHelper.class.toString());

  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private Settings settings;

  private SchemaTopics schemaTopics = null;

  @PostConstruct
  public void init() {
    schemaTopics = kafkaFacade.getSchema(SCHEMANAME, SCHEMAVERSION);
  }

  public void setupKafkaServingTopic(Project project, TfServingWrapper servingWrapper,
                                     TfServing newDbServing, TfServing oldDbServing) throws TfServingException {

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
        // This is an update and the topic name hasn't changed. skip
        return;
      }

      // The user has selected a an already existing Kafka topic. Check that it matches the schema requirements
      ProjectTopics topic = checkSchemaRequirements(project, servingWrapper);
      newDbServing.setKafkaTopic(topic);
    }

    return;
  }

  public TopicDTO buildTopicDTO(TfServing serving, Users user) throws TfServingException {
    if (serving.getKafkaTopic() == null) {
      return null;
    }

    List<PartitionDetailsDTO> topicPartitionsDetails;
    try {
      topicPartitionsDetails = kafkaFacade.getTopicDetailsfromKafkaCluster(serving.getProject(), user,
          serving.getKafkaTopic().getTopicName());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not retrieve the TopicDTO", e);
      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAGETINFOERROR);
    }

    return new TopicDTO(serving.getKafkaTopic().getTopicName(),
        topicPartitionsDetails.get(0).getReplicas().size(),
        topicPartitionsDetails.size(),
        serving.getKafkaTopic().getSchemaTopics().getSchemaTopicsPK().getName(),
        serving.getKafkaTopic().getSchemaTopics().getSchemaTopicsPK().getVersion());
  }

  private ProjectTopics setupKafkaTopic(Project project, TfServingWrapper servingWrapper) throws TfServingException {

    try {
      // Check that the user is not trying to create a topic with  more replicas than brokers.
      if (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() != null &&
          (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() <= 0 ||
              servingWrapper.getKafkaTopicDTO().getNumOfReplicas() > settings.getBrokerEndpoints().size())) {
        throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR);

      } else if (servingWrapper.getKafkaTopicDTO().getNumOfReplicas() == null) {
        // set default value
        servingWrapper.getKafkaTopicDTO().setNumOfReplicas(settings.getKafkaDefaultNumReplicas());
      }

    } catch (AppException a) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR);
    }

    // Check that the user is not trying to create a topic with negative partitions
    if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() != null
        && servingWrapper.getKafkaTopicDTO().getNumOfPartitions() <= 0) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR);
    } else if (servingWrapper.getKafkaTopicDTO().getNumOfPartitions() == null) {
      // set default value
      servingWrapper.getKafkaTopicDTO().setNumOfPartitions(settings.getKafkaDefaultNumPartitions());
    }


    String servingTopicName = getServingTopicName(servingWrapper);

    TopicDTO topicDTO = new TopicDTO(servingTopicName, servingWrapper.getKafkaTopicDTO().getNumOfReplicas(),
        servingWrapper.getKafkaTopicDTO().getNumOfPartitions(), SCHEMANAME, SCHEMAVERSION);

    ProjectTopics pt = null;
    try {
      pt = kafkaFacade.createTopicInProject(project, topicDTO);

      // Add the ACLs for this topic. By default all users should be able to do everything
      AclDTO aclDto = new AclDTO(project.getName(),
            Settings.KAFKA_ACL_WILDCARD,
            "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
            Settings.KAFKA_ACL_WILDCARD);

      kafkaFacade.addAclsToTopic(topicDTO.getName(), project.getId(), aclDto);
    } catch (AppException ae) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR);
    }

    return pt;
  }

  private ProjectTopics checkSchemaRequirements(Project project, TfServingWrapper servingWrapper)
      throws TfServingException {
    ProjectTopics topic = null;
    try {
      topic = kafkaFacade.findTopicByNameAndProject(project, servingWrapper.getKafkaTopicDTO().getName());
    } catch (NoResultException e) {
      // The requested topic does not exists.
      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR);
    }

    if (!(topic.getSchemaTopics().getSchemaTopicsPK().getName().equalsIgnoreCase(SCHEMANAME) &&
        topic.getSchemaTopics().getSchemaTopicsPK().getVersion() == SCHEMAVERSION)) {

      throw new TfServingException(TfServingException.TfServingExceptionErrors.KAFKAERROR,
          SCHEMANAME + " required. Version: " + String.valueOf(SCHEMAVERSION));
    }

    return topic;
  }

  private String getServingTopicName(TfServingWrapper servingWrapper) {
    return servingWrapper.getTfServing().getModelName() + "-inf" + RandomStringUtils.randomNumeric(4);
  }
}
