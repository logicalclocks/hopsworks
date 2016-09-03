package io.hops.tf;

import io.hops.kafka.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import se.kth.bbc.project.*;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.util.Settings;
import kafka.admin.AdminUtils;
import kafka.common.TopicExistsException;
import kafka.common.TopicAlreadyMarkedForDeletionException;
import org.I0Itec.zkclient.ZkClient;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkConnection;

@Stateless
public class TensorflowFacade {

  private final static Logger LOGGER = Logger.getLogger(TensorflowFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TensorflowFacade() throws AppException, Exception {
  }

  /**
   * Get all the Topics for the given project.
   * <p/>
   * @param projectId
   * @return
   */
  public List<TopicDTO> findTopicsByProject(Integer projectId) {
    TypedQuery<ProjectTopics> query = em.createNamedQuery(
            "ProjectTopics.findByProjectId",
            ProjectTopics.class);
    query.setParameter("projectId", projectId);
    List<ProjectTopics> res = query.getResultList();
    List<TopicDTO> topics = new ArrayList<>();
    for (ProjectTopics pt : res) {
      topics.add(new TopicDTO(pt.getProjectTopicsPK().getTopicName()));
    }
    return topics;
  }

  /**
   * Get all shared Topics for the given project.
   * <p/>
   * @param projectId
   * @return
   */
  public List<TopicDTO> findSharedTopicsByProject(Integer projectId) {
    TypedQuery<SharedTopics> query = em.createNamedQuery(
            "SharedTopics.findByProjectId",
            SharedTopics.class);
    query.setParameter("projectId", projectId);
    List<SharedTopics> res = query.getResultList();
    List<TopicDTO> topics = new ArrayList<>();
    for (SharedTopics pt : res) {
      topics.add(new TopicDTO(pt.getSharedTopicsPK().getTopicName()));
    }
    return topics;
  }

  public List<PartitionDetailsDTO> getTopicDetails(Project project, String topicName)
          throws AppException, Exception {
    List<TopicDTO> topics = findTopicsByProject(project.getId());
    if (topics.isEmpty()) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "No Kafka topics found in this project.");
    }
    for (TopicDTO topic : topics) {
      if (topic.getName().equalsIgnoreCase(topicName)) {
        List<PartitionDetailsDTO> topicDetailDTO = getTopicDetailsfromKafkaCluster(topicName);
        return topicDetailDTO;
      }
    }

    List<PartitionDetailsDTO> pDto = new ArrayList<>();

    return pDto;
  }

  private int getPort(String zkIp) {
    int zkPort = Integer.parseInt(zkIp.split(COLON_SEPARATOR)[1]);
    return zkPort;
  }

  public InetAddress getIp(String zkIp) throws AppException {

    String ip = zkIp.split(COLON_SEPARATOR)[0];
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException ex) {
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
              "Zookeeper service is not available right now...");
    }
  }

  //this should return list of projects the topic belongs to as owner or shared
  public List<Project> findProjectforTopic(String topicName)
          throws AppException {
    TypedQuery<ProjectTopics> query = em.createNamedQuery(
            "ProjectTopics.findByTopicName", ProjectTopics.class);
    query.setParameter("topicName", topicName);

    if (query == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "No project found for this Kafka topic.");
    }
    List<ProjectTopics> resp = query.getResultList();
    List<Project> projects = new ArrayList<>();
    for (ProjectTopics pt : resp) {
      Project p = em.find(Project.class, pt.getProjectTopicsPK().getProjectId());
      if (p != null) {
        projects.add(p);
      }
    }

    if (projects.isEmpty()) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "No project found for this Kafka topic.");
    }

    return projects;
  }

  public void createTopicInProject(Integer projectId, TopicDTO topicDto)
          throws AppException {

    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Project does not exist in database.");
    }

    String topicName = topicDto.getName();

    TypedQuery<ProjectTopics> query = em.createNamedQuery(
            "ProjectTopics.findByTopicName", ProjectTopics.class);
    query.setParameter("topicName", topicName);
    List<ProjectTopics> res = query.getResultList();

    if (!res.isEmpty()) {
      throw new AppException(Response.Status.FOUND.getStatusCode(),
              "Kafka topic already exists in database. Pick a different topic name.");
    }

    //check if the replication factor is not greater than the 
    //number of running borkers
    Set<String> brokerEndpoints = getBrokerEndpoints();
    if (brokerEndpoints.size() < topicDto.getNumOfReplicas()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Topic replication factor can be a maximum of" + brokerEndpoints.size());
    }

    // create the topic in kafka 
    ZkClient zkClient = new ZkClient(getIp(settings.getZkConnectStr()).getHostName(),
            sessionTimeoutMs, connectionTimeout, ZKStringSerializer$.MODULE$);
    ZkConnection zkConnection = new ZkConnection(settings.getZkConnectStr());
    ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

    try {
      if (!AdminUtils.topicExists(zkUtils, topicName)) {
        AdminUtils.createTopic(zkUtils, topicName,
                topicDto.getNumOfPartitions(),
                topicDto.getNumOfReplicas(),
                new Properties());
      }
    } catch (TopicExistsException ex) {
      throw new AppException(Response.Status.FOUND.getStatusCode(),
              "Kafka topic already exists in Zookeeper. Pick a different topic name.");
    } finally {
      zkClient.close();
      try {
        zkConnection.close();
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex.getMessage());
      }
    }

    //if schema is empty, select a default schema, not implemented
    //persist topic into database
    SchemaTopics schema = em.find(SchemaTopics.class,
            new SchemaTopicsPK(topicDto.getSchemaName(),
                    topicDto.getSchemaVersion()));

    if (schema == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "topic has no schema");
    }

    /*
        What is the possibility of the program failing here? The topic is created on
        zookeeper, but not persisted onto db. User cannot access the topic, cannot
        create a topic of the same name. In such scenario, the zk timer should
        remove the topic from zk.
        
        One possibility is: schema has a global name space, it is not project specific.
        While the schema is selected by this topic, it could be deleted by another 
        user. Hence the above schema query will be empty. 
     */
    ProjectTopics pt = new ProjectTopics(topicName, projectId, schema);

    em.persist(pt);
    em.flush();

    //add default topic acl for the existing project members
    // addAclsToTopic(topicName, projectId, project.getName(), "*", "allow", "*", "*", "Data owner");
  }

  public void removeTopicFromProject(Project project, String topicName)
          throws AppException {

    ProjectTopics pt = em.find(ProjectTopics.class,
            new ProjectTopicsPK(topicName, project.getId()));

    if (pt == null) {
      throw new AppException(Response.Status.FOUND.getStatusCode(),
              "Kafka topic does not exist in database.");
    }

    //remove from database
    em.remove(pt);
    /*
        What is the possibility of the program failing below? The topic is removed from
        db, but not yet from zk. 
        
        Possibilities:
            1. ZkClient is unable to establish a connection, maybe due to timeouts.
            2. In case delete.topic.enable is not set to true in the Kafka server 
               configuration, delete topic marks a topic for deletion. Subsequent 
               topic (with the same name) create operation fails. 
     */
    //remove from zookeeper
    ZkClient zkClient = new ZkClient(getIp(settings.getZkConnectStr()).getHostName(),
            sessionTimeoutMs, connectionTimeout, ZKStringSerializer$.MODULE$);
    ZkConnection zkConnection = new ZkConnection(settings.getZkConnectStr());
    ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

    try {
      AdminUtils.deleteTopic(zkUtils, topicName);
    } catch (TopicAlreadyMarkedForDeletionException ex) {
      throw new AppException(Response.Status.FOUND.getStatusCode(),
              topicName + " alread marked for deletion.");
    } finally {
      zkClient.close();
      try {
        zkConnection.close();
      } catch (InterruptedException ex) {
        Logger.getLogger(TensorflowFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }
  }

}
