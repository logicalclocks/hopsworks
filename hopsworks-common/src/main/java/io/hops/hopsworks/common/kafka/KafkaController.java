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

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.SchemaTopics;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopics;
import io.hops.hopsworks.common.dao.kafka.TopicAcls;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDefaultValueDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.zookeeper.KeeperException;
import org.elasticsearch.common.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class KafkaController {

  private final static Logger LOGGER = Logger.getLogger(KafkaController.class.getName());

  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private CertsFacade userCerts;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  
  public void createTopic(Project project, TopicDTO topicDto) throws KafkaException,
    ProjectException, UserException, InterruptedException, ExecutionException {
    
    if (topicDto == null) {
      throw new IllegalArgumentException("topicDto was not provided.");
    }
    
    String topicName = topicDto.getName();
    
    if (kafkaFacade.findTopicByName(topicDto.getName()).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS, Level.FINE, "topic name: " + topicName);
    }
    
    if (kafkaFacade.findTopicsByProject(project).size() > project.getKafkaMaxNumTopics()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_LIMIT_REACHED, Level.FINE,
        "topic name: " + topicName + ", project: " + project.getName());
    }
    
    //check if the replication factor is not greater than the
    //number of running brokers
    try {
      Set<String> brokerEndpoints = settings.getBrokerEndpoints();
      if (brokerEndpoints.size() < topicDto.getNumOfReplicas()) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_REPLICATION_ERROR, Level.FINE,
          "maximum: " + brokerEndpoints.size());
      }
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
    
    kafkaFacade.createTopicInProject(project, topicDto);
  
    //By default, all members of the project are granted full permissions
    //on the topic
    addFullPermissionAclsToTopic(project.getName(), topicDto.getName(), project.getId());
  }
  
  public void removeTopicFromProject(Project project, String topicName) throws KafkaException {
    
    ProjectTopics pt = kafkaFacade.findTopicByNameAndProject(project, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName));
    
    kafkaFacade.removeTopicFromProject(pt);
  }
  
  public List<TopicDTO> findTopicsByProject(Project project) {
    List<ProjectTopics> ptList = kafkaFacade.findTopicsByProject(project);
  
    List<TopicDTO> topics = new ArrayList<>();
    if(ptList != null && !ptList.isEmpty()) {
      for (ProjectTopics pt : ptList) {
        topics.add(new TopicDTO(pt.getTopicName(),
          pt.getSchemaTopics().getSchemaTopicsPK().getName(),
          pt.getSchemaTopics().getSchemaTopicsPK().getVersion(),
          false));
      }
    }
    return topics;
  }
  
  private KafkaFuture<List<PartitionDetailsDTO>> getTopicDetailsFromKafkaCluster(String topicName) {
    return kafkaFacade.getTopicsFromKafkaCluster(Collections.singleton(topicName))
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
  
  public KafkaFuture<List<PartitionDetailsDTO>> getTopicDetails (Project project, String topicName) throws
    KafkaException {
    
    kafkaFacade.findTopicByNameAndProject(project, topicName).orElseThrow(() ->
      new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName)
    );
  
    return getTopicDetailsFromKafkaCluster(topicName);
  }
  
  public void shareTopicWithProject(Project project, String topicName, Integer destProjectId) throws
    ProjectException, KafkaException, UserException {
    
    if (project.getId().equals(destProjectId)) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.DESTINATION_PROJECT_IS_TOPIC_OWNER, Level.FINE);
    }
    
    if (!kafkaFacade.findTopicByNameAndProject(project, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.PROJECT_IS_NOT_THE_OWNER_OF_THE_TOPIC, Level.FINE);
    }
    
    if (!kafkaFacade.findTopicByNameAndProject(project, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName);
    }
    
    if (!Optional.ofNullable(projectFacade.find(destProjectId)).isPresent()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
        "Could not find project: " + destProjectId);
    }
    
    if (kafkaFacade.findSharedTopicByProjectAndTopic(destProjectId, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_SHARED, Level.FINE, "topic: " + topicName);
    }
    
    kafkaFacade.shareTopic(project, topicName, destProjectId);
    //By default, all members of the project are granted full permissions on the topic
    addFullPermissionAclsToTopic(destProjectId, topicName, project.getId());
  }
  
  private void addFullPermissionAclsToTopic(Integer aclProjectId, String topicName, Integer projectId)
    throws ProjectException, KafkaException, UserException {
    Project p = Optional.ofNullable(projectFacade.find(aclProjectId)).orElseThrow(() ->
      new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
      "Could not find project: " + aclProjectId));
    
    addFullPermissionAclsToTopic(p.getName(), topicName, projectId);
  }
  
  private void addFullPermissionAclsToTopic(String aclProjectName, String topicName, Integer projectId)
    throws ProjectException, KafkaException, UserException {
    AclDTO aclDto = new AclDTO(aclProjectName,
      Settings.KAFKA_ACL_WILDCARD,
      "allow",
      Settings.KAFKA_ACL_WILDCARD,
      Settings.KAFKA_ACL_WILDCARD,
      Settings.KAFKA_ACL_WILDCARD);
    addAclsToTopic(topicName, projectId, aclDto);
  }
  
  public void addAclsToTopic(String topicName, Integer projectId, AclDTO dto) throws ProjectException,
    KafkaException, UserException {
    addAclsToTopic(topicName, projectId,
      dto.getProjectName(),
      dto.getUserEmail(), dto.getPermissionType(),
      dto.getOperationType(), dto.getHost(), dto.getRole());
  }
  
  private void addAclsToTopic(String topicName, Integer projectId,
    String selectedProjectName, String userEmail, String permissionType,
    String operationType, String host, String role) throws ProjectException, KafkaException, UserException {
  
    if(Strings.isNullOrEmpty(topicName) || projectId == null || projectId < 0 || userEmail == null){
      throw new IllegalArgumentException("Topic, userEmail and projectId must be provided. ProjectId must be a " +
        "non-negative number");
    }
  
    //get the project id
    Project topicOwnerProject = Optional.ofNullable(projectFacade.find(projectId)).orElseThrow(() ->
      new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId));
    Project project;
  
    if (!topicOwnerProject.getName().equals(selectedProjectName)) {
      project = Optional.ofNullable(projectFacade.findByName(selectedProjectName))
        .orElseThrow(() ->
          new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "The specified project " +
            "for the topic" +
            topicName + " was not found"));
    } else {
      project = topicOwnerProject;
    }
    
    ProjectTopics pt = kafkaFacade.findTopicByNameAndProject(topicOwnerProject, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "Topic: " + topicName));
      
    if (!userEmail.equals("*")) {
      //fetch the user name from database
      Users user = Optional.ofNullable(userFacade.findByEmail(userEmail))
        .orElseThrow(() ->
          new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + userEmail));
      String principalName = KafkaConst.buildPrincipalName(selectedProjectName, user.getUsername());
      Optional<TopicAcls> topicAcl = kafkaFacade.getTopicAcls(topicName, principalName, permissionType, operationType,
        host, role);
      
      //TODO: previously there was a check of all the parts of the acl - is that necessary?
      if (topicAcl.isPresent()) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_ALREADY_EXISTS, Level.FINE,
          "topicAcl:" + topicAcl.get().toString());
      }
      
      kafkaFacade.addAclsToTopic(pt, user, permissionType, operationType, host, role, principalName);
    } else {
      for (ProjectTeam p : project.getProjectTeamCollection()) {
        Users selectedUser = p.getUser();
        String principalName = KafkaConst.buildPrincipalName(selectedProjectName, selectedUser.getUsername());
        if (!kafkaFacade.getTopicAcls(topicName, principalName, permissionType, operationType, host, role)
          .isPresent()) {
          kafkaFacade.addAclsToTopic(pt, selectedUser, permissionType, operationType, host, role, principalName);
        }
      }
    }
  }
  
  public TopicDefaultValueDTO topicDefaultValues() throws KafkaException {
    try {
      Set<String> brokers = settings.getBrokerEndpoints();
      return new TopicDefaultValueDTO(
        settings.getKafkaDefaultNumReplicas(),
        settings.getKafkaDefaultNumPartitions(),
        brokers.size());
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "", ex.getMessage(), ex);
    }
  }
  
  public List<SharedProjectDTO> getTopicSharedProjects (String topicName, Integer ownerProjectId) {
    List<SharedTopics> projectIds = kafkaFacade.findSharedTopicsByTopicAndOwnerProject(topicName, ownerProjectId);
  
    List<SharedProjectDTO> shareProjectDtos = new ArrayList<>();
    for (SharedTopics st : projectIds) {
    
      Project project = projectFacade.find(st.getSharedTopicsPK()
        .getProjectId());
      if (project != null) {
        shareProjectDtos.add(new SharedProjectDTO(project.getName(),
          project.getId()));
      }
    }
  
    return shareProjectDtos;
  }
  
  public void unshareTopicFromAllProjects(Project ownerProject, String topicName)
    throws KafkaException, ProjectException {
    //check if ownerProject is the owner of the topic
    if (!kafkaFacade.findTopicByNameAndProject(ownerProject, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "Topic " + topicName +
      " does not belong to project " + ownerProject.getName());
    }
    
    List<SharedTopics> list = kafkaFacade.findSharedTopicsByTopicName(topicName);
    
    for (SharedTopics st : list) {
      unshareTopic(ownerProject, topicName, st.getProjectId());
    }
  }
  
  public void unshareTopic(Project ownerProject, String topicName, Integer destProjectId)
    throws ProjectException, KafkaException {
    
    Project destProject = projectFacade.findById(destProjectId).orElseThrow(() ->
      new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "project: " + destProjectId));
    
    SharedTopics st = kafkaFacade.findSharedTopicByProjectAndTopic(destProjectId, topicName).orElseThrow(() ->
      new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_SHARED, Level.FINE,
        "topic: " + topicName + ", project: " + destProjectId));
    
    kafkaFacade.unshareTopic(st);
    
    kafkaFacade.removeAclFromTopic(topicName, destProject);
  }
  
  public String getKafkaCertPaths(Project project) {
    UserCerts userCert = userCerts.findUserCert(project.getName(), project.
        getOwner().getUsername());
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null
        && userCert.getUserCert().length > 0
        && userCert.getUserKey() != null
        && userCert.getUserKey().length > 0) {

      File certDir = new File(settings.getHopsworksTrueTempCertDir() + "/" + project.getName());

      if (!certDir.exists()) {
        try {
          certDir.mkdirs();
        } catch (Exception ex) {

        }
      }
      try {
        FileOutputStream fos;
        fos = new FileOutputStream(certDir.getAbsolutePath() + "/keystore.jks");
        fos.write(userCert.getUserKey());
        fos.close();

        fos = new FileOutputStream(certDir.getAbsolutePath() + "/truststore.jks");
        fos.write(userCert.getUserCert());
        fos.close();

      } catch (Exception e) {

      }
      return certDir.getAbsolutePath();
    } else {
      return null;
    }
  }

  /**
   * Add a new project member to all project's Kafka topics.
   *
   * @param project
   * @param member
   */
  public void addProjectMemberToTopics(Project project, String member)
    throws KafkaException, ProjectException, UserException {
    //Get all topics (shared with project as well)
    List<TopicDTO> topics = findTopicsByProject(project);
    List<SharedTopics> sharedTopics = kafkaFacade.findSharedTopicsByProject(project.getId());
    //For every topic that has been shared with the current project, add the new member to its ACLs
    for (SharedTopics sharedTopic : sharedTopics) {
      addAclsToTopic(sharedTopic.getSharedTopicsPK().getTopicName(), sharedTopic.getProjectId(),
          new AclDTO(project.getName(), member, "allow",
              Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD));
    }

    //Iterate over topics and add user to ACLs 
    for (TopicDTO topic : topics) {
      addAclsToTopic(topic.getName(), project.getId(), new AclDTO(project.getName(), member, "allow",
          Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD));
    }
  }
  
  public void removeProjectMemberFromTopics(Project project, Users user) throws ProjectException {
    //Get all topics (shared with project as well)
    List<SharedTopics> sharedTopics = kafkaFacade.findSharedTopicsByProject(project.getId());
    //For every topic that has been shared with the current project, add the new member to its ACLs
    List<Integer> projectSharedTopics = new ArrayList<>();

    //Get all projects from which topics have been shared
    for (SharedTopics sharedTopic : sharedTopics) {
      projectSharedTopics.add(sharedTopic.getProjectId());
    }

    if (!projectSharedTopics.isEmpty()) {
      for (Integer projectSharedTopic : projectSharedTopics) {
        kafkaFacade.removeAclsForUser(user, projectSharedTopic);
      }
    }

    //Remove acls for use in current project
    kafkaFacade.removeAclsForUser(user, project);
  }
  
  /**
   * Get all shared Topics for the given project.
   *
   * @param projectId
   * @return
   */
  public List<TopicDTO> findSharedTopicsByProject(Integer projectId) {
    List<SharedTopics> res = kafkaFacade.findSharedTopicsByProject(projectId);
    List<TopicDTO> topics = new ArrayList<>();
    for (SharedTopics pt : res) {
      topics.add(new TopicDTO(pt.getSharedTopicsPK().getTopicName(), pt.getProjectId(), true));
    }
    return topics;
  }

  public void updateTopicSchemaVersion(Project project, String topicName, Integer schemaVersion) throws KafkaException {
    ProjectTopics pt = kafkaFacade.getTopicByProjectAndTopicName(project, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,  "topic: " + topicName));
    
    String schemaName = pt.getSchemaTopics().getSchemaTopicsPK().getName();
  
    SchemaTopics st = kafkaFacade.getSchemaByNameAndVersion(schemaName, schemaVersion)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_VERSION_NOT_FOUND, Level.FINE,
        "schema: " + schemaName + ", version: " + schemaVersion));
    
    kafkaFacade.updateTopicSchemaVersion(pt, st);
  }
}
