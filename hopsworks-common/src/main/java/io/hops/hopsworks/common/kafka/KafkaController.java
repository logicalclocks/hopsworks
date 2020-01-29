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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.AclUser;
import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.Subjects;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsFacade;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopics;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicAcls;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDefaultValueDTO;
import io.hops.hopsworks.common.dao.kafka.TopicAclsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaController {

  private final static Logger LOGGER = Logger.getLogger(KafkaController.class.getName());

  @EJB
  private CertsFacade userCerts;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  @EJB
  private SharedTopicsFacade sharedTopicsFacade;
  @EJB
  private HopsKafkaAdminClient hopsKafkaAdminClient;
  @EJB
  private SubjectsFacade subjectsFacade;
  @EJB
  private TopicAclsFacade topicAclsFacade;
  @EJB
  private ProjectController projectController;
  
  public void createTopic(Project project, TopicDTO topicDto, UriInfo uriInfo) throws KafkaException,
    ProjectException, UserException, InterruptedException, ExecutionException {
    
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
      Set<String> brokerEndpoints = settings.getBrokerEndpoints();
      if (brokerEndpoints.size() < topicDto.getNumOfReplicas()) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_REPLICATION_ERROR, Level.FINE,
          "maximum: " + brokerEndpoints.size());
      }
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
    
    createTopicInProject(project, topicDto);
  
    //By default, all members of the project are granted full permissions
    //on the topic
    addFullPermissionAclsToTopic(project.getId(), topicDto.getName(), project.getId());
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
  
  public List<TopicDTO> findTopicsByProject(Project project) {
    List<ProjectTopics> ptList = projectTopicsFacade.findTopicsByProject(project);
  
    List<TopicDTO> topics = new ArrayList<>();
    for (ProjectTopics pt : ptList) {
      topics.add(new TopicDTO(pt.getTopicName(),
        pt.getSubjects().getSubject(),
        pt.getSubjects().getVersion(),
        false));
    }
    return topics;
  }
  
  public ProjectTopics createTopicInProject(Project project, TopicDTO topicDto)
    throws KafkaException, InterruptedException, ExecutionException {
    
    Subjects schema =
      subjectsFacade.findSubjectByNameAndVersion(project, topicDto.getSchemaName(), topicDto.getSchemaVersion())
        .orElseThrow(() ->
          new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_NOT_FOUND, Level.FINE, "topic: " + topicDto.getName()));
    
    // create the topic in kafka
    if (createTopicInKafka(topicDto).get() == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS_IN_ZOOKEEPER, Level.INFO,
        "topic name: " + topicDto.getName());
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
     * user. Hence the above schema query will be empty.
     */
    ProjectTopics pt = new ProjectTopics(topicDto.getName(), project, schema);
    
    projectTopicsFacade.save(pt);
    
    return pt;
  }
  
  private KafkaFuture<CreateTopicsResult> createTopicInKafka(TopicDTO topicDTO) {
    return hopsKafkaAdminClient.listTopics().names().thenApply((set) -> {
      if (set.contains(topicDTO.getName())) {
        return null;
      } else {
        NewTopic newTopic =
          new NewTopic(topicDTO.getName(), topicDTO.getNumOfPartitions(), topicDTO.getNumOfReplicas().shortValue());
        return hopsKafkaAdminClient.createTopics(Collections.singleton(newTopic));
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
  
  public KafkaFuture<List<PartitionDetailsDTO>> getTopicDetails (Project project, String topicName) throws
    KafkaException {
  
    projectTopicsFacade.findTopicByNameAndProject(project, topicName).orElseThrow(() ->
      new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName)
    );
  
    return getTopicDetailsFromKafkaCluster(topicName);
  }
  
  public SharedTopicsDTO shareTopicWithProject(Project project, String topicName, Integer destProjectId) throws
    ProjectException, KafkaException, UserException {
    
    if (project.getId().equals(destProjectId)) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.DESTINATION_PROJECT_IS_TOPIC_OWNER, Level.FINE);
    }
    
    if (!projectTopicsFacade.findTopicByNameAndProject(project, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.PROJECT_IS_NOT_THE_OWNER_OF_THE_TOPIC, Level.FINE);
    }
    
    if (!projectTopicsFacade.findTopicByNameAndProject(project, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName);
    }
    
    if (!Optional.ofNullable(projectFacade.find(destProjectId)).isPresent()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
        "Could not find project: " + destProjectId);
    }
  
    sharedTopicsFacade.shareTopic(project, topicName, destProjectId);
    //By default, all members of the project are granted full permissions on the topic
    addFullPermissionAclsToTopic(destProjectId, topicName, project.getId());
    
    Optional<SharedTopics> optionalSt =
      sharedTopicsFacade.findSharedTopicByTopicAndProjectIds(topicName, project.getId(), destProjectId);
  
    SharedTopicsDTO dto = new SharedTopicsDTO();
    optionalSt.ifPresent(st -> {
      dto.setProjectId(st.getProjectId());
      dto.setSharedTopicsPK(st.getSharedTopicsPK());
    });
    
    return dto;
  }
  
  public Optional<TopicAcls> getTopicAclsByDto(String topicName, AclDTO dto) throws UserException{
    Users user = Optional.ofNullable(userFacade.findByEmail(dto.getUserEmail()))
      .orElseThrow(() ->
        new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + dto.getUserEmail()));
  
    String principalName = KafkaConst.buildPrincipalName(dto.getProjectName(), user.getUsername());
    
    return topicAclsFacade.getTopicAcls(topicName, dto, principalName);
  }
  
  private void addFullPermissionAclsToTopic(Integer aclProjectId, String topicName, Integer projectId)
    throws ProjectException, KafkaException, UserException {
    Project p = Optional.ofNullable(projectFacade.find(aclProjectId)).orElseThrow(() ->
      new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
      "Could not find project: " + aclProjectId));
    
    List<AclDTO> acls = p.getProjectTeamCollection()
      .stream()
      .map(member -> member.getUser().getEmail())
      .map(email -> new AclDTO(p.getName(), email, "allow",
        Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD))
      .collect(Collectors.toList());
    
    for (AclDTO acl : acls) {
      addAclsToTopic(topicName, projectId, acl);
    }
  }
  
  public List<AclUser> getTopicAclUsers(Project project, String topicName) {
    if (project == null || Strings.isNullOrEmpty(topicName)) {
      throw new IllegalArgumentException("ProjectId must be non-null, topic must be provided");
    }
    
    List<AclUser> aclUsers = new ArrayList<>();
  
    List<String> teamMembers = new ArrayList<>();
    for (ProjectTeam pt : project.getProjectTeamCollection()) {
      teamMembers.add(pt.getUser().getEmail());
    }
    teamMembers.add("*");//wildcard used for rolebased acl
    //contains project and its members
    Map<String, List<String>> projectMemberCollections = new HashMap<>();
    projectMemberCollections.put(project.getName(), teamMembers);
  
    //get all the projects this topic is shared with
    List<SharedTopics> sharedTopicsList = sharedTopicsFacade.findSharedTopicsByTopicName(topicName);
  
    List<String> sharedMembers;
    for (SharedTopics st : sharedTopicsList) {
      sharedMembers = new ArrayList<>();
      Project p = projectFacade.find(st.getSharedTopicsPK().getProjectId());
      for (ProjectTeam pt : p.getProjectTeamCollection()) {
        sharedMembers.add(pt.getUser().getEmail());
      }
      sharedMembers.add("*");
      projectMemberCollections.put(p.getName(), sharedMembers);
    }
    for (Map.Entry<String, List<String>> user : projectMemberCollections.
      entrySet()) {
      aclUsers.add(new AclUser(user.getKey(), new HashSet<>(user.getValue())));
    }
    return aclUsers;
  }
  
  public Pair<TopicAcls, Response.Status> addAclsToTopic(String topicName, Integer projectId, AclDTO dto) throws
    ProjectException,
    KafkaException, UserException {
    return addAclsToTopic(topicName, projectId,
      dto.getProjectName(),
      dto.getUserEmail(), dto.getPermissionType(),
      dto.getOperationType(), dto.getHost(), dto.getRole());
  }
  
  private Pair<TopicAcls, Response.Status> addAclsToTopic(String topicName, Integer projectId,
    String selectedProjectName, String userEmail, String permissionType,
    String operationType, String host, String role) throws ProjectException, KafkaException, UserException {
  
    if(Strings.isNullOrEmpty(topicName) || userEmail == null){
      throw new IllegalArgumentException("Topic and userEmail must be provided.");
    }
  
    //get the project id
    Project topicOwnerProject = Optional.ofNullable(projectFacade.find(projectId)).orElseThrow(() ->
      new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId));
    
    if (!topicOwnerProject.getName().equals(selectedProjectName)) {
      if (projectFacade.findByName(selectedProjectName) == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "The specified project " +
          "for the topic" + topicName + " was not found");
      }
    }
    
    ProjectTopics pt = projectTopicsFacade.findTopicByNameAndProject(topicOwnerProject, topicName)
      .orElseThrow(() ->
        new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "Topic: " + topicName));
      
    //should not be able to create multiple ACLs at the same time
    if (userEmail.equals("*")) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_FOR_ANY_USER, Level.FINE, "topic: " + topicName);
    }
    
    //fetch the user name from database
    Users user = Optional.ofNullable(userFacade.findByEmail(userEmail))
      .orElseThrow(() ->
        new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + userEmail));
    String principalName = KafkaConst.buildPrincipalName(selectedProjectName, user.getUsername());
    
    Optional<TopicAcls> optionalAcl = topicAclsFacade.getTopicAcls(topicName, principalName, permissionType,
      operationType, host, role);
    if (optionalAcl.isPresent()) {
      return Pair.of(optionalAcl.get(), Response.Status.OK);
    }
    
    TopicAcls acl = topicAclsFacade.addAclsToTopic(pt, user, permissionType, operationType, host, role,
      principalName);
    return Pair.of(acl, Response.Status.CREATED);
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
    List<SharedTopics> projectIds =
      sharedTopicsFacade.findSharedTopicsByTopicAndOwnerProject(topicName, ownerProjectId);
  
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
  
  public void unshareTopic(Project requesterProject, String topicName, String destProjectName) throws ProjectException
    , KafkaException {
    List<SharedTopics> list = new ArrayList<>();
    Project destProject = projectFacade.findByName(destProjectName);
    //check if topic exists
    ProjectTopics topic = projectTopicsFacade.findTopicByName(topicName).orElseThrow(() ->
      new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "Topic:" + topicName));
    //check if requesterProject is the owner of the topic
    if (topic.getProject().equals(requesterProject)) {
      if (destProject == null) {
        list.addAll(sharedTopicsFacade.findSharedTopicsByTopicName(topicName));
      } else {
        SharedTopics st =
          sharedTopicsFacade.findSharedTopicByProjectAndTopic(destProject.getId(), topicName).orElseThrow(() ->
            new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_SHARED, Level.FINE,
              "topic: " + topicName + ", project: " + destProject.getName()));
        list.add(st);
      }
    } else {
      if (destProject == null) {
        SharedTopics st =
          sharedTopicsFacade.findSharedTopicByProjectAndTopic(requesterProject.getId(), topicName).orElseThrow(() ->
            new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_SHARED, Level.FINE,
              "topic: " + topicName + ", project: " + requesterProject.getId()));
        list.add(st);
      }
    }
  
  
    for (SharedTopics st : list) {
      sharedTopicsFacade.remove(st);
      Project projectACLs = projectFacade.findById(st.getSharedTopicsPK().getProjectId()).orElseThrow(() ->
        new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
          "project: " + st.getSharedTopicsPK().getProjectId()));
      topicAclsFacade.removeAclFromTopic(topicName, projectACLs);
    }
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
    List<SharedTopics> sharedTopics = sharedTopicsFacade.findSharedTopicsByProject(project.getId());
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
    sharedTopicsFacade.findSharedTopicsByProject(project.getId())
      .stream()
      .map(st -> {
        try {
          return projectController.findProjectById(st.getSharedTopicsPK().getProjectId());
        } catch (ProjectException ex) {
          throw new RuntimeException(ex);
        }
      })
      .map(Project::getName)
      .forEach(name -> topicAclsFacade.removeAclsForUserAndPrincipalProject(user, name));
    topicAclsFacade.removeAclsForUser(user, project);
  }
  
  /**
   * Get all shared Topics for the given project.
   *
   * @param projectId
   * @return
   */
  public List<TopicDTO> findSharedTopicsByProject(Integer projectId) {
    List<SharedTopics> res = sharedTopicsFacade.findSharedTopicsByProject(projectId);
    List<TopicDTO> topics = new ArrayList<>();
    for (SharedTopics pt : res) {
      projectTopicsFacade.findTopicByName(pt.getSharedTopicsPK().getTopicName())
        .map(topic -> new TopicDTO(pt.getSharedTopicsPK().getTopicName(),
          pt.getProjectId(),
          topic.getSubjects().getSubject(),
          topic.getSubjects().getVersion(),
          topic.getSubjects().getSchema().getSchema(),
          true))
        .ifPresent(topics::add);
    }
    return topics;
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
  
  public void removeAclsForUser(Users user, Integer projectId) throws ProjectException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId:" + projectId);
    }
    topicAclsFacade.removeAclsForUser(user, project);
  }
  
  public void removeAclForProject(Integer projectId) throws ProjectException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId:" + projectId);
    }
    topicAclsFacade.removeAclForProject(project);
  }
  
  public Integer updateTopicAcl(Project project, String topicName, Integer aclId, AclDTO aclDto) throws
    KafkaException,
    ProjectException, UserException {
    
    if (!projectTopicsFacade.findTopicByNameAndProject(project, topicName).isPresent()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, topicName);
    }
    
    TopicAcls ta = topicAclsFacade.find(aclId);
    if (ta == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOUND, Level.FINE,  "topic: " +topicName);
    }
  
    //remove previous acl
    topicAclsFacade.remove(ta);
    
    //add the new acls
    Pair<TopicAcls, Response.Status> aclTuple = addAclsToTopic(topicName, project.getId(), aclDto);
    return aclTuple.getLeft().getId();
  }
  
  public Optional<TopicAcls> findAclByIdAndTopic(String topicName, Integer aclId) throws KafkaException{
    Optional<TopicAcls> acl = Optional.ofNullable(topicAclsFacade.find(aclId));
    if (acl.isPresent()) {
      if (!topicName.equals(acl.get().getProjectTopics().getTopicName())) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOR_TOPIC, Level.FINE, "topic: " + topicName);
      }
    } else {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOUND, Level.FINE, "aclId: " + aclId);
    }
    return acl;
  }
  
  public void removeAclFromTopic(String topicName, Integer aclId) throws KafkaException {
    TopicAcls ta = topicAclsFacade.find(aclId);
    if (ta == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOUND, Level.FINE, "topic: " +topicName);
    }
    
    if (!ta.getProjectTopics().getTopicName().equals(topicName)) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOR_TOPIC, Level.FINE, "topic: " + topicName);
    }
    
    topicAclsFacade.remove(ta);
  }
  
  public SubjectDTO getSubjectForTopic(Project project, String topic) throws KafkaException {
    Optional<ProjectTopics> pt = projectTopicsFacade.findTopicByNameAndProject(project, topic);
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


