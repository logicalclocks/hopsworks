package io.hops.hopsworks.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class PKIUtils {

  final static Logger logger = Logger.getLogger(PKIUtils.class.getName());

  public static String signCertificate(String csr, String caDir,
          String hopsMasterPassword, boolean isIntermediate) throws
          IOException, InterruptedException {
    File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"),
            ".csr");
    FileUtils.writeStringToFile(csrFile, csr);

    if (verifyCSR(csrFile)) {
      return signCSR(csrFile, caDir, hopsMasterPassword, isIntermediate);
    }
    return null;
  }

  private static boolean verifyCSR(File csr) throws IOException,
          InterruptedException {

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
    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).
            redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(
            process.getInputStream(), Charset.forName("UTF8")));
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

  private static String signCSR(File csr, String caDir,
          String hopsMasterPassword, boolean intermediate) throws IOException,
          InterruptedException {

    File generatedCertFile = File.createTempFile(System.getProperty(
            "java.io.tmpdir"), ".cert.pem");

    logger.info("Signing CSR...");
    List<String> cmds = new ArrayList<>();

    cmds.add("openssl");
    cmds.add("ca");
//    cmds.add("-policy policy_loose");
    cmds.add("-batch");
    cmds.add("-config");
    if (intermediate) {
      cmds.add(caDir + "/openssl-intermediate.cnf");
    } else {
      cmds.add(caDir + "/openssl-ca.cnf");
    }
    cmds.add("-passin");
    cmds.add("pass:" + hopsMasterPassword);
    cmds.add("-extensions");
    if (intermediate) {
      cmds.add("usr_cert");
    } else {
      cmds.add("v3_intermediate_ca");
    }
    cmds.add("-days");
    cmds.add("3650");
    cmds.add("-notext");
    cmds.add("-md");
    cmds.add("sha256");
    cmds.add("-in");
    cmds.add(csr.getAbsolutePath());
    cmds.add("-out");
    cmds.add(generatedCertFile.getAbsolutePath());
    StringBuilder sb = new StringBuilder("/usr/bin/");
    for (String s : cmds) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).
            redirectErrorStream(true).start();
    BufferedReader br = new BufferedReader(new InputStreamReader(
            process.getInputStream(), Charset.forName("UTF8")));
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
}
