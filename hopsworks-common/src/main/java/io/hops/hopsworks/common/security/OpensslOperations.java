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

import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.FileUtils;
import sun.security.provider.X509Factory;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.security.CertificatesMgmService.CERTIFICATE_SUFFIX;

//TODO: Can we make concurrent modifications on different CAs?

@Singleton
@DependsOn("Settings")
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@AccessTimeout(value = 120000)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OpensslOperations {
  private final static Logger LOG = Logger.getLogger(OpensslOperations.class.getName());
  private final static String SUDO = "/usr/bin/sudo";
  private final static String OPENSSL = "openssl";
  private final Base64.Encoder b64encoder = Base64.getEncoder();
  
  @EJB
  private Settings settings;
  @EJB
  private PKI pki;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  
  @Lock(LockType.WRITE)
  public String createUserCertificate(String projectName, String userName, String countryCode, String city, String
      organization, String email, String orcid, String userKeyPassword) throws IOException {
    
    return createServiceCertificate(Utils.getProjectUsername(projectName, userName), countryCode, city, organization,
        email, orcid, userKeyPassword);
  }
  
  @Lock(LockType.WRITE)
  public String createServiceCertificate(String service, String countryCode, String city, String organization,
      String email, String orcid, String userKeyPassword) throws IOException {

    String intermediateCADir = pki.getCAParentPath(PKI.CAType.INTERMEDIATE);
    File certificateFile = pki.getCertPath(PKI.CAType.INTERMEDIATE, service).toFile();
    File keyFile = pki.getKeyPath(PKI.CAType.INTERMEDIATE, service).toFile();

    if (certificateFile.exists() || keyFile.exists()) {
      String errorMsg = "X.509 key-pair already exists in " + certificateFile.getAbsolutePath() + " and " +
          keyFile.getAbsolutePath();
      LOG.log(Level.SEVERE, errorMsg);
      throw new IOException(errorMsg);
    }
  
    // Need to execute CreatingUserCerts.sh as 'root' using sudo.
    // Solution is to add them to /etc/sudoers.d/glassfish file. Chef cookbook does this for us.
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(SUDO)
        .addCommand(Paths.get(intermediateCADir, Settings.SSL_CREATE_CERT_SCRIPTNAME).toString())
        .addCommand(service)
        .addCommand(countryCode)
        .addCommand(city)
        .addCommand(organization)
        .addCommand(email)
        .addCommand(orcid)
        .addCommand(userKeyPassword)
        .build();
    return executeCommand(processDescriptor);
  }
  
  @Lock(LockType.WRITE)
  public String deleteUserCertificate(String projectSpecificUsername) throws IOException {
    String intermediateCADir = pki.getCAParentPath(PKI.CAType.INTERMEDIATE);
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(SUDO)
        .addCommand(Paths.get(intermediateCADir, Settings.SSL_DELETE_CERT_SCRIPTNAME).toString())
        .addCommand(projectSpecificUsername)
        .build();
    
    return executeCommand(processDescriptor);
  }
  
  @Lock(LockType.WRITE)
  public String deleteProjectCertificate(String projectName) throws IOException {
    String intermediateCADir = pki.getCAParentPath(PKI.CAType.INTERMEDIATE);
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(SUDO)
        .addCommand(Paths.get(intermediateCADir, Settings.SSL_DELETE_PROJECT_CERTS_SCRIPTNAME).toString())
        .addCommand(projectName)
        .build();
    
    return executeCommand(processDescriptor);
  }
  
  public boolean isPresentProjectCertificates(String projectName) {
    File certFolder = pki.getCACertPath(PKI.CAType.INTERMEDIATE).toFile();
    String[] certs = certFolder.list();
    if (certs != null && certs.length > 0) {
      for (String certFile : certs) {
        if (certFile.startsWith(projectName + "__")) {
          return true;
        }
      }
    }
    return false;
  }
  
  @Lock(LockType.WRITE)
  public String signCertificateRequest(String csr, CertificateType certType)
      throws IOException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
    try {
      FileUtils.writeStringToFile(csrFile, csr);
  
      if (verifyCSR(csrFile)) {
        return signCSR(csrFile, csr, certType);
      }
      return null;
    } finally {
      csrFile.delete();
    }
  }
  
  @Lock(LockType.WRITE)
  public void revokeCertificate(String certificateIdentifier, CertificateType certType,
                                boolean createCRL, boolean deleteCert) throws IOException, CAException {
    revokeCertificate(certificateIdentifier, CERTIFICATE_SUFFIX, certType, createCRL, deleteCert);
  }
  
  @Lock(LockType.WRITE)
  public void revokeCertificate(String certificateIdentifier, String fileSuffix,
                                CertificateType certType, boolean createCRL, boolean deleteCert)
      throws IOException, CAException {
    LOG.log(Level.FINE, "Revoking certificate " + certificateIdentifier + fileSuffix);
    PKI.CAType caType = pki.getResponsibileCA(certType);
    String openSslConfig = pki.getCAConfPath(caType).toString();
    String certsDir = pki.getCACertsDir(caType).toString();

    Path certificatePath = Paths.get(certsDir, certificateIdentifier + fileSuffix);
    File certificateFile = certificatePath.toFile();
    if (!certificateFile.exists()) {
      throw new CAException(RESTCodes.CAErrorCode.CERTNOTFOUND, Level.WARNING, certType);
    }
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("ca")
        .addCommand("-batch")
        .addCommand("-config")
        .addCommand(openSslConfig)
        .addCommand("-passin")
        .addCommand("pass:" + pki.getCAKeyPassword(caType))
        .addCommand("-revoke")
        .addCommand(certificatePath.toString())
        .build();
    
    executeCommand(processDescriptor);
    if (createCRL) {
      createCRL(caType);
    }

    if (deleteCert) {
      certificateFile.delete();
    }
  }
  
  @Lock(LockType.WRITE)
  public void pruneDatabase(PKI.CAType caType) throws IOException {
    LOG.log(Level.FINE, "Pruning OpenSSL database");
    String openSslConf = pki.getCAConfPath(caType).toString();
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("ca")
        .addCommand("-batch")
        .addCommand("-config")
        .addCommand(openSslConf)
        .addCommand("-updatedb")
        .addCommand("-passin")
        .addCommand("pass:" + pki.getCAKeyPassword(caType))
        .build();
    
    executeCommand(processDescriptor);
  }
  
  @Lock(LockType.WRITE)
  public String createAndReadCRL(PKI.CAType caType) throws IOException {
    createCRL(caType);
    File crl = pki.getCACRLPath(caType).toFile();

    return FileUtils.readFileToString(crl);
  }
  
  @Lock(LockType.WRITE)
  public void createCRL(PKI.CAType caType) throws IOException {
    pruneDatabase(caType);
    LOG.log(Level.FINE, "Creating Certificate Revocation List");
    String openSslConfig = pki.getCAConfPath(caType).toString();
    String crlFile = pki.getCACRLPath(caType).toString();

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("ca")
        .addCommand("-batch")
        .addCommand("-config")
        .addCommand(openSslConfig)
        .addCommand("-gencrl")
        .addCommand("-passin")
        .addCommand("pass:" + pki.getCAKeyPassword(caType))
        .addCommand("-out")
        .addCommand(crlFile)
        .build();
    executeCommand(processDescriptor);
    LOG.log(Level.FINE, "Created CRL");
  }
  
  @Lock(LockType.WRITE)
  public void validateCertificate(X509Certificate certificate, PKI.CAType caType) throws IOException {
    File tmpCertFile = File.createTempFile("cert-", ".pem");
    try (FileWriter fw = new FileWriter(tmpCertFile, false)) {
      fw.write(X509Factory.BEGIN_CERT);
      fw.write("\n");
      fw.write(b64encoder.encodeToString(certificate.getEncoded()));
      fw.write("\n");
      fw.write(X509Factory.END_CERT);
      fw.flush();
  
  
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand(OPENSSL)
          .addCommand("verify")
          .addCommand("-CAfile")
          .addCommand(pki.getChainOfTrustFilePath(caType).toString())
          .addCommand("-crl_check")
          .addCommand("-CRLfile")
          .addCommand(pki.getCACRLPath(caType).toString())
          .addCommand(tmpCertFile.getAbsolutePath())
          .build();
      executeCommand(processDescriptor);
    } catch (GeneralSecurityException ex) {
      throw new IOException(ex);
    } finally {
      tmpCertFile.delete();
    }
  }
  
  private boolean verifyCSR(File csr) throws IOException {
    LOG.log(Level.FINE, "Verifying Certificate Signing Request...");
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("req")
        .addCommand("-in")
        .addCommand(csr.getAbsolutePath())
        .addCommand("-noout")
        .addCommand("-verify")
        .redirectErrorStream(true)
        .build();
    
    // For a weird reason, the result string of openssl -verify is in stderr, so redirect stderr to stdout
    String stdout = executeCommand(processDescriptor);
    if (stdout.contains("verify OK")) {
      LOG.log(Level.INFO, "CSR verification passed for " + csr.getAbsolutePath());
      return true;
    }
    
    return false;
  }
  
  private String signCSR(File csr, String csrStr, CertificateType certType) throws IOException {
    LOG.log(Level.FINE, "Signing Certificate Signing Request...");
    PKI.CAType caType = pki.getResponsibileCA(certType);
    String opensslConfFile = pki.getCAConfPath(caType).toString();
    String effectiveExtension = pki.getEffectiveExtensions(caType);

    String signScript = Paths.get(settings.getHopsworksDomainDir(), "bin", "global-ca-sign-csr.sh").toString();
    String fileName;
    try {
      String subjectStr = getSubjectFromCSR(csrStr);
      Map<String, String> subject = pki.getKeyValuesFromSubject(subjectStr);
      fileName = pki.getCertFileName(certType, subject);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Error while extracting CN out of CSR", ex);
      throw ex;
    }

    long valueInDays = pki.getValidityPeriod(certType);

    File signedCertificateFile = pki.getCertPath(caType, fileName).toFile();
  
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(SUDO)
        .addCommand(signScript)
        .addCommand(opensslConfFile)
        .addCommand(pki.getCAKeyPassword(caType))
        .addCommand(effectiveExtension)
        .addCommand(csr.getAbsolutePath())
        .addCommand(signedCertificateFile.getAbsolutePath())
        .addCommand(String.valueOf(valueInDays))
        .build();
    
    String stdout = executeCommand(processDescriptor);
    LOG.log(Level.FINE, stdout);
    LOG.log(Level.INFO, "Signed CSR");
    
    return FileUtils.readFileToString(signedCertificateFile);
  }

  @Lock(LockType.WRITE)
  public String getSerialNumberFromCert(String cert) throws IOException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".pem");
    FileUtils.writeStringToFile(csrFile, cert);
    //openssl x509 -in certs-dir/hops-site-certs/pub.pem -noout -serial
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("x509")
        .addCommand("-in")
        .addCommand(csrFile.getAbsolutePath())
        .addCommand("-noout")
        .addCommand("-serial")
        .redirectErrorStream(true)
        .build();

    return executeCommand(processDescriptor);
  }

  @Lock(LockType.WRITE)
  public String getSubjectFromCSR(String csr) throws IOException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
    FileUtils.writeStringToFile(csrFile, csr);
    //openssl req -in certs-dir/hops-site-certs/csr.pem -noout -subject
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(OPENSSL)
        .addCommand("req")
        .addCommand("-in")
        .addCommand(csrFile.getAbsolutePath())
        .addCommand("-noout")
        .addCommand("-subject")
        .redirectErrorStream(true)
        .build();

    return executeCommand(processDescriptor);
  }
  
  private String executeCommand(ProcessDescriptor processDescriptor) throws IOException {
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      String stdOut = processResult.getStdout().trim();
      String stderr = processResult.getStderr().trim();
      if (processResult.getExitCode() != 0) {
        throw new IOException(stderr);
      }
      return stdOut;
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Error while executing OpenSSL command: " + processDescriptor);
      throw ex;
    }
  }
}
