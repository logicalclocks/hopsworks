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

package io.hops.hopsworks.api.app;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.app.EmailJsonDTO;
import io.hops.hopsworks.common.dao.app.JobWorkflowDTO;
import io.hops.hopsworks.common.dao.app.KeystoreDTO;
import io.hops.hopsworks.common.dao.app.TopicJsonDTO;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.security.HopsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/appservice")
@Stateless
@Api(value = "Application Service",
    description = "Application Service")
public class ApplicationService {

  private static final Logger LOGGER = Logger.getLogger(ApplicationService.class.getName());

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
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;

  @POST
  @Path("mail")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response sendEmail(@Context SecurityContext sc,
      @Context HttpServletRequest req, EmailJsonDTO mailInfo) throws UserException {
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
      @Context HttpServletRequest req, TopicJsonDTO topicInfo) throws UserException {
    String projectUser = checkAndGetProjectUser(topicInfo.getKeyStoreBytes(),
        topicInfo.getKeyStorePwd().toCharArray());

    SchemaDTO schemaDto = kafka.getSchemaForTopic(topicInfo.getTopicName());
    if(schemaDto != null) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(schemaDto).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).
        entity(schemaDto).build();
    }
  }

  @POST
  @Path("notebook")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response versionNotebook(@Context HttpServletRequest req,
                                  KeystoreDTO keystoreDTO) throws UserException {

    String projectUser = checkAndGetProjectUser(keystoreDTO.
            getKeyStoreBytes(), keystoreDTO.getKeyStorePwd().toCharArray());

    //check if user is member of project

    Users user = userFacade.findByUsername(hdfsUserBean.getUserName(projectUser));
    String username = user.getUsername();

    Project project = projectFacade.findByName(hdfsUserBean.getProjectName(
            projectUser));

    Collection<HdfsUsers> hdfsUsers = hdfsUsersFacade.findProjectUsers(project.getName());
    if(hdfsUsers == null) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    HdfsUsers jupyterHdfsUser = null;
    for(HdfsUsers hdfsUser: hdfsUsers) {
      if(hdfsUser.getUsername().equals(username)) {
        jupyterHdfsUser = hdfsUser;
      }
    }

    Collection<JupyterProject> jps = project.getJupyterProjectCollection();
    for(JupyterProject jp: jps) {
      if(jp.getHdfsUserId() == jupyterHdfsUser.getId()) {

        JSONObject obj = new JSONObject(ClientBuilder.newClient()
                .target(settings.getRestEndpoint() + "/hopsworks-api/jupyter/" + jp.getPort() + "/api/sessions")
                .request()
                .method("GET")
                .readEntity(String.class));
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  /////////////////////////////////////////////////
  //Endpoints that act as access point of HopsUtil or other services to create job workflows
  @POST
  @Path("jobs/executions")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Submit IDs of jobs to start")
  public Response startJobs(@Context SecurityContext sc,
      @Context HttpServletRequest req, JobWorkflowDTO jobsDTO) throws GenericException, UserException, JobException {
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
      executionController.start(job, user);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("jobs")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Retrieve jobs state")
  public Response getJobsWithRunningState(@Context SecurityContext sc,
      @Context HttpServletRequest req, JobWorkflowDTO jobsDTO) throws UserException {
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
   */
  private String checkAndGetProjectUser(byte[] keyStore, char[] keyStorePwd) throws UserException {
    
    try {
      String dn = certificatesController.validateCertificate(keyStore, keyStorePwd);
      String commonName = HopsUtil.extractCNFromSubject(dn);
  
      UserCerts userCert = certificateBean.findUserCert(hdfsUserBean.
          getProjectName(commonName), hdfsUserBean.getUserName(commonName));
  
      if (userCert.getUserKey() == null || userCert.getUserKey().length == 0) {
        throw new GeneralSecurityException("Could not find certificates for user " + commonName);
      }
      
      String username = hdfsUserBean.getUserName(commonName);
      Users user = userFacade.findByUsername(username);
      if (user == null) {
        throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE, "user: " + commonName);
      }
      
      String decryptedPassword = HopsUtils.decrypt(user.getPassword(), userCert.getUserKeyPwd(),
          certificatesMgmService.getMasterEncryptionPassword());
      
      String storedCN = certificatesController.extractCNFromCertificate(userCert.getUserKey(),
          decryptedPassword.toCharArray(), commonName);
      if (!storedCN.equals(commonName)) {
        throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE, "user: " + commonName);
      }
      
      return commonName;
    } catch (Exception ex) {
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.SEVERE, null, ex.getMessage());
    }
  }

  private void assertAdmin(String projectUser) throws UserException {
    String username = hdfsUserBean.getUserName(projectUser);
    Users user = userFacade.findByUsername(username);
    if (!usersController.isUserInRole(user, "HOPS_ADMIN")) {
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE,
        "Method can be only be invoked by an admin");
    }
  }
}
