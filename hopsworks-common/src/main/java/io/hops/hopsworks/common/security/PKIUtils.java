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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.FileUtils;

public class PKIUtils {

  final static Logger logger = Logger.getLogger(PKIUtils.class.getName());

  public static void revokeCert(Settings settings, String certFile, boolean intermediate) throws IOException,
      InterruptedException {
    logger.info("Revoking certificate...");
    List<String> cmds = new ArrayList<>();
    cmds.add("openssl");
    cmds.add("ca");
    cmds.add("-batch");
    cmds.add("-config");
    if (intermediate) {
      cmds.add(settings.getIntermediateCaDir() + "/openssl-intermediate.cnf");
    } else {
      cmds.add(settings.getCaDir() + "/openssl-ca.cnf");
    }
    cmds.add("-passin");
    cmds.add("pass:" + settings.getHopsworksMasterPasswordSsl());
    cmds.add("-revoke");
    cmds.add(certFile);

    StringBuilder sb = new StringBuilder("/usr/bin/");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    while ((line = br.readLine()) != null) {
      logger.info(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed to revoke certificate. Exit value: "
          + exitValue);
    }
    logger.info("Revoked certificate.");
    //update the crl
    createCRL(settings, intermediate);
  }
  
  public static String createCRL(Settings settings, boolean intermediate) throws IOException,
      InterruptedException {
    logger.info("Creating crl...");
    String generatedCrlFile;
    List<String> cmds = new ArrayList<>();
    cmds.add("openssl");
    cmds.add("ca");
    cmds.add("-batch");
    cmds.add("-config");
    if (intermediate) {
      cmds.add(settings.getIntermediateCaDir() + "/openssl-intermediate.cnf");
      generatedCrlFile = settings.getIntermediateCaDir() + "/crl/intermediate.crl.pem";
    } else {
      cmds.add(settings.getCaDir() + "/openssl-ca.cnf");
      generatedCrlFile = settings.getCaDir() + "/crl/ca.crl.pem";
    }
    cmds.add("-passin");
    cmds.add("pass:" + settings.getHopsworksMasterPasswordSsl());
    cmds.add("-gencrl");
    cmds.add("-out");
    cmds.add(generatedCrlFile);

    StringBuilder sb = new StringBuilder("/usr/bin/");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    while ((line = br.readLine()) != null) {
      logger.info(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed to create crl. Exit value: " + exitValue);
    }
    logger.info("Created crl.");
    return FileUtils.readFileToString(new File(generatedCrlFile));
  }

  public static String verifyCertificate(Settings settings, String cert, boolean intermediate)
      throws IOException, InterruptedException {
    File certFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".pem");
    FileUtils.writeStringToFile(certFile, cert);
    String certDir = intermediate ? settings.getIntermediateCaDir() : settings.getCaDir();
    String crlFile = intermediate ? certDir + "/crl/intermediate.crl.pem" : certDir + "/crl/ca.crl.pem";
    String pubCert = intermediate ? "/intermediate/certs/intermediate.cert.pem " : "/certs/ca.cert.pem ";
    //update the crl
    createCRL(settings, intermediate);
    logger.info("Checking certificate...");
    List<String> cmds = new ArrayList<>();
    cmds.add("openssl");
    cmds.add("verify");
    cmds.add("-crl_check");
    cmds.add("-CAfile");
    cmds.add("<(cat " + certDir + pubCert + crlFile + ")");
    cmds.add(certFile.getAbsolutePath());
    StringBuilder sb = new StringBuilder("/usr/bin/");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    StringBuilder lines = new StringBuilder("");
    while ((line = br.readLine()) != null) {
      logger.info(line);
      lines.append(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed cert check. Exit value: " + exitValue);
    }
    logger.info("Done cert check.");
    return lines.toString();
  }

  public static String getSubjectFromCSR(String csr) throws IOException, InterruptedException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
    FileUtils.writeStringToFile(csrFile, csr);
    List<String> cmds = new ArrayList<>();
    //openssl req -in certs-dir/hops-site-certs/csr.pem -noout -subject
    cmds.add("openssl");
    cmds.add("req");
    cmds.add("-in");
    cmds.add(csrFile.getAbsolutePath());
    cmds.add("-noout");
    cmds.add("-subject");

    StringBuilder sb = new StringBuilder("/usr/bin/ ");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());
    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    StringBuilder lines = new StringBuilder("");
    while ((line = br.readLine()) != null) {
      logger.info(line);
      lines.append(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed to get subject. Exit value: " + exitValue);
    }
    return lines.toString();
  }

  public static String getSerialNumberFromCert(String cert) throws IOException, InterruptedException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".pem");
    FileUtils.writeStringToFile(csrFile, cert);
    List<String> cmds = new ArrayList<>();
    //openssl x509 -in certs-dir/hops-site-certs/pub.pem -noout -serial
    cmds.add("openssl");
    cmds.add("x509");
    cmds.add("-in");
    cmds.add(csrFile.getAbsolutePath());
    cmds.add("-noout");
    cmds.add("-serial");

    StringBuilder sb = new StringBuilder("/usr/bin/ ");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());
    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    StringBuilder lines = new StringBuilder("");
    while ((line = br.readLine()) != null) {
      logger.info(line);
      lines.append(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed to get Serial Number. Exit value: " + exitValue);
    }
    return lines.toString();
  }
}
