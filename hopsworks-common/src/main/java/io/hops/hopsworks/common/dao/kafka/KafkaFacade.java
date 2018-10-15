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
package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.exception.KafkaException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.common.TopicAlreadyMarkedForDeletionException;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaParseException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.zookeeper.KeeperException;
import org.elasticsearch.common.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KafkaFacade {

  private static final  Logger LOGGER = Logger.getLogger(KafkaFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  private ProjectFacade projectsFacade;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private BaseHadoopClientsService baseHadoopService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;

  private static final String COLON_SEPARATOR = ":";
  public static final String SLASH_SEPARATOR = "//";
  public static final String KAFKA_SECURITY_PROTOCOL = "SSL";
  private static final String KAFKA_BROKER_EXTERNAL_PROTOCOL = "EXTERNAL";
  private static final String PROJECT_DELIMITER = "__";
  public static final String DLIMITER = "[\"]";

  protected EntityManager getEntityManager() {
    return em;
  }

  public KafkaFacade() {
  }

  /**
   * Get all the Topics for the given project.
   *
   * @param project
   * @return
   */
  public List<TopicDTO> findTopicsByProject(Project project) {

    List<ProjectTopics> res = em.createNamedQuery("ProjectTopics.findByProject", ProjectTopics.class)
        .setParameter("project", project)
        .getResultList();

    List<TopicDTO> topics = new ArrayList<>();
    if(res != null && !res.isEmpty()) {
      topics = new ArrayList<>();
      for (ProjectTopics pt : res) {
        topics.add(new TopicDTO(pt.getTopicName(),
            pt.getSchemaTopics().getSchemaTopicsPK().getName(),
            pt.getSchemaTopics().getSchemaTopicsPK().getVersion()));
      }
    }
    return topics;
  }

  public ProjectTopics findTopicByNameAndProject(Project project, String topicName) {
    try {
      return em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", project)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get all shared Topics for the given project.
   *
   */
  public List<SharedTopics> findSharedTopicsByProject(Integer projectId) {
    TypedQuery<SharedTopics> query = em.createNamedQuery(
        "SharedTopics.findByProjectId",
        SharedTopics.class);
    query.setParameter("projectId", projectId);
    return query.getResultList();
  }
  
  public List<PartitionDetailsDTO> getTopicDetails(Project project, Users user,
    String topicName) throws Exception {
    List<TopicDTO> topics = findTopicsByProject(project);
    List<PartitionDetailsDTO> topicDetailDTO = new ArrayList<>();
    if (topics != null && !topics.isEmpty()) {
      for (TopicDTO topic : topics) {
        if (topic.getName().equalsIgnoreCase(topicName)) {
          topicDetailDTO.addAll(getTopicDetailsfromKafkaCluster(project, user, topicName));
          return topicDetailDTO;
        }
      }
    }
    return topicDetailDTO;
  }

  private int getPort(String zkIp) {
    return Integer.parseInt(zkIp.split(COLON_SEPARATOR)[1]);
  }

  public InetAddress getIp(String zkIp) throws ServiceException {

    String ip = zkIp.split(COLON_SEPARATOR)[0];
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ZOOKEEPER_SERVICE_UNAVAILABLE, Level.SEVERE,
        ex.getMessage());
    }
  }

  //this should return list of projects the topic belongs to as owner or shared
  public List<Project> findProjectforTopic(String topicName) {
    TypedQuery<ProjectTopics> query = em.createNamedQuery(
        "ProjectTopics.findByTopicName", ProjectTopics.class);
    query.setParameter("topicName", topicName);

    List<ProjectTopics> resp = query.getResultList();
    List<Project> projects =  new ArrayList<>();;
    if (resp != null && !resp.isEmpty()) {
      for (ProjectTopics pt : resp) {
        Project p = em.find(Project.class, pt.getProject());
        if (p != null) {
          projects.add(p);
        }
      }
    }
    return projects;
  }

  public ProjectTopics createTopicInProject(Integer projectId, TopicDTO topicDto)
    throws ProjectException, KafkaException, ServiceException {

    if (projectId == null || topicDto == null) {
      throw new IllegalArgumentException("projectId or topicDto were not provided.");
    }

    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
    }

    return createTopicInProject(project, topicDto);
  }

  public ProjectTopics createTopicInProject(Project project, TopicDTO topicDto)
      throws KafkaException, ServiceException {

    String topicName = topicDto.getName();

    List<ProjectTopics> res = em.createNamedQuery("ProjectTopics.findByTopicName", ProjectTopics.class)
        .setParameter("topicName", topicName)
        .getResultList();

    if (!res.isEmpty()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS, Level.FINE, "topic name: " + topicName);
    }

    List<ProjectTopics> topicsInProject = em.createNamedQuery("ProjectTopics.findByProject", ProjectTopics.class)
        .setParameter("project", project)
        .getResultList();

    if (topicsInProject != null && (topicsInProject.size() >= project.getKafkaMaxNumTopics())) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_LIMIT_REACHED, Level.FINE,
        "topic name: " + topicName + ", project: " +project.getName());
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
    

    // create the topic in kafka 
    ZkClient zkClient = new ZkClient(getIp(settings.getZkConnectStr()).
        getHostName(),
        Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, Settings.ZOOKEEPER_CONNECTION_TIMEOUT_MS, ZKStringSerializer$.MODULE$);
    ZkConnection zkConnection = new ZkConnection(settings.getZkConnectStr());
    ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

    try {
      if (!AdminUtils.topicExists(zkUtils, topicName)) {
        AdminUtils.createTopic(zkUtils, topicName,
            topicDto.getNumOfPartitions(),
            topicDto.getNumOfReplicas(),
            new Properties(), RackAwareMode.Enforced$.MODULE$);
      }
    } catch (TopicExistsException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS_IN_ZOOKEEPER, Level.INFO,
        "topic name: " + topicName, ex.getMessage());
    } finally {
      zkClient.close();
      try {
        zkConnection.close();
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex.getMessage());
      }
    }

    SchemaTopics schema = em.find(SchemaTopics.class,
        new SchemaTopicsPK(topicDto.getSchemaName(),
            topicDto.getSchemaVersion()));
  
    if (schema == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_NOT_FOUND, Level.FINE, "topic: " + topicName);
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
    ProjectTopics pt = new ProjectTopics(topicName, project, schema);

    em.persist(pt);
    em.flush();

    return pt;
    //add default topic acl for the existing project members
    // addAclsToTopic(topicName, projectId, project.getName(), "*", "allow", "*", "*", "Data owner");
  }

  public void removeTopicFromProject(Project project, String topicName) throws KafkaException, ServiceException {

    ProjectTopics pt = null;
    try {
      pt = em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", project)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName);
    }

    //remove from database
    em.remove(pt);
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
    ZkClient zkClient = new ZkClient(getIp(settings.getZkConnectStr()).
        getHostName(),
        Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, Settings.ZOOKEEPER_CONNECTION_TIMEOUT_MS, ZKStringSerializer$.MODULE$);
    ZkConnection zkConnection = new ZkConnection(settings.getZkConnectStr());
    ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

    try {
      AdminUtils.deleteTopic(zkUtils, topicName);
    } finally {
      zkClient.close();
      try {
        zkConnection.close();
      } catch (InterruptedException ex) {
        Logger.getLogger(KafkaFacade.class.getName()).
            log(Level.SEVERE, null, ex);
      }
    }
  }

  public void removeAllTopicsFromProject(Project project) throws
    InterruptedException, ServiceException {

    List<ProjectTopics> topics = em.createNamedQuery("ProjectTopics.findByProject", ProjectTopics.class)
        .setParameter("project", project)
        .getResultList();

    if (topics == null || topics.isEmpty()) {
      return;
    }

    ZkClient zkClient = null;
    ZkConnection zkConnection = null;

    try {
      zkClient = new ZkClient(getIp(settings.getZkConnectStr()).getHostName(),
          Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, Settings.ZOOKEEPER_CONNECTION_TIMEOUT_MS, ZKStringSerializer$.MODULE$);
      zkConnection = new ZkConnection(settings.getZkConnectStr());
      for (ProjectTopics topic : topics) {
        //remove from database
        em.remove(topic);

        //remove from zookeeper
        ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);
        try {
          AdminUtils.deleteTopic(zkUtils, topic.getTopicName());
        } catch (TopicAlreadyMarkedForDeletionException ex) {
          //ignore this error, if the topic is already being removed it will end
          //up in the state that we want.
        }
      }
    } finally {
      if (zkClient != null) {
        zkClient.close();
      }
      if (zkConnection != null) {
        zkConnection.close();
      }
    }
  }


  public void removeAclsForUser(Users user, Integer projectId) throws ProjectException {
    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId:" + projectId);
    }
    removeAclsForUser(user, project);
  }

  public void removeAclsForUser(Users user, Project project) {
    em.createNamedQuery("TopicAcls.deleteByUser", TopicAcls.class)
        .setParameter("user", user)
        .setParameter("project", project)
        .executeUpdate();
  }

  public TopicDefaultValueDTO topicDefaultValues() throws InterruptedException, IOException, KeeperException {

    Set<String> brokers = settings.getBrokerEndpoints();

    return new TopicDefaultValueDTO(
        settings.getKafkaDefaultNumReplicas(),
        settings.getKafkaDefaultNumPartitions(),
        brokers.size());
  }

  public void shareTopic(Project owningProject, String topicName,
      Integer projectId) throws KafkaException {
    if (owningProject.getId().equals(projectId)) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.DESTINATION_PROJECT_IS_TOPIC_OWNER, Level.FINE);
    }

    ProjectTopics pt = null;
    try {
      pt = em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", owningProject)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "topic: " + topicName);
    }

    SharedTopics sharedTopics = em.find(SharedTopics.class,
        new SharedTopicsPK(topicName, projectId));
    if (sharedTopics != null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_ALREADY_SHARED, Level.FINE, "topic: " + topicName);
    }
    //persist shared topic to database
    SharedTopics st = new SharedTopics(topicName, owningProject.getId(), projectId);
    em.persist(st);
    em.flush();
  }
  
  public void unShareTopic(String topicName, Integer ownerProjectId) throws KafkaException {
    SharedTopics pt = em.find(SharedTopics.class, new SharedTopicsPK(topicName, ownerProjectId));
    if (pt == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_SHARED, Level.FINE, "topic: " + topicName);
    }
    em.remove(pt);
  }

  public List<SharedProjectDTO> topicIsSharedTo(String topicName,
      Integer projectId) {

    List<SharedProjectDTO> shareProjectDtos = new ArrayList<>();

    TypedQuery<SharedTopics> query = em.createNamedQuery(
        "SharedTopics.findByTopicNameAndProjectId", SharedTopics.class);
    query.setParameter("topicName", topicName);
    query.setParameter("projectId", projectId);
    List<SharedTopics> projectIds = query.getResultList();

    for (SharedTopics st : projectIds) {

      Project project = em.find(Project.class, st.getSharedTopicsPK()
          .getProjectId());
      if (project != null) {
        shareProjectDtos.add(new SharedProjectDTO(project.getName(),
            project.getId()));
      }
    }

    return shareProjectDtos;
  }
  
  public List<AclUserDTO> aclUsers(Integer projectId, String topicName) {
  
    if (projectId == null || projectId < 0 || Strings.isNullOrEmpty(topicName)) {
      throw new IllegalArgumentException("ProjectId must be non-null non-negative number, topic must be provided");
    }
    //get the owner project name
    Project project = em.find(Project.class, projectId);
    List<AclUserDTO> aclUsers = new ArrayList<>();
  
    List<String> teamMembers = new ArrayList<>();
    for (ProjectTeam pt : project.getProjectTeamCollection()) {
      teamMembers.add(pt.getUser().getEmail());
    }
    teamMembers.add("*");//wildcard used for rolebased acl
    //contains project and its members
    Map<String, List<String>> projectMemberCollections = new HashMap<>();
    projectMemberCollections.put(project.getName(), teamMembers);
  
    //get all the projects this topic is shared with
    TypedQuery<SharedTopics> query = em.createNamedQuery(
      "SharedTopics.findByTopicName", SharedTopics.class);
    query.setParameter("topicName", topicName);
  
    List<String> sharedMembers = new ArrayList<>();
    for (SharedTopics sharedTopics : query.getResultList()) {
      project = em.find(Project.class, sharedTopics.getSharedTopicsPK()
        .getProjectId());
      for (ProjectTeam pt : project.getProjectTeamCollection()) {
        sharedMembers.add(pt.getUser().getEmail());
      }
      sharedMembers.add("*");
      projectMemberCollections.put(project.getName(), sharedMembers);
    }
    for (Map.Entry<String, List<String>> user : projectMemberCollections.
      entrySet()) {
      aclUsers.add(new AclUserDTO(user.getKey(), user.getValue()));
    }
    return aclUsers;
  }

  public void addAclsToTopic(String topicName, Integer projectId, AclDTO dto)
    throws KafkaException, ProjectException, UserException {

    addAclsToTopic(topicName, projectId,
        dto.getProjectName(),
        dto.getUserEmail(), dto.getPermissionType(),
        dto.getOperationType(), dto.getHost(), dto.getRole());
  }

  private void addAclsToTopic(String topicName, Integer projectId,
      String selectedProjectName, String userEmail, String permission_type,
      String operation_type, String host, String role) throws ProjectException, KafkaException, UserException {

    if(Strings.isNullOrEmpty(topicName) || projectId == null || projectId < 0 || userEmail == null){
      throw new IllegalArgumentException("Topic, userEmail and projectId must be provided. ProjectId must be a " +
        "non-negative " +
        "number");
    }

    //get the project id
    Project topicOwnerProject = projectsFacade.find(projectId);
    Project project;

    if (!topicOwnerProject.getName().equals(selectedProjectName)) {
      project = projectsFacade.findByName(selectedProjectName);
      if (project == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "The specified project " +
          "for the topic" +
          topicName + " was not found");
      }
    } else {
      project = topicOwnerProject;
    }

    ProjectTopics pt = null;
    try {
      pt = em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", topicOwnerProject)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, "Topic: " + topicName);
    }

    //if acl definition applies only for a specific user
    if (!userEmail.equals("*")) {
      //fetch the user name from database       
      Users user = userFacade.findByEmail(userEmail);

      if (user == null) {
        throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + userEmail);
      }

      String principalName = selectedProjectName + PROJECT_DELIMITER + user.getUsername();

      //Check if the requested ACL already exists
      TopicAcls topicAcl = getTopicAcl(topicName, principalName, permission_type, operation_type, host, role);
      if (topicAcl != null) {
        if (topicAcl.getProjectTopics().getTopicName().equals(topicName)
            && topicAcl.getHost().equals(host)
            && topicAcl.getOperationType().equalsIgnoreCase(operation_type)
            && topicAcl.getPermissionType().equalsIgnoreCase(permission_type)
            && topicAcl.getRole().equalsIgnoreCase(role)) {
          throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_ALREADY_EXISTS, Level.FINE,
            "topicAcl:" + topicAcl.toString());
        }
      }
      TopicAcls ta = new TopicAcls(pt, user, permission_type, operation_type, host, role, principalName);

      em.persist(ta);
      em.flush();
    } else {
      for (ProjectTeam p : project.getProjectTeamCollection()) {
        Users selectedUser = p.getUser();
        String principalName = selectedProjectName + PROJECT_DELIMITER + selectedUser.getUsername();
        TopicAcls topicAcl = getTopicAcl(topicName, principalName, permission_type, operation_type, host, role);
        //Check if new acl already exists for this user
        if (topicAcl == null) {
          TopicAcls ta = new TopicAcls(pt, selectedUser, permission_type, operation_type, host, role, principalName);
          em.persist(ta);
          em.flush();
        }
      }
    }
  }

  public void updateTopicAcl(Project project, String topicName, Integer aclId, AclDTO aclDto) throws KafkaException,
    ProjectException, UserException {

    ProjectTopics pt = null;
    try {
      pt = em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", project)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE, topicName);
    }

    TopicAcls ta = em.find(TopicAcls.class, aclId);
    if (ta == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOUND, Level.FINE,  "topic: " +topicName);
    }
    //remove previous acl
    em.remove(ta);

    //add the new acls  
    addAclsToTopic(topicName, project.getId(), aclDto);
  }

  public void removeAclFromTopic(String topicName, Integer aclId) throws KafkaException {
    TopicAcls ta = em.find(TopicAcls.class, aclId);
    if (ta == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOUND, Level.FINE, "topic: " +topicName);
    }

    if (!ta.getProjectTopics().getTopicName().equals(topicName)) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.ACL_NOT_FOR_TOPIC, Level.FINE, "topic: " + topicName);
    }

    em.remove(ta);
  }

  public TopicAcls getTopicAcl(String topicName,
      String principal, String permission_type,
      String operation_type, String host, String role) {
    TypedQuery<TopicAcls> query = em.createNamedQuery(
        "TopicAcls.findAcl", TopicAcls.class)
        .setParameter("topicName", topicName)
        .setParameter("principal", principal)
        .setParameter("role", role)
        .setParameter("host", host)
        .setParameter("operationType", operation_type)
        .setParameter("permissionType", permission_type);
    if (query.getResultList() != null && query.getResultList().size() == 1) {
      return query.getResultList().get(0);
    }
    return null;
  }

  public List<AclDTO> getTopicAcls(String topicName, Project project) throws KafkaException {
    ProjectTopics pt = null;
    try {
      pt = em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", project)
          .setParameter("topicName", topicName)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.TOPIC_NOT_FOUND, Level.FINE,  "topic: " + topicName);
    }
    
    TypedQuery<TopicAcls> query = em.createNamedQuery(
      "TopicAcls.findByTopicName", TopicAcls.class)
      .setParameter("topicName", topicName);
    List<TopicAcls> acls = query.getResultList();
    
    List<AclDTO> aclDtos = new ArrayList<>();
    String projectName;
    for (TopicAcls ta : acls) {
      projectName = ta.getPrincipal().split(PROJECT_DELIMITER)[0];
      aclDtos.add(new AclDTO(ta.getId(), projectName,
        ta.getUser().getEmail(), ta.getPermissionType(),
        ta.getOperationType(), ta.getHost(), ta.getRole()));
    }
    
    return aclDtos;
  }

  public SchemaCompatiblityCheck schemaBackwardCompatibility(SchemaDTO schemaDto) {

    String schemaContent = schemaDto.getContents();

    SchemaCompatibility.SchemaPairCompatibility schemaCompatibility;
    Schema writer;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findByName", SchemaTopics.class);
    query.setParameter("name", schemaDto.getName());

    try {
      Schema reader = new Schema.Parser().parse(schemaContent);

      for (SchemaTopics schemaTopic : query.getResultList()) {

        writer = new Schema.Parser().parse(schemaTopic.getContents());

        schemaCompatibility = SchemaCompatibility.
            checkReaderWriterCompatibility(reader, writer);

        switch (schemaCompatibility.getType()) {

          case COMPATIBLE:
            break;
          case INCOMPATIBLE:
            return SchemaCompatiblityCheck.INCOMPATIBLE;
          case RECURSION_IN_PROGRESS:
            break;
        }
      }
    } catch (SchemaParseException ex) {
      return SchemaCompatiblityCheck.INVALID;
    }
    return SchemaCompatiblityCheck.COMPATIBLE;
  }

  //if schema exists, increment it if not start version from 1
  public void addSchemaForTopics(SchemaDTO schemaDto) {

    int newVersion = 1;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findByName", SchemaTopics.class);
    query.setParameter("name", schemaDto.getName());

    List<SchemaTopics> schemaTopics = query.getResultList();

    if (schemaTopics != null && !schemaTopics.isEmpty()) {
      for (SchemaTopics schemaTopic : schemaTopics) {

        int schemaVersion = schemaTopic.getSchemaTopicsPK().getVersion();
        if (newVersion < schemaVersion) {
          newVersion = schemaVersion;
        }
      }
      newVersion++;
    }
    SchemaTopics schema = new SchemaTopics(schemaDto.getName(), newVersion, schemaDto.getContents(), new Date());

    em.persist(schema);
    em.flush();
  }
  
  public SchemaDTO getSchemaForTopic(String topicName) {
    
    List<ProjectTopics> topics = em.createNamedQuery(
      "ProjectTopics.findByTopicName", ProjectTopics.class)
      .setParameter("topicName", topicName).getResultList();
    if (topics != null && !topics.isEmpty()) {
      ProjectTopics topic = topics.get(0);
      
      SchemaTopics schema = em.find(SchemaTopics.class,
        new SchemaTopicsPK(
          topic.getSchemaTopics().getSchemaTopicsPK().getName(),
          topic.getSchemaTopics().getSchemaTopicsPK().getVersion()));
      
      if (schema != null) {
        return new SchemaDTO(schema.getContents());
      }
    }
    return null;
  }

  public SchemaTopics getSchema(String schemaName, Integer schemaVersion) {
    return em.createNamedQuery("SchemaTopics.findByNameAndVersion", SchemaTopics.class)
        .setParameter("name", schemaName)
        .setParameter("version", schemaVersion)
        .getSingleResult();
  }

  public List<SchemaDTO> listSchemasForTopics() {
    //get all schemas, and return the DTO
    Map<String, List<Integer>> schemas = new HashMap<>();
    List<SchemaDTO> schemaDtos = new ArrayList<>();
    String schemaName;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findAll", SchemaTopics.class);

    for (SchemaTopics schema : query.getResultList()) {
      schemaName = schema.getSchemaTopicsPK().getName();
      schemas.computeIfAbsent(schemaName, k -> new ArrayList<>());
      schemas.get(schemaName).add(schema.getSchemaTopicsPK().getVersion());
    }
    for (Map.Entry<String, List<Integer>> schema : schemas.entrySet()) {
      schemaDtos.add(new SchemaDTO(schema.getKey(), schema.getValue()));
    }

    return schemaDtos;
  }

  public SchemaDTO getSchemaContent(String schemaName, Integer schemaVersion) throws KafkaException {
    SchemaTopics schemaTopic = em.find(SchemaTopics.class,
        new SchemaTopicsPK(schemaName, schemaVersion));
    if (schemaTopic == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_NOT_FOUND, Level.FINE, "Schema: " + schemaName);
    }
    return new SchemaDTO(schemaTopic.getContents());
  }
  
  public void deleteSchema(String schemaName, Integer version) throws KafkaException {
    //Check if schema is currently used by a topic.
    List<ProjectTopics> topics = em.createNamedQuery(
      "ProjectTopics.findBySchemaVersion", ProjectTopics.class)
      .setParameter("schema_name", schemaName)
      .setParameter("schema_version", version)
      .getResultList();
    if (topics != null && !topics.isEmpty()) {
      //Create a list of topic names to display to user
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_IN_USE, Level.FINE);
    } else {
      //get the bean and remove it
      SchemaTopics schema = em.find(SchemaTopics.class, new SchemaTopicsPK(schemaName, version));
      em.remove(schema);
      em.flush();
    }
  }

  public List<PartitionDetailsDTO> getTopicDetailsfromKafkaCluster(
      Project project, Users user, String topicName) throws KafkaException, CryptoPasswordNotFoundException {
  
    Set<String> brokers = settings.getKafkaBrokers();
    Map<Integer, List<String>> replicas = new HashMap<>();
    Map<Integer, List<String>> inSyncReplicas = new HashMap<>();
    Map<Integer, String> leaders = new HashMap<>();
    List<PartitionDetailsDTO> partitionDetails = new ArrayList<>();

    //Keep only INTERNAL protocol brokers
    Iterator<String> iter = brokers.iterator();
    while (iter.hasNext()) {
      String seed = iter.next();
      if (seed.split(COLON_SEPARATOR)[0].equalsIgnoreCase(KAFKA_BROKER_EXTERNAL_PROTOCOL)) {
        iter.remove();
      }
    }
    try {
      HopsUtils.copyProjectUserCerts(project, user.getUsername(),
          settings.getHopsworksTmpCertDir(), null,
          certificateMaterializer, settings.getHopsRpcTls());
      String projectSpecificUser = hdfsUsersController.getHdfsUserName(project,
          user);
      String certPassword = baseHadoopService.getProjectSpecificUserCertPassword(projectSpecificUser);
      //Get information from first broker, all of them will have the same information once they are synced
      String brokerAddress = brokers.iterator().next().split("://")[1];
      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.IntegerDeserializer");
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringDeserializer");

      //configure the ssl parameters
      props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KAFKA_SECURITY_PROTOCOL);
      props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.getProjectTruststoreName(project.getName(),
          user.getUsername()));
      props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, certPassword);
      props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, settings.getHopsworksTmpCertDir() + File.separator
          + HopsUtils.getProjectKeystoreName(project.getName(), user.getUsername()));
      props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, certPassword);
      props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, certPassword);
      try (KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props)) {
        List<PartitionInfo> partitions = consumer.partitionsFor(topicName);
        for (PartitionInfo partition : partitions) {
          int id = partition.partition();
          //list the leaders of each parition
          leaders.put(id, partition.leader().host());

          //list the replicas of the partition
          replicas.put(id, new ArrayList<>());
          for (Node node : partition.replicas()) {
            replicas.get(id).add(node.host());
          }

          //list the insync replicas of the parition
          inSyncReplicas.put(id, new ArrayList<>());
          for (Node node : partition.inSyncReplicas()) {
            inSyncReplicas.get(id).add(node.host());
          }

          partitionDetails.add(new PartitionDetailsDTO(id, leaders.get(id), replicas.get(id), replicas.get(id)));
        }
      } catch (Exception ex) {
        throw new KafkaException(RESTCodes.KafkaErrorCode.BROKER_METADATA_ERROR, Level.SEVERE,
          "Broker: " + brokerAddress,
          ex.getMessage(), ex);
      }
    } finally {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    }
    Collections.sort(partitionDetails, (PartitionDetailsDTO c1, PartitionDetailsDTO c2) -> {
      if (c1.getId() < c2.getId()) {
        return -1;
      }
      if (c1.getId() > c2.getId()) {
        return 1;
      }
      return 0;
    });
    return partitionDetails;
  }

}
