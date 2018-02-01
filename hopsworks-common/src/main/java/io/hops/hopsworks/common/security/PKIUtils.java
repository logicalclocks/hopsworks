/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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

  public static String signCertificate(Settings settings, String csr,
      boolean isIntermediate) throws IOException, InterruptedException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"),
        ".csr");
    FileUtils.writeStringToFile(csrFile, csr);

    if (verifyCSR(csrFile)) {
      return signCSR(settings, csrFile, isIntermediate);
    }
    return null;
  }

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

  private static boolean verifyCSR(File csr) throws IOException, InterruptedException {
    logger.info("Verifying CSR...");
    List<String> cmds = new ArrayList<>();

    cmds.add("openssl");
    cmds.add("req");
    cmds.add("-in");
    cmds.add(csr.getAbsolutePath());
    cmds.add("-noout");
    cmds.add("-verify");
    StringBuilder sb = new StringBuilder("/usr/bin/openssl ");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());
    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    while ((line = br.readLine()) != null) {
      if (line.equalsIgnoreCase("verify failure")) {
        logger.info("verify failure");
        return false;
      } else if (line.equalsIgnoreCase("verify OK")) {
        logger.info("verify OK");
        return true;
      }
    }
    process.waitFor();
    if (process.exitValue() != 0) {
      throw new RuntimeException("failed to verify csr");
    }
    return false;
  }

  private static String signCSR(Settings settings, File csr, boolean intermediate) throws IOException,
      InterruptedException {

    String caFile;
    String extension;
    if (intermediate) {
      caFile = settings.getIntermediateCaDir() + "/openssl-intermediate.cnf";
      extension = "usr_cert";
    } else {
      caFile = settings.getCaDir() + "/openssl-ca.cnf";
      extension = "v3_intermediate_ca";
    }
    String hopsMasterPassword = settings.getHopsworksMasterPasswordSsl();

    String prog = settings.getHopsworksDomainDir()
        + "/bin/global-ca-sign-csr.sh";

    String csrPath = csr.getAbsolutePath();

    File generatedCertFile = File.createTempFile(System.getProperty(
        "java.io.tmpdir"), ".cert.pem");

    logger.info("Signing CSR...");
    List<String> commands = new ArrayList<>();
    commands.add("/usr/bin/sudo");
    commands.add(prog);
    commands.add(caFile);
    commands.add(hopsMasterPassword);
    commands.add(extension);
    commands.add(csrPath);
    commands.add(generatedCertFile.getAbsolutePath());

    StringBuilder sb = new StringBuilder();
    for (String s : commands) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    Process process = new ProcessBuilder(commands).redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
    String line;
    while ((line = br.readLine()) != null) {
      logger.info(line);
    }
    process.waitFor();
    int exitValue = process.exitValue();
    if (exitValue != 0) {
      throw new RuntimeException("Failed to sign certificate. Exit value: "
          + exitValue);
    }
    logger.info("Signed certificate. Verifying....");

    return FileUtils.readFileToString(generatedCertFile);
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
