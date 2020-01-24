/*
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
 */
package io.hops.hopsworks.ca.controllers;

import io.hops.hopsworks.ca.controllers.CAConf.CAConfKeys;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;

import javax.annotation.Resource;
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
import javax.enterprise.concurrent.ManagedExecutorService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: Can we make concurrent modifications on different CAs?

@Singleton
@DependsOn("CAConf")
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@AccessTimeout(value = 120000)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OpensslOperations {
  private final static Logger LOGGER = Logger.getLogger(OpensslOperations.class.getName());

  private final static String SUDO = "/usr/bin/sudo";
  private final static String OPENSSL = "openssl";

  private static final String CERTIFICATE_SUFFIX = ".cert.pem";

  @Resource(lookup = "concurrent/hopsExecutorService")
  private ManagedExecutorService executorService;

  @EJB
  private CAConf CAConf;
  @EJB
  private PKI pki;

  @Lock(LockType.WRITE)
  public String signCertificateRequest(String csr, CertificateType certType)
      throws IOException, CAException {
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
  public void revokeCertificate(String certificateIdentifier, CertificateType certType) throws IOException,
      CAException {

    LOGGER.log(Level.FINE, "Revoking certificate " + certificateIdentifier + CERTIFICATE_SUFFIX);
    PKI.CAType caType = pki.getResponsibileCA(certType);
    String openSslConfig = pki.getCAConfPath(caType).toString();
    String certsDir = pki.getCACertsDir(caType).toString();

    Path certificatePath = Paths.get(certsDir, certificateIdentifier + CERTIFICATE_SUFFIX);
    File certificateFile = certificatePath.toFile();
    if (!certificateFile.exists()) {
      throw new CAException(RESTCodes.CAErrorCode.CERTNOTFOUND, Level.WARNING, certType);
    }
    
    List<String> command = new ArrayList<>();
    command.add(OPENSSL);
    command.add("ca");
    command.add("-batch");
    command.add("-config");
    command.add(openSslConfig);
    command.add("-passin");
    command.add("pass:" + pki.getCAKeyPassword(caType));
    command.add("-revoke");
    command.add(certificatePath.toString());

    executeCommand(command, false);
    createCRL(caType);
    certificateFile.delete();
  }

  private void pruneDatabase(PKI.CAType caType) throws IOException {
    LOGGER.log(Level.FINE, "Pruning OpenSSL database");
    String openSslConf = pki.getCAConfPath(caType).toString();

    List<String> command = new ArrayList<>();
    command.add(OPENSSL);
    command.add("ca");
    command.add("-batch");
    command.add("-config");
    command.add(openSslConf);
    command.add("-updatedb");
    command.add("-passin");
    command.add("pass:" + pki.getCAKeyPassword(caType));

    executeCommand(command, false);
  }
  
  private void createCRL(PKI.CAType caType) throws IOException {
    pruneDatabase(caType);
    LOGGER.log(Level.FINE, "Creating Certificate Revocation List");
    String openSslConfig = pki.getCAConfPath(caType).toString();
    String crlFile = pki.getCACRLPath(caType).toString();

    List<String> commands = new ArrayList<>(10);
    commands.add(OPENSSL);
    commands.add("ca");
    commands.add("-batch");
    commands.add("-config");
    commands.add(openSslConfig);
    commands.add("-gencrl");
    commands.add("-passin");
    commands.add("pass:" + pki.getCAKeyPassword(caType));
    commands.add("-out");
    commands.add(crlFile);
    executeCommand(commands, false);
    LOGGER.log(Level.FINE, "Created CRL");
  }

  private boolean verifyCSR(File csr) throws IOException {
    LOGGER.log(Level.FINE, "Verifying Certificate Signing Request...");
    List<String> command = new ArrayList<>(6);
    command.add(OPENSSL);
    command.add("req");
    command.add("-in");
    command.add(csr.getAbsolutePath());
    command.add("-noout");
    command.add("-verify");

    // For a weird reason, the result string of openssl -verify is in stderr, so redirect stderr to stdout
    String stdout = executeCommand(command, true);
    if (stdout.contains("verify OK")) {
      LOGGER.log(Level.INFO, "CSR verification passed for " + csr.getAbsolutePath());
      return true;
    }
    
    return false;
  }
  
  private String signCSR(File csr, String csrStr, CertificateType certType) throws IOException, CAException {
    LOGGER.log(Level.FINE, "Signing Certificate Signing Request...");
    PKI.CAType caType = pki.getResponsibileCA(certType);
    String opensslConfFile = pki.getCAConfPath(caType).toString();
    String effectiveExtension = pki.getEffectiveExtensions(caType);

    String signScript = Paths.get(CAConf.getString(CAConfKeys.SUDOERS_DIR), "global-ca-sign-csr.sh").toString();
    String fileName;
    try {
      String subjectStr = getSubjectFromCSR(csrStr);
      Map<String, String> subject = pki.getKeyValuesFromSubject(subjectStr);
      fileName = pki.getCertFileName(certType, subject);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Error while extracting CN out of CSR", ex);
      throw ex;
    }

    // Expiration Date formatted as ASN1 UTCTime
    String expirationDate = pki.getValidityPeriod(certType);

    // Check if a certificate with the same name already exists,
    // If you don't and a certificate exists, then openssl will overwrite it making
    // it impossibile to revoke the first certificate
    File signedCertificateFile = pki.getCertPath(caType, fileName).toFile();
    if (signedCertificateFile.exists()) {
      throw new CAException(RESTCodes.CAErrorCode.CERTEXISTS, Level.FINE, CertificateType.PROJECT);
    }

    List<String> command = new ArrayList<>();
    command.add(SUDO);
    command.add(signScript);
    command.add(opensslConfFile);
    command.add(pki.getCAKeyPassword(caType));
    command.add(effectiveExtension);
    command.add(csr.getAbsolutePath());
    command.add(signedCertificateFile.getAbsolutePath());
    command.add(expirationDate);

    executeCommand(command, false);
    LOGGER.log(Level.FINE, "Signed CSR");

    return FileUtils.readFileToString(signedCertificateFile);
  }

  @Lock(LockType.WRITE)
  public String getSubjectFromCSR(String csr) throws IOException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
    FileUtils.writeStringToFile(csrFile, csr);
    //openssl req -in certs-dir/hops-site-certs/csr.pem -noout -subject
    List<String> command = new ArrayList<>();
    command.add(OPENSSL);
    command.add("req");
    command.add("-in");
    command.add(csrFile.getAbsolutePath());
    command.add("-noout");
    command.add("-subject");

    return executeCommand(command, false).trim();
  }

  private String executeCommand(List<String> commands, boolean redirectErrorStream) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.redirectErrorStream(redirectErrorStream);

    try {
      Process proc = processBuilder.start();
      OpenSSLOutputReader outReader = new OpenSSLOutputReader(proc.getInputStream());
      OpenSSLOutputReader errReader = new OpenSSLOutputReader(proc.getErrorStream());

      Future outReaderFuture = executorService.submit(outReader);
      Future errReaderFuture = executorService.submit(errReader);

      boolean exited = proc.waitFor(2, TimeUnit.MINUTES);

      if (!exited || proc.exitValue() != 0) {
        errReaderFuture.get();
        throw new IOException(errReader.getOpensslOutput());
      }

      outReaderFuture.get();
      return outReader.getOpensslOutput();
    } catch (InterruptedException | ExecutionException ex) {
      LOGGER.log(Level.SEVERE, "Error while waiting for OpenSSL command to execute");
      throw new IOException(ex);
    }
  }

  private class OpenSSLOutputReader implements Runnable {

    private InputStream in = null;
    private String opensslOutput;

    public OpenSSLOutputReader(InputStream in) {
      this.in = in;
    }

    @Override
    public void run() {
      StringBuilder outputBuilder = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      char[] charArray = new char[1000];
      try {
        int actualBuffered = 0;
        while ((actualBuffered = br.read(charArray, 0, 1000)) != -1) {
          outputBuilder.append(charArray, 0, actualBuffered);
        }
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Could not read the Openssl output", e);
      } finally {
        try {
          br.close();
        } catch (IOException e) {}
      }
      opensslOutput = outputBuilder.toString();
    }

    public String getOpensslOutput() {
      return opensslOutput;
    }
  }
}
