package io.hops.hopsworks.api.app;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.ProjectService;
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
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.SecurityContext;

@Path("/appservice")
@Stateless
public class ApplicationService {

  final static Logger logger = Logger.getLogger(ApplicationService.class.
          getName());

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
  protected UserManager userManager;

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
      Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE, null,
              ex);
      return noCacheResponse.getNoCacheResponseBuilder(
              Response.Status.SERVICE_UNAVAILABLE).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            build();

  }

  //when do we need this api? It's used when the KafKa clients want to access
  // the schema for a give topic which a message is published to and consumerd from
  @POST
  @Path("schema")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
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

  private String checkAndGetProjectUser(byte[] keyStore, char[] keyStorePwd)
          throws AppException {
    try {
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      ByteArrayInputStream stream = new ByteArrayInputStream(keyStore);

      ks.load(stream, keyStorePwd);

      String projectUser = "";
      Enumeration<String> aliases = ks.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        if (!alias.equals("caroot")) {
          projectUser = alias;
          break;
        }
      }

      UserCerts userCert = certificateBean.findUserCert(hdfsUserBean.
              getProjectName(projectUser), hdfsUserBean.getUserName(projectUser));

      if (!Arrays.equals(userCert.getUserKey(), keyStore)) {
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Certificat error!");
      }
      return projectUser;
    } catch (IOException | CertificateException | KeyStoreException |
            NoSuchAlgorithmException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Certificat error!");
    }
  }

  private void assertAdmin(String projectUser) throws AppException {
    String user = hdfsUserBean.getUserName(projectUser);
    if (!userManager.findGroups(user).contains("HOPS_ADMIN")) {
      throw new AppException((Response.Status.UNAUTHORIZED.getStatusCode()),
              "only admins can call this function");
    }
  }
}
