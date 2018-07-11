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

import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SystemCommandExecutor;
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
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  
  @Lock(LockType.WRITE)
  public String createUserCertificate(String projectName, String userName, String countryCode, String city, String
      organization, String email, String orcid, String userKeyPassword) throws IOException {
    
    return createServiceCertificate(Utils.getProjectUsername(projectName, userName), countryCode, city, organization,
        email, orcid, userKeyPassword);
  }
  
  @Lock(LockType.WRITE)
  public String createServiceCertificate(String service, String countryCode, String city, String organization,
      String email, String orcid, String userKeyPassword) throws IOException {
    String intermediateCADir = settings.getIntermediateCaDir();
    File certificateFile = Paths.get(intermediateCADir, "certs", service + ".cert.pem").toFile();
    File keyFile = Paths.get(intermediateCADir, "private", service + ".key.pem").toFile();
    if (certificateFile.exists() || keyFile.exists()) {
      String errorMsg = "X.509 key-pair already exists in " + certificateFile.getAbsolutePath() + " and " +
          keyFile.getAbsolutePath();
      LOG.log(Level.SEVERE, errorMsg);
      throw new IOException(errorMsg);
    }
  
    // Need to execute CreatingUserCerts.sh as 'root' using sudo.
    // Solution is to add them to /etc/sudoers.d/glassfish file. Chef cookbook does this for us.
    List<String> commands = new ArrayList<>(9);
    commands.add(SUDO);
    commands.add(Paths.get(intermediateCADir, Settings.SSL_CREATE_CERT_SCRIPTNAME).toString());
    commands.add(service);
    commands.add(countryCode);
    commands.add(city);
    commands.add(organization);
    commands.add(email);
    commands.add(orcid);
    commands.add(userKeyPassword);
  
    return executeCommand(commands, false);
  }
  
  @Lock(LockType.WRITE)
  public String deleteUserCertificate(String projectSpecificUsername) throws IOException {
    String intermediateCADir = settings.getIntermediateCaDir();
    List<String> commands = new ArrayList<>(3);
    commands.add(SUDO);
    commands.add(Paths.get(intermediateCADir, Settings.SSL_DELETE_CERT_SCRIPTNAME).toString());
    commands.add(projectSpecificUsername);
    
    return executeCommand(commands, false);
  }
  
  @Lock(LockType.WRITE)
  public String deleteProjectCertificate(String projectName) throws IOException {
    String intermediateCADir = settings.getIntermediateCaDir();
    List<String> commands = new ArrayList<>(3);
    commands.add(SUDO);
    commands.add(Paths.get(intermediateCADir, Settings.SSL_DELETE_PROJECT_CERTS_SCRIPTNAME).toString());
    commands.add(projectName);
    
    return executeCommand(commands, false);
  }
  
  public boolean isPresentProjectCertificates(String projectName) {
    String intermediateCADir = settings.getIntermediateCaDir();
    File certFolder = Paths.get(intermediateCADir, "certs").toFile();
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
  public String signCertificateRequest(String csr, boolean isIntermediate, boolean isServiceCertificate,
      boolean isAppCertificate) throws IOException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
    try {
      FileUtils.writeStringToFile(csrFile, csr);
  
      if (verifyCSR(csrFile)) {
        return signCSR(csrFile, csr, isIntermediate, isServiceCertificate, isAppCertificate);
      }
      return null;
    } finally {
      csrFile.delete();
    }
  }
  
  @Lock(LockType.WRITE)
  public void revokeCertificate(String certificateIdentifier, boolean isIntermediate, boolean createCRL)
      throws IOException {
    revokeCertificate(certificateIdentifier, ".cert.pem", isIntermediate, createCRL);
  }
  
  @Lock(LockType.WRITE)
  public void revokeCertificate(String certificateIdentifier, String fileSuffix, boolean isIntermediate,
      boolean createCRL) throws IOException {
    LOG.log(Level.FINE, "Revoking certificate " + certificateIdentifier + fileSuffix);
    String openSslConfig = getOpensslConf(isIntermediate);
    String certsDir;
    if (isIntermediate) {
      certsDir = Paths.get(settings.getIntermediateCaDir(), "certs").toString();
    } else {
      certsDir = Paths.get(settings.getCaDir(), "certs").toString();
    }
    List<String> commands = new ArrayList<>();
    commands.add(OPENSSL);
    commands.add("ca");
    commands.add("-batch");
    commands.add("-config");
    commands.add(openSslConfig);
    commands.add("-passin");
    commands.add("pass:" + settings.getHopsworksMasterPasswordSsl());
    commands.add("-revoke");
    commands.add(Paths.get(certsDir, certificateIdentifier + fileSuffix).toString());
    
    executeCommand(commands, false);
    if (createCRL) {
      createCRL(isIntermediate);
    }
  }
  
  @Lock(LockType.WRITE)
  public void pruneDatabase(boolean isIntermediate) throws IOException {
    LOG.log(Level.FINE, "Pruning OpenSSL database");
    String openSslConf = getOpensslConf(isIntermediate);
    List<String> commands = new ArrayList<>();
    commands.add(OPENSSL);
    commands.add("ca");
    commands.add("-batch");
    commands.add("-config");
    commands.add(openSslConf);
    commands.add("-updatedb");
    commands.add("-passin");
    commands.add("pass:" + settings.getHopsworksMasterPasswordSsl());
    
    executeCommand(commands, false);
  }
  
  @Lock(LockType.WRITE)
  public String createAndReadCRL(boolean isIntermediate) throws IOException {
    createCRL(isIntermediate);
    File crl;
    if (isIntermediate) {
      crl = Paths.get(settings.getIntermediateCaDir(), "crl", "intermediate.crl.pem").toFile();
    } else {
      crl = Paths.get(settings.getCaDir(), "crl", "ca.crl.pem").toFile();
    }
    
    return FileUtils.readFileToString(crl);
  }
  
  @Lock(LockType.WRITE)
  public void createCRL(boolean isIntermediate) throws IOException {
    pruneDatabase(isIntermediate);
    LOG.log(Level.FINE, "Creating Certificate Revocation List");
    String openSslConfig = getOpensslConf(isIntermediate);
    String crlFile;
    if (isIntermediate) {
      crlFile = Paths.get(settings.getIntermediateCaDir(), "crl", "intermediate.crl.pem").toString();
    } else {
      crlFile = Paths.get(settings.getCaDir(), "crl", "ca.crl.pem").toString();
    }
    List<String> commands = new ArrayList<>(10);
    commands.add(OPENSSL);
    commands.add("ca");
    commands.add("-batch");
    commands.add("-config");
    commands.add(openSslConfig);
    commands.add("-gencrl");
    commands.add("-passin");
    commands.add("pass:" + settings.getHopsworksMasterPasswordSsl());
    commands.add("-out");
    commands.add(crlFile);
    executeCommand(commands, false);
    LOG.log(Level.FINE, "Created CRL");
  }
  
  @Lock(LockType.WRITE)
  public void validateCertificate(X509Certificate certificate) throws IOException {
    File tmpCertFile = File.createTempFile("cert-", ".pem");
    try (FileWriter fw = new FileWriter(tmpCertFile, false)) {
      fw.write(X509Factory.BEGIN_CERT);
      fw.write("\n");
      fw.write(b64encoder.encodeToString(certificate.getEncoded()));
      fw.write("\n");
      fw.write(X509Factory.END_CERT);
      fw.flush();
  
  
      List<String> commands = new ArrayList<>();
      commands.add(OPENSSL);
      commands.add("verify");
      commands.add("-CAfile");
      commands.add(Paths.get(settings.getIntermediateCaDir(), "certs", "ca-chain.cert.pem").toString());
      commands.add(tmpCertFile.getAbsolutePath());
      executeCommand(commands, false);
    } catch (GeneralSecurityException ex) {
      throw new IOException(ex);
    } finally {
      tmpCertFile.delete();
    }
  }
  
  private boolean verifyCSR(File csr) throws IOException {
    LOG.log(Level.FINE, "Verifying Certificate Signing Request...");
    List<String> commands = new ArrayList<>(6);
    commands.add(OPENSSL);
    commands.add("req");
    commands.add("-in");
    commands.add(csr.getAbsolutePath());
    commands.add("-noout");
    commands.add("-verify");
    
    // For a weird reason, the result string of openssl -verify is in stderr, so redirect stderr to stdout
    String stdout = executeCommand(commands, true);
    if (stdout.contains("verify OK")) {
      LOG.log(Level.INFO, "CSR verification passed for " + csr.getAbsolutePath());
      return true;
    }
    
    return false;
  }
  
  private String signCSR(File csr, String csrStr, boolean isIntermediate, boolean isServiceCertificate,
      boolean isAppCertificate) throws IOException {
    LOG.log(Level.FINE, "Signing Certificate Signing Request...");
    String opensslConfFile = getOpensslConf(isIntermediate);
    String effectiveExtension;
    if (isIntermediate) {
      effectiveExtension = "usr_cert";
    } else {
      effectiveExtension = "v3_intermediate_ca";
    }
    String hopsMasterPassword = settings.getHopsworksMasterPasswordSsl();
    String signScript = Paths.get(settings.getHopsworksDomainDir(), "bin", "global-ca-sign-csr.sh").toString();
    String fileName;
    try {
      String subjectStr = PKIUtils.getSubjectFromCSR(csrStr);
      Map<String, String> subject = PKIUtils.getKeyValuesFromSubject(subjectStr);
      fileName = subject.get("CN");
      if (isAppCertificate) {
        fileName = fileName + "__" + subject.get("O");
        // OU field serves as the application certificate version
        String ou = subject.get("OU");
        if (ou != null) {
          fileName += "__" + ou;
        }
      }
    } catch (InterruptedException ex) {
      LOG.log(Level.SEVERE, "Error while extracting Subject fields out of CSR", ex);
      throw new IOException(ex);
    }
    File signedCertificateFile;
    if (isIntermediate) {
      signedCertificateFile = Paths.get(settings.getIntermediateCaDir(), "certs", fileName + ".cert.pem")
          .toFile();
    } else {
      signedCertificateFile = Paths.get(settings.getCaDir(), "certs", fileName + ".cert.pem").toFile();
    }
    
    long valueInDays = 3650;
    if (settings.isServiceKeyRotationEnabled() && isServiceCertificate) {
      String serviceKeyRotationIntervalRaw = settings.getServiceKeyRotationInterval();
    // Add four more days to interval just to be sure
      valueInDays = getCertificateValidityInDays(serviceKeyRotationIntervalRaw) + 4;
    } else if (isAppCertificate) {
      String appCertificateValidityRaw = settings.getApplicationCertificateValidityPeriod();
      valueInDays = getCertificateValidityInDays(appCertificateValidityRaw);
    }
    
    List<String> commands = new ArrayList<>();
    commands.add(SUDO);
    commands.add(signScript);
    commands.add(opensslConfFile);
    commands.add(hopsMasterPassword);
    commands.add(effectiveExtension);
    commands.add(csr.getAbsolutePath());
    commands.add(signedCertificateFile.getAbsolutePath());
    commands.add(String.valueOf(valueInDays));
    
    String stdout = executeCommand(commands, false);
    LOG.log(Level.FINE, stdout);
    LOG.log(Level.INFO, "Signed CSR");
    
    return FileUtils.readFileToString(signedCertificateFile);
  }
  
  private long getCertificateValidityInDays(String rawConfigurationProperty) {
    Long timeValue = Settings.getConfTimeValue(rawConfigurationProperty);
    TimeUnit unitValue = Settings.getConfTimeTimeUnit(rawConfigurationProperty);
    return TimeUnit.DAYS.convert(timeValue, unitValue);
  }
  
  private String executeCommand(List<String> commands, boolean redirectErrorStream) throws IOException {
    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands, redirectErrorStream);
    try {
      int returnValue = commandExecutor.executeCommand();
      String stdout = commandExecutor.getStandardOutputFromCommand();
      String stderr = commandExecutor.getStandardErrorFromCommand();
      if (returnValue != 0) {
        throw new IOException(stderr);
      }
      return stdout;
    } catch (InterruptedException ex) {
      LOG.log(Level.SEVERE, "Error while waiting for OpenSSL command to execute");
      throw new IOException(ex);
    }
  }
  
  private String getOpensslConf(boolean isIntermediate) {
    if (isIntermediate) {
      return Paths.get(settings.getIntermediateCaDir(), "openssl-intermediate.cnf").toString();
    }
    return Paths.get(settings.getCaDir(), "openssl-ca.cnf").toString();
  }
}
