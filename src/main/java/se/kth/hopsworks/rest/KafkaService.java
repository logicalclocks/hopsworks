package se.kth.hopsworks.rest;

import io.hops.kafka.KafkaFacade;
import io.hops.hdfs.HdfsLeDescriptorsFacade;
import io.hops.kafka.KafkaFacade;
import io.hops.kafka.ProjectTopics;
import io.hops.kafka.TopicDTO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.ErasureCodeJob;
import se.kth.bbc.fileoperations.ErasureCodeJobConfiguration;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.DatasetController;
import se.kth.hopsworks.controller.FileTemplateDTO;
import se.kth.hopsworks.controller.JobController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.dataset.DatasetRequest;
import se.kth.hopsworks.dataset.DatasetRequestFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import io.hops.kafka.KafkaFacade;
import io.hops.kafka.ProjectTopics;
import io.hops.kafka.TopicDetailDTO;
import org.mortbay.util.ajax.JSON;
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
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response findTopicsInProjectID(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    List<TopicDTO> listTopics = kafka.findTopicsByProject(this.project);
    GenericEntity<List<TopicDTO>> topics
        = new GenericEntity<List<TopicDTO>>(listTopics) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(topics).build();
  }

  @GET
  @Path("/create/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createTopic(@PathParam("topic") String topicName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    //create the topic in the database and the Kafka cluster
    kafkaFacade.createTopicInProject(this.project, topicName); 
    
    json.setSuccessMessage("The Topic has been created.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }

  @GET
  @Path("/remove/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeTopic(@PathParam("name") String topicName,
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
  public Response getDetailsTopic(@PathParam("name") String topicName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    TopicDetailDTO topic = kafka.getTopicDetails(project, topicName);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        topic).build();
  }
  
 
  @GET
  @Path("/share/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response shareTopic(@PathParam("name") String topicName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    kafkaFacade.shareTopicToProject(topicName, project);
    json.setSuccessMessage("The topic has been shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }
  
  @GET
  @Path("/removeshare/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response removeSharedTopic(@PathParam("name") String topicName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    kafkaFacade.removeSharedTopicFromProject(topicName, project);
    json.setSuccessMessage("Topic has been removed from shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }
  
  @GET
  @Path("/addtopicacls/{acls}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response addAclsToTopic(@PathParam("name") String topicName,
          @PathParam("username") String userName,
          @PathParam("permission_type") String permissionType,
          @PathParam("operation_type") String operationType,
          @PathParam("host") String host,
          @PathParam("role") String role,
          @PathParam("shared") String shared,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    kafkaFacade.addAclsToTopic(topicName, userName, permissionType,
            operationType, host, role, shared);
    
    json.setSuccessMessage("Topic acls has been added.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }
  
  @GET
  @Path("/removetopicacls/{acls}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response removeAclsToTopic(@PathParam("name") String topicName,
          @PathParam("username") String userName,
          @PathParam("permission_type") String permissionType,
          @PathParam("operation_type") String operationType,
          @PathParam("host") String host,
          @PathParam("role") String role,
          @PathParam("shared") String shared,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    kafkaFacade.removeAclsFromTopic(topicName, userName, permissionType,
            operationType, host, role, shared);
    
    json.setSuccessMessage("Topic acls has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }
}
