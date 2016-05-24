package se.kth.hopsworks.rest;

import io.hops.kafka.AclDTO;
import io.hops.kafka.HdfsUserDTO;
import io.hops.kafka.TopicDTO;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.users.UserFacade;
import io.hops.kafka.KafkaFacade;
import io.hops.kafka.PartitionDetailsDTO;
import io.hops.kafka.SchemaDTO;
import io.hops.kafka.SharedProjectDTO;
import io.hops.kafka.TopicDefaultValueDTO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaService {

    private final static Logger LOGGER = Logger.getLogger(KafkaService.class.
            getName());

    @EJB
    private ProjectFacade projectFacade;
    @EJB
    private ActivityFacade activityFacade;
    @EJB
    private UserManager userBean;
    @EJB
    private NoCacheResponse noCacheResponse;
    @EJB
    private AsynchronousJobExecutor async;
    @EJB
    private UserFacade userfacade;
    @EJB
    private KafkaFacade kafkaFacade;
    @EJB
    private Settings settings;
    @EJB
    private KafkaFacade kafka;

    private Integer projectId;
    private Project project;
    private String path;

    public KafkaService() {
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
        this.project = this.projectFacade.find(projectId);
        String projectPath = settings.getProjectPath(this.project.getName());
        this.path = projectPath + File.separator;
    }

    public Integer getProjectId() {
        return projectId;
    }

    /**
     * Gets the list of topics for this project
     *
     * @param sc
     * @param req
     * @return
     * @throws AppException
     */
    @GET
    @Path("/topics")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response getTopics(@Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<TopicDTO> listTopics = kafka.findTopicsByProject(projectId);
        GenericEntity<List<TopicDTO>> topics
                = new GenericEntity<List<TopicDTO>>(listTopics) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                topics).build();
    }

    @GET
    @Path("/sharedTopics")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response getSharedTopics(@Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<TopicDTO> listTopics = kafka.findSharedTopicsByProject(projectId);
        GenericEntity<List<TopicDTO>> topics
                = new GenericEntity<List<TopicDTO>>(listTopics) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                topics).build();
    }

    @POST
    @Path("/topic/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response createTopic(TopicDTO topicDto,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }
        //create the topic in the database and the Kafka cluster
        kafkaFacade.createTopicInProject(this.projectId, topicDto);

        json.setSuccessMessage("The Topic has been created.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @DELETE
    @Path("/topic/{topic}/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response removeTopic(@PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }
        //remove the topic from the database and Kafka cluster
        kafkaFacade.removeTopicFromProject(this.project, topicName);

        json.setSuccessMessage("The topic has been removed.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @GET
    @Path("/details/{topic}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response getDetailsTopic(@PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<PartitionDetailsDTO> topic = kafka.getTopicDetails(project, topicName);

        GenericEntity<List<PartitionDetailsDTO>> topics
                = new GenericEntity<List<PartitionDetailsDTO>>(topic) {
        };

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                topics).build();
    }

    @GET
    @Path("/topic/defaultValues")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response topicDefaultValues(
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        TopicDefaultValueDTO values
                = kafkaFacade.topicDefaultValues();

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                values).build();
    }

    @GET
    @Path("/topic/{topic}/share/{projId}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response shareTopic(
            @PathParam("topic") String topicName,
            @PathParam("projId") int projectId,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();
        if (this.projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        kafkaFacade.shareTopic(this.projectId, topicName, projectId);
        json.setSuccessMessage("The topic has been shared.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @DELETE
    @Path("/topic/{topic}/unshare/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response unShareTopic(
            @PathParam("topic") String topicName,
            @PathParam("projectId") int projectId,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        kafkaFacade.unShareTopic(topicName, this.projectId, projectId);
        json.setSuccessMessage("Topic has been removed from shared.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @DELETE
    @Path("/topic/{topic}/unshare")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response unShareTopicFromProject(
            @PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        kafkaFacade.unShareTopic(topicName, this.projectId);
        json.setSuccessMessage("Topic has been removed from shared.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @GET
    @Path("/{topic}/sharedwith")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response topicIsSharedTo(@PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        List<SharedProjectDTO> projectDtoList = kafkaFacade.topicIsSharedTo(topicName, this.projectId);

        GenericEntity<List<SharedProjectDTO>> projectDtos
                = new GenericEntity<List<SharedProjectDTO>>(projectDtoList) {
        };

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                projectDtos).build();
    }

    @GET
    @Path("/aclUsers/topic/{topicName}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response aclUsers(@PathParam("topicName") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<HdfsUserDTO> aclUsersDtos
                = kafkaFacade.aclUsers(projectId, topicName);

        GenericEntity<List<HdfsUserDTO>> aclUsers
                = new GenericEntity<List<HdfsUserDTO>>(aclUsersDtos) {
        };

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                aclUsers).build();
    }

    @POST
    @Path("/topic/{topic}/addAcl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response addAclsToTopic(@PathParam("topic") String topicName,
            AclDTO aclDto, @Context SecurityContext sc, @Context HttpServletRequest req)
            throws AppException, Exception {
        JsonResponse json = new JsonResponse();
        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        try {
            kafkaFacade.addAclsToTopic(topicName, projectId, aclDto);
        } catch (Exception e) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Problem adding ACL to topic.");
        }

        json.setSuccessMessage("ACL has been added to the topic.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @DELETE
    @Path("/topic/{topic}/removeAcl/{aclId}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response removeAclsFromTopic(@PathParam("topic") String topicName,
            @PathParam("aclId") int aclId,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();
        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        try {
            kafkaFacade.removeAclsFromTopic(topicName, aclId);
        } catch (Exception e) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Topic acl not found in database");
        }

        json.setSuccessMessage("Topic acls has been removed.");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @GET
    @Path("/acls/{topic}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response getTopicAcls(@PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }
        List<AclDTO> aclDto = null;
        try {
            aclDto = kafkaFacade.getTopicAcls(topicName, projectId);
        } catch (Exception e) {
        }
        if (aclDto == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "Topic has not ACLs");
        }

        GenericEntity<List<AclDTO>> aclDtos
                = new GenericEntity<List<AclDTO>>(aclDto) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(aclDtos).build();
    }

    @PUT
    @Path("/topic/{topic}/updateAcl/{aclId}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response updateTopicAcls(@PathParam("topic") String topicName,
            @PathParam("aclId") String aclId, AclDTO aclDto,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        kafkaFacade.updateTopicAcl(projectId, Integer.parseInt(aclId), aclDto);

        json.setSuccessMessage("TopicAcl updated successfuly");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @POST
    @Path("/schema/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response updateTopicSchema(SchemaDTO schamaData,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        kafkaFacade.updateSchemaForTopics(schamaData);

        json.setSuccessMessage("Schema for Topic created/updated successfuly");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    //when do we need this api?
    @GET
    @Path("/schema/{topic}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response getSchemaForTopics(@PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<SchemaDTO> schemaDto = kafka.getSchemaForTopic(topicName);
        GenericEntity<List<SchemaDTO>> schema
                = new GenericEntity<List<SchemaDTO>>(schemaDto) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                schema).build();
    }

    @GET
    @Path("/schemas")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response listSchemasForTopics(
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        List<SchemaDTO> schemaDtos = kafka.listSchemasForTopics();
        GenericEntity<List<SchemaDTO>> schemas
                = new GenericEntity<List<SchemaDTO>>(schemaDtos) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                schemas).build();
    }

    @DELETE
    @Path("/schema/{schema}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response deleteSchemaForTopics(@PathParam("schema") String schemaName,
            @PathParam("version") Integer version,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {
        JsonResponse json = new JsonResponse();

        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        kafka.deleteSchemaForTopics(schemaName, version);
        json.setSuccessMessage("Schema for topic removed successfuly");

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }
    
}
