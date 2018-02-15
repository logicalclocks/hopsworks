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

package io.hops.hopsworks.common.util;

import com.google.common.io.Files;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.net.util.Base64;

/**
 * Utility methods.
 * <p>
 */
public class HopsUtils {

  private static final Logger LOG = Logger.getLogger(HopsUtils.class.getName());
  public static final int ROOT_DIR_PARTITION_KEY = 0;
  public static final short ROOT_DIR_DEPTH = 0;
  public static int RANDOM_PARTITIONING_MAX_LEVEL = 1;
  public static int ROOT_INODE_ID = 1;
  public static int PROJECTS_DIR_DEPTH = 1;
  public static String PROJECTS_DIR_NAME = "Projects";
  
  private static final FsPermission materialPermissions = new FsPermission(FsAction.ALL, FsAction.NONE, FsAction.NONE);
  
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

  public static int fileOrDirPartitionId(int parentId, String name) {
    return parentId;
  }

  public static int projectPartitionId(String name) {
    return calculatePartitionId(ROOT_INODE_ID, PROJECTS_DIR_NAME,
        PROJECTS_DIR_DEPTH);
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
   * @return
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
  
  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
      String localTmpDir, String remoteTmpDir, CertificateMaterializer
      certMat, boolean isRpcTlsEnabled) {
    copyUserKafkaCerts(userCerts, project, username, localTmpDir, remoteTmpDir,
        null, null, null, null, null, null, certMat, isRpcTlsEnabled);
  }

  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
      String localTmpDir, String remoteTmpDir, JobType jobType,
      DistributedFileSystemOps dfso,
      List<LocalResourceDTO> projectLocalResources,
      Map<String, String> jobSystemProperties,
      String applicationId, CertificateMaterializer certMat,
      boolean isRpcTlsEnabled) {
    copyUserKafkaCerts(userCerts, project, username, localTmpDir, remoteTmpDir,
        jobType, dfso, projectLocalResources, jobSystemProperties,
        null, applicationId, certMat, isRpcTlsEnabled);
  }
  
  private static boolean checkUserMatCertsInHDFS(String username, String remoteFSDir,
      DistributedFileSystemOps dfso, Settings settings) throws IOException {
    Path kstoreU = new Path(remoteFSDir + Path.SEPARATOR +
        username + Path.SEPARATOR + username + "__kstore.jks");
    Path tstoreU = new Path(remoteFSDir + Path.SEPARATOR +
        username + Path.SEPARATOR + username + "__tstore.jks");
    Path passwdU = new Path(remoteFSDir + Path.SEPARATOR +
        username + Path.SEPARATOR + username + "__cert.key");

    if (!settings.getHopsRpcTls()) {
      return dfso.exists(kstoreU.toString()) && dfso.exists(tstoreU.toString())
          && dfso.exists(passwdU.toString());
    }

    return dfso.exists(kstoreU.toString()) && dfso.exists(tstoreU.toString());
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
      String remoteFSDir, CertificateMaterializer certificateMaterializer) throws IOException {
    
    certificateMaterializer.removeCertificatesLocal(projectName);
    String remoteDirectory = remoteFSDir + Path.SEPARATOR + projectName + Settings.PROJECT_GENERIC_USER_SUFFIX;
    certificateMaterializer.removeCertificatesRemote(null, projectName, remoteDirectory);
  }

  /**
   * Remote user certificates materialized both from the local
   * filesystem and from HDFS
   * @param username
   * @param remoteFSDir
   * @param certificateMaterializer
   * @throws IOException
   */
  public static void cleanupCertificatesForUser(String username,
      String projectName, String remoteFSDir, CertificateMaterializer certificateMaterializer) throws IOException {

    certificateMaterializer.removeCertificatesLocal(username, projectName);
    String projectSpecificUsername = projectName + HdfsUsersController
        .USER_NAME_DELIMITER + username;
    String remoteDirectory = remoteFSDir + Path.SEPARATOR + projectSpecificUsername;
    certificateMaterializer.removeCertificatesRemote(username, projectName, remoteDirectory);
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
  public static void materializeCertificatesForUser(String projectName, String userName, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer
      certificateMaterializer, Settings settings) throws
      IOException {

    String projectSpecificUsername = projectName + "__" + userName;
  
    certificateMaterializer.materializeCertificatesLocal(userName, projectName);
    
    
    String remoteDirectory = createRemoteDirectory(remoteFSDir, projectSpecificUsername, projectSpecificUsername, dfso);
    certificateMaterializer.materializeCertificatesRemote(userName, projectName, projectSpecificUsername,
        projectSpecificUsername, materialPermissions, remoteDirectory);
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
  
    String remoteDirectory = createRemoteDirectory(remoteFSDir, projectGenericUser, projectGenericUser, dfso);
    
    certificateMaterializer.materializeCertificatesRemote(null, projectName, projectGenericUser,
        projectGenericUser, materialPermissions, remoteDirectory);
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
   * Utility method that copies Kafka user certificates from the Database, to
   * either hdfs to be passed as LocalResources to the YarnJob or to used
   * by another method.
   *
   * @param userCerts
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
  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
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
    
    //Pull the certificate of the client
    /*UserCerts userCert = userCerts.findUserCert(project.getName(),
        username);*/
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null && userCert.getUserCert().length > 0
        && userCert.getUserKey() != null && userCert.getUserKey().length
        > 0) {
    
      Map<String, byte[]> kafkaCertFiles = new HashMap<>();
      kafkaCertFiles.put(Settings.T_CERTIFICATE, userCert.getUserCert());
      kafkaCertFiles.put(Settings.K_CERTIFICATE, userCert.getUserKey());
      
      //Create tmp cert directory if not exists for certificates to be copied to hdfs.
      //Certificates will later be deleted from this directory when copied to HDFS.
      
      // This is done in CertificateMaterializer
      /*File certDir = new File(localTmpDir);
      if (!certDir.exists()) {
        try {
          certDir.setExecutable(false);
          certDir.setReadable(true, true);
          certDir.setWritable(true, true);
          certDir.mkdir();
        } catch (SecurityException ex) {
          LOG.log(Level.SEVERE, ex.getMessage());//handle it
        }
      }*/
      Map<String, File> kafkaCerts = new HashMap<>();
      try {
        String kCertName = HopsUtils.getProjectKeystoreName(project.getName(),
            username);
        String tCertName = HopsUtils.getProjectTruststoreName(project.
            getName(), username);
        String passName = getProjectMaterialPasswordName(project.getName(),
            username);
        // if file doesnt exists, then create it
        try {
          if (jobType == null) {
            // This is done in CertificateMaterializer
            
            //Copy the certificates in the local tmp dir
            /*File kCert = new File(localTmpDir
                + File.separator + kCertName);
            File tCert = new File(localTmpDir
                + File.separator + tCertName);
            if (!kCert.exists()) {
              Files.write(kafkaCertFiles.get(Settings.K_CERTIFICATE),
                  kCert);
              Files.write(kafkaCertFiles.get(Settings.T_CERTIFICATE),
                  tCert);
            }*/
          } else //If it is a Flink job, copy the certificates into the config dir
          {
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
                  Files.write(kafkaCertFiles.get(Settings.K_CERTIFICATE),
                      f_k_cert);
                  Files.write(kafkaCertFiles.get(Settings.T_CERTIFICATE),
                      t_k_cert);
                }
  
                // If RPC TLS is enabled, password file would be injected by the
                // NodeManagers. We don't need to add it as LocalResource
                if (!isRpcTlsEnabled) {
                  File certPass = new File(appDir.toString() + File.separator +
                      passName);
                  certPass.setExecutable(false);
                  certPass.setReadable(true, true);
                  certPass.setWritable(false);
                  FileUtils.writeStringToFile(certPass, userCert
                      .getUserKeyPwd(), false);
                  jobSystemProperties.put(Settings.CRYPTO_MATERIAL_PASSWORD,
                      certPass.toString());
                }
                jobSystemProperties.put(Settings.K_CERTIFICATE, f_k_cert.toString());
                jobSystemProperties.put(Settings.T_CERTIFICATE, t_k_cert.toString());
                break;
              case TENSORFLOW:
              case PYSPARK:
              case TFSPARK:
              case SPARK:
                kafkaCerts.put(Settings.K_CERTIFICATE, new File(
                    localTmpDir + File.separator + kCertName));
                kafkaCerts.put(Settings.T_CERTIFICATE, new File(
                    localTmpDir + File.separator + tCertName));
                // If RPC TLS is enabled, password file would be injected by the
                // NodeManagers. We don't need to add it as LocalResource
                if (!isRpcTlsEnabled) {
                  kafkaCerts.put(Settings.CRYPTO_MATERIAL_PASSWORD, new File(
                      localTmpDir + File.separator + passName));
                }
                for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
                  /*if (!entry.getValue().exists()) {
                    entry.getValue().createNewFile();
                  }*/
                
                  //Write the actual file(cert) to localFS
                  //Create HDFS kafka certificate directory. This is done
                  //So that the certificates can be used as LocalResources
                  //by the YarnJob
                  if (!dfso.exists(remoteTmpDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpDir), new FsPermission(
                            FsAction.ALL,
                            FsAction.ALL, FsAction.ALL));
                  }
                  //Put project certificates in its own dir
                  String certUser = project.getName() + "__"
                      + username;
                  String remoteTmpProjDir = remoteTmpDir + File.separator
                      + certUser;
                  if (!dfso.exists(remoteTmpProjDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpProjDir),
                        new FsPermission(FsAction.ALL,
                            FsAction.ALL, FsAction.NONE));
                    dfso.setOwner(new Path(remoteTmpProjDir),
                        certUser, certUser);
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
                
                  /*Files.write(kafkaCertFiles.get(entry.getKey()), entry.
                      getValue());*/
                  dfso.copyToHDFSFromLocal(false, entry.getValue().
                          getAbsolutePath(),
                      remoteProjAppDir + File.separator
                          + entry.getValue().getName());
                
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
          LOG.log(Level.SEVERE,
              "Error writing Kakfa certificates to local fs", ex);
        }
      
      } finally {
        //In case the certificates where not removed
        /*for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
          if (entry.getValue().exists()) {
            entry.getValue().delete();
          }
        }*/
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
}
