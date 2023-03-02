/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDefaultValueDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.kafka.schemas.Subjects;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.zookeeper.KeeperException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KafkaController {

  private final static Logger LOGGER = Logger.getLogger(KafkaController.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  @EJB
  private HopsKafkaAdminClient hopsKafkaAdminClient;
  @EJB
  private SubjectsFacade subjectsFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private KafkaBrokers kafkaBrokers;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;

  
  public void createTopic(Project project, TopicDTO topicDto) throws KafkaException {
    
    if (topicDto == null) {
      throw new IllegalArgumentException("topicDto was not provided.");
    }
    
    String topicName = topicDto.getName();
    
    if (projectTopicsFacade.findTopicByName(topicDto.getName()).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS, Level.FINE, "topic name: " + topicName);
    }
    
    if (projectTopicsFacade.findTopicsByProject(project).size() > project.getKafkaMaxNumTopics()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_LIMIT_REACHED, Level.FINE,
        "topic name: " + topicName + ", project: " + project.getName());
    }
    
    //check if the replication factor is not greater than the
    //number of running brokers
    try {
      Set<String> brokerEndpoints = kafkaBrokers.getBrokerEndpoints();
      if (brokerEndpoints.size() < topicDto.getNumOfReplicas()) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_REPLICATION_ERROR, Level.FINE,
          "maximum: " + brokerEndpoints.size());
      }
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
    
    createTopicInProject(project, topicDto);
  }
  
  public void removeTopicFromProject(Project project, String topicName) throws KafkaException {
    
    ProjectTopics pt = projectTopicsFacade.findTopicByNameAndProject(project, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName));
    
    //remove from database
    projectTopicsFacade.remove(pt);
    /*
     * What is the possibility of the program failing below? The topic is
     * removed from
     * db, but not yet from zk. *
     * Possibilities:
     * 1. ZkClient is unable to establish a connection, maybe due to timeouts.
     * 2. In case delete.topic.enable is not set to true in the Kafka server
     * configuration, delete topic marks a topic for deletion. Subsequent
     * topic (with the same name) create operation fails.
     */
    //remove from zookeeper
    hopsKafkaAdminClient.deleteTopics(Collections.singleton(pt.getTopicName()));
  }
  
  public boolean projectTopicExists(Project project, String topicName) {
    return projectTopicsFacade.findTopicByNameAndProject(project, topicName).isPresent();
  }

  public List<TopicDTO> findTopicsByProject(Project project) {
    List<ProjectTopics> ptList = projectTopicsFacade.findTopicsByProject(project);
  
    List<TopicDTO> topics = new ArrayList<>();
    for (ProjectTopics pt : ptList) {
      topics.add(
          new TopicDTO(pt.getTopicName(),
              pt.getSubjects().getSubject(),
              pt.getSubjects().getVersion(),
              false));
    }
    return topics;
  }

  /**
   * Get all Topics for the given project (doesn't include shared).
   *
   * @param project
   * @return
   */
  public List<TopicDTO> findAllTopicsByProject(Project project) {
    return findTopicsByProject(project);
  }
  
  public ProjectTopics createTopicInProject(Project project, TopicDTO topicDto) throws KafkaException {
    
    Subjects schema =
      subjectsFacade.findSubjectByNameAndVersion(project, topicDto.getSchemaName(), topicDto.getSchemaVersion())
        .orElseThrow(() ->
          new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_NOT_FOUND, Level.FINE, "topic: " + topicDto.getName()));
    
    // create the topic in kafka
    try {
      if (createTopicInKafka(topicDto).get(3000, TimeUnit.MILLISECONDS) == null) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS_IN_ZOOKEEPER, Level.INFO,
          "topic name: " + topicDto.getName());
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new KafkaException(
        RESTCodes.KafkaErrorCode.TOPIC_CREATION_FAILED, Level.WARNING, "Topic name: " + topicDto.getName(),
        e.getMessage(), e);
    }
  
    /*
     * What is the possibility of the program failing here? The topic is created
     * on
     * zookeeper, but not persisted onto db. User cannot access the topic,
     * cannot
     * create a topic of the same name. In such scenario, the zk timer should
     * remove the topic from zk.
     *
     * One possibility is: schema has a global name space, it is not project
     * specific.
     * While the schema is selected by this topic, it could be deleted by
     * another
     * user. Hence, the above schema query will be empty.
     */
    ProjectTopics pt = new ProjectTopics(topicDto.getName(), topicDto.getNumOfPartitions(),
        topicDto.getNumOfReplicas(), project, schema);
    
    projectTopicsFacade.save(pt);
    
    return pt;
  }
  
  private KafkaFuture<CreateTopicsResult> createTopicInKafka(TopicDTO topicDTO) {
    return hopsKafkaAdminClient.listTopics().names().thenApply(
      set -> {
        if (set.contains(topicDTO.getName())) {
          return null;
        } else {
          NewTopic newTopic =
            new NewTopic(topicDTO.getName(), topicDTO.getNumOfPartitions(), topicDTO.getNumOfReplicas().shortValue());
          try {
            return hopsKafkaAdminClient.createTopics(Collections.singleton(newTopic));
          } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
          }
        }
      });
  }
  
  private KafkaFuture<List<PartitionDetailsDTO>> getTopicDetailsFromKafkaCluster(String topicName) {
    return hopsKafkaAdminClient.describeTopics(Collections.singleton(topicName))
      .all()
      .thenApply((map) -> map.getOrDefault(topicName, null))
      .thenApply((td) -> {
        if (td != null) {
          List<PartitionDetailsDTO> partitionDetails = new ArrayList<>();
          List<TopicPartitionInfo> partitions = td.partitions();
          for (TopicPartitionInfo partition : partitions) {
            int id = partition.partition();
            List<String> replicas = partition.replicas()
              .stream()
              .map(Node::host)
              .collect(Collectors.toList());
            List<String> inSyncReplicas = partition.isr()
              .stream()
              .map(Node::host)
              .collect(Collectors.toList());
            partitionDetails.add(new PartitionDetailsDTO(id, partition.leader().host(), replicas, inSyncReplicas));
          }
          partitionDetails.sort(Comparator.comparing(PartitionDetailsDTO::getId));
          return partitionDetails;
        } else {
          return Collections.emptyList();
        }
      });
  }
  
  public KafkaFuture<List<PartitionDetailsDTO>> getTopicDetails(Project project, String topicName) throws
    KafkaException {
  
    projectTopicsFacade.findTopicByNameAndProject(project, topicName).orElseThrow(() ->
      new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName)
    );
  
    return getTopicDetailsFromKafkaCluster(topicName);
  }
  
  public TopicDefaultValueDTO topicDefaultValues() throws KafkaException {
    try {
      Set<String> brokers = kafkaBrokers.getBrokerEndpoints();
      return new TopicDefaultValueDTO(
        settings.getKafkaDefaultNumReplicas(),
        settings.getKafkaDefaultNumPartitions(),
        brokers.size());
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "", ex.getMessage(), ex);
    }
  }

  public void updateTopicSchemaVersion(Project project, String topicName, Integer schemaVersion) throws KafkaException {
    ProjectTopics pt = projectTopicsFacade.findTopicByNameAndProject(project, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,  "topic: " + topicName));
    
    String schemaName = pt.getSubjects().getSubject();
  
    Subjects st = subjectsFacade.findSubjectByNameAndVersion(project, schemaName, schemaVersion)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_VERSION_NOT_FOUND, Level.FINE,
        "schema: " + schemaName + ", version: " + schemaVersion));
    
    projectTopicsFacade.updateTopicSchemaVersion(pt, st);
  }
  
  public SubjectDTO getSubjectForTopic(Project project, String topic) throws KafkaException {
    Optional<ProjectTopics> pt = projectTopicsFacade.findTopicByNameAndProject(project, topic);
    if (!pt.isPresent()) {
      List<DatasetSharedWith> datasetSharedWithList = datasetSharedWithFacade.findByProject(project);
      pt = datasetSharedWithList.stream().map(datasetSharedWith ->
              projectTopicsFacade.findTopicByNameAndProject(datasetSharedWith.getDataset().getProject(), topic))
          .findFirst()
          .orElseThrow(() -> new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_SHARED, Level.FINE,
              "topic: " + topic + ", project: " + project.getName()));
    }
    if (!pt.isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,
              "project=" + project.getName() + ", topic=" + topic);
    }
    return new SubjectDTO(pt.get().getSubjects());
  }
  
  public void updateTopicSubjectVersion(Project project, String topic, String subject, Integer version)
    throws KafkaException, SchemaException {
    ProjectTopics pt = projectTopicsFacade.findTopicByNameAndProject(project, topic)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,  "topic: " + topic));
  
    String topicSubject = pt.getSubjects().getSubject();
  
    Subjects st = subjectsFacade.findSubjectByNameAndVersion(project, subject, version)
      .orElseThrow(() ->
        new SchemaException(RESTCodes.SchemaRegistryErrorCode.VERSION_NOT_FOUND, Level.FINE,
          "schema: " + topicSubject + ", version: " + version));
  
    projectTopicsFacade.updateTopicSchemaVersion(pt, st);
  }
}


