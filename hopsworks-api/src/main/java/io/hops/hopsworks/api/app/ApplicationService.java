package io.hops.hopsworks.api.app;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
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
import io.hops.hopsworks.common.dao.app.TopicJsonDTO;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.CertificatesController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.SecurityContext;

@Path("/appservice")
@Stateless
@Api(value = "Application Service",
    description = "Application Service")
public class ApplicationService {

  final static Logger LOGGER = Logger.getLogger(ApplicationService.class.
      getName());
  
  private final Pattern projectGenericPattern = Pattern.compile("\\w*" +
      Settings.PROJECT_GENERIC_USER_SUFFIX);

  private final Pattern projectSpecificPattern = Pattern.compile("\\w*"+
      HdfsUsersController.USER_NAME_DELIMITER + "\\w*");

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
  private ProjectController projectController;
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;
  @EJB
  private CertificatesController certificatesController;

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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
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

  @GET
  @Path("/certpw")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response getCertPw(@QueryParam("keyStore") String keyStore, @QueryParam("projectUser") String projectUser,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    
    try {
      CertPwDTO respDTO = null;
      Users user;
      if (projectGenericPattern.matcher(projectUser).matches()) {
        // In that case projectUser is the projectName__PROJECTGENERICUSER.
        // It is used by the Spark interpreter of Zeppelin which runs as user Project
        String[] splits = projectUser.split(HdfsUsersController.USER_NAME_DELIMITER,2);
        user = projectFacade.findByName(splits[0]).getOwner();
        respDTO = projectController.getProjectWideCertPw(user, projectUser, keyStore);
      } else if (projectSpecificPattern.matcher(projectUser).matches()){
        //Find user
        String username = hdfsUserBean.getUserName(projectUser);
        String projectName = hdfsUserBean.getProjectName(projectUser);
        user = userFacade.findByUsername(username);
        respDTO = projectController.getProjectSpecificCertPw(user, projectName, keyStore);
      } else {
        // Project user doesn't match any standard format.
        // Something fishy is going on here. Throw an exception.
        throw new Exception("Unrecognized format for project user");
      }

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(respDTO).build();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Could not retrieve certificate passwords for " +
          "user:" + projectUser, ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
    }
  }

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
    String commonName = certificatesController.extractCNFromCertificate
        (keyStore, keyStorePwd);
  
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
