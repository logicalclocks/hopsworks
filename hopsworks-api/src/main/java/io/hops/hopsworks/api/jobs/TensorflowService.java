package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.NoCacheResponse;
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
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorflowFacade;
import io.hops.hopsworks.common.dao.tensorflow.TfResourceCluster;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.util.Settings;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
//import org.apache.avro.Schema;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorflowService {

  private final static Logger LOGGER = Logger.getLogger(TensorflowService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
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
  private TensorflowFacade tf;

  private Integer projectId;
  private Project project;
  private String path;

  public TensorflowService() {
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
  @Path("/programs")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getPrograms(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException, Exception {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

//        List<TopicDTO> listTopics = tf.findProgramsByProject(projectId);
    GenericEntity<List<TopicDTO>> programs
            = new GenericEntity<List<TopicDTO>>(null) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            programs).build();
  }

  @POST
  @Path("/cpu/allocate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response allocateResources(TfResourceCluster resourceReq,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    //create the topic in the database and the Kafka cluster
//        kafkaFacade.createTopicInProject(this.projectId, tfDto);

    json.setSuccessMessage("The Topic has been created.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/cpu/{program}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response freeResources(@PathParam("program") String topicName,
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
  @Path("/details/{program}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getTopicDetails(@PathParam("program") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException, Exception {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            null).build();
  }

}
