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

package io.hops.hopsworks.common.util;

import com.google.common.io.Files;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.templates.AppendConfigReplacementPolicy;
import io.hops.hopsworks.common.util.templates.ConfigProperty;
import io.hops.hopsworks.common.util.templates.ConfigReplacementPolicy;
import io.hops.hopsworks.common.util.templates.IgnoreConfigReplacementPolicy;
import io.hops.hopsworks.common.util.templates.OverwriteConfigReplacementPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.net.util.Base64;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.Key;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods.
 * <p>
 */
public class HopsUtils {

  private static final Logger LOG = Logger.getLogger(HopsUtils.class.getName());
  private static final int ROOT_DIR_PARTITION_KEY = 0;
  public static final short ROOT_DIR_DEPTH = 0;
  private static int RANDOM_PARTITIONING_MAX_LEVEL = 1;
  public static int ROOT_INODE_ID = 1;
  private static final FsPermission materialPermissions = new FsPermission(FsAction.ALL, FsAction.NONE, FsAction.NONE);
  private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\r?\\n");
  // e.x. spark.files=hdfs://someFile,hdfs://anotherFile
  private static final Pattern SPARK_PROPS_PATTERN = Pattern.compile("(.+?)=(.+)");
  public static final ConfigReplacementPolicy OVERWRITE = new OverwriteConfigReplacementPolicy();
  public static final ConfigReplacementPolicy IGNORE = new IgnoreConfigReplacementPolicy();
  public static final ConfigReplacementPolicy APPEND = new AppendConfigReplacementPolicy();
  
  /**
   *
   * @param <E>
   * @param value
   * @param enumClass
   * @return
   */
  public static <E extends Enum<E>> boolean isInEnum(String value,
      Class<E> enumClass) {
    for (E e : enumClass.getEnumConstants()) {
      if (e.name().equals(value)) {
        return true;
      }
    }
    return false;
  }

  public static int dataSetPartitionId(Inode parent, String name) {
    return calculatePartitionId(parent.getId(), name, 3);
  }

  public static int calculatePartitionId(int parentId, String name, int depth) {
    if (isTreeLevelRandomPartitioned(depth)) {
      return partitionIdHashFunction(parentId, name, depth);
    } else {
      return parentId;
    }
  }

  private static int partitionIdHashFunction(int parentId, String name,
      int depth) {
    if (depth == ROOT_DIR_DEPTH) {
      return ROOT_DIR_PARTITION_KEY;
    } else {
      return (name + parentId).hashCode();
    }
  }

  private static boolean isTreeLevelRandomPartitioned(int depth) {
    return depth <= RANDOM_PARTITIONING_MAX_LEVEL;
  }

  /**
   * Retrieves the global hadoop classpath.
   *
   * @param params
   * @return hadoop global classpath
   */
  public static String getHadoopClasspathGlob(String... params) {
    ProcessBuilder pb = new ProcessBuilder(params);
    try {
      Process process = pb.start();
      int errCode = process.waitFor();
      if (errCode != 0) {
        return "";
      }
      StringBuilder sb = new StringBuilder();
      try (BufferedReader br
          = new BufferedReader(new InputStreamReader(process.
              getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line);
        }
      }
      //Now we must remove the yarn shuffle library as it creates issues for 
      //Zeppelin Spark Interpreter
      StringBuilder classpath = new StringBuilder();

      for (String path : sb.toString().split(File.pathSeparator)) {
        if (!path.contains("yarn") && !path.contains("jersey") && !path.
            contains("servlet")) {
          classpath.append(path).append(File.pathSeparator);
        }
      }
      if (classpath.length() > 0) {
        return classpath.toString().substring(0, classpath.length() - 1);
      }

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class.getName()).log(Level.SEVERE, null, ex);
    }
    return "";
  }

  public static String getProjectKeystoreName(String project, String user) {
    return project + HdfsUsersController.USER_NAME_DELIMITER + user +
        "__kstore.jks";
  }

  public static String getProjectTruststoreName(String project, String user) {
    return project + HdfsUsersController.USER_NAME_DELIMITER + user +
        "__tstore.jks";
  }
  
  public static String getProjectMaterialPasswordName(String project, String
      user) {
    return project + HdfsUsersController.USER_NAME_DELIMITER + user + "__cert.key";
  }
  
  public static void copyProjectUserCerts(Project project, String username,
                                          String localTmpDir, String remoteTmpDir, CertificateMaterializer
      certMat, boolean isRpcTlsEnabled) {
    copyProjectUserCerts(project, username, localTmpDir, remoteTmpDir,
        null, null, null, null, null, null, certMat, isRpcTlsEnabled);
  }

  public static void copyProjectUserCerts(Project project, String username,
      String localTmpDir, String remoteTmpDir, JobType jobType,
      DistributedFileSystemOps dfso,
      List<LocalResourceDTO> projectLocalResources,
      Map<String, String> jobSystemProperties,
      String applicationId, CertificateMaterializer certMat,
      boolean isRpcTlsEnabled) {
    copyProjectUserCerts(project, username, localTmpDir, remoteTmpDir,
        jobType, dfso, projectLocalResources, jobSystemProperties,
        null, applicationId, certMat, isRpcTlsEnabled);
  }

  /**
   * Remote user generic project certificates materialized both from the local
   * filesystem and from HDFS
   * @param projectName
   * @param remoteFSDir
   * @param certificateMaterializer
   * @throws IOException
   */
  public static void cleanupCertificatesForProject(String projectName,
      String remoteFSDir, CertificateMaterializer certificateMaterializer, Settings settings) throws IOException {
    
    certificateMaterializer.removeCertificatesLocal(projectName);
    
    // If Hops RPC TLS is enabled then we haven't put them in HDFS, so we should not delete them
    if (!settings.getHopsRpcTls()) {
      String remoteDirectory = remoteFSDir + Path.SEPARATOR + projectName + Settings.PROJECT_GENERIC_USER_SUFFIX;
      certificateMaterializer.removeCertificatesRemote(null, projectName, remoteDirectory);
    }
  }

  /**
   * Remote user certificates materialized both from the local
   * filesystem and from HDFS
   * @param username
   * @param remoteFSDir
   * @param certificateMaterializer
   * @throws IOException
   */
  public static void cleanupCertificatesForUserCustomDir(String username,
      String projectName, String remoteFSDir, CertificateMaterializer certificateMaterializer, String directory,
      Settings settings) throws IOException {

    certificateMaterializer.removeCertificatesLocalCustomDir(username, projectName, directory);
    String projectSpecificUsername = projectName + HdfsUsersController
        .USER_NAME_DELIMITER + username;
    // If Hops RPC TLS is enabled, we haven't put user certificates in HDFS, so we shouldn't try to remove them
    if (!settings.getHopsRpcTls()) {
      String remoteDirectory = remoteFSDir + Path.SEPARATOR + projectSpecificUsername;
      certificateMaterializer.removeCertificatesRemote(username, projectName, remoteDirectory);
    }
  }
  
  public static void cleanupCertificatesForUser(String username, String projectName, String remoteFSDir,
      CertificateMaterializer certificateMaterializer, Settings settings) throws IOException {
    cleanupCertificatesForUserCustomDir(username, projectName, remoteFSDir, certificateMaterializer, null, settings);
  }
  
  /**
   * Utility method that materializes user certificates in the local
   * filesystem and in HDFS
   * @param projectName
   * @param remoteFSDir
   * @param dfso
   * @param certificateMaterializer
   * @param settings
   * @throws IOException
   */
  public static void materializeCertificatesForUserCustomDir(String projectName, String userName, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer
      certificateMaterializer, Settings settings, String directory) throws
      IOException {

    String projectSpecificUsername = projectName + "__" + userName;
  
    certificateMaterializer.materializeCertificatesLocalCustomDir(userName, projectName, directory);
    
    // When Hops RPC TLS is enabled, Yarn will take care of application certificate
    if (!settings.getHopsRpcTls()) {
      String remoteDirectory =
          createRemoteDirectory(remoteFSDir, projectSpecificUsername, projectSpecificUsername, dfso);
      certificateMaterializer.materializeCertificatesRemote(userName, projectName, projectSpecificUsername,
          projectSpecificUsername, materialPermissions, remoteDirectory);
    }
  }
  
  public static void materializeCertificatesForUser(String projectName, String userName, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer certificateMaterializer, Settings settings)
    throws IOException {
    materializeCertificatesForUserCustomDir(projectName, userName, remoteFSDir, dfso, certificateMaterializer,
        settings, null);
  }
  
  /**
   * Utility method that materializes user certificates in the local
   * filesystem and in HDFS
   *
   * @param projectName
   * @param remoteFSDir
   * @param dfso
   * @param certificateMaterializer
   * @param settings
   * @throws IOException
   */
  public static void materializeCertificatesForProject(String projectName, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer certificateMaterializer, Settings settings)
      throws IOException {
    
    
    certificateMaterializer.materializeCertificatesLocal(projectName);
    
    
    String projectGenericUser = projectName + Settings.PROJECT_GENERIC_USER_SUFFIX;
  
    // When Hops RPC TLS is enabled, Yarn will take care of application certificate
    // so we don't need them in HDFS
    if (!settings.getHopsRpcTls()) {
      String remoteDirectory = createRemoteDirectory(remoteFSDir, projectGenericUser, projectGenericUser, dfso);
  
      certificateMaterializer.materializeCertificatesRemote(null, projectName, projectGenericUser,
          projectGenericUser, materialPermissions, remoteDirectory);
    }
  }
  
  private static String createRemoteDirectory(String remoteFSDir, String certsSpecificDir, String owner,
      DistributedFileSystemOps dfso) throws IOException {
    boolean createdDir = false;
    
    if (!dfso.exists(remoteFSDir)) {
      Path remoteFSTarget = new Path(remoteFSDir);
      dfso.mkdir(remoteFSTarget, new FsPermission(
          FsAction.ALL, FsAction.ALL, FsAction.ALL));
      createdDir = true;
    }
  
    Path projectRemoteFSDir = new Path(remoteFSDir + Path.SEPARATOR + certsSpecificDir);
    if (!dfso.exists(projectRemoteFSDir.toString())) {
      dfso.mkdir(projectRemoteFSDir, new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.NONE));
      dfso.setOwner(projectRemoteFSDir, owner, owner);
      createdDir = true;
    }
    if (createdDir) {
      dfso.flushCache(owner, owner);
    }
    
    return projectRemoteFSDir.toString();
  }

  /**
   * Utility method that copies project user certificates from the Database, to
   * either hdfs to be passed as LocalResources to the YarnJob or to used
   * by another method.
   *
   * @param project
   * @param username
   * @param localTmpDir
   * @param remoteTmpDir
   * @param jobType
   * @param dfso
   * @param projectLocalResources
   * @param jobSystemProperties
   * @param flinkCertsDir
   * @param applicationId
   */
  public static void copyProjectUserCerts(Project project, String username,
      String localTmpDir, String remoteTmpDir, JobType jobType,
      DistributedFileSystemOps dfso,
      List<LocalResourceDTO> projectLocalResources,
      Map<String, String> jobSystemProperties,
      String flinkCertsDir, String applicationId, CertificateMaterializer
      certMat, boolean isRpcTlsEnabled) {
  
    // Let the Certificate Materializer handle the certificates
    UserCerts userCert = new UserCerts(project.getName(), username);
    try {
      certMat.materializeCertificatesLocal(username, project.getName());
      CertificateMaterializer.CryptoMaterial material = certMat.getUserMaterial(username, project.getName());
      userCert.setUserKey(material.getKeyStore().array());
      userCert.setUserCert(material.getTrustStore().array());
      userCert.setUserKeyPwd(new String(material.getPassword()));
    } catch (IOException | CryptoPasswordNotFoundException ex) {
      throw new RuntimeException("Could not materialize user certificates", ex);
    }
    
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null && userCert.getUserCert().length > 0
        && userCert.getUserKey() != null && userCert.getUserKey().length > 0) {
    
      Map<String, byte[]> certFiles = new HashMap<>();
      certFiles.put(Settings.T_CERTIFICATE, userCert.getUserCert());
      certFiles.put(Settings.K_CERTIFICATE, userCert.getUserKey());
      
      try {
        String kCertName = HopsUtils.getProjectKeystoreName(project.getName(), username);
        String tCertName = HopsUtils.getProjectTruststoreName(project.getName(), username);
        String passName = getProjectMaterialPasswordName(project.getName(), username);

        try {
          if (jobType != null) {
            switch (jobType) {
              case FLINK:
                File appDir = Paths.get(flinkCertsDir, applicationId).toFile();
                if (!appDir.exists()) {
                  appDir.mkdir();
                }
              
                File f_k_cert = new File(appDir.toString() + File.separator +
                    kCertName);
                f_k_cert.setExecutable(false);
                f_k_cert.setReadable(true, true);
                f_k_cert.setWritable(false);
                
                File t_k_cert = new File(appDir.toString() + File.separator +
                    tCertName);
                t_k_cert.setExecutable(false);
                t_k_cert.setReadable(true, true);
                t_k_cert.setWritable(false);

                if (!f_k_cert.exists()) {
                  Files.write(certFiles.get(Settings.K_CERTIFICATE), f_k_cert);
                  Files.write(certFiles.get(Settings.T_CERTIFICATE), t_k_cert);
                }
  
  
                File certPass = new File(appDir.toString() + File.separator +
                    passName);
                certPass.setExecutable(false);
                certPass.setReadable(true, true);
                certPass.setWritable(false);
                FileUtils.writeStringToFile(certPass, userCert
                    .getUserKeyPwd(), false);
                jobSystemProperties.put(Settings.CRYPTO_MATERIAL_PASSWORD,
                    certPass.toString());
                jobSystemProperties.put(Settings.K_CERTIFICATE, f_k_cert.toString());
                jobSystemProperties.put(Settings.T_CERTIFICATE, t_k_cert.toString());
                break;
              case TENSORFLOW:
              case PYSPARK:
              case TFSPARK:
              case SPARK:
                Map<String, File> certs = new HashMap<>();
                certs.put(Settings.K_CERTIFICATE, new File(
                    localTmpDir + File.separator + kCertName));
                certs.put(Settings.T_CERTIFICATE, new File(
                    localTmpDir + File.separator + tCertName));
                certs.put(Settings.CRYPTO_MATERIAL_PASSWORD, new File(
                    localTmpDir + File.separator + passName));
                
                for (Map.Entry<String, File> entry : certs.entrySet()) {
                  //Write the actual file(cert) to localFS
                  //Create HDFS certificate directory. This is done
                  //So that the certificates can be used as LocalResources
                  //by the YarnJob
                  if (!dfso.exists(remoteTmpDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpDir), new FsPermission(
                            FsAction.ALL,
                            FsAction.ALL, FsAction.ALL));
                  }
                  //Put project certificates in its own dir
                  String certUser = project.getName() + "__" + username;
                  String remoteTmpProjDir = remoteTmpDir + File.separator + certUser;
                  if (!dfso.exists(remoteTmpProjDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpProjDir),
                        new FsPermission(FsAction.ALL,
                            FsAction.ALL, FsAction.NONE));
                    dfso.setOwner(new Path(remoteTmpProjDir), certUser, certUser);
                  }
                
                  String remoteProjAppDir = remoteTmpProjDir + File.separator
                      + applicationId;
                  Path remoteProjAppPath = new Path(remoteProjAppDir);
                  if (!dfso.exists(remoteProjAppDir)) {
                    dfso.mkdir(remoteProjAppPath,
                        new FsPermission(FsAction.ALL,
                            FsAction.ALL, FsAction.NONE));
                    dfso.setOwner(remoteProjAppPath, certUser, certUser);
                  }
                
                  dfso.copyToHDFSFromLocal(false, entry.getValue().
                          getAbsolutePath(),
                      remoteProjAppDir + File.separator + entry.getValue().getName());
                
                  dfso.setPermission(new Path(remoteProjAppDir
                          + File.separator
                          + entry.getValue().getName()),
                      new FsPermission(FsAction.ALL, FsAction.NONE,
                          FsAction.NONE));
                  dfso.setOwner(new Path(remoteProjAppDir + File.separator
                      + entry.getValue().getName()), certUser, certUser);
                
                  projectLocalResources.add(new LocalResourceDTO(
                      entry.getKey(),
                      "hdfs://" + remoteProjAppDir + File.separator + entry
                          .getValue().getName(),
                      LocalResourceVisibility.APPLICATION.toString(),
                      LocalResourceType.FILE.toString(), null));
                }
                break;
              default:
                break;
            }
          }
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, "Error writing project user certificates to local fs", ex);
        }
      
      } finally {
        if (jobType != null) {
          certMat.removeCertificatesLocal(username, project.getName());
        }
      }
    }
  }

  /**
   *
   * @param jobName
   * @param dissalowedChars
   * @return
   */
  public static boolean jobNameValidator(String jobName, String dissalowedChars) {
    for (char c : dissalowedChars.toCharArray()) {
      if (jobName.contains("" + c)) {
        return false;
      }
    }
    return true;
  }

  private static Key generateKey(String userKey, String masterKey) {
    // This is for backwards compatibility
    // sha256 of 'adminpw'
    if (masterKey.equals("5fcf82bc15aef42cd3ec93e6d4b51c04df110cf77ee715f62f3f172ff8ed9de9")) {
      return new SecretKeySpec(userKey.substring(0, 16).getBytes(), "AES");
    }
    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      sb.append(userKey.charAt(i));
      if (masterKey.length() > i + 1) {
        sb.append(masterKey.charAt(i + 1));
      } else {
        sb.append(userKey.charAt(Math.max(0, userKey.length() - i)));
      }
    }
    return new SecretKeySpec(sb.toString().getBytes(), "AES");
  }
  
  /**
   *
   * @param key
   * @param plaintext
   * @return
   * @throws Exception
   */
  public static String encrypt(String key, String plaintext, String masterEncryptionPassword)
      throws Exception {
    
    Key aesKey = generateKey(key, masterEncryptionPassword);
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] encrypted = cipher.doFinal(plaintext.getBytes());
    return Base64.encodeBase64String(encrypted);
  }

  /**
   *
   * @param key
   * @param ciphertext
   * @return
   * @throws Exception
   */
  public static String decrypt(String key, String ciphertext, String masterEncryptionPassword)
      throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    Key aesKey = generateKey(key, masterEncryptionPassword);
    cipher.init(Cipher.DECRYPT_MODE, aesKey);
    String decrypted = new String(cipher.doFinal(Base64.decodeBase64(ciphertext)));
    return decrypted;
  }

  /**
   * Generates a pseudo-random password for the user keystore. A list of characters is excluded.
   *
   * @param length
   * @return
   */
  public static String randomString(int length) {
    char[] characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    Random random = new SecureRandom();
    char[] result = new char[length];
    for (int i = 0; i < result.length; i++) {
      // picks a random index out of character set > random character
      int randomCharIndex = random.nextInt(characterSet.length);
      result[i] = characterSet[randomCharIndex];
    }
    return new String(result);
  }

  /**
   * Converts HDFS quota from Bytes to a human friendly format
   * @param quota
   * @return
   */
  public static String spaceQuotaToString(long quota) {
    DecimalFormat df = new DecimalFormat("##.##");

    if(quota > 1099511627776L){
      float tbSize = quota / 1099511627776L;
      return df.format(tbSize) + "TB";
    } else if (quota > 1073741824L){
      float gbSize = quota / 1073741824L;
      return df.format(gbSize) + "GB";
    } else if (quota >= 0){
      float mbSize = quota / 1048576;
      return df.format(mbSize) + "MB";
    } else {
      return "-1MB";
    }
  }

  /**
   * Converts HDFS quota from human friendly format to Bytes
   * @param quotaString
   * @return
   */
  public static long spaceQuotaToLong(String quotaString){
    long quota = -1l;
    if (quotaString.endsWith("TB")) {
      quota = Long.parseLong(quotaString.substring(0, quotaString.length()-2));
      quota = quota * 1099511627776L;
    } else if (quotaString.endsWith("GB")) {
      quota = Long.parseLong(quotaString.substring(0, quotaString.length() - 2));
      quota = quota * 1073741824L;
    } else if (quotaString.endsWith("MB")) {
      quota = Long.parseLong(quotaString.substring(0, quotaString.length()-2));
      quota = quota * 1048576;
    } else {
      quota = Long.parseLong(quotaString);
    }

    return quota;
  }

  /**
   * Convert processing quota from Seconds to human friendly format
   * The format produced is : -?[0-9]{1,}:([0-9]{1,2}:){2}[0-9]{1,2}
   * (Quota might be negative)
   * - Seconds
   * - Minutes
   * - Hours
   * - Days
   * @param quota
   * @return
   */
  public static String procQuotaToString(float quota) {
    float absQuota = Math.abs(quota);
    long days = Math.round(Math.floor(absQuota / 86400));
    absQuota %= 86400;
    long hours = Math.round(Math.floor(absQuota / 3600));
    absQuota %= 3600;
    long minutes = Math.round(Math.floor(absQuota / 60));
    int seconds = Math.round(absQuota % 60);

    if (quota >= 0) {
      return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
    } else {
      return String.format("-%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
    }
  }

  /**
   * Convert processing quota from human friendly to seconds
   * The format accepted is -?[0-9]{1,}:([0-9]{1,2}:){2}[0-9]{1,2}
   * @param quota
   * @return
   */
  public static float procQuotaToFloat(String quota){
    String[] quotaSplits = quota.split(":", 4);
    float quotaSeconds = 0f;
    // Days (might have the - sign)
    quotaSeconds += Math.abs(Float.valueOf(quotaSplits[0])) * 86400;
    // Hours
    quotaSeconds += Float.valueOf(quotaSplits[1]) * 3600;
    // Minutes
    quotaSeconds += Float.valueOf(quotaSplits[2]) * 60;
    // Seconds
    quotaSeconds += Float.valueOf(quotaSplits[3]);

    if (Float.valueOf(quotaSplits[0]) < 0) {
      quotaSeconds *= -1f;
    }

    return quotaSeconds;
  }
  
  /**
   * Parse configuration properties defined by the user in Jupyter dashboard or Job Service.
   *
   * @param sparkProps Spark properties in one string
   * @return Map of property name and value
   */
  public static Map<String, String> parseSparkProperties(String sparkProps) {
    Map<String, String> sparkProperties = new HashMap<>();
    if (sparkProps != null) {
      Arrays.asList(NEW_LINE_PATTERN.split(sparkProps)).stream()
          .map(l -> l.trim())
          .forEach(l -> {
            // User defined properties should be in the form of property_name=value
              Matcher propMatcher = SPARK_PROPS_PATTERN.matcher(l);
              if (propMatcher.matches()) {
                sparkProperties.put(propMatcher.group(1), propMatcher.group(2));
              }
            });
    }
    if (LOG.isLoggable(Level.FINE)) {
      StringBuilder sb = new StringBuilder();
      sb.append("User defined spark properties are: ");
      if (sparkProperties.isEmpty()) {
        sb.append("NONE");
        LOG.log(Level.FINE, sb.toString());
      } else {
        for (Map.Entry<String, String> prop : sparkProperties.entrySet()) {
          sb.append(prop.getKey()).append("=").append(prop.getValue()).append("\n");
        }
        LOG.log(Level.FINE, sb.toString());
      }
    }
    return sparkProperties;
  }
  
  /**
   * Validate user defined properties against a list of blacklisted Spark properties
   * @param sparkProps Parsed user defined properties
   * @param sparkDir spark installation directory
   */
  public static Map<String, String> validateUserProperties(String sparkProps, String sparkDir) throws IOException {
    Map<String, String> userProperties = parseSparkProperties(sparkProps);
    Set<String> blackListedProps = readBlacklistedSparkProperties(sparkDir);
    for (String userProperty : userProperties.keySet()) {
      if (blackListedProps.contains(userProperty)) {
        throw new IllegalArgumentException("User defined property <" + userProperty + "> is blacklisted!");
      }
    }
    return userProperties;
  }
  
  /**
   * Read blacklisted Spark properties from file
   * @return Blacklisted Spark properties
   * @throws IOException
   */
  private static Set<String> readBlacklistedSparkProperties(String sparkDir) throws IOException {
    File sparkBlacklistFile = Paths.get(sparkDir, Settings.SPARK_BLACKLISTED_PROPS).toFile();
    LineIterator lineIterator = FileUtils.lineIterator(sparkBlacklistFile);
    Set<String> blacklistedProps = new HashSet<>();
    try {
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (!line.startsWith("#")) {
          blacklistedProps.add(line);
        }
      }
      return blacklistedProps;
    } finally {
      LineIterator.closeQuietly(lineIterator);
    }
  }
  
  /**
   * Merge system and user defined configuration properties based on the replacement policy of each property
   * @param hopsworksParams System/default properties
   * @param userParameters User defined properties parsed by parseSparkProperties(String sparkProps)
   * @return A map with the replacement pattern and value for each property
   */
  public static Map<String, String> mergeHopsworksAndUserParams(Map<String, ConfigProperty> hopsworksParams,
      Map<String, String> userParameters, boolean isJob) {
    Map<String, String> finalParams = new HashMap<>();
    Set<String> notReplacedUserParams = new HashSet<>();
    
    for (Map.Entry<String, String> userParam : userParameters.entrySet()) {
      if (hopsworksParams.containsKey(userParam.getKey())) {
        ConfigProperty prop = hopsworksParams.get(userParam.getKey());
        prop.replaceValue(userParam.getValue());
        finalParams.put(prop.getReplacementPattern(), prop.getValue());
      } else {
        notReplacedUserParams.add(userParam.getKey());
        if(isJob){
          finalParams.put(userParam.getKey(), userParam.getValue());
        }
      }
    }
    
    String userParamsStr = "";
    if (!notReplacedUserParams.isEmpty()) {
      StringBuilder userParamsSb = new StringBuilder();
      userParamsSb.append(",\n");
      notReplacedUserParams.stream()
          .forEach(p ->
              userParamsSb.append("\"").append(p).append("\": ").append("\"").append(userParameters.get(p))
                  .append("\"," + "\n"));
      
      userParamsStr = userParamsSb.toString();
      // Remove last comma and add a new line char
      userParamsStr = userParamsStr.trim().substring(0, userParamsStr.length() - 2) + "\n";
    }
    finalParams.put("spark_user_defined_properties", userParamsStr);
    
    for (ConfigProperty configProperty : hopsworksParams.values()) {
      finalParams.putIfAbsent(configProperty.getReplacementPattern(), configProperty.getValue());
    }
    
    return finalParams;
  }
  
}
