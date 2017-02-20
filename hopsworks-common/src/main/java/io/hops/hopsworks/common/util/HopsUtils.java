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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;

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
    return project + "__" + user + "__kstore.jks";
  }

  public static String getProjectTruststoreName(String project, String user) {
    return project + "__" + user + "__tstore.jks";
  }

  public static void copyUserKafkaCerts(CertsFacade userCerts,
          Project project, String username,
          String localTmpDir, String remoteTmpDir) {
    copyUserKafkaCerts(userCerts, project, username, localTmpDir, remoteTmpDir,
            null, null, null, null, null);
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
   * @param nameNodeIpPort
   */
  public static void copyUserKafkaCerts(CertsFacade userCerts,
          Project project, String username,
          String localTmpDir, String remoteTmpDir, JobType jobType,
          DistributedFileSystemOps dfso,
          List<LocalResourceDTO> projectLocalResources,
          Map<String, String> jobSystemProperties, String nameNodeIpPort) {
    try {
      //Pull the certificate of the client
      UserCerts userCert = userCerts.findUserCert(project.getName(),
              username);
      //Check if the user certificate was actually retrieved
      if (userCert.getUserCert() != null && userCert.getUserCert().length > 0
              && userCert.getUserKey() != null && userCert.getUserKey().length
              > 0) {

        Map<String, byte[]> kafkaCertFiles = new HashMap<>();
        kafkaCertFiles.put(Settings.KAFKA_T_CERTIFICATE, userCert.getUserCert());
        kafkaCertFiles.put(Settings.KAFKA_K_CERTIFICATE, userCert.getUserKey());
        //Create tmp cert directory if not exists for certificates to be copied to hdfs.
        //Certificates will later be deleted from this directory when copied to HDFS. 
        File certDir = new File(localTmpDir);
        if (!certDir.exists()) {
          try {
            certDir.setExecutable(false);
            certDir.setReadable(true, true);
            certDir.setWritable(true, true);
            certDir.mkdir();
          } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());//handle it
          }
        }
        Map<String, File> kafkaCerts = new HashMap<>();
        try {
          String kCertName = HopsUtils.getProjectKeystoreName(project.getName(),
                  username);
          String tCertName = HopsUtils.getProjectTruststoreName(project.
                  getName(), username);

          // if file doesnt exists, then create it
          try {
            if (jobType == null) {
              //Copy the certificates in the local tmp dir
              File kCert = new File(localTmpDir
                      + File.separator + kCertName);
//              kCert.setExecutable(false);
//              kCert.setReadable(true, true);
//              kCert.setWritable(false);
              File tCert = new File(localTmpDir
                      + File.separator + tCertName);
//              tCert.setExecutable(false);
//              tCert.setReadable(true, true);
//              tCert.setWritable(false);
              if (!kCert.exists()) {
                Files.write(kafkaCertFiles.get(Settings.KAFKA_K_CERTIFICATE),
                        kCert);
                Files.write(kafkaCertFiles.get(Settings.KAFKA_T_CERTIFICATE),
                        tCert);
              }
            } else //If it is a Flink job, copy the certificates into the glassfish config dir
            {
              switch (jobType) {
                case FLINK:
                  File f_k_cert = new File(Settings.FLINK_KAFKA_CERTS_DIR
                          + File.separator + kCertName);
                  f_k_cert.setExecutable(false);
                  f_k_cert.setReadable(true, true);
                  f_k_cert.setWritable(false);
                  File t_k_cert = new File(Settings.FLINK_KAFKA_CERTS_DIR
                          + File.separator + tCertName);
                  t_k_cert.setExecutable(false);
                  t_k_cert.setReadable(true, true);
                  t_k_cert.setWritable(false);
                  if (!f_k_cert.exists()) {
                    Files.write(kafkaCertFiles.get(
                            Settings.KAFKA_K_CERTIFICATE),
                            f_k_cert);
                    Files.write(kafkaCertFiles.get(
                            Settings.KAFKA_T_CERTIFICATE),
                            t_k_cert);
                  }
                  jobSystemProperties.put(Settings.KAFKA_K_CERTIFICATE,
                          Settings.FLINK_KAFKA_CERTS_DIR
                          + File.separator + kCertName);
                  jobSystemProperties.put(Settings.KAFKA_T_CERTIFICATE,
                          Settings.FLINK_KAFKA_CERTS_DIR
                          + File.separator + tCertName);
                  break;
                case SPARK:
                  kafkaCerts.put(Settings.KAFKA_K_CERTIFICATE, new File(
                          localTmpDir + File.separator + kCertName));
                  kafkaCerts.put(Settings.KAFKA_T_CERTIFICATE, new File(
                          localTmpDir + File.separator + tCertName));
                  for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
                    if (!entry.getValue().exists()) {
                      entry.getValue().createNewFile();
                    }

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
                    Files.write(kafkaCertFiles.get(entry.getKey()), entry.
                            getValue());
                    dfso.copyToHDFSFromLocal(true, entry.getValue().
                            getAbsolutePath(),
                            remoteTmpDir + File.separator + certUser
                            + File.separator
                            + entry.getValue().getName());

                    dfso.setPermission(new Path(remoteTmpProjDir
                            + File.separator
                            + entry.getValue().getName()),
                            new FsPermission(FsAction.ALL, FsAction.NONE,
                                    FsAction.NONE));
                    dfso.setOwner(new Path(remoteTmpProjDir + File.separator
                            + entry.getValue().getName()), certUser, certUser);

                    projectLocalResources.add(new LocalResourceDTO(
                            entry.getKey(),
                            "hdfs://" + nameNodeIpPort + remoteTmpDir
                            + File.separator + certUser + File.separator
                            + entry.getValue().getName(),
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
          for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
            if (entry.getValue().exists()) {
              entry.getValue().delete();
            }
          }
        }
      }
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

}
