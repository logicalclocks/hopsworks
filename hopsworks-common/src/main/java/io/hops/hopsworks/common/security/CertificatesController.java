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
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertificatesController {
  private final static Logger LOG = Logger.getLogger
      (CertificatesController.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  /**
   * Creates x509 certificates for a project specific user and project generic
   * @param project Associated project
   * @param user Hopsworks user
   * @param generateProjectWideCerts Flag controlling whether it should create
   *                               project wide certificates in addition to
   *                               project specific user's. When adding a new
   *                               member to a project the flag should be false.
   * @return
   */
  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Future<CertsResult> generateCertificates(Project project, Users user,
      boolean generateProjectWideCerts) throws Exception {
    String userKeyPwd = HopsUtils.randomString(64);
    String encryptedKey = HopsUtils.encrypt(user.getPassword(), userKeyPwd,
        certificatesMgmService.getMasterEncryptionPassword());
    ReentrantLock lock = certificatesMgmService.getOpensslLock();
    try {
      lock.lock();
      LocalhostServices.createUserCertificates(settings.getIntermediateCaDir(),
          project.getName(),
          user.getUsername(),
          user.getAddress().getCountry(),
          user.getAddress().getCity(),
          user.getOrganization().getOrgName(),
          user.getEmail(),
          user.getOrcid(),
          userKeyPwd);
      LOG.log(Level.FINE, "Created project specific certificates for user: "
          + project.getName() + "__" + user.getUsername());
    } finally {
      lock.unlock();
    }
  
    // Project-wide certificates are needed because Zeppelin submits
    // requests as user: ProjectName__PROJECTGENERICUSER
    if (generateProjectWideCerts) {
      try {
        lock.lock();
        LocalhostServices.createServiceCertificates(settings.getIntermediateCaDir(),
            project.getProjectGenericUser(),
            user.getAddress().getCountry(),
            user.getAddress().getCity(),
            user.getOrganization().getOrgName(),
            user.getEmail(),
            user.getOrcid(),
            userKeyPwd);
      } finally {
        lock.unlock();
      }
      certsFacade.putProjectGenericUserCerts(project.getProjectGenericUser(), encryptedKey);
      LOG.log(Level.FINE, "Created project generic certificates for project: "
          + project.getName());
    }
  
    certsFacade.putUserCerts(project.getName(), user.getUsername(), encryptedKey);
    return new AsyncResult<>(
        new CertsResult(project.getName(), user.getUsername()));
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void deleteProjectCertificates(Project project) throws IOException {
    String projectName = project.getName();
    ReentrantLock lock = certificatesMgmService.getOpensslLock();
    try {
      lock.lock();
      LocalhostServices.deleteProjectCertificates(settings.getIntermediateCaDir(),
          projectName);
    } finally {
      lock.unlock();
    }
    
    // Remove project generic certificates used by Spark interpreter in
    // Zeppelin. User specific certificates are removed by the foreign key
    // constraint in the DB
    certsFacade.removeProjectGenericCertificates(project.getProjectGenericUser());
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void deleteUserSpecificCertificates(Project project, Users user)
      throws IOException {
    String hdfsUsername = project.getName() + HdfsUsersController
        .USER_NAME_DELIMITER + user.getUsername();
    ReentrantLock lock = certificatesMgmService.getOpensslLock();
    try {
      lock.lock();
      LocalhostServices.deleteUserCertificates(settings.getIntermediateCaDir(),
          hdfsUsername);
    } finally {
      lock.unlock();
    }
    certsFacade.removeUserProjectCerts(project.getName(), user.getUsername());
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String extractCNFromCertificate(byte[] rawKeyStore, char[]
      keyStorePwd) throws AppException {
    return extractCNFromCertificate(rawKeyStore, keyStorePwd, null);
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String extractCNFromCertificate(byte[] rawKeyStore,
      char[] keystorePwd, String certificateAlias) throws AppException {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream inStream = new ByteArrayInputStream(rawKeyStore);
      keyStore.load(inStream, keystorePwd);
      
      if (certificateAlias == null) {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
          certificateAlias = aliases.nextElement();
          if (!certificateAlias.equals("caroot")) {
            break;
          }
        }
      }
      
      X509Certificate certificate = (X509Certificate) keyStore
          .getCertificate(certificateAlias.toLowerCase());
      String subjectDN = certificate.getSubjectX500Principal()
          .getName("RFC2253");
      String[] dnTokens = subjectDN.split(",");
      String[] cnTokens = dnTokens[0].split("=", 2);
      
      return cnTokens[1];
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException
        | CertificateException ex) {
      LOG.log(Level.SEVERE, "Error while extracting CN from certificate", ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR
          .getStatusCode(), ex.getMessage());
    }
  }
  
  public class CertsResult {
    private final String projectName;
    private final String username;
    
    public CertsResult(String projectName, String username) {
      this.projectName = projectName;
      this.username = username;
    }
    
    public String getProjectName() {
      return projectName;
    }
    
    public String getUsername() {
      return username;
    }
  }
}
