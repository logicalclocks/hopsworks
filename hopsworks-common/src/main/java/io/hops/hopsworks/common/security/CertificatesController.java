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
import io.hops.hopsworks.persistence.entity.certificates.UserCerts;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.proxies.CAProxy;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.HopsUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CertificatesController {
  private static final Logger LOGGER = Logger.getLogger(CertificatesController.class.getName());

  private final static String SECURITY_PROVIDER = "BC";
  private final static String KEY_ALGORITHM = "RSA";
  private final static String SIGNATURE_ALGORITHM = "SHA256withRSA";
  private final static String CERTIFICATE_TYPE = "X.509";
  private final static int KEY_SIZE = 1024;

  @EJB
  private CertsFacade certsFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private Settings settings;
  @Inject
  @Any
  private Instance<CertificateHandler> certificateHandlers;
  @EJB
  private CAProxy caProxy;

  private KeyPairGenerator keyPairGenerator = null;
  private CertificateFactory certificateFactory = null;

  private enum Endpoint {
    PROJECT("project"),
    DELA("dela");

    private final String endpointPath;

    Endpoint(String endpointPath) {
      this.endpointPath = endpointPath;
    }

    @Override
    public String toString() {
      return endpointPath;
    }
  }

  @PostConstruct
  public void init() {
    Security.addProvider(new BouncyCastleProvider());
    try {
      keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
      keyPairGenerator.initialize(KEY_SIZE);

      certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not initialize the key generator", e);
    }
  }

  /**
   * Creates x509 certificates for a project specific user and project generic
   *
   * @param project                  Associated project
   * @param user                     Hopsworks user
   * @return
   */
  @Asynchronous
  public Future<CertsResult> generateCertificates(Project project, Users user) throws Exception {
    String userKeyPwd = HopsUtils.randomString(64);
    String encryptedKey = HopsUtils.encrypt(user.getPassword(), userKeyPwd,
        certificatesMgmService.getMasterEncryptionPassword());

    Pair<KeyStore, KeyStore> userKeystores =
        generateStores(project.getName() + Settings.HOPS_USERNAME_SEPARATOR + user.getUsername(),
            userKeyPwd, Endpoint.PROJECT);

    UserCerts uc = certsFacade.putUserCerts(project.getName(), user.getUsername(),
      convertKeystoreToByteArray(userKeystores.getValue0(), userKeyPwd),
      convertKeystoreToByteArray(userKeystores.getValue1(), userKeyPwd),
      encryptedKey);

    // Run custom certificateHandlers
    for (CertificateHandler certificateHandler : certificateHandlers) {
      certificateHandler.generate(project, user, uc);
    }

    LOGGER.log(Level.FINE, "Created project generic certificates for project: " + project.getName());

    return new AsyncResult<>(new CertsResult(project.getName(), user.getUsername()));
  }
  
  public void revokeProjectCertificates(Project project)
      throws GenericException, HopsSecurityException, IOException {
    revokeProjectCertificates(project, null);
  }
  
  public void revokeProjectCertificates(Project project, Users owner)
      throws GenericException, HopsSecurityException, IOException {
    String projectName = project.getName();

    Set<Users> users2deleteCertificates = project.getProjectTeamCollection()
        .stream().map(ProjectTeam::getUser).collect(Collectors.toSet());
    if (owner != null) {
      users2deleteCertificates.add(owner);
    }
    // Iterate through Project members and delete their certificates
    for (Users user2delete : users2deleteCertificates) {
      String certificateIdentifier = projectName + Settings.HOPS_USERNAME_SEPARATOR + user2delete.getUsername();
      // Ordering here is important
      // *First* revoke and *then* delete the certificate
      revokeCertificate(certificateIdentifier, Endpoint.PROJECT);

      // Run custom handlers
      for (CertificateHandler certificateHandler : certificateHandlers) {
        certificateHandler.revoke(project, user2delete);
      }
    }
  }

  public void revokeUserSpecificCertificates(Project project, Users user)
      throws GenericException, HopsSecurityException, IOException {
    String certificateIdentifier = project.getName() + Settings.HOPS_USERNAME_SEPARATOR + user.getUsername();

    // Ordering here is important
    // *First* revoke and *then* delete the certificate
    certsFacade.removeUserProjectCerts(project.getName(), user.getUsername());
    revokeCertificate(certificateIdentifier, Endpoint.PROJECT);

    // Run custom handlers
    for (CertificateHandler certificateHandler : certificateHandlers) {
      certificateHandler.revoke(project, user);
    }
  }

  public CSR signDelaClusterCertificate(CSR csr)
      throws GenericException, HopsSecurityException, UnsupportedEncodingException {
    return signCSR(csr, Endpoint.DELA);
  }

  public void revokeDelaClusterCertificate(String certificateIdentifier)
      throws GenericException, HopsSecurityException {
    revokeCertificate(certificateIdentifier, Endpoint.DELA);
  }

  public BigInteger extractSerialNumberFromCert(String certificate) throws CertificateException {
    InputStream certStream = new ByteArrayInputStream(certificate.getBytes());
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate x509Certificate = (X509Certificate)cf.generateCertificate(certStream);
    return x509Certificate.getSerialNumber();
  }

  public X500Name extractSubjectFromCSR(String csr) throws IOException {
    PemReader pemReader = new PemReader(new StringReader(csr));
    PemObject pemObject = pemReader.readPemObject();
    pemReader.close();

    PKCS10CertificationRequest csrObject = new PKCS10CertificationRequest(pemObject.getContent());
    return csrObject.getSubject();
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

  private byte[] convertKeystoreToByteArray(KeyStore keyStore, String password)
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
    ByteArrayOutputStream keyStoreStream = new ByteArrayOutputStream();
    keyStore.store(keyStoreStream, password.toCharArray());
    return keyStoreStream.toByteArray();
  }

  private Pair<KeyStore, KeyStore> generateStores(String CN, String userKeyPwd, Endpoint endpoint)
      throws HopsSecurityException, GenericException {
    try {
      // Generate keypair
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      CSR csr = generateCSR(CN, keyPair);
      CSR signedCsr = signCSR(csr, endpoint);
      return buildStores(CN, userKeyPwd, keyPair.getPrivate(), signedCsr);

    } catch (OperatorCreationException | IOException e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE, null, null, e);
    }
  }

  private CSR generateCSR(String cn, KeyPair keyPair) throws OperatorCreationException, IOException {
    // Generate CSR
    X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
        .addRDN(BCStyle.CN, cn)
        .build();
    PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic())
        .build(new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(SECURITY_PROVIDER)
            .build(keyPair.getPrivate()));

    // Stringfiy the csr so that it can be sent as json payload
    PemObject pemObject = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());
    StringWriter csrSTR = new StringWriter();
    JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(csrSTR);
    jcaPEMWriter.writeObject(pemObject);
    jcaPEMWriter.close();
    csrSTR.close();
    return new CSR(csrSTR.toString());
  }

  private CSR signCSR(CSR csr, Endpoint endpoint) throws HopsSecurityException, GenericException,
    UnsupportedEncodingException {
    switch (endpoint) {
      case PROJECT:
        return caProxy.signProjectCSR(csr);
      case DELA:
        return caProxy.signDelaCSR(csr);
      default:
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.FINE,
            null, "Unknown CSR type " + endpoint.toString());
    }
  }
  
  private void revokeCertificate(String certificateIdentifier, Endpoint endpoint)
      throws GenericException, HopsSecurityException {
    switch (endpoint) {
      case PROJECT:
        caProxy.revokeProjectX509(certificateIdentifier);
        break;
      case DELA:
        caProxy.revokeDelaX509(certificateIdentifier);
        break;
      default:
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERTIFICATE_REVOKATION_ERROR,
            Level.FINE, null, "Unknown revocation type " + endpoint.toString());
    }
  }

  private Pair<KeyStore, KeyStore> buildStores(String CN, String userKeyPwd,
                                               Key privateKey, CSR signedCert) throws HopsSecurityException {
    KeyStore keyStore = null;
    KeyStore trustStore = null;
    try {
      X509Certificate certificate = (X509Certificate) certificateFactory
          .generateCertificate(new ByteArrayInputStream(signedCert.getSignedCert().getBytes()));
      X509Certificate issuer = (X509Certificate) certificateFactory
          .generateCertificate(new ByteArrayInputStream(signedCert.getIntermediateCaCert().getBytes()));
      X509Certificate rootCa = (X509Certificate) certificateFactory
          .generateCertificate(new ByteArrayInputStream(signedCert.getRootCaCert().getBytes()));

      keyStore = KeyStore.getInstance("JKS");
      keyStore.load(null, null);
      X509Certificate[] chain = new X509Certificate[2];
      chain[0] = certificate;
      chain[1] = issuer;
      keyStore.setKeyEntry(CN, privateKey, userKeyPwd.toCharArray(), chain);

      trustStore = KeyStore.getInstance("JKS");
      trustStore.load(null, null);
      trustStore.setCertificateEntry("hops_root_ca", rootCa);
    } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE, null, null, e);
    }

    return new Pair<>(keyStore, trustStore);
  }
}
