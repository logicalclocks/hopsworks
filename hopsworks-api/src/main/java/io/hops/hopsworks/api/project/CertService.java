package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * <p>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertService {

  private final static Logger LOGGER = Logger.getLogger(CertService.class.getName());
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private HdfsUsersController hdfsUserBean;

  private Project project;

  public CertService setProject(Project project) {
    this.project = project;
    return this;
  }

  @GET
  @Path("/certpw")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getCertPw(@QueryParam("keyStore") String keyStore,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    //Find user
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    try {
      //Compare the sent certificate with the one in the database
      String keypw = HopsUtils.decrypt(user.getPassword(), settings.getHopsworksMasterPasswordSsl(), certsFacade.
          findUserCert(project.getName(), user.getUsername()).getUserKeyPwd());
      validateCert(Base64.decodeBase64(keyStore), keypw.toCharArray());

      CertPwDTO respDTO = new CertPwDTO();
      respDTO.setKeyPw(keypw);
      respDTO.setTrustPw(settings.getHopsworksMasterPasswordSsl());
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(respDTO).build();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Could not retrieve certificate passwords for user:" + user.getUsername(), ex);
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
  private void validateCert(byte[] keyStore, char[] keyStorePwd)
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

      UserCerts userCert = certsFacade.findUserCert(hdfsUserBean.
          getProjectName(projectUser), hdfsUserBean.getUserName(projectUser));

      if (!Arrays.equals(userCert.getUserKey(), keyStore)) {
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
            "Certificate error!");
      }
    } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Certificate error!");
    }
  }

}
