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
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.security.HopsUtil;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertificatesController {
  private static final  Logger LOG = Logger.getLogger(CertificatesController.class.getName());
  
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private OpensslOperations opensslOperations;
  @Inject
  @Any
  private Instance<CertificateHandler> certificateHandlers;

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
      
      opensslOperations.createUserCertificate(project.getName(),
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
        opensslOperations.createServiceCertificate(project.getProjectGenericUser(),
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

    UserCerts uc = certsFacade.putUserCerts(project.getName(), user.getUsername(), encryptedKey);

    // Run custom certificateHandlers
    for (CertificateHandler certificateHandler : certificateHandlers) {
      certificateHandler.generate(project, user, uc);
    }

    return new AsyncResult<>(
        new CertsResult(project.getName(), user.getUsername()));
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void deleteProjectCertificates(Project project) throws CAException, IOException {
    String projectName = project.getName();
    ReentrantLock lock = certificatesMgmService.getOpensslLock();
    try {
      lock.lock();
      // Iterate through Project members and delete their certificates
      for (ProjectTeam team : project.getProjectTeamCollection()) {
        String certificateIdentifier = projectName + HdfsUsersController.USER_NAME_DELIMITER + team.getUser()
            .getUsername();
        // Ordering here is important
        // *First* revoke and *then* delete the certificate
        opensslOperations.revokeCertificate(certificateIdentifier, CertificateType.PROJECT_USER,
            false, false);
        opensslOperations.deleteUserCertificate(certificateIdentifier);

        // Run custom handlers
        for (CertificateHandler certificateHandler : certificateHandlers) {
          certificateHandler.revoke(project, team.getUser());
        }
      }
      opensslOperations.revokeCertificate(project.getProjectGenericUser(), CertificateType.PROJECT_USER,
          false, false);
      opensslOperations.deleteProjectCertificate(projectName);
    } finally {
      opensslOperations.createCRL(PKI.CAType.INTERMEDIATE);
      lock.unlock();
    }
    
    // Remove project generic certificates used by Spark interpreter in
    // Zeppelin. User specific certificates are removed by the foreign key
    // constraint in the DB
    certsFacade.removeProjectGenericCertificates(project.getProjectGenericUser());
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void deleteUserSpecificCertificates(Project project, Users user)
      throws CAException, IOException {
    String hdfsUsername = project.getName() + HdfsUsersController
        .USER_NAME_DELIMITER + user.getUsername();
    ReentrantLock lock = certificatesMgmService.getOpensslLock();
    try {
      lock.lock();
      // Ordering here is important
      // *First* revoke and *then* delete the certificate
      opensslOperations.revokeCertificate(hdfsUsername, CertificateType.PROJECT_USER, true, false);
      opensslOperations.deleteUserCertificate(hdfsUsername);
    } finally {
      lock.unlock();
    }
    certsFacade.removeUserProjectCerts(project.getName(), user.getUsername());

    // Run custom handlers
    for (CertificateHandler certificateHandler : certificateHandlers) {
      certificateHandler.revoke(project, user);
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String extractCNFromCertificate(byte[] rawKeyStore, char[]
      keyStorePwd) throws HopsSecurityException {
    return extractCNFromCertificate(rawKeyStore, keyStorePwd, null);
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String extractCNFromCertificate(byte[] rawKeyStore,
      char[] keystorePwd, String certificateAlias) throws HopsSecurityException {
    try {
      
      X509Certificate certificate = getCertificateFromKeyStore(rawKeyStore, keystorePwd, certificateAlias);
      if (certificate == null) {
        throw new GeneralSecurityException("Could not get certificate from keystore");
      }
      String subjectDN = certificate.getSubjectX500Principal()
          .getName("RFC2253");
      String cn = HopsUtil.extractCNFromSubject(subjectDN);
      if (cn == null) {
        throw new KeyStoreException("Could not extract CN from client certificate");
      }
      return cn;
    } catch (GeneralSecurityException | IOException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CN_EXTRACT_ERROR, Level.SEVERE,
        "certificateAlias: " + certificateAlias, ex.getMessage(), ex);
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String validateCertificate(byte[] rawKeyStore, char[] keyStorePassword)
      throws GeneralSecurityException, IOException {
  
    X509Certificate certificate = getCertificateFromKeyStore(rawKeyStore, keyStorePassword, null);
    if (certificate == null) {
      throw new GeneralSecurityException("Could not get certificate from keystore");
    }
  
    opensslOperations.validateCertificate(certificate, PKI.CAType.INTERMEDIATE);
    return certificate.getSubjectX500Principal().getName("RFC2253");
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
  
  private X509Certificate getCertificateFromKeyStore(byte[] rawKeyStore, char[] keyStorePwd, String certificateAlias)
    throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    InputStream inStream = new ByteArrayInputStream(rawKeyStore);
    keyStore.load(inStream, keyStorePwd);
  
    if (certificateAlias == null) {
      Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        certificateAlias = aliases.nextElement();
        if (!certificateAlias.equals("caroot")) {
          break;
        }
      }
    }
  
    return (X509Certificate) keyStore.getCertificate(certificateAlias.toLowerCase());
  }
}
