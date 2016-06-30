package io.hops.kafka;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import se.kth.bbc.project.*;
import java.util.List;
import java.util.Map;
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
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkConnection;
import se.kth.hopsworks.user.model.Users;

@Stateless
public class KafkaFacade {

    private final static Logger LOGGER = Logger.getLogger(KafkaFacade.class.
            getName());

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @EJB
    Settings settings;

    public static final String COLON_SEPARATOR = ":";

    public static final String SLASH_SEPARATOR = "//";

    public static final String SECURITY_PROTCOL = "SSL";

    public static final String TWO_UNDERSCROES = "__";

    public static final String DLIMITER = "[\"]";

    public String CLIENT_ID = "list_topics";

    public final int connectionTimeout = 30 * 1000;// 30 seconds

    public final int BUFFER_SIZE = 20 * 1000;

    public Set<String> zkBrokerList;

    public Set<String> topicList;

    public int sessionTimeoutMs = 30 * 1000;//30 seconds

    public static ZooKeeper zk;

    protected EntityManager getEntityManager() {
        return em;
    }

    public KafkaFacade() throws AppException, Exception {
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

        String topicName = topicDto.getName();

        TypedQuery<ProjectTopics> query = em.createNamedQuery(
                "ProjectTopics.findByTopicName", ProjectTopics.class);
        query.setParameter("topicName", topicName);
        List<ProjectTopics> res = query.getResultList();

        if (!res.isEmpty()) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic already exists. Pick a different topic name.");
        }

        //check if the replication factor is not greater that than the 
        //number of running borkers
        Set<String> brokerEndpoints = getBrokerEndpoints();
        if (brokerEndpoints.size() < topicDto.getNumOfReplicas()) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Topic replication factor can be maximum of" + brokerEndpoints.size());
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
                    "Kafka topic already exists. Pick a different topic name.");
        } finally {
            zkClient.close();
        }

        //if schema is empty, select a default schema
        //persist topic into database
        SchemaTopics schema = em.find(SchemaTopics.class,
                new SchemaTopicsPK(topicDto.getSchemaName(),
                        topicDto.getSchemaVersion()));

        if (schema == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "topic has not schema");
        }

        ProjectTopics pt = new ProjectTopics(topicName, projectId, schema);

        em.merge(pt);
        em.persist(pt);
        em.flush();
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
        }
    }

    public TopicDefaultValueDTO topicDefaultValues() throws AppException {

        Set<String> brokers = getBrokerEndpoints();

        TopicDefaultValueDTO valueDto = new TopicDefaultValueDTO(
                settings.getKafkaDefaultNumReplicas(),
                settings.getKafkaDefaultNumPartitions(),
                brokers.size() + "");

        return valueDto;
    }

    public void shareTopic(Integer owningProjectId, String topicName,
            Integer projectId) throws AppException {

        if (owningProjectId.equals(projectId)) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Destination projet is topic owner");
        }

        ProjectTopics pt = em.find(ProjectTopics.class,
                new ProjectTopicsPK(topicName, owningProjectId));
        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic does not exist in database.");
        }

        SharedTopics sharedTopics = em.find(SharedTopics.class,
                new SharedTopicsPK(topicName, projectId));
        if (sharedTopics != null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Topic is already Shared to project.");
        }
        //persist shared topic to database
        SharedTopics st = new SharedTopics(topicName, owningProjectId, projectId);
        em.persist(st);
        em.flush();
    }

    public void unShareTopic(String topicName, Integer owningProjectId,
            Integer destProjectId) throws AppException {

        SharedTopics pt = em.find(SharedTopics.class,
                new SharedTopicsPK(topicName, destProjectId));
        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic is not shared to the project.");
        }

        em.remove(pt);
    }

    public void unShareTopic(String topicName, Integer ownerProjectId)
            throws AppException {

        SharedTopics pt = em.find(SharedTopics.class,
                new SharedTopicsPK(topicName, ownerProjectId));
        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic is not shared to the project.");
        }

        em.remove(pt);
        // remove the associated acl from database; 

    }

    public List<SharedProjectDTO> topicIsSharedTo(String topicName,
            Integer projectId) {

        List<SharedProjectDTO> shareProjectDtos = new ArrayList<>();

        TypedQuery<SharedTopics> query = em.createNamedQuery(
                "SharedTopics.findByTopicName", SharedTopics.class);
        query.setParameter("topicName", topicName);

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

    public List<AclUserDTO> aclUsers(Integer projectId, String topicName)
            throws AppException {

        List<AclUserDTO> aclUsers = new ArrayList<>();

        //contains project and its members
        Map<String, List<String>> projectMembers = new HashMap<>();

        //get the owner project name 
        Project project = em.find(Project.class, projectId);
        if (project == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "The owner project does not exist in database.");
        }

        projectMembers.put(project.getName(), new ArrayList<String>());

        //get all the projects this topic is shared with
        TypedQuery<SharedTopics> query = em.createNamedQuery(
                "SharedTopics.findByTopicName", SharedTopics.class);
        query.setParameter("topicName", topicName);

        for (SharedTopics sharedTopics : query.getResultList()) {
            project = em.find(Project.class, sharedTopics.getSharedTopicsPK()
                    .getProjectId());

            projectMembers.put(project.getName(), new ArrayList<String>());
        }

        //So far, we got the project names that this topic is shared with and
        // the owner. Next is to find all the user and filter all the 
        //users for the topic project which will be the acl users.
        TypedQuery<Project> projects = em.createNamedQuery(
                "Project.findAll", Project.class);
        Set<String> keySet = projectMembers.keySet();
        for (Project p : projects.getResultList()) {
            if (keySet.contains(p.getName())) {
                projectMembers.get(p.getName()).add(p.getOwner().getEmail());
            }
        }

        for (Map.Entry<String, List<String>> user : projectMembers.entrySet()) {
            aclUsers.add(new AclUserDTO(user.getKey(), user.getValue()));
        }

        return aclUsers;
    }

    public void addAclsToTopic(String topicName, Integer projectId, AclDTO dto)
            throws AppException {

        addAclsToTopic(topicName, projectId,
                dto.getProjectName(),
                dto.getUserEmail(), dto.getPermissionType(),
                dto.getOperationType(), dto.getHost(), dto.getRole());
    }

    private void addAclsToTopic(String topicName, Integer projectId,
            String selectedProjectName, String userEmail, String permission_type,
            String operation_type, String host, String role) throws AppException {

        //get the project id
        Project project = em.find(Project.class, projectId);
        if (project == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "The specified project for the topic is not in database");
        }

        ProjectTopics pt = em.find(ProjectTopics.class,
                new ProjectTopicsPK(topicName, projectId));
        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Topic does not belong to the project.");
        }

        //fetch the user name from database       
        TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
                Users.class).setParameter("email", userEmail);
        List<Users> users = query.getResultList();

        if (users == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "User does not exist.");
        }
        Users selectedUser = users.get(0);
        String principalName = selectedProjectName + TWO_UNDERSCROES
                + selectedUser.getUsername();

        TopicAcls ta = new TopicAcls(pt, selectedUser,
                permission_type, operation_type, host, role, principalName);

        em.persist(ta);
        em.flush();
    }

    public void removeAclsFromTopic(String topicName, Integer aclId)
            throws AppException {
        TopicAcls ta = em.find(TopicAcls.class, aclId);
        if (ta == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "aclId not found in database");
        }

        if (!ta.getProjectTopics().getProjectTopicsPK().getTopicName()
                .equals(topicName)) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "aclId does not belong the specified topic");
        }

        em.remove(ta);
    }

    public List<AclDTO> getTopicAcls(String topicName, Integer projectId)
            throws AppException {
        ProjectTopics pt = em.find(ProjectTopics.class,
                new ProjectTopicsPK(topicName, projectId));
        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic does not exist in database.");
        }

        TypedQuery<TopicAcls> query = em.createNamedQuery(
                "TopicAcls.findByTopicName", TopicAcls.class)
                .setParameter("topicName", topicName);
        List<TopicAcls> acls = query.getResultList();

        List<AclDTO> aclDtos = new ArrayList<>();
        String projectName;
        for (TopicAcls ta : acls) {
            projectName = ta.getPrincipal().split(TWO_UNDERSCROES)[0];
            aclDtos.add(new AclDTO(ta.getId(), projectName,
                    ta.getUser().getEmail(), ta.getPermissionType(),
                    ta.getOperationType(), ta.getHost(), ta.getRole()));
        }

        return aclDtos;
    }

    public void updateTopicAcl(Integer projectId, String topicName,
            Integer aclId, AclDTO aclDto) throws AppException {

        TopicAcls ta = em.find(TopicAcls.class, aclId);
        if (ta == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "aclId not found in database");
        }
        //remove previous acl
        em.remove(ta);
        //update acl
        //fetch the user name from database       
        TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
                Users.class).setParameter("email", aclDto.getUserEmail());
        List<Users> users = query.getResultList();

        if (users == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "User does not exist.");
        }
        Users selectedUser = users.get(0);
        String projectName = aclDto.getProjectName();
        String principalName = projectName + TWO_UNDERSCROES + selectedUser.getUsername();

        ta.setHost(aclDto.getHost());
        ta.setOperationType(aclDto.getOperationType());
        ta.setPermissionType(aclDto.getPermissionType());
        ta.setRole(aclDto.getRole());
        ta.setUser(selectedUser);
        ta.setPrincipal(principalName);

        TypedQuery<Project> queryProject = em.createNamedQuery(
                "Project.findByName", Project.class)
                .setParameter("name", projectName);
        List<Project> projects = queryProject.getResultList();

        if (projects == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "project does not exist.");
        }

        Project selectedProject = projects.get(0);
        ta.setProjectTopics(new ProjectTopics(
                new ProjectTopicsPK(topicName, selectedProject.getId())));

        em.persist(ta);
        em.flush();
    }

    //if schema exists, increment it if not start version from 1
    public void addSchemaForTopics(SchemaDTO schemaDto) {

        int maxVersion = 1;

        TypedQuery<SchemaTopics> query = em.createNamedQuery(
                "SchemaTopics.findByName", SchemaTopics.class);
        query.setParameter("name", schemaDto.getName());

        List<SchemaTopics> schemaTopics = query.getResultList();

        SchemaTopics schema = null;
        if (schemaTopics != null && !schemaTopics.isEmpty()) {
            for (SchemaTopics schemaTopic : schemaTopics) {

                int schemaVersion = schemaTopic.getSchemaTopicsPK().getVersion();
                if (maxVersion < schemaVersion) {
                    maxVersion = schemaVersion;
                }
            }
            maxVersion++;
        }
        if (schema == null) {
            schema = new SchemaTopics(schemaDto.getName(), maxVersion,
                    schemaDto.getContents(), new Date());
        }

        em.persist(schema);
        em.flush();
    }

    public SchemaDTO getSchemaForTopic(String topicName)
            throws AppException {

        List<ProjectTopics> topics = em.createNamedQuery(
                "ProjectTopics.findByTopicName", ProjectTopics.class)
                .setParameter("topicName", topicName).getResultList();

        if (topics == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "topic not found in database");
        }

        ProjectTopics topic = topics.get(0);

        SchemaTopics schema = em.find(SchemaTopics.class,
                new SchemaTopicsPK(
                        topic.getSchemaTopics().getSchemaTopicsPK().getName(),
                        topic.getSchemaTopics().getSchemaTopicsPK().getVersion()));

        if (schema == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "topic has not schema");
        }

        SchemaDTO schemaDto = new SchemaDTO(schema.getContents());

        return schemaDto;
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

            if (schemas.get(schemaName) == null) {
                schemas.put(schemaName, new ArrayList<Integer>());
            }

            schemas.get(schemaName).add(schema.getSchemaTopicsPK().getVersion());
        }
        for (Map.Entry<String, List<Integer>> schema : schemas.entrySet()) {
            schemaDtos.add(new SchemaDTO(schema.getKey(), schema.getValue()));
        }

        return schemaDtos;
    }

    public SchemaDTO getSchemaContent(String schemaName,
            Integer schemaVersion) throws AppException {

        SchemaTopics schemaTopic = em.find(SchemaTopics.class,
                new SchemaTopicsPK(schemaName, schemaVersion));
        if (schemaTopic == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Schema: " + schemaName + " not found in database");
        }

        SchemaDTO schemaDto = new SchemaDTO(schemaTopic.getContents());

        return schemaDto;
    }

    public void deleteSchema(String schemaName, Integer version)
            throws AppException {

        //get the bean and remove it
        SchemaTopics schema = em.find(SchemaTopics.class,
                new SchemaTopicsPK(schemaName, version));

        if (schema == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Schema: " + schemaName + " not found in database");
        }

        try {
            em.remove(schema);
        } catch (Exception ex) {
            throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
                    ex.getMessage());
        }
    }

    public Set<String> getBrokerEndpoints() throws AppException {

        Set<String> brokerList = new HashSet<>();

        try {
            zk = new ZooKeeper(settings.getZkConnectStr(),
                    sessionTimeoutMs, null);

            List<String> ids = zk.getChildren("/brokers/ids", false);
            for (String id : ids) {
                String brokerInfo = new String(zk.getData("/brokers/ids/" + id,
                        false, null));
                String[] tokens = brokerInfo.split(DLIMITER);
                for (String str : tokens) {
                    if (str.contains(SLASH_SEPARATOR)) {
                        brokerList.add(str);
                    }
                }
            }
        } catch (IOException ex) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Unable to find the zookeeper server: " + ex);
        } catch (KeeperException | InterruptedException ex) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Unable to retrieve seed brokers from the kafka cluster: " + ex);
        }finally{
            try {
                zk.close();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Cound not close the zookeeper connection: ", ex.toString());
            }
        }

        return brokerList;
    }

    public Set<String> getTopicList() throws Exception {

        CLIENT_ID = "list_topics";

        zkBrokerList = getBrokerEndpoints();

        for (String seed : zkBrokerList) {
            kafka.javaapi.consumer.SimpleConsumer simpleConsumer = null;
            try {
                simpleConsumer = new SimpleConsumer(getBrokerIp(seed)
                        .getHostAddress(),
                        getBrokerPort(seed), connectionTimeout, BUFFER_SIZE, CLIENT_ID);

                //add ssl certificate to the consumer here
                List<String> topics = new ArrayList<>();

                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = simpleConsumer.send(req);
                List<kafka.javaapi.TopicMetadata> topicMetadata = resp.topicsMetadata();

                for (kafka.javaapi.TopicMetadata item : topicMetadata) {
                    topicList.add(item.topic());
                }

            } catch (Exception ex) {
                throw new Exception("Error communicating to broker: " + ex);
            } finally {
                if (simpleConsumer != null) {
                    simpleConsumer.close();
                }
            }
        }

        //val allTopics = zkUtils.getAllTopics()
        return topicList;
    }

    private List<PartitionDetailsDTO> getTopicDetailsfromKafkaCluster(
            String topicName) throws Exception {

        CLIENT_ID = "topic_detail";

        zkBrokerList = getBrokerEndpoints();

        Map<Integer, List<String>> replicas = new HashMap<>();
        Map<Integer, List<String>> inSyncReplicas = new HashMap<>();
        Map<Integer, String> leaders = new HashMap<>();

        List<PartitionDetailsDTO> partitionDetailsDto = new ArrayList<>();
        PartitionDetailsDTO pd = new PartitionDetailsDTO();

        //Simple Consumer cannot connect to a secured kafka cluster,
        //try connnecting only to plaintext endpoints
        Iterator<String> iter = zkBrokerList.iterator();
        while (iter.hasNext()) {
            String seed = iter.next();
            if (seed.split(COLON_SEPARATOR)[0].equalsIgnoreCase(SECURITY_PROTCOL)) {
                iter.remove();
            }
        }

        for (String seed : zkBrokerList) {
            kafka.javaapi.consumer.SimpleConsumer simpleConsumer = null;
            try {
                simpleConsumer = new SimpleConsumer(getBrokerIp(seed)
                        .getHostAddress(),
                        getBrokerPort(seed), connectionTimeout, BUFFER_SIZE, CLIENT_ID);

                //add ssl certificate to the consumer here
                List<String> topics = new ArrayList<>();
                topics.add(topicName);

                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = simpleConsumer.send(req);
                List<kafka.javaapi.TopicMetadata> topicsMetadata = resp.topicsMetadata();

                for (kafka.javaapi.TopicMetadata metadata : topicsMetadata) {

                    for (kafka.javaapi.PartitionMetadata partitionMetadata : metadata.partitionsMetadata()) {
                        int partId = partitionMetadata.partitionId();

                        //list the leaders of each parition
                        leaders.put(partId, partitionMetadata.leader().host());

                        //list the replicas of the parition
                        replicas.put(partId, new ArrayList<String>());
                        for (kafka.cluster.BrokerEndPoint broker : partitionMetadata.replicas()) {
                            replicas.get(partId).add(broker.host());
                        }

                        //list the insync replicas of the parition
                        inSyncReplicas.put(partId, new ArrayList<String>());
                        for (kafka.cluster.BrokerEndPoint broker : partitionMetadata.isr()) {
                            inSyncReplicas.get(partId).add(broker.host());
                        }

                        partitionDetailsDto.add(new PartitionDetailsDTO(partId, leaders.get(partId),
                                replicas.get(partId), replicas.get(partId)));
                    }
                }
            } catch (Exception ex) {
                throw new Exception("Error communicating to broker: Kafka SimpleConsumer cant connect to secured cluster - " + seed, ex);
            } finally {
                if (simpleConsumer != null) {
                    simpleConsumer.close();
                }
            }
        }

        return partitionDetailsDto;
    }

    private InetAddress getBrokerIp(String str) {

        String endpoint = str.split(SLASH_SEPARATOR)[1];

        String ip = endpoint.split(COLON_SEPARATOR)[0];

        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException ex) {
            Logger.getLogger(KafkaFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private int getBrokerPort(String str) {

        String endpoint = str.split(SLASH_SEPARATOR)[1];

        String ip = endpoint.split(COLON_SEPARATOR)[1];
        return Integer.parseInt(ip);

    }

}
