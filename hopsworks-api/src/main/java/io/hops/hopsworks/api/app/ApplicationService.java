package io.hops.hopsworks.api.app;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.dao.app.EmailJsonDTO;
import io.hops.hopsworks.common.dao.app.JobWorkflowDTO;
import io.hops.hopsworks.common.dao.app.TopicJsonDTO;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.SecurityContext;

@Path("/appservice")
@Stateless
@Api(value = "Application Service",
    description = "Application Service")
public class ApplicationService {

  final static Logger LOGGER = Logger.getLogger(ApplicationService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private EmailBean email;
  @EJB
  private KafkaFacade kafka;
  @EJB
  private CertsFacade certificateBean;
  @EJB
  private HdfsUsersController hdfsUserBean;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UserManager userManager;
  @EJB
  private CertificatesController certificatesController;
  @EJB
  private ExecutionController executionController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private ExecutionFacade executionFacade;

  @POST
  @Path("mail")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response sendEmail(@Context SecurityContext sc,
      @Context HttpServletRequest req, EmailJsonDTO mailInfo) throws
      AppException {
    String projectUser = checkAndGetProjectUser(mailInfo.
        getKeyStoreBytes(), mailInfo.getKeyStorePwd().toCharArray());

    assertAdmin(projectUser);

    String dest = mailInfo.getDest();
    String subject = mailInfo.getSubject();
    String message = mailInfo.getMessage();

    try {
      email.sendEmail(dest, Message.RecipientType.TO, subject, message);
    } catch (MessagingException ex) {
      Logger.getLogger(ApplicationService.class.getName()).log(Level.SEVERE, null,
          ex);
      return noCacheResponse.getNoCacheResponseBuilder(
          Response.Status.SERVICE_UNAVAILABLE).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        build();

  }

  //when do we need this endpoint? It's used when the Kafka clients want to access
  // the schema for a given topic which a message is being published to and consumed from
  @POST
  @Path("schema")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSchemaForTopics(@Context SecurityContext sc,
      @Context HttpServletRequest req, TopicJsonDTO topicInfo) throws
      AppException {
    String projectUser = checkAndGetProjectUser(topicInfo.getKeyStoreBytes(),
        topicInfo.getKeyStorePwd().toCharArray());

    Project project = projectFacade.findByName(hdfsUserBean.getProjectName(
        projectUser));

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }

    SchemaDTO schemaDto = kafka.getSchemaForTopic(topicInfo.getTopicName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(schemaDto).build();

  }

  /////////////////////////////////////////////////
  //Endpoints that act as access point of HopsUtil or other services to create job workflows
  @POST
  @Path("jobs/executions")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Submit IDs of jobs to start")
  public Response startJobs(@Context SecurityContext sc,
      @Context HttpServletRequest req, JobWorkflowDTO jobsDTO) throws AppException {

    String projectUser = checkAndGetProjectUser(jobsDTO.getKeyStoreBytes(), jobsDTO.getKeyStorePwd().toCharArray());
    Users user = userFacade.findByUsername(hdfsUserBean.getUserName(projectUser));
    for (Integer jobId : jobsDTO.getJobIds()) {
      Jobs job = jobFacade.findById(jobId);
      try {
        executionController.start(job, user);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("jobs")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Retrieve jobs state")
  public Response getJobsWithRunningState(@Context SecurityContext sc,
      @Context HttpServletRequest req, JobWorkflowDTO jobsDTO) throws AppException {
    String projectUser = checkAndGetProjectUser(jobsDTO.getKeyStoreBytes(), jobsDTO.getKeyStorePwd().toCharArray());
    Project project = projectFacade.findByName(projectUser.split(Settings.DOUBLE_UNDERSCORE)[0]);
    List<Jobs> jobsRunning = jobFacade.getRunningJobs(project, projectUser, jobsDTO.getJobIds());
    List<Integer> jobIds = new ArrayList<>();
    for (Jobs job : jobsRunning) {
      jobIds.add(job.getId());
    }
    JobWorkflowDTO jobsrRunningDTO = new JobWorkflowDTO();
    jobsrRunningDTO.setJobIds(jobIds);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jobsrRunningDTO).build();
  }
  /////////////////////////////////////////////////

  /**
   * Returns the project user from the keystore and verifies it.
   *
   * @param keyStore
   * @param keyStorePwd
   * @return
   * @throws AppException
   */
  private String checkAndGetProjectUser(byte[] keyStore, char[] keyStorePwd)
      throws AppException {
    String commonName = certificatesController.extractCNFromCertificate(keyStore, keyStorePwd);

    UserCerts userCert = certificateBean.findUserCert(hdfsUserBean.
        getProjectName(commonName), hdfsUserBean.getUserName(commonName));

    if (!Arrays.equals(userCert.getUserKey(), keyStore)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "Certificate error!");
    }
    return commonName;
  }

  private void assertAdmin(String projectUser) throws AppException {
    String user = hdfsUserBean.getUserName(projectUser);
    if (!userManager.findGroups(user).contains("HOPS_ADMIN")) {
      throw new AppException((Response.Status.UNAUTHORIZED.getStatusCode()),
          "only admins can call this function");
    }
  }
}
