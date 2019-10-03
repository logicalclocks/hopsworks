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
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaParseException;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.elasticsearch.common.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;



@Stateless
public class KafkaFacade {

  private static final  Logger LOGGER = Logger.getLogger(KafkaFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private KafkaAdminClient kafkaAdminClient;
  
  protected EntityManager getEntityManager() {
    return em;
  }

  public KafkaFacade() {
  }
  
  public List<ProjectTopics> findTopicsByProject (Project project) {
    return em.createNamedQuery("ProjectTopics.findByProject", ProjectTopics.class)
      .setParameter("project", project)
      .getResultList();
  }

  public Optional<ProjectTopics> findTopicByNameAndProject(Project project, String topicName) {
    try {
      return Optional.of(em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
          .setParameter("project", project)
          .setParameter("topicName", topicName)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public List<SharedTopics> findSharedTopicsByProject(Integer projectId) {
    return em.createNamedQuery("SharedTopics.findByProjectId", SharedTopics.class)
      .setParameter("projectId", projectId)
      .getResultList();
  }
  
  public List<SharedTopics> findSharedTopicsByTopicName (String topicName) {
    return em.createNamedQuery("SharedTopics.findByTopicName", SharedTopics.class)
      .setParameter("topicName", topicName)
      .getResultList();
  }
  
  public Optional<ProjectTopics> findTopicByName(String topicName) {
    try {
      return Optional.of(em.createNamedQuery("ProjectTopics.findByTopicName", ProjectTopics.class)
        .setParameter("topicName", topicName)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<SchemaTopics> findSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(em.find(SchemaTopics.class,
        new SchemaTopicsPK(schemaName, schemaVersion)));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }
  
  private KafkaFuture<CreateTopicsResult> createTopicInKafka(TopicDTO topicDTO) {
    return kafkaAdminClient.listTopics().names().thenApply((set) -> {
      if (set.contains(topicDTO.getName())) {
        return null;
      } else {
        NewTopic newTopic =
          new NewTopic(topicDTO.getName(), topicDTO.getNumOfPartitions(), topicDTO.getNumOfReplicas().shortValue());
        return kafkaAdminClient.createTopics(Collections.singleton(newTopic));
      }
    });
  }

  public ProjectTopics createTopicInProject(Project project, TopicDTO topicDto)
      throws KafkaException, InterruptedException, ExecutionException {
    
    SchemaTopics schema =
      findSchemaByNameAndVersion(topicDto.getSchemaName(), topicDto.getSchemaVersion()).orElseThrow(() ->
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

    em.persist(pt);
    em.flush();

    return pt;
  }
  
  public void removeTopicFromProject(ProjectTopics pt) {
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
    kafkaAdminClient.deleteTopics(Collections.singleton(pt.getTopicName()));
  }

  public void removeAllTopicsFromProject(Project project) {

    List<ProjectTopics> topics = findTopicsByProject(project);

    if (topics == null || topics.isEmpty()) {
      return;
    }
    
    List<String> topicNameList = topics.stream()
      .map(ProjectTopics::getTopicName)
      .collect(Collectors.toList());
    
    kafkaAdminClient.deleteTopics(topicNameList);
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
  
  public void removeAclForProject(Integer projectId) throws ProjectException {
    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId:" + projectId);
    }
    removeAclForProject(project);
  }
  
  public void removeAclForProject(Project project) {
    em.createNamedQuery("TopicAcls.findAll", TopicAcls.class)
      .getResultList()
      .stream()
      .filter(acl -> acl.getPrincipal().split(KafkaConst.PROJECT_DELIMITER)[0].equals(project.getName()))
      .forEach(acl -> em.remove(acl));
  }
  
  public Optional<SharedTopics> findSharedTopicByProjectAndTopic (Integer projectId, String topicName) {
    return Optional.ofNullable(em.find(SharedTopics.class,
      new SharedTopicsPK(topicName, projectId)));
  }

  public void shareTopic(Project owningProject, String topicName, Integer projectId) {
    SharedTopics st = new SharedTopics(topicName, owningProject.getId(), projectId);
    em.persist(st);
    em.flush();
  }
  
  public void unshareTopic(SharedTopics st) {
    em.remove(st);
  }
  
  public List<SharedTopics> findSharedTopicsByTopicAndOwnerProject (String topicName, Integer ownerProjectId) {
    return em.createNamedQuery(
      "SharedTopics.findByTopicAndOwnerProjectId", SharedTopics.class)
      .setParameter("topicName", topicName)
      .setParameter("ownerProjectId", ownerProjectId)
      .getResultList();
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
  
  public void addAclsToTopic(ProjectTopics pt, Users user, String permissionType, String operationType, String host,
    String role, String principalName) {
    
    TopicAcls ta = new TopicAcls(pt, user, permissionType, operationType, host, role, principalName);
    em.persist(ta);
    em.flush();
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
    kafkaController.addAclsToTopic(topicName, project.getId(), aclDto);
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
  
  public void removeAclFromTopic(String topicName, Project project) {
    em.createNamedQuery(
      "TopicAcls.findByTopicName", TopicAcls.class)
      .setParameter("topicName", topicName)
      .getResultList()
      .stream()
      .filter(acl -> acl.getPrincipal().split(KafkaConst.PROJECT_DELIMITER)[0].equals(project.getName()))
      .forEach(acl -> em.remove(acl));
  }

  public Optional<TopicAcls> getTopicAcls(String topicName,
      String principal, String permission_type,
      String operation_type, String host, String role) {
    return Optional.ofNullable(em.createNamedQuery(
        "TopicAcls.findAcl", TopicAcls.class)
        .setParameter("topicName", topicName)
        .setParameter("principal", principal)
        .setParameter("role", role)
        .setParameter("host", host)
        .setParameter("operationType", operation_type)
        .setParameter("permissionType", permission_type)
        .getResultList())
      .filter((list) -> list.size() == 1)
      .map((list) -> list.get(0));
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
      projectName = ta.getPrincipal().split(KafkaConst.PROJECT_DELIMITER)[0];
      aclDtos.add(new AclDTO(ta.getId(), projectName,
        ta.getUser().getEmail(), ta.getPermissionType(),
        ta.getOperationType(), ta.getHost(), ta.getRole()));
    }
    
    return aclDtos;
  }
  
  public void validateSchema(SchemaDTO schemaDTO) throws KafkaException {
    if(schemaDTO == null){
      throw new IllegalArgumentException("No schema provided");
    }
    validateSchemaNameAgainstBlacklist(schemaDTO.getName(), RESTCodes.KafkaErrorCode.CREATE_SCHEMA_RESERVED_NAME);
  }
  
  public void validateSchemaNameAgainstBlacklist(String schemaName, RESTCodes.KafkaErrorCode restCode)
    throws KafkaException {
    if(Settings.KAFKA_SCHEMA_BLACKLIST.contains(schemaName)){
      throw new KafkaException(restCode, Level.FINE);
    }
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
    validateSchemaNameAgainstBlacklist(schemaName, RESTCodes.KafkaErrorCode.DELETE_RESERVED_SCHEMA);
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

  public DescribeTopicsResult getTopicsFromKafkaCluster(Collection<String> topicNames) {
    return kafkaAdminClient.describeTopics(topicNames);
  }
  
  public Optional<ProjectTopics> getTopicByProjectAndTopicName(Project project, String topicName) {
    try {
      return Optional.of(em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
        .setParameter("project", project)
        .setParameter("topicName", topicName)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<SchemaTopics> getSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(em.createNamedQuery("SchemaTopics.findByNameAndVersion", SchemaTopics.class)
        .setParameter("name", schemaName)
        .setParameter("version", schemaVersion)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public void updateTopicSchemaVersion(ProjectTopics pt, SchemaTopics st) {
    pt.setSchemaTopics(new SchemaTopics(st.schemaTopicsPK.getName(), st.schemaTopicsPK.getVersion()));
    em.merge(pt);
    em.flush();
  }
  
  public enum TopicsSorts {
    NAME("NAME", "LOWER(t.name)", "ASC"),
    SCHEMA_NAME("SCHEMA_NAME", "LOWER(t.schemaName)", "ASC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
  
    private TopicsSorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
  
    public String getValue() {
      return value;
    }
  
    public String getSql() {
      return sql;
    }
  
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getJoin() {
      return null;
    }
  
    @Override
    public String toString() {
      return value;
    }
  }
  
  public enum TopicsFilters {
    SHARED("SHARED", "t.isShared = :shared", "shared", "false");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private TopicsFilters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return value;
    }

  }

}
