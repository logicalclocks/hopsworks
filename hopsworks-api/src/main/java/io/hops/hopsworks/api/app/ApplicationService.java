/*
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
 *
 */

package io.hops.hopsworks.api.app;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.app.ServingEndpointJsonDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
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
import io.hops.hopsworks.common.dao.tfserving.TfServing;
import io.hops.hopsworks.common.dao.tfserving.TfServingFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.user.UsersController;
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
  private UsersController usersController;
  @EJB
  private CertificatesController certificatesController;
  @EJB
  private ExecutionController executionController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private TfServingFacade tfServingFacade;

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

  @POST
  @Path("tfserving")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServingEndpoint(@Context SecurityContext sc,
                            @Context HttpServletRequest req, ServingEndpointJsonDTO servingEndpointJsonDTO) throws
          AppException {

    String projectUser = checkAndGetProjectUser(servingEndpointJsonDTO.
            getKeyStoreBytes(), servingEndpointJsonDTO.getKeyStorePwd().toCharArray());

    String projectName = servingEndpointJsonDTO.getProject();
    String model = servingEndpointJsonDTO.getModel();

    //check if user is member of project
    Project project = projectFacade.findByName(hdfsUserBean.getProjectName(
            projectUser));

    if(project != null && project.getName().equals(projectName)) {

      String host = null;
      String port = null;
      List <TfServing> tfServings = tfServingFacade.findForProject(project);
      for(TfServing tfServing: tfServings) {
        if(tfServing.getModelName().equals(model)) {
          host = tfServing.getHostIp();
          port = tfServing.getPort().toString();
          break;
        }
      }

      JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
      if(host != null && port != null) {
        arrayObjectBuilder.add("host", host);
        arrayObjectBuilder.add("port", port);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(arrayObjectBuilder.build()).build();
      } else {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
      }
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.UNAUTHORIZED).
              build();
    }
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
    Project project = projectFacade.findByName(projectUser.split(Settings.DOUBLE_UNDERSCORE)[0]);
    //Get the jobs to run, if the user is not the creator, run no jobs and return error message
    List<Jobs> jobsToRun = new ArrayList<>();
    for (Integer jobId : jobsDTO.getJobIds()) {
      Jobs job = jobFacade.findById(jobId);
      if (!job.getProject().equals(project) || !job.getCreator().equals(user)) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.UNAUTHORIZED).entity(
            "User is not authorized to start some of the requested jobs").build();
      }
      jobsToRun.add(job);
    }
    for (Jobs job : jobsToRun) {
      try {
        executionController.start(job, user);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "An error occured while starting job with id:" + job.getId()).build();
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
   * @param keyStore project-user keystore
   * @param keyStorePwd project-user password
   * @return CN of certificate
   * @throws AppException When user is not authorized to access project.
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
    String username = hdfsUserBean.getUserName(projectUser);
    Users user = userFacade.findByUsername(username);
    if (!usersController.isUserInRole(user, "HOPS_ADMIN")) {
      throw new AppException((Response.Status.UNAUTHORIZED.getStatusCode()),
          "only admins can call this function");
    }
  }
}
