package se.kth.hopsworks.rest;

import io.hops.kafka.AclDTO;
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
import io.hops.kafka.SharedProjectDTO;
import io.hops.kafka.TopicDetailsDTO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import se.kth.hopsworks.controller.ProjectDTO;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaService {

    private final static Logger logger = Logger.getLogger(KafkaService.class.
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
        kafkaFacade.createTopicInProject(this.project, topicDto.getName());

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
        JsonResponse json = new JsonResponse();
        if (projectId == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Incomplete request!");
        }

        TopicDetailsDTO topic = kafka.getTopicDetails(project, topicName);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                topic).build();
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

        kafkaFacade.shareTopic( this.projectId, topicName, projectId);
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
    
    @GET
    @Path("/{topic}/sharedwith")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response topicIsSharedTo( @PathParam("topic") String topicName,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException, Exception {

        List<SharedProjectDTO> projectDtoList = kafkaFacade.topicIsSharedTo(topicName, this.projectId);
        
        GenericEntity<List<SharedProjectDTO>> projectDtos
                = new GenericEntity<List<SharedProjectDTO>>(projectDtoList) {
        };

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                projectDtos).build();
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
            kafkaFacade.addAclsToTopic(topicName, project.getName(), aclDto);
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
            aclDto = kafka.getTopicAcls(topicName, projectId);
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
}
