/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.featurestore.DatabricksInstanceFacade;
import io.hops.hopsworks.featurestore.databricks.client.DbCluster;
import io.hops.hopsworks.featurestore.databricks.client.DatabricksClient;
import io.hops.hopsworks.featurestore.databricks.client.DbClusterStart;
import io.hops.hopsworks.featurestore.databricks.client.DbLibrary;
import io.hops.hopsworks.featurestore.databricks.client.DbLibraryInstall;
import io.hops.hopsworks.featurestore.databricks.client.DbPyPiLibrary;
import io.hops.hopsworks.featurestore.databricks.client.DbfsCreate;
import io.hops.hopsworks.featurestore.databricks.client.DbfsPut;
import io.hops.hopsworks.persistence.entity.certificates.UserCerts;
import io.hops.hopsworks.persistence.entity.integrations.DatabricksInstance;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;
import io.hops.hopsworks.servicediscovery.tags.HiveTags;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.security.ssl.SSLFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatabricksController {

  public static final String HOPSWORKS_PROJECT_TAG = "hopsworks_project_id";
  public static final String HOPSWORKS_USER_TAG = "hopsworks_user";

  private static final String TOKEN_PREFIX = "db_";
  private static final String DBFS_SCHEME = "dbfs://";
  private static final String DBFS_VM = "/dbfs";
  private static final Logger LOGGER = Logger.getLogger(DatabricksController.class.getName());

  private static final String SPARK_CONF_PREFIX = "spark.hadoop.";

  private static final String HIVE_PACKAGE_NAME = "apache-hive";
  private static final String HUDI_PACKAGE_NAME = "hudi";

  private static final String CLIENT_DIR = "client";

  @EJB
  private SecretsController secretsController;
  @EJB
  private DatabricksClient databricksClient;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DatabricksInstanceFacade databricksInstanceFacade;

  public List<DbCluster> listClusters(Users user, String dbInstanceUrl) throws UserException, FeaturestoreException {
    String token = secretsController.get(user, TOKEN_PREFIX + dbInstanceUrl).getPlaintext();
    return databricksClient.listClusters(dbInstanceUrl, token);
  }

  public DbCluster getCluster(Users user, String dbInstanceUrl, String clusterId)
      throws UserException, FeaturestoreException {
    String token = secretsController.get(user, TOKEN_PREFIX + dbInstanceUrl).getPlaintext();
    return databricksClient.getCluster(dbInstanceUrl, clusterId, token);
  }

  public String getNotebookJupyter(Users user, String dbInstanceUrl, String path)
      throws UserException, FeaturestoreException {
    Optional<DatabricksInstance> dbInstanceOpt = databricksInstanceFacade.getInstance(user, dbInstanceUrl);
    if (!dbInstanceOpt.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_INSTANCE_NOT_EXISTS,
          Level.FINE, "Instance URL: " + dbInstanceUrl);
    }

    String token = secretsController.get(user, TOKEN_PREFIX + dbInstanceUrl).getPlaintext();
    return databricksClient.getNotebookJupyter(dbInstanceUrl, token, path);
  }

  public byte[] getNotebookArchive(Users user, String dbInstanceUrl, String path)
      throws UserException, FeaturestoreException {
    Optional<DatabricksInstance> dbInstanceOpt = databricksInstanceFacade.getInstance(user, dbInstanceUrl);
    if (!dbInstanceOpt.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_INSTANCE_NOT_EXISTS,
          Level.FINE, "Instance URL: " + dbInstanceUrl);
    }

    String token = secretsController.get(user, TOKEN_PREFIX + dbInstanceUrl).getPlaintext();
    return databricksClient.getNotebookArchive(dbInstanceUrl, token, path);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public DatabricksInstance registerInstance(Users user, String dbInstanceUrl, String apiKey)
      throws FeaturestoreException, UserException {
    Optional<DatabricksInstance> dbInstanceOpt = databricksInstanceFacade.getInstance(user, dbInstanceUrl);
    if (dbInstanceOpt.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_INSTANCE_ALREADY_EXISTS,
          Level.FINE, "Instance URL: " + dbInstanceUrl);
    }

    Secret secret = secretsController
        .add(user, TOKEN_PREFIX + dbInstanceUrl, apiKey, VisibilityType.PRIVATE, null);
    DatabricksInstance dbInstance = new DatabricksInstance(dbInstanceUrl, secret, user);
    databricksInstanceFacade.add(dbInstance);
    return dbInstance;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void deleteInstance(Users user, String dbInstanceUrl) throws FeaturestoreException, UserException {
    String secretName = TOKEN_PREFIX + dbInstanceUrl;
    secretsController.delete(user, secretName);
    DatabricksInstance dbInstance = databricksInstanceFacade.getInstance(user, dbInstanceUrl)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_INSTANCE_NOT_EXISTS,
            Level.FINE, "Instance URL: " + dbInstanceUrl));
    databricksInstanceFacade.delete(dbInstance);
  }

  // Configuring a Databricks cluster requires the following steps:
  // - Upload artifacts (client.tgz and initScript.sh)
  // - Configure the Spark properties and initScript
  // - Install Python HSFS library
  // - Restart the cluster
  public DbCluster configureCluster(String dbInstanceUrl, String clusterId, Users user,
                                    Users targetUser, Project project)
      throws UserException, IOException, ServiceDiscoveryException, FeaturestoreException, ServiceException {
    String token = secretsController.get(user, TOKEN_PREFIX + dbInstanceUrl).getPlaintext();
    Path baseDbfsPath = buildBaseDbfsPath(project, targetUser);
    Path baseClientDbfsPath = buildClientBaseDbfsPath();

    DbCluster dbCluster = databricksClient.getCluster(dbInstanceUrl, clusterId, token);

    // Upload client jars
    List<String> dbfsJars = uploadClientJars(dbInstanceUrl, baseClientDbfsPath, token);

    // Upload or refresh the certificates on dbfs
    uploadCertificastes(dbInstanceUrl, targetUser, project, baseDbfsPath, token);

    // edit cluster configuration
    editCluster(dbInstanceUrl, dbCluster, project, targetUser, baseDbfsPath, token);

    // Start cluster before installing libraries
    if (dbCluster.getState().equalsIgnoreCase("TERMINATED")) {
      startCluster(dbInstanceUrl, dbCluster, token);
    }

    waitForCuster(dbInstanceUrl, clusterId, token);

    // installing libraries requires a running cluster (?)
    installLibraries(dbInstanceUrl, dbCluster.getId(), dbfsJars, token);

    return databricksClient.getCluster(dbInstanceUrl, dbCluster.getId(), token);
  }

  private void waitForCuster(String dbInstanceUrl, String clusterId, String token)
      throws IOException, FeaturestoreException {
    // wait for cluster to start
    int i = 5;
    while (i > 0) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) { }

      DbCluster dbCluster = databricksClient.getCluster(dbInstanceUrl, clusterId, token);
      if (!dbCluster.getState().equalsIgnoreCase("TERMINATED")) {
        break;
      }
      i--;
    }
    if (i == 0) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_CANNOT_START_CLUSTER, Level.FINE);
    }
  }

  private void uploadCertificastes(String dbInstanceUrl, Users user, Project project,
                                   Path baseDbfsPath, String token) throws FeaturestoreException, IOException {
    String projectUser = hdfsUsersController.getHdfsUserName(project, user);
    CertificateMaterializer.CryptoMaterial certs = getCertificates(user, project);

    DbfsPut keyStorePut = new DbfsPut(baseDbfsPath.resolve(projectUser + Settings.KEYSTORE_SUFFIX).toString(),
        certs.getKeyStore().array(), true);
    databricksClient.uploadOneShot(dbInstanceUrl, keyStorePut, token);

    DbfsPut trustStorePut = new DbfsPut(baseDbfsPath.resolve(projectUser + Settings.TRUSTSTORE_SUFFIX).toString(),
        certs.getTrustStore().array(), true);
    databricksClient.uploadOneShot(dbInstanceUrl, trustStorePut, token);

    DbfsPut passwordPut = new DbfsPut(baseDbfsPath.resolve(projectUser + Settings.CERT_PASS_SUFFIX).toString(),
        new String(certs.getPassword()).getBytes(), true);
    databricksClient.uploadOneShot(dbInstanceUrl, passwordPut, token);
  }

  private List<String> uploadClientJars(String dbInstanceUrl, Path baseClientDbfsPath, String token)
      throws FeaturestoreException, IOException {
    // Iterate over all files in the client dir and uploads them to Dbfs.
    // Return a list of files available on dbfs

    // TODO(Fabio): In the future, use managed executor here.
    List<String> dbfsJars = new ArrayList<>();
    File clientDir = Paths.get(settings.getClientPath(), CLIENT_DIR).toFile();
    for (File f : clientDir.listFiles()) {
      if (!f.getName().contains(HIVE_PACKAGE_NAME) && !f.getName().contains(HUDI_PACKAGE_NAME)) {
        dbfsJars.add(uploadClientJar(dbInstanceUrl, baseClientDbfsPath, f.getName(), f.getAbsolutePath(), token));
      }
    }

    return dbfsJars;
  }

  private String uploadClientJar(String dbInstanceUrl, Path baseClientDbfsPath, String fileName,
                               String filePath, String token) throws FeaturestoreException, IOException {
    String dbfsFilePath = baseClientDbfsPath.resolve(fileName).toString();
    if (databricksClient.fileExists(dbInstanceUrl, dbfsFilePath, token)) {
      // The library is already available on dbfs, just return the path
      return dbfsFilePath;
    }

    DbfsCreate dbfsCreate = new DbfsCreate(dbfsFilePath, false);
    try (InputStream inputStream = new FileInputStream(filePath)){
      databricksClient.uploadLarge(dbInstanceUrl, dbfsCreate, inputStream, token);
    }

    return dbfsFilePath;
  }

  private void editCluster(String dbInstanceUrl, DbCluster dbCluster, Project project,
                           Users user, Path baseDbfsPath, String token)
      throws FeaturestoreException, IOException, ServiceDiscoveryException {
    if (dbCluster.getSparkConfiguration() == null) {
      dbCluster.setSparkConfiguration(new HashMap<>());
    }
    dbCluster.getSparkConfiguration().putAll(getSparkProperties(project, user, baseDbfsPath));

    if (dbCluster.getTags() == null) {
      dbCluster.setTags(new HashMap<>());
    }
    dbCluster.getTags().put(HOPSWORKS_PROJECT_TAG, String.valueOf(project.getId()));
    dbCluster.getTags().put(HOPSWORKS_USER_TAG, user.getUsername());
    databricksClient.editCluster(dbInstanceUrl, dbCluster, token);
  }

  private Map<String, String> getSparkProperties(Project project, Users user,
                                                 Path baseDbfsPath) throws ServiceDiscoveryException {
    String projectUser = hdfsUsersController.getHdfsUserName(project, user);
    return getSparkProperties(DBFS_VM + baseDbfsPath.resolve(projectUser + Settings.KEYSTORE_SUFFIX),
        DBFS_VM + baseDbfsPath.resolve(projectUser + Settings.TRUSTSTORE_SUFFIX),
        DBFS_VM + baseDbfsPath.resolve(projectUser + Settings.CERT_PASS_SUFFIX), null);
  }

  public Map<String, String> getSparkProperties(String keyStorePath, String trustStorePath,
                                                String passwordPath, String jarPath) throws ServiceDiscoveryException {
    Map<String, String> sparkConfiguration = new HashMap<>();
    sparkConfiguration.put("spark.hadoop.fs.hopsfs.impl", "io.hops.hopsfs.client.HopsFileSystem");
    sparkConfiguration.put("spark.hadoop.hops.ipc.server.ssl.enabled", "true");
    sparkConfiguration.put("spark.hadoop.hops.ssl.hostname.verifier", "ALLOW_ALL");
    sparkConfiguration.put("spark.hadoop.hops.rpc.socket.factory.class.default",
        "io.hops.hadoop.shaded.org.apache.hadoop.net.HopsSSLSocketFactory");
    sparkConfiguration.put(SPARK_CONF_PREFIX + HopsSSLSocketFactory.CryptoKeys.SOCKET_ENABLED_PROTOCOL.getValue(),
        "TLSv1.2");
    sparkConfiguration.put(SPARK_CONF_PREFIX + SSLFactory.LOCALIZED_PASSWD_FILE_PATH_KEY, passwordPath);
    sparkConfiguration.put(SPARK_CONF_PREFIX + SSLFactory.LOCALIZED_KEYSTORE_FILE_PATH_KEY, keyStorePath);
    sparkConfiguration.put(SPARK_CONF_PREFIX + SSLFactory.LOCALIZED_TRUSTSTORE_FILE_PATH_KEY, trustStorePath);
    sparkConfiguration.put("spark.serializer", "org.apache.spark.serializer.KryoSerializer");

    if (jarPath != null) {
      sparkConfiguration.put("spark.sql.hive.metastore.jars", jarPath);

      Service hiveMetastoreService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          HopsworksService.HIVE.getNameWithTag(HiveTags.metastore));

      sparkConfiguration.put("spark.hadoop.hive.metastore.uris",
          "thrift://" + hiveMetastoreService.getAddress() + ":" + hiveMetastoreService.getPort());
    }

    return sparkConfiguration;
  }

  private void startCluster(String dbInstanceUrl, DbCluster dbCluster, String token)
      throws FeaturestoreException, IOException {
    DbClusterStart dbClusterStart = new DbClusterStart(dbCluster.getId());
    databricksClient.startCluster(dbInstanceUrl, dbClusterStart, token);
  }

  private void installLibraries(String dbInstanceUrl, String clusterId, List<String> dbfsJars, String token)
      throws FeaturestoreException, IOException {
    DbLibraryInstall dbLibraryInstall = new DbLibraryInstall(clusterId);

    // Hops has an additional digit at the end
    // this is just a temporary hack, we'll get rid of hops in the next release
    List<DbLibrary> libraries = new ArrayList<>();
    libraries.add(new DbLibrary(new DbPyPiLibrary("hsfs~=" + getPyPiLibraryVersion())));

    // Add all the libraries that have been uploaded
    libraries.addAll(dbfsJars.stream()
        .filter(jar -> !jar.contains(HIVE_PACKAGE_NAME) && !jar.contains(HUDI_PACKAGE_NAME))
        .map(jar -> DBFS_SCHEME + jar)
        .map(DbLibrary::new)
        .collect(Collectors.toList()));

    dbLibraryInstall.setLibraries(libraries);
    databricksClient.installLibraries(dbInstanceUrl, dbLibraryInstall, token);
  }

  private String getPyPiLibraryVersion() {
    String[] versionDigits = settings.getHopsworksVersion().split("\\.");
    return versionDigits[0] + "." + versionDigits[1] + ".0";
  }

  private CertificateMaterializer.CryptoMaterial getCertificates(Users user, Project project) throws IOException {
    UserCerts projectCerts = certsFacade.findUserCert(project.getName(), user.getUsername());
    ByteBuffer keyStore = ByteBuffer.wrap(projectCerts.getUserKey());
    ByteBuffer trustStore = ByteBuffer.wrap(projectCerts.getUserCert());
    String decryptedPassword;
    try {
      decryptedPassword = HopsUtils.decrypt(user.getPassword(), projectCerts.getUserKeyPwd(),
          certificatesMgmService.getMasterEncryptionPassword());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error while decrypting certificate password for user <" + user.getUsername() + ">");
      throw new IOException(e);
    }

    return new CertificateMaterializer.CryptoMaterial(keyStore, trustStore, decryptedPassword.toCharArray());
  }

  private Path buildBaseDbfsPath(Project project, Users user) throws ServiceException {
    try {
      return Paths.get("/hopsworks",
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks)).getAddress(),
          hdfsUsersController.getHdfsUserName(project, user));
    } catch (ServiceDiscoveryException se) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_DISCOVERY_ERROR, Level.SEVERE,
          "Cannot resolve Hopsworks service", se.getMessage(), se);
    }
  }

  private Path buildClientBaseDbfsPath() throws ServiceException {
    try {
      return Paths.get("/hopsworks",
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks)).getAddress());
    } catch (ServiceDiscoveryException se) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_DISCOVERY_ERROR, Level.SEVERE,
          "Cannot resolve Hopsworks service", se.getMessage(), se);
    }
  }
}
