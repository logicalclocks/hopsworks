package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import static io.hops.hopsworks.common.dao.kafka.KafkaFacade.DLIMITER;
import static io.hops.hopsworks.common.dao.kafka.KafkaFacade.SLASH_SEPARATOR;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.dela.DelaClientType;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Settings implements Serializable {

  private static final Logger logger = Logger.getLogger(Settings.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @PostConstruct
  private void init() {
    try {
      setKafkaBrokers(getBrokerEndpoints());
    } catch (AppException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  public static String AGENT_EMAIL = "kagent@hops.io";
  public static final String SITE_EMAIL = "admin@kth.se";
  /**
   * Global Variables taken from the DB
   */
  private static final String VARIABLE_PYTHON_KERNEL = "python_kernel";
  private static final String VARIABLE_HADOOP_VERSION = "hadoop_version";
  private static final String VARIABLE_JAVA_HOME = "JAVA_HOME";
  private static final String VARIABLE_HOPSWORKS_IP = "hopsworks_ip";
  private static final String VARIABLE_HOPSWORKS_PORT = "hopsworks_port";
  private static final String VARIABLE_KIBANA_IP = "kibana_ip";
  private static final String VARIABLE_LIVY_IP = "livy_ip";
  private static final String VARIABLE_LIVY_ZEPPELIN_SESSION_TIMEOUT = "livy_zeppelin_session_timeout";
  private static final String VARIABLE_ZEPPELIN_INTERPRETERS = "zeppelin_interpreters";
  private static final String VARIABLE_JHS_IP = "jhs_ip";
  private static final String VARIABLE_RM_IP = "rm_ip";
  private static final String VARIABLE_RM_PORT = "rm_port";
  private static final String VARIABLE_LOGSTASH_IP = "logstash_ip";
  private static final String VARIABLE_LOGSTASH_PORT = "logstash_port";
  private static final String VARIABLE_OOZIE_IP = "oozie_ip";
  private static final String VARIABLE_SPARK_HISTORY_SERVER_IP
      = "spark_history_server_ip";
  private static final String VARIABLE_ELASTIC_IP = "elastic_ip";
  private static final String VARIABLE_ELASTIC_PORT = "elastic_port";
  private static final String VARIABLE_ELASTIC_REST_PORT = "elastic_rest_port";
  private static final String VARIABLE_SPARK_USER = "spark_user";
  private static final String VARIABLE_YARN_SUPERUSER = "yarn_user";
  private static final String VARIABLE_HDFS_SUPERUSER = "hdfs_user";
  private static final String VARIABLE_HOPSWORKS_USER = "hopsworks_user";
  private static final String VARIABLE_STAGING_DIR = "staging_dir";
  private static final String VARIABLE_ZEPPELIN_DIR = "zeppelin_dir";
  private static final String VARIABLE_ZEPPELIN_PROJECTS_DIR
      = "zeppelin_projects_dir";
  private static final String VARIABLE_ZEPPELIN_SYNC_INTERVAL
      = "zeppelin_sync_interval";
  private static final String VARIABLE_ZEPPELIN_USER = "zeppelin_user";
  private static final String VARIABLE_JUPYTER_DIR = "jupyter_dir";
  private static final String VARIABLE_SPARK_DIR = "spark_dir";
  private static final String VARIABLE_FLINK_DIR = "flink_dir";
  private static final String VARIABLE_FLINK_USER = "flink_user";
  private static final String VARIABLE_NDB_DIR = "ndb_dir";
  private static final String VARIABLE_MYSQL_DIR = "mysql_dir";
  private static final String VARIABLE_HADOOP_DIR = "hadoop_dir";
  private static final String VARIABLE_HOPSWORKS_DIR = "hopsworks_dir";
  private static final String VARIABLE_YARN_DEFAULT_QUOTA = "yarn_default_quota";
  private static final String VARIABLE_HDFS_DEFAULT_QUOTA = "hdfs_default_quota";
  private static final String VARIABLE_MAX_NUM_PROJ_PER_USER
      = "max_num_proj_per_user";

  // HIVE configuration variables
  private static final String VARIABLE_HIVE_SERVER_HOSTNAME = "hiveserver_ssl_hostname";
  private static final String VARIABLE_HIVE_SERVER_HOSTNAME_EXT
      = "hiveserver_ext_hostname";
  private static final String VARIABLE_HIVE_SUPERUSER = "hive_superuser";
  private static final String VARIABLE_HIVE_WAREHOUSE = "hive_warehouse";
  private static final String VARIABLE_HIVE_SCRATCHDIR = "hive_scratchdir";
  private static final String VARIABLE_HIVE_DEFAULT_QUOTA = "hive_default_quota";
  private static final String VARIABLE_HIVE_LLAP_SLIDER_DIR = "hive_llap_slider_dir";
  private static final String VARIABLE_HIVE_LLAP_LOCAL_DIR = "hive_llap_local_dir";
  public static final String VARIABLE_LLAP_APP_ID = "hive_llap_app_id";
  public static final String VARIABLE_LLAP_START_PROC = "hive_llap_start_proc";

  private static final String VARIABLE_ADAM_USER = "adam_user";
  private static final String VARIABLE_ADAM_DIR = "adam_dir";
  private static final String VARIABLE_TWOFACTOR_AUTH = "twofactor_auth";
  private static final String VARIABLE_TWOFACTOR_EXCLUD = "twofactor-excluded-groups";
  private static final String VARIABLE_KAFKA_DIR = "kafka_dir";
  private static final String VARIABLE_KAFKA_USER = "kafka_user";
  private static final String VARIABLE_ZK_DIR = "zk_dir";
  private static final String VARIABLE_ZK_USER = "zk_user";
  private static final String VARIABLE_ZK_IP = "zk_ip";
  private static final String VARIABLE_KAFKA_IP = "kafka_ip";
  private static final String VARIABLE_DRELEPHANT_IP = "drelephant_ip";
  private static final String VARIABLE_DRELEPHANT_DB = "drelephant_db";
  private static final String VARIABLE_DRELEPHANT_PORT = "drelephant_port";
  private static final String VARIABLE_YARN_WEB_UI_IP = "yarn_ui_ip";
  private static final String VARIABLE_HDFS_WEB_UI_IP = "hdfs_ui_ip";
  private static final String VARIABLE_HDFS_WEB_UI_PORT = "hdfs_ui_port";
  private static final String VARIABLE_YARN_WEB_UI_PORT = "yarn_ui_port";
  private static final String VARIABLE_FILE_PREVIEW_IMAGE_SIZE
      = "file_preview_image_size";
  private static final String VARIABLE_FILE_PREVIEW_TXT_SIZE
      = "file_preview_txt_size";
  private static final String VARIABLE_HOPSWORKS_REST_ENDPOINT
      = "hopsworks_endpoint";
  private static final String VARIABLE_REST_PORT = "rest_port";
  private static final String VARIABLE_HOPS_RPC_TLS = "hops_rpc_tls";
  public static final String ERASURE_CODING_CONFIG = "erasure-coding-site.xml";

  private static final String VARIABLE_KAFKA_NUM_PARTITIONS
      = "kafka_num_partitions";
  private static final String VARIABLE_KAFKA_NUM_REPLICAS = "kafka_num_replicas";
  private static final String VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD
      = "hopsworks_master_password";
  private static final String VARIABLE_GLASSFISH_CERT_CENERATED
      = "glassfish_cert";
  private static final String VARIABLE_CUDA_DIR = "conda_dir";
  private static final String VARIABLE_ANACONDA_USER = "anaconda_user";
  private static final String VARIABLE_ANACONDA_DIR = "anaconda_dir";
  private static final String VARIABLE_ANACONDA_INSTALLED = "anaconda_enabled";

  private static final String VARIABLE_HOPSUTIL_VERSION = "hopsutil_version";

  private static final String VARIABLE_INFLUXDB_IP = "influxdb_ip";
  private static final String VARIABLE_INFLUXDB_PORT = "influxdb_port";
  private static final String VARIABLE_INFLUXDB_USER = "influxdb_user";
  private static final String VARIABLE_INFLUXDB_PW = "influxdb_pw";
  private static final String VARIABLE_ANACONDA_ENV = "anaconda_env";
  private static final String VARIABLE_GRAPHITE_PORT = "graphite_port";
  private static final String VARIABLE_RESOURCE_DIRS = "resources";
  private static final String VARIABLE_CERTS_DIRS = "certs_dir";
  private static final String VARIABLE_VAGRANT_ENABLED = "vagrant_enabled";
  private static final String VARIABLE_MAX_STATUS_POLL_RETRY = "max_status_poll_retry";
  private static final String VARIABLE_CERT_MATER_DELAY = "cert_mater_delay";
  private static final String VARIABLE_WHITELIST_USERS_LOGIN = "whitelist_users";
  private static final String VARIABLE_RECOVERY_PATH = "recovery_endpoint";
  private static final String VARIABLE_VERIFICATION_PATH = "verification_endpoint";
  private static final String VARIABLE_ALERT_EMAIL_ADDRS = "alert_email_addrs";

  private String setVar(String varName, String defaultValue) {
    Variables userName = findById(varName);
    if (userName != null && userName.getValue() != null && (userName.getValue().
        isEmpty() == false)) {
      String user = userName.getValue();
      if (user != null && user.isEmpty() == false) {
        return user;
      }
    }
    return defaultValue;
  }

  private String setStrVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      String val = var.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setDirVar(String varName, String defaultValue) {
    Variables dirName = findById(varName);
    if (dirName != null && dirName.getValue() != null && (new File(dirName.
        getValue()).isDirectory())) {
      String val = dirName.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setIpVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null && Ip.validIp(var.getValue())) {
      String val = var.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setDbVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      // TODO - check this is a valid DB name
      String val = var.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private Boolean setBoolVar(String varName, Boolean defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      String val = var.getValue();
      if (val != null && val.isEmpty() == false) {
        return Boolean.parseBoolean(val);
      }
    }
    return defaultValue;
  }

  private Integer setIntVar(String varName, Integer defaultValue) {
    Variables var = findById(varName);
    try {
      if (var != null && var.getValue() != null) {
        String val = var.getValue();
        if (val != null && val.isEmpty() == false) {
          return Integer.parseInt(val);
        }
      }
    } catch (NumberFormatException ex) {
      logger.info("Error - not an integer! " + varName
          + " should be an integer. Value was " + defaultValue);
    }
    return defaultValue;
  }

  private long setLongVar(String varName, Long defaultValue) {
    Variables var = findById(varName);
    try {
      if (var != null && var.getValue() != null) {
        String val = var.getValue();
        if (val != null && val.isEmpty() == false) {
          return Long.parseLong(val);
        }
      }
    } catch (NumberFormatException ex) {
      logger.info("Error - not a long! " + varName
          + " should be an integer. Value was " + defaultValue);
    }

    return defaultValue;
  }

  private boolean cached = false;

  private void populateCache() {
    if (!cached) {
      PYTHON_KERNEL = setBoolVar(VARIABLE_PYTHON_KERNEL, PYTHON_KERNEL);
      JAVA_HOME = setVar(VARIABLE_JAVA_HOME, JAVA_HOME);
      TWOFACTOR_AUTH = setVar(VARIABLE_TWOFACTOR_AUTH, TWOFACTOR_AUTH);
      TWOFACTOR_EXCLUDE = setVar(VARIABLE_TWOFACTOR_EXCLUD, TWOFACTOR_EXCLUDE);
      HOPSWORKS_USER = setVar(VARIABLE_HOPSWORKS_USER, HOPSWORKS_USER);
      HDFS_SUPERUSER = setVar(VARIABLE_HDFS_SUPERUSER, HDFS_SUPERUSER);
      YARN_SUPERUSER = setVar(VARIABLE_YARN_SUPERUSER, YARN_SUPERUSER);
      SPARK_USER = setVar(VARIABLE_SPARK_USER, SPARK_USER);
      SPARK_DIR = setDirVar(VARIABLE_SPARK_DIR, SPARK_DIR);
      FLINK_USER = setVar(VARIABLE_FLINK_USER, FLINK_USER);
      FLINK_DIR = setDirVar(VARIABLE_FLINK_DIR, FLINK_DIR);
      STAGING_DIR = setDirVar(VARIABLE_STAGING_DIR, STAGING_DIR);
      HOPSUTIL_VERSION = setVar(VARIABLE_HOPSUTIL_VERSION, HOPSUTIL_VERSION);
      HIVE_SERVER_HOSTNAME = setStrVar(VARIABLE_HIVE_SERVER_HOSTNAME,
          HIVE_SERVER_HOSTNAME);
      HIVE_SERVER_HOSTNAME_EXT = setStrVar(VARIABLE_HIVE_SERVER_HOSTNAME_EXT,
          HIVE_SERVER_HOSTNAME_EXT);
      HIVE_SUPERUSER = setStrVar(VARIABLE_HIVE_SUPERUSER, HIVE_SUPERUSER);
      HIVE_WAREHOUSE = setStrVar(VARIABLE_HIVE_WAREHOUSE, HIVE_WAREHOUSE);
      HIVE_LLAP_SLIDER_DIR = setStrVar(VARIABLE_HIVE_LLAP_SLIDER_DIR, HIVE_LLAP_SLIDER_DIR);
      HIVE_LLAP_LOCAL_FS_DIR = setStrVar(VARIABLE_HIVE_LLAP_LOCAL_DIR, HIVE_LLAP_LOCAL_FS_DIR);
      HIVE_SCRATCHDIR = setStrVar(VARIABLE_HIVE_SCRATCHDIR, HIVE_SCRATCHDIR);
      HIVE_DB_DEFAULT_QUOTA = setStrVar(VARIABLE_HIVE_DEFAULT_QUOTA, HIVE_DB_DEFAULT_QUOTA);
      ALERT_EMAIL_ADDRS = setStrVar(VARIABLE_ALERT_EMAIL_ADDRS, "");
      ZEPPELIN_USER = setVar(VARIABLE_ZEPPELIN_USER, ZEPPELIN_USER);
      ZEPPELIN_DIR = setDirVar(VARIABLE_ZEPPELIN_DIR, ZEPPELIN_DIR);
      ZEPPELIN_PROJECTS_DIR = setDirVar(VARIABLE_ZEPPELIN_PROJECTS_DIR,
          ZEPPELIN_PROJECTS_DIR);
      ZEPPELIN_SYNC_INTERVAL = setLongVar(VARIABLE_ZEPPELIN_SYNC_INTERVAL,
          ZEPPELIN_SYNC_INTERVAL);
      HADOOP_VERSION = setVar(VARIABLE_HADOOP_VERSION, HADOOP_VERSION);
      JUPYTER_DIR = setDirVar(VARIABLE_JUPYTER_DIR, JUPYTER_DIR);
      ADAM_USER = setVar(VARIABLE_ADAM_USER, ADAM_USER);
      ADAM_DIR = setDirVar(VARIABLE_ADAM_DIR, ADAM_DIR);
      MYSQL_DIR = setDirVar(VARIABLE_MYSQL_DIR, MYSQL_DIR);
      HADOOP_DIR = setDirVar(VARIABLE_HADOOP_DIR, HADOOP_DIR);
      HOPSWORKS_INSTALL_DIR = setDirVar(VARIABLE_HOPSWORKS_DIR,
          HOPSWORKS_INSTALL_DIR);
      CERTS_DIR = setDirVar(VARIABLE_CERTS_DIRS, CERTS_DIR);
      NDB_DIR = setDirVar(VARIABLE_NDB_DIR, NDB_DIR);
      ELASTIC_IP = setIpVar(VARIABLE_ELASTIC_IP, ELASTIC_IP);
      ELASTIC_PORT = setIntVar(VARIABLE_ELASTIC_PORT, ELASTIC_PORT);
      ELASTIC_REST_PORT = setIntVar(VARIABLE_ELASTIC_REST_PORT,
          ELASTIC_REST_PORT);
      HOPSWORKS_IP = setIpVar(VARIABLE_HOPSWORKS_IP, HOPSWORKS_IP);
      HOPSWORKS_PORT = setIntVar(VARIABLE_HOPSWORKS_PORT, HOPSWORKS_PORT);
      RM_IP = setIpVar(VARIABLE_RM_IP, RM_IP);
      RM_PORT = setIntVar(VARIABLE_RM_PORT, RM_PORT);
      LOGSTASH_IP = setIpVar(VARIABLE_LOGSTASH_IP, LOGSTASH_IP);
      LOGSTASH_PORT = setIntVar(VARIABLE_LOGSTASH_PORT, LOGSTASH_PORT);
      JHS_IP = setIpVar(VARIABLE_JHS_IP, JHS_IP);
      LIVY_IP = setIpVar(VARIABLE_LIVY_IP, LIVY_IP);
      LIVY_ZEPPELIN_SESSION_TIMEOUT = setVar(VARIABLE_LIVY_ZEPPELIN_SESSION_TIMEOUT, LIVY_ZEPPELIN_SESSION_TIMEOUT);
      ZEPPELIN_INTERPRETERS = setVar(VARIABLE_ZEPPELIN_INTERPRETERS, ZEPPELIN_INTERPRETERS);
      OOZIE_IP = setIpVar(VARIABLE_OOZIE_IP, OOZIE_IP);
      SPARK_HISTORY_SERVER_IP = setIpVar(VARIABLE_SPARK_HISTORY_SERVER_IP,
          SPARK_HISTORY_SERVER_IP);
      ZK_IP = setIpVar(VARIABLE_ZK_IP, ZK_IP);
      ZK_USER = setVar(VARIABLE_ZK_USER, ZK_USER);
      ZK_DIR = setDirVar(VARIABLE_ZK_DIR, ZK_DIR);
      DRELEPHANT_IP = setIpVar(VARIABLE_DRELEPHANT_IP, DRELEPHANT_IP);
      DRELEPHANT_PORT = setIntVar(VARIABLE_DRELEPHANT_PORT, DRELEPHANT_PORT);
      DRELEPHANT_DB = setDbVar(VARIABLE_DRELEPHANT_DB, DRELEPHANT_DB);
      KIBANA_IP = setIpVar(VARIABLE_KIBANA_IP, KIBANA_IP);
      KAFKA_USER = setVar(VARIABLE_KAFKA_USER, KAFKA_USER);
      KAFKA_DIR = setDirVar(VARIABLE_KAFKA_DIR, KAFKA_DIR);
      KAFKA_DEFAULT_NUM_PARTITIONS = setDirVar(VARIABLE_KAFKA_NUM_PARTITIONS,
          KAFKA_DEFAULT_NUM_PARTITIONS);
      KAFKA_DEFAULT_NUM_REPLICAS = setDirVar(VARIABLE_KAFKA_NUM_REPLICAS,
          KAFKA_DEFAULT_NUM_REPLICAS);
      YARN_DEFAULT_QUOTA = setDirVar(VARIABLE_YARN_DEFAULT_QUOTA,
          YARN_DEFAULT_QUOTA);
      YARN_WEB_UI_IP = setIpVar(VARIABLE_YARN_WEB_UI_IP, YARN_WEB_UI_IP);
      HDFS_WEB_UI_IP = setIpVar(VARIABLE_HDFS_WEB_UI_IP, HDFS_WEB_UI_IP);
      HDFS_WEB_UI_PORT = setIntVar(VARIABLE_HDFS_WEB_UI_PORT, HDFS_WEB_UI_PORT);
      YARN_WEB_UI_PORT = setIntVar(VARIABLE_YARN_WEB_UI_PORT, YARN_WEB_UI_PORT);
      HDFS_DEFAULT_QUOTA_MBs = setDirVar(VARIABLE_HDFS_DEFAULT_QUOTA,
          HDFS_DEFAULT_QUOTA_MBs);
      MAX_NUM_PROJ_PER_USER = setDirVar(VARIABLE_MAX_NUM_PROJ_PER_USER,
          MAX_NUM_PROJ_PER_USER);
      HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD = setVar(
          VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD,
          HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD);
      GLASSFISH_CERT_GENERATED = setVar(VARIABLE_GLASSFISH_CERT_CENERATED,
          GLASSFISH_CERT_GENERATED);
      CLUSTER_CERT = setVar(VARIABLE_CLUSTER_CERT, CLUSTER_CERT);
      FILE_PREVIEW_IMAGE_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_IMAGE_SIZE,
          10000000);
      FILE_PREVIEW_TXT_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_TXT_SIZE, 100);
      HOPSWORKS_REST_ENDPOINT = setStrVar(VARIABLE_HOPSWORKS_REST_ENDPOINT,
          HOPSWORKS_REST_ENDPOINT);
      REST_PORT = setIntVar(VARIABLE_REST_PORT, REST_PORT);
      CUDA_DIR = setDirVar(VARIABLE_CUDA_DIR, CUDA_DIR);
      ANACONDA_USER = setStrVar(VARIABLE_ANACONDA_USER, ANACONDA_USER);
      ANACONDA_DIR = setDirVar(VARIABLE_ANACONDA_DIR, ANACONDA_DIR);
      ANACONDA_ENV = setStrVar(VARIABLE_ANACONDA_ENV, ANACONDA_ENV);
      ANACONDA_INSTALLED = Boolean.parseBoolean(setStrVar(
          VARIABLE_ANACONDA_INSTALLED, ANACONDA_INSTALLED.toString()));
      INFLUXDB_IP = setStrVar(VARIABLE_INFLUXDB_IP, INFLUXDB_IP);
      INFLUXDB_PORT = setStrVar(VARIABLE_INFLUXDB_PORT, INFLUXDB_PORT);
      INFLUXDB_USER = setStrVar(VARIABLE_INFLUXDB_USER, INFLUXDB_USER);
      INFLUXDB_PW = setStrVar(VARIABLE_INFLUXDB_PW, INFLUXDB_PW);
      RESOURCE_DIRS = setStrVar(VARIABLE_RESOURCE_DIRS, RESOURCE_DIRS);
      VAGRANT_ENABLED = setIntVar(VARIABLE_VAGRANT_ENABLED, VAGRANT_ENABLED);
      MAX_STATUS_POLL_RETRY = setIntVar(VARIABLE_MAX_STATUS_POLL_RETRY, MAX_STATUS_POLL_RETRY);
      HOPS_RPC_TLS = setStrVar(VARIABLE_HOPS_RPC_TLS, HOPS_RPC_TLS);
      CERTIFICATE_MATERIALIZER_DELAY = setStrVar(VARIABLE_CERT_MATER_DELAY,
          CERTIFICATE_MATERIALIZER_DELAY);
      WHITELIST_USERS_LOGIN = setStrVar(VARIABLE_WHITELIST_USERS_LOGIN,
          WHITELIST_USERS_LOGIN);
      RECOVERY_PATH = setStrVar(VARIABLE_RECOVERY_PATH, RECOVERY_PATH);
      VERIFICATION_PATH = setStrVar(VARIABLE_VERIFICATION_PATH, VERIFICATION_PATH);
      populateDelaCache();
      populateLDAPCache();
      //Set Zeppelin Default Interpreter
      zeppelinDefaultInterpreter = getZeppelinDefaultInterpreter(ZEPPELIN_INTERPRETERS);
      cached = true;
    }
  }

  private void checkCache() {
    if (!cached) {
      populateCache();
    }
  }

  public synchronized void refreshCache() {
    cached = false;
    populateCache();
  }
  
  public synchronized void updateVariable(String variableName, String variableValue) {
    updateVariableInternal(variableName, variableValue);
    refreshCache();
  }
  
  public synchronized void updateVariables(Map<String, String> variablesToUpdate) {
    for (Map.Entry<String, String> entry : variablesToUpdate.entrySet()) {
      updateVariableInternal(entry.getKey(), entry.getValue());
    }
    refreshCache();
  }
  
  /**
   * This method will invalidate the cache of variables.
   * The next call to read a variable after invalidateCache() will trigger a read of all variables
   * from the database.
   */
  public synchronized void invalidateCache() {
    cached = false;
  }

  /**
   * ******************************************************************
   */
  private static final String GLASSFISH_DIR = "/srv/hops/glassfish";

  public static synchronized String getGlassfishDir() {
    return GLASSFISH_DIR;
  }

  private String TWOFACTOR_AUTH = "false";
  private String TWOFACTOR_EXCLUDE = "AGENT;CLUSTER_AGENT";

  public synchronized String getTwoFactorAuth() {
    checkCache();
    return TWOFACTOR_AUTH;
  }

  public synchronized String getTwoFactorExclude() {
    checkCache();
    return TWOFACTOR_EXCLUDE;
  }
  
  public static enum TwoFactorMode {
    MANDATORY("mandatory", "User can not disable two factor auth."),
    OPTIONAL("true", "Users can choose to disable two factor auth.");

    private final String name;
    private final String description;
    
    private TwoFactorMode(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  private String HOPS_RPC_TLS = "false";

  public synchronized boolean getHopsRpcTls() {
    checkCache();
    return HOPS_RPC_TLS.toLowerCase().equals("true");
  }

  /**
   * Default Directory locations
   */
  public static String PRIVATE_DIRS = "/private_dirs/";

  private String SPARK_DIR = "/srv/hops/spark";
  public static final String SPARK_EXAMPLES_DIR = "/examples/jars";
  public static final String HOPS_VERSION = "2.4.0";

  public static final String SPARK_HISTORY_SERVER_ENV
      = "spark.yarn.historyServer.address";
  public static final String SPARK_NUMBER_EXECUTORS_ENV
      = "spark.executor.instances";
  public static final String SPARK_DYNAMIC_ALLOC_ENV
      = "spark.dynamicAllocation.enabled";
  public static final String SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV
      = "spark.dynamicAllocation.minExecutors";
  public static final String SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV
      = "spark.dynamicAllocation.maxExecutors";
  public static final String SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV
      = "spark.dynamicAllocation.initialExecutors";
  public static final String SPARK_SHUFFLE_SERVICE
      = "spark.shuffle.service.enabled";
  public static final String SPARK_DRIVER_MEMORY_ENV = "spark.driver.memory";
  public static final String SPARK_DRIVER_CORES_ENV = "spark.driver.cores";
  public static final String SPARK_DRIVER_EXTRACLASSPATH = "spark.driver.extraClassPath";
  public static final String SPARK_EXECUTOR_MEMORY_ENV = "spark.executor.memory";
  public static final String SPARK_EXECUTOR_CORES_ENV = "spark.executor.cores";
  public static final String SPARK_EXECUTOR_EXTRACLASSPATH = "spark.executor.extraClassPath";
  public static final String SPARK_DRIVER_STAGINGDIR_ENV
      = "spark.yarn.stagingDir";
  public static final String SPARK_JAVA_LIBRARY_PROP = "java.library.path";
  public static final String SPARK_METRICS_ENV = "spark.metrics.conf";
  public static final String SPARK_MAX_APP_ATTEMPTS = "spark.yarn.maxAppAttempts";
  public static final String SPARK_EXECUTOR_EXTRA_JAVA_OPTS = "spark.executor.extraJavaOptions";
  
  //PySpark properties
  public static final String SPARK_APP_NAME_ENV = "spark.app.name";
  public static final String SPARK_EXECUTORENV_PYTHONPATH = "spark.executorEnv.PYTHONPATH";
  public static final String SPARK_EXECUTORENV_LD_LIBRARY_PATH = "spark.executorEnv.LD_LIBRARY_PATH";
  public static final String SPARK_EXECUTORENV_HDFS_USER = "spark.executorEnv.HDFS_USER";
  public static final String SPARK_EXECUTORENV_HADOOP_USER_NAME = "spark.executorEnv.HADOOP_USER_NAME";
  public static final String SPARK_YARN_IS_PYTHON_ENV = "spark.yarn.isPython";
  public static final String SPARK_YARN_SECONDARY_JARS = "spark.yarn.secondary.jars";
  public static final String SPARK_PYTHONPATH = "PYTHONPATH";
  public static final String SPARK_PYSPARK_PYTHON = "PYSPARK_PYTHON";
  //TFSPARK properties
  public static final String SPARK_TF_GPUS_ENV = "spark.executor.gpus";
  public static final String SPARK_TF_PS_ENV = "spark.tensorflow.num.ps";
  public static final String SPARK_TF_ENV = "spark.tensorflow.application";

  //Spark log4j and metrics properties
  public static final String SPARK_LOG4J_CONFIG = "log4j.configuration";
  public static final String SPARK_LOG4J_PROPERTIES = "log4j.properties";
  //If the value of this property changes, it must be changed in spark-chef log4j.properties as well
  public static final String LOGSTASH_JOB_INFO = "hopsworks.logstash.job.info";
  public static final String SPARK_METRICS_PROPERTIES = "metrics.properties";

  public static final String SPARK_CACHE_FILENAMES
      = "spark.yarn.cache.filenames";
  public static final String SPARK_CACHE_SIZES = "spark.yarn.cache.sizes";
  public static final String SPARK_CACHE_TIMESTAMPS
      = "spark.yarn.cache.timestamps";
  public static final String SPARK_CACHE_VISIBILITIES
      = "spark.yarn.cache.visibilities";
  public static final String SPARK_CACHE_TYPES = "spark.yarn.cache.types";
  //PYSPARK constants
  public static final String SPARK_PY_MAINCLASS
      = "org.apache.spark.deploy.PythonRunner";
  public static final String PYSPARK_ZIP = "pyspark.zip";
  public static final String PYSPARK_PY4J = "py4j-0.10.4-src.zip";
  public static final String TFSPARK_PYTHON_ZIP = "Python.zip";
  public static final String TFSPARK_PYTHON_NAME = "Python";
  public static final String TFSPARK_ZIP = "tfspark.zip";

  public synchronized String getSparkDir() {
    checkCache();
    return SPARK_DIR;
  }

  public synchronized String getSparkConfDir() {
    return getSparkDir() + "/conf";
  }

  public synchronized String getSparkExampleDir() {
    checkCache();
    return SPARK_EXAMPLES_DIR;
  }

  private final String SPARK_CONF_FILE = "/spark-defaults.conf";

  public synchronized String getSparkConfFile() {
    return getSparkConfDir() + SPARK_CONF_FILE;
  }

  private String ADAM_USER = "glassfish";

  public synchronized String getAdamUser() {
    checkCache();
    return ADAM_USER;
  }

  // "/tmp" by default
  private String STAGING_DIR = "/srv/hops/domains/domain1/staging";

  public synchronized String getStagingDir() {
    checkCache();
    return STAGING_DIR;
  }

  private final String FLINK_CONF_DIR = "conf";
  private String FLINK_DIR = "/srv/hops/flink";

  public synchronized String getFlinkDir() {
    checkCache();
    return FLINK_DIR;
  }

  public String getFlinkConfDir() {
    String flinkDir = getFlinkDir();
    return flinkDir + File.separator + FLINK_CONF_DIR;
  }

  private final String FLINK_CONF_FILE = "flink-conf.yaml";

  public String getFlinkConfFile() {
    return getFlinkConfDir() + File.separator + FLINK_CONF_FILE;
  }
  private String MYSQL_DIR = "/usr/local/mysql";

  public synchronized String getMySqlDir() {
    checkCache();
    return MYSQL_DIR;
  }
  private String NDB_DIR = "/var/lib/mysql-cluster";

  public synchronized String getNdbDir() {
    checkCache();
    return NDB_DIR;
  }

  private String ADAM_DIR = "/srv/hops/adam";

  public synchronized String getAdamDir() {
    checkCache();
    return ADAM_DIR;
  }

  private String HADOOP_DIR = "/srv/hops/hadoop";

  // This returns the unversioned base installation directory for hops-hadoop
  // For example, "/srv/hops/hadoop" - it does not return "/srv/hops/hadoop-2.8.2"
  public synchronized String getHadoopSymbolicLinkDir() {
    checkCache();
    return HADOOP_DIR;
  }

  public synchronized String getHadoopVersionedDir() {
    checkCache();
    return HADOOP_DIR + "-" + getHadoopVersion();
  }

  private String HIVE_SERVER_HOSTNAME = "127.0.0.1:9085";
  private String HIVE_SERVER_HOSTNAME_EXT = "127.0.0.1:9084";

  public synchronized String getHiveServerHostName(boolean ext) {
    checkCache();
    if (ext) {
      return HIVE_SERVER_HOSTNAME_EXT;
    }
    return HIVE_SERVER_HOSTNAME;
  }

  private String HIVE_SUPERUSER = "hive";

  public synchronized String getHiveSuperUser() {
    checkCache();
    return HIVE_SUPERUSER;
  }

  private String HIVE_WAREHOUSE = "/apps/hive/warehouse";

  public synchronized String getHiveWarehouse() {
    checkCache();
    return HIVE_WAREHOUSE;
  }

  private String HIVE_LLAP_SLIDER_DIR = "/home/hive/.slider";

  public synchronized String getHiveLlapSliderDir() {
    checkCache();
    return HIVE_LLAP_SLIDER_DIR;
  }

  private String HIVE_LLAP_LOCAL_FS_DIR = "/srv/hops/apache-hive/bin/llap";

  public synchronized String getHiveLlapLocalDir() {
    checkCache();
    return HIVE_LLAP_LOCAL_FS_DIR;
  }

  private String HIVE_SCRATCHDIR = "/tmp/hive";

  public synchronized String getHiveScratchdir() {
    checkCache();
    return HIVE_SCRATCHDIR;
  }

  private String HIVE_DB_DEFAULT_QUOTA = "50000";

  public synchronized Long getHiveDbDefaultQuota() {
    checkCache();
    return Long.parseLong(HIVE_DB_DEFAULT_QUOTA);
  }

  private String HOPSWORKS_EXTERNAL_IP = "127.0.0.1";

  public synchronized String getHopsworksExternalIp() {
    checkCache();
    return HOPSWORKS_EXTERNAL_IP;
  }

  public synchronized void setHopsworksExternalIp(String ip) {
    HOPSWORKS_EXTERNAL_IP = ip;
  }

  private String HOPSWORKS_IP = "127.0.0.1";

  public synchronized String getHopsworksIp() {
    checkCache();
    return HOPSWORKS_IP;
  }

  private Integer HOPSWORKS_PORT = 8080;

  public synchronized Integer getHopsworksPort() {
    checkCache();
    return HOPSWORKS_PORT;
  }

  private static String CERTS_DIR = "/srv/hops/certs-dir";

  public synchronized String getCertsDir() {
    checkCache();
    return CERTS_DIR;
  }

  public synchronized String getHopsworksMasterEncPasswordFile() {
    checkCache();
    return getCertsDir() + File.separator + "encryption_master_password";
  }
  
  private static String HOPSWORKS_INSTALL_DIR = "/srv/hops/domains";

  public synchronized String getHopsworksInstallDir() {
    checkCache();
    return HOPSWORKS_INSTALL_DIR;
  }

  public synchronized String getHopsworksDomainDir() {
    checkCache();
    return HOPSWORKS_INSTALL_DIR + "/domain1";
  }

  public synchronized String getIntermediateCaDir() {
    checkCache();
    return getCertsDir() + Settings.INTERMEDIATE_CA_DIR;
  }

  public synchronized String getCaDir() {
    checkCache();
    return getCertsDir();
  }

  //User under which yarn is run
  private String YARN_SUPERUSER = "rmyarn";

  public synchronized String getYarnSuperUser() {
    checkCache();
    return YARN_SUPERUSER;
  }
  private String HOPSWORKS_USER = "glassfish";

  public synchronized String getHopsworksUser() {
    checkCache();
    return HOPSWORKS_USER;
  }
  private String HDFS_SUPERUSER = "hdfs";

  public synchronized String getHdfsSuperUser() {
    checkCache();
    return HDFS_SUPERUSER;
  }
  private String SPARK_USER = "spark";

  public synchronized String getSparkUser() {
    checkCache();
    return SPARK_USER;
  }

  public static String JAVA_HOME = "/usr/lib/jvm/default-java";

  public synchronized String getJavaHome() {
    checkCache();
    return JAVA_HOME;
  }

  private String FLINK_USER = "flink";

  public synchronized String getFlinkUser() {
    checkCache();
    return FLINK_USER;
  }

  private String ZEPPELIN_USER = "spark";

  public synchronized String getZeppelinUser() {
    checkCache();
    return ZEPPELIN_USER;
  }

  private String YARN_DEFAULT_QUOTA = "60000";

  public synchronized String getYarnDefaultQuota() {
    checkCache();
    return YARN_DEFAULT_QUOTA;
  }

  private String YARN_WEB_UI_IP = "127.0.0.1";
  private int YARN_WEB_UI_PORT = 8088;

  public synchronized String getYarnWebUIAddress() {
    checkCache();
    return YARN_WEB_UI_IP + ":" + YARN_WEB_UI_PORT;
  }

  private String HDFS_WEB_UI_IP = "127.0.0.1";
  private int HDFS_WEB_UI_PORT = 50070;

  public synchronized String getHDFSWebUIAddress() {
    checkCache();
    return HDFS_WEB_UI_IP + ":" + HDFS_WEB_UI_PORT;
  }

  private String HDFS_DEFAULT_QUOTA_MBs = "200000";

  public synchronized long getHdfsDefaultQuotaInMBs() {
    checkCache();
    return Long.parseLong(HDFS_DEFAULT_QUOTA_MBs);
  }

  private String MAX_NUM_PROJ_PER_USER = "5";

  public synchronized Integer getMaxNumProjPerUser() {
    checkCache();
    int num = 5;
    try {
      num = Integer.parseInt(MAX_NUM_PROJ_PER_USER);
    } catch (NumberFormatException ex) {
      // should print to log here
    }
    return num;
  }

  private static String HADOOP_VERSION = "2.8.2";
  private static String HADOOP_MINOR_VERSION = "2.8.2.2-SNAPSHOT";

  public synchronized String getHadoopVersion() {
    checkCache();
    return HADOOP_VERSION;
  }

  //Hadoop locations
  public synchronized String getHadoopConfDir() {
    return hadoopConfDir(getHadoopSymbolicLinkDir());
  }

  private static String hadoopConfDir(String hadoopDir) {
    return hadoopDir + "/" + HADOOP_CONF_RELATIVE_DIR;
  }

  public static String getHadoopConfDir(String hadoopDir) {
    return hadoopConfDir(hadoopDir);
  }

  public synchronized String getYarnConfDir() {
    return getHadoopConfDir();
  }

  public static String getYarnConfDir(String hadoopDir) {
    return hadoopConfDir(hadoopDir);
  }
  
  public String getHopsLeaderElectionJarPath() {
    return getHadoopSymbolicLinkDir() + "/share/hadoop/hdfs/lib/hops-leader-election-" + getHadoopVersion() + ".jar";
  }
  
  //Default configuration file names
  public static final String DEFAULT_YARN_CONFFILE_NAME = "yarn-site.xml";
  public static final String DEFAULT_HADOOP_CONFFILE_NAME = "core-site.xml";
  public static final String DEFAULT_HDFS_CONFFILE_NAME = "hdfs-site.xml";
  public static final String DEFAULT_SPARK_CONFFILE_NAME = "spark-defaults.conf";

  //Environment variable keys
  //TODO: Check if ENV_KEY_YARN_CONF_DIR should be replaced with ENV_KEY_YARN_CONF
  public static final String ENV_KEY_YARN_CONF_DIR = "hdfs";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
  public static final String ENV_KEY_YARN_CONF = "YARN_CONF_DIR";
  public static final String ENV_KEY_SPARK_CONF_DIR = "SPARK_CONF_DIR";
  //YARN constants
  public static final int YARN_DEFAULT_APP_MASTER_MEMORY = 1024;
  public static final String YARN_DEFAULT_OUTPUT_PATH = "Logs/Yarn/";
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
  public static final String HADOOP_HOME_KEY = "HADOOP_HOME";
//  private static String HADOOP_COMMON_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
//  private static final String HADOOP_HDFS_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
//  private static final String HADOOP_YARN_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";
//  public static final String HADOOP_CONF_DIR_VALUE = HADOOP_CONF_DIR;

  public static final String HADOOP_CONF_RELATIVE_DIR = "etc/hadoop";
  public static final String SPARK_CONF_RELATIVE_DIR = "conf";
  public static final String YARN_CONF_RELATIVE_DIR = HADOOP_CONF_RELATIVE_DIR;

  //Spark constants
  public static final String SPARK_STAGING_DIR = ".sparkStaging";
  public static final String SPARK_JARS = "spark.yarn.jars";
  public static final String SPARK_ARCHIVE = "spark.yarn.archive";
  // Subdirectory where Spark libraries will be placed.
  public static final String SPARK_LOCALIZED_LIB_DIR = "__spark_libs__";
  public static final String SPARK_LOCALIZED_CONF_DIR = "__spark_conf__";
  public static final String SPARK_LOCALIZED_PYTHON_DIR = "__pyfiles__";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";
  public static final String HOPSUTIL_JAR = "hops-util.jar";
  public static final String HOPS_KAFKA_TOUR_JAR = "hops-spark.jar";

  public static final String HOPS_TOUR_DATASET = "TestJob";
  public static final String HOPS_TOUR_DATASET_JUPYTER = "Jupyter";
  // Distribution-defined classpath to add to processes
  public static final String ENV_DIST_CLASSPATH = "SPARK_DIST_CLASSPATH";
  public static final String SPARK_AM_MAIN
      = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Logs/Spark/";
  public static final String SPARK_CONFIG_FILE = "conf/spark-defaults.conf";
  public static final String SPARK_BLACKLISTED_PROPS
      = "conf/spark-blacklisted-properties.txt";
  public static final int SPARK_MIN_EXECS = 1;
  public static final int SPARK_MAX_EXECS = 300;
  public static final int SPARK_INIT_EXECS = 1;

  //Flink constants
  public static final String FLINK_DEFAULT_OUTPUT_PATH = "Logs/Flink/";
  public static final String FLINK_DEFAULT_CONF_FILE = "flink-conf.yaml";
  public static final String FLINK_DEFAULT_LOG4J_FILE = "log4j.properties";
  public static final String FLINK_DEFAULT_LOGBACK_FILE = "logback.xml";
  public static final String FLINK_LOCRSC_FLINK_JAR = "flink.jar";
  public static final String FLINK_LOCRSC_APP_JAR = "app.jar";
  public static final String FLINK_AM_MAIN
      = "org.apache.flink.yarn.ApplicationMaster";
  public static final int FLINK_APP_MASTER_MEMORY = 768;

  //TensorFlow constants
  public static final String TENSORFLOW_DEFAULT_OUTPUT_PATH = "Logs/TensorFlow/";
  public static final String TENSORFLOW_JAR = "hops-tensorflow-0.0.1.jar";
  //Used to pass the project user to yarn containers for tensorflow
  public static final String HADOOP_USER_NAME = "HADOOP_USER_NAME";
  public static final String YARNTF_HOME_DIR = "YARNTF_HOME_DIR";
  public static final String YARNTF_STAGING_DIR = ".yarntfStaging";
  public static final String HOPS_TENSORFLOW_TOUR_DATA = "tensorflow_demo";

  public static String getTensorFlowJarPath(String tfUser) {
    return "hdfs:///user/" + tfUser + "/" + TENSORFLOW_JAR;
  }

  public synchronized String getLocalFlinkJarPath() {
    return getFlinkDir() + "/flink.jar";
  }

  public synchronized String getHdfsFlinkJarPath() {
    return hdfsFlinkJarPath(getFlinkUser());
  }

  private static String hdfsFlinkJarPath(String flinkUser) {
    return "hdfs:///user/" + flinkUser + "/flink.jar";
  }

  public static String getHdfsFlinkJarPath(String flinkUser) {
    return hdfsFlinkJarPath(flinkUser);
  }

  public synchronized String getFlinkDefaultClasspath() {
    return flinkDefaultClasspath(getFlinkDir());
  }

  private static String flinkDefaultClasspath(String flinkDir) {
    return flinkDir + "/lib/*";
  }

  public static String getFlinkDefaultClasspath(String flinkDir) {
    return flinkDefaultClasspath(flinkDir);
  }

  public synchronized String getLocalSparkJarPath() {
    return getSparkDir() + "/spark.jar";
  }

  public synchronized String getHdfsSparkJarPath() {
    return hdfsSparkJarPath(getSparkUser());
  }

  private static String hdfsSparkJarPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/spark-jars.zip";
  }

  public static String getHdfsSparkJarPath(String sparkUser) {
    return hdfsSparkJarPath(sparkUser);
  }

  public static String getPySparkLibsPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser;
  }

  public static String getSparkLog4JPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/log4j.properties";
  }

  public String getSparkLog4JPath() {
    return "hdfs:///user/" + getSparkUser() + "/log4j.properties";
  }

  public static String getSparkMetricsPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/metrics.properties";
  }

  public String getSparkMetricsPath() {
    return "hdfs:///user/" + getSparkUser() + "/metrics.properties";
  }

  //TODO put the spark metrics in each project and take it from there
  public static String getProjectSparkMetricsPath(String projectName) {
    return "hdfs:///user/glassfish/metrics.properties";
  }

  public static String getProjectSparkLog4JPath(String sparkUser) {
    return "hdfs:///user/glassfish/log4j.properties";
  }

  public synchronized String getSparkDefaultClasspath() {
    return sparkDefaultClasspath(getSparkDir());
  }

  private static String sparkDefaultClasspath(String sparkDir) {
//    return sparkDir + "/conf:" + sparkDir + "/lib/*";
    return sparkDir + "/lib/*";
  }

  public static String getSparkDefaultClasspath(String sparkDir) {
    return sparkDefaultClasspath(sparkDir);
  }

  /**
   * Constructs the path to the marker file of a streaming job that uses
   * HopsUtil.
   *
   * @param job
   * @param appId
   * @return
   */
  public static String getJobMarkerFile(Jobs job, String appId) {
    return getHdfsRootPath(job.getProject().getName()) + "/Resources/.marker-"
        + job.getJobType().getName().toLowerCase()
        + "-" + job.getName()
        + "-" + appId;
  }

  public static String getHdfsRootPath(String projectname) {
    return "/" + DIR_ROOT + "/" + projectname;
  }

  /**
   * Static final fields are allowed in session beans:
   * http://stackoverflow.com/questions/9141673/static-variables-restriction-in-session-beans
   */
//ADAM constants
  public static final String ADAM_MAINCLASS = "org.bdgenomics.adam.cli.ADAMMain";
//  public static final String ADAM_DEFAULT_JAR_HDFS_PATH = "hdfs:///user/adam/repo/adam-cli.jar";
  //Or: "adam-cli/target/appassembler/repo/org/bdgenomics/adam/adam-cli/0.15.1-SNAPSHOT/adam-cli-0.15.1-SNAPSHOT.jar"
  public static final String ADAM_DEFAULT_OUTPUT_PATH = "Logs/Adam/";
  public static final String ADAM_DEFAULT_HDFS_REPO = "/user/adam/repo/";

  public String getAdamJarHdfsPath() {
    return "hdfs:///user/" + getAdamUser() + "/adam-cli.jar";
  }

  //Directory names in HDFS
  public static final String DIR_ROOT = "Projects";
  public static final String DIR_SAMPLES = "Samples";
  public static final String DIR_RESULTS = "Results";
  public static final String DIR_CONSENTS = "consents";
  public static final String DIR_META_TEMPLATES = File.separator + DIR_ROOT + File.separator + "Uploads"
      + File.separator;
  public static final String PROJECT_STAGING_DIR = "Resources";

  // Elasticsearch
  private String ELASTIC_IP = "127.0.0.1";

  public synchronized String getElasticIp() {
    checkCache();
    return ELASTIC_IP;
  }

  private int ELASTIC_PORT = 9300;

  public synchronized int getElasticPort() {
    checkCache();
    return ELASTIC_PORT;
  }

  private int ELASTIC_REST_PORT = 9200;

  public synchronized int getElasticRESTPort() {
    checkCache();
    return ELASTIC_REST_PORT;
  }

  public synchronized String getElasticEndpoint() {
    return getElasticIp() + ":" + getElasticPort();
  }

  public synchronized String getElasticRESTEndpoint() {
    return getElasticIp() + ":" + getElasticRESTPort();
  }

  private static int JOB_LOGS_EXPIRATION = 604800;

  /**
   * TTL for job logs in elasticsearch, in seconds.
   *
   * @return
   */
  public static int getJobLogsExpiration() {
    return JOB_LOGS_EXPIRATION;
  }

  private static long JOB_LOGS_DISPLAY_SIZE = 1000000;

  public static long getJobLogsDisplaySize() {
    return JOB_LOGS_DISPLAY_SIZE;
  }

  private static final String JOB_LOGS_ID_FIELD = "jobid";

  public static String getJobLogsIdField() {
    return JOB_LOGS_ID_FIELD;
  }

  // CertificateMaterializer service. Delay for deleting crypto material from
  // the local filesystem. The lower the value the more frequent we reach DB
  // for materialization
  // Suffix, defaults to minutes if omitted:
  // ms: milliseconds
  // s: seconds
  // m: minutes (default)
  // h: hours
  // d: days
  private String CERTIFICATE_MATERIALIZER_DELAY = "1m";

  public synchronized String getCertificateMaterializerDelay() {
    checkCache();
    return CERTIFICATE_MATERIALIZER_DELAY;
  }

  // Spark
  private String SPARK_HISTORY_SERVER_IP = "127.0.0.1";

  public synchronized String getSparkHistoryServerIp() {
    checkCache();
    return SPARK_HISTORY_SERVER_IP + ":18080";
  }

  // Oozie
  private String OOZIE_IP = "127.0.0.1";

  public synchronized String getOozieIp() {
    checkCache();
    return OOZIE_IP;
  }

  // MapReduce Job History Server
  private String JHS_IP = "127.0.0.1";

  public synchronized String getJhsIp() {
    checkCache();
    return JHS_IP;
  }

  // Resource Manager for YARN
  private String RM_IP = "127.0.0.1";

  public synchronized String getRmIp() {
    checkCache();
    return RM_IP;
  }

  // Resource Manager Port 
  private int RM_PORT = 8088;

  public synchronized Integer getRmPort() {
    checkCache();
    return RM_PORT;
  }

  private String LOGSTASH_IP = "127.0.0.1";

  public synchronized String getLogstashIp() {
    checkCache();
    return LOGSTASH_IP;
  }

  // Resource Manager Port 
  private int LOGSTASH_PORT = 8088;

  public synchronized Integer getLogstashPort() {
    checkCache();
    return LOGSTASH_PORT;
  }

  // Livy Server`
  private String LIVY_IP = "127.0.0.1";
  private final String LIVY_YARN_MODE = "yarn";
  private String LIVY_ZEPPELIN_SESSION_TIMEOUT = "3600";

  public synchronized String getLivyIp() {
    checkCache();
    return LIVY_IP;
  }

  public synchronized String getLivyUrl() {
    return "http://" + getLivyIp() + ":8998";
  }

  public synchronized String getLivyYarnMode() {
    checkCache();
    return LIVY_YARN_MODE;
  }

  public synchronized String getLivyZeppelinSessionTimeout() {
    checkCache();
    return LIVY_ZEPPELIN_SESSION_TIMEOUT;
  }

  public static final int ZK_PORT = 2181;

  // Kibana
  public static final String KIBANA_DEFAULT_INDEX = "hopsdefault";
  private String KIBANA_IP = "10.0.2.15";
  public static final int KIBANA_PORT = 5601;

  public synchronized String getKibanaUri() {
    checkCache();
    return "http://" + KIBANA_IP + ":" + KIBANA_PORT;
  }

  // Zookeeper 
  private String ZK_IP = "10.0.2.15";

  public synchronized String getZkConnectStr() {
    checkCache();
    return ZK_IP + ":" + ZK_PORT;
  }

  private String ZK_USER = "zk";

  public synchronized String getZkUser() {
    checkCache();
    return ZK_USER;
  }

  /*
   * Comma-separated list of user emails that should not be persisted in the
   * userlogins table for auditing.
   * kagent -> agent@hops.io
   */
  private String WHITELIST_USERS_LOGIN = "agent@hops.io";

  public synchronized String getWhitelistUsersLogin() {
    checkCache();
    return WHITELIST_USERS_LOGIN;
  }

  // Zeppelin
  private String ZEPPELIN_DIR = "/srv/hops/zeppelin";
  private String ZEPPELIN_INTERPRETERS
      = "org.apache.zeppelin.livy.LivySparkInterpreter,org.apache.zeppelin.livy.LivyPySparkInterpreter,"
      + "org.apache.zeppelin.livy.LivySparkRInterpreter,org.apache.zeppelin.livy.LivySparkSQLInterpreter,"
      + "org.apache.zeppelin.spark.SparkInterpreter,org.apache.zeppelin.spark.PySparkInterpreter,"
      + "org.apache.zeppelin.rinterpreter.RRepl,org.apache.zeppelin.rinterpreter.KnitR,"
      + "org.apache.zeppelin.spark.SparkRInterpreter,org.apache.zeppelin.spark.SparkSqlInterpreter,"
      + "org.apache.zeppelin.spark.DepInterpreter,org.apache.zeppelin.markdown.Markdown,"
      + "org.apache.zeppelin.angular.AngularInterpreter,org.apache.zeppelin.flink.FlinkInterpreter";

  private String zeppelinDefaultInterpreter;

  /**
   *
   * @return
   */
  public synchronized String getZeppelinInterpreters() {
    checkCache();
    return ZEPPELIN_INTERPRETERS;
  }

  /**
   *
   * @return
   */
  public synchronized String getZeppelinDefaultInterpreter() {
    return zeppelinDefaultInterpreter;
  }

  /**
   * Extract default interpreter from zeppelin interpreters.
   *
   * @return
   */
  private String getZeppelinDefaultInterpreter(String interpreters) {
    //Split interpreters
    return interpreters.split(",")[0].split("\\.")[3];
  }

  public synchronized String getZeppelinDir() {
    checkCache();
    return ZEPPELIN_DIR;
  }

  private String ZEPPELIN_PROJECTS_DIR = "/srv/hops/zeppelin/Projects";

  public synchronized String getZeppelinProjectsDir() {
    checkCache();
    return ZEPPELIN_PROJECTS_DIR;
  }

  private long ZEPPELIN_SYNC_INTERVAL = 24 * 60 * 60 * 1000;

  public synchronized long getZeppelinSyncInterval() {
    checkCache();
    return ZEPPELIN_SYNC_INTERVAL;
  }

  public static final int JUPYTER_PORT = 8888;

  // Jupyter
  private String JUPYTER_DIR = "/srv/hops/jupyter";

  public synchronized String getJupyterDir() {
    checkCache();
    return JUPYTER_DIR;
  }

  private String KAFKA_USER = "kafka";

  public synchronized String getKafkaUser() {
    checkCache();
    return KAFKA_USER;
  }

  private String KAFKA_DIR = "/srv/kafka";

  public synchronized String getKafkaDir() {
    checkCache();
    return KAFKA_DIR;
  }

  private String ANACONDA_USER = "anaconda";

  public synchronized String getAnacondaUser() {
    checkCache();
    return ANACONDA_USER;
  }

  private String ANACONDA_DIR = "/srv/hops/anaconda/anaconda";

  public synchronized String getAnacondaDir() {
    checkCache();
    return ANACONDA_DIR;
  }

  private String CUDA_DIR = "/usr/local/cuda";

  public synchronized String getCudaDir() {
    checkCache();
    return CUDA_DIR;
  }

  /**
   * Constructs the path to the project environment in Anaconda
   *
   * @param projectName
   * @return
   */
  public String getAnacondaProjectDir(String projectName) {
    return getAnacondaDir() + File.separator + "envs" + File.separator
        + projectName;
  }

  private String ANACONDA_ENV = "kagent";

  public synchronized String getAnacondaEnv() {
    checkCache();
    return ANACONDA_ENV;
  }

  private Boolean ANACONDA_INSTALLED = false;

  public synchronized Boolean isAnacondaInstalled() {
    checkCache();
    return ANACONDA_INSTALLED;
  }

//  private String CONDA_CHANNEL_URL = "https://repo.continuum.io/pkgs/free/linux-64/";
  private String CONDA_CHANNEL_URL = "default";

  public synchronized String getCondaChannelUrl() {
    checkCache();
    return CONDA_CHANNEL_URL;
  }

  private int REST_PORT = 8080;

  public synchronized int getRestPort() {
    checkCache();
    return REST_PORT;
  }

  private String HOPSWORKS_REST_ENDPOINT = "http://192.168.56.101:8080";
  
  /**
   * Generates the Endpoint for kafka.
   *
   * @return
   */
  public synchronized String getRestEndpoint() {
    checkCache();
    return "http://" + HOPSWORKS_REST_ENDPOINT;
  }

  private String HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD = "adminpw";

  public synchronized String getHopsworksMasterPasswordSsl() {
    checkCache();
    return HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD;
  }

  private String GLASSFISH_CERT_GENERATED = "false";

  public synchronized boolean isGlassfishCertGenerated() {
    checkCache();
    return Boolean.parseBoolean(GLASSFISH_CERT_GENERATED);
  }

  private String KAFKA_DEFAULT_NUM_PARTITIONS = "2";
  private String KAFKA_DEFAULT_NUM_REPLICAS = "1";

  public synchronized String getKafkaDefaultNumPartitions() {
    checkCache();
    return KAFKA_DEFAULT_NUM_PARTITIONS;
  }

  public synchronized String getKafkaDefaultNumReplicas() {
    checkCache();
    return KAFKA_DEFAULT_NUM_REPLICAS;
  }

  private String ZK_DIR = "/srv/zookeeper";

  public synchronized String getZkDir() {
    checkCache();
    return ZK_DIR;
  }

  // Dr Elephant
  private String DRELEPHANT_IP = "127.0.0.1";
  private String DRELEPHANT_DB = "hopsworks";
  public static int DRELEPHANT_PORT = 11000;

  public synchronized String getDrElephantUrl() {
    checkCache();
    return "http://" + DRELEPHANT_IP + ":" + DRELEPHANT_PORT;
  }

  public synchronized String getDrElephantDb() {
    checkCache();
    return DRELEPHANT_DB;
  }

  private String CLUSTER_CERT = "asdasxasx8as6dx8a7sx7asdta8dtasxa8";

  public synchronized String getCLUSTER_CERT() {
    checkCache();
    return CLUSTER_CERT;
  }

  // Hopsworks
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String HOPS_USERS_HOMEDIR = "/home/";
  public static final String HOPS_USERNAME_SEPARATOR = "__";
  private static final String CA_DIR = "";
  private static final String INTERMEDIATE_CA_DIR = CA_DIR + "/intermediate";
  public static final String SSL_CREATE_CERT_SCRIPTNAME = "createusercerts.sh";
  public static final String SSL_DELETE_CERT_SCRIPTNAME = "deleteusercerts.sh";
  public static final String SSL_DELETE_PROJECT_CERTS_SCRIPTNAME
      = "deleteprojectcerts.sh";
  public static final String UNZIP_FILES_SCRIPTNAME = "unzip-hdfs-files.sh";
  public static final int USERNAME_LEN = 8;
  public static final int MAX_USERNAME_SUFFIX = 99;
  public static final int MAX_RETRIES = 500;
  public static final String META_NAME_FIELD = "name";
  public static String META_PUBLIC_FIELD = "public_ds";
  public static final String META_DESCRIPTION_FIELD = "description";
  public static final String META_INDEX = "projects";
  public static final String META_PROJECT_TYPE = "proj";
  public static final String META_DATASET_TYPE = "ds";
  public static final String META_INODE_TYPE = "inode";
  public static final String META_PROJECT_ID_FIELD = "project_id";
  public static final String META_DATASET_ID_FIELD = "dataset_id";
  public static final String META_ID = "_id";
  public static final String META_DATA_NESTED_FIELD = "xattr";
  public static final String META_DATA_FIELDS = META_DATA_NESTED_FIELD + ".*";

  //Filename conventions
  public static final String PROJECT_DISALLOWED_CHARS
          = " -/\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String PRINT_PROJECT_DISALLOWED_CHARS
    = "__, -, space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #,ö,ä,å,Ö,Å,Ä,à,á,é,è,â,î,ï,ü,Ü,@,$,{,},[,],+,~,^";
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String SUBDIR_DISALLOWED_CHARS = "/\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String PRINT_FILENAME_DISALLOWED_CHARS
    = "__, space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #,ö,ä,å,Ö,Å,Ä,à,á,é,è,â,î,ï,ü,Ü,@,$,{,},[,],+,~,^";
  public static final String SHARED_FILE_SEPARATOR = "::";
  public static final String DOUBLE_UNDERSCORE = "__";

  public static final String K_CERTIFICATE = "k_certificate";
  public static final String T_CERTIFICATE = "t_certificate";
  public static final String CRYPTO_MATERIAL_PASSWORD = "material_passwd";

  //Used to retrieve schema by HopsUtil
  public static final String HOPSWORKS_PROJECTID_PROPERTY = "hopsworks.projectid";
  public static final String HOPSWORKS_PROJECTNAME_PROPERTY = "hopsworks.projectname";
  public static final String HOPSWORKS_PROJECTUSER_PROPERTY = "hopsworks.projectuser";
  public static final String HOPSWORKS_JOBNAME_PROPERTY = "hopsworks.job.name";
  public static final String HOPSWORKS_JOBTYPE_PROPERTY = "hopsworks.job.type";
  public static final String HOPSWORKS_APPID_PROPERTY = "hopsworks.job.appid";
  public static final String KAFKA_BROKERADDR_ENV_VAR = "hopsworks.kafka.brokeraddress";
  public static final String KAFKA_JOB_ENV_VAR = "hopsworks.kafka.job";
  public static final String KAFKA_JOB_TOPICS_ENV_VAR = "hopsworks.kafka.job.topics";
  public static final String HOPSWORKS_SESSIONID_PROPERTY = "hopsworks.sessionid";
  public static final String HOPSWORKS_KEYSTORE_PROPERTY = "hopsworks.keystore";
  public static final String KEYSTORE_VAL_ENV_VAR = "keyPw";
  public static final String HOPSWORKS_TRUSTSTORE_PROPERTY = "hopsworks.truststore";
  public static final String TRUSTSTORE_VAL_ENV_VAR = "trustPw";
  public static final String KAFKA_CONSUMER_GROUPS = "hopsworks.kafka.consumergroups";
  public static final String HOPSWORKS_REST_ENDPOINT_PROPERTY = "hopsworks.restendpoint";
  public static final String HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY = "hopsworks.elastic.endpoint";

  public static int FILE_PREVIEW_IMAGE_SIZE = 10000000;
  public static int FILE_PREVIEW_TXT_SIZE = 100;
  public static final int FILE_PREVIEW_TXT_SIZE_BYTES = 1024 * 384;
  public static final String README_TEMPLATE = "*This is an auto-generated README.md"
    + " file for your Dataset!*\n"
    + "To replace it, go into your DataSet and edit the README.md file.\n"
    + "\n" + "*%s* DataSet\n" + "===\n" + "\n"
    + "## %s";

  public static final String FILE_PREVIEW_TEXT_TYPE = "text";
  public static final String FILE_PREVIEW_IMAGE_TYPE = "image";
  public static final String FILE_PREVIEW_MODE_TAIL = "tail";
  
  public String getHopsworksTmpCertDir() {
    return Paths.get(getCertsDir(), "transient").toString();
  }

  public String getHdfsTmpCertDir() {
    return "/user/" + getHdfsSuperUser() + "/" + "kafkacerts";
  }

  public String getHopsworksTrueTempCertDir() {
    return "/tmp/usercerts/";
  }

  public String getFlinkKafkaCertDir() {
    return getHopsworksDomainDir() + File.separator + "config";
  }

  //Dataset request subject
  public static String MESSAGE_DS_REQ_SUBJECT = "Dataset access request.";

  // QUOTA
  public static final float DEFAULT_YARN_MULTIPLICATOR = 1.0f;

  public static final String SPARK_METRICS_PROPS = "metrics.properties";

  /**
   * Returns the maximum image size in bytes that can be previewed in the
   * browser.
   *
   * @return
   */
  public synchronized int getFilePreviewImageSize() {
    checkCache();
    return FILE_PREVIEW_IMAGE_SIZE;
  }

  /**
   * Returns the maximum number of lines of the file that can be previewed in
   * the
   * browser.
   *
   * @return
   */
  public synchronized int getFilePreviewTxtSize() {
    checkCache();
    return FILE_PREVIEW_TXT_SIZE;
  }

  public static String INFLUXDB_IP = "localhost";
  public static String INFLUXDB_PORT = "8086";
  public static String INFLUXDB_USER = "hopsworks";
  public static String INFLUXDB_PW = "hopsworks";

  public synchronized String getInfluxDBAddress() {
    checkCache();
    return "http://" + INFLUXDB_IP + ":" + INFLUXDB_PORT;
  }

  public synchronized String getInfluxDBUser() {
    checkCache();
    return INFLUXDB_USER;
  }

  public synchronized String getInfluxDBPW() {
    checkCache();
    return INFLUXDB_PW;
  }

  //Project creation: default datasets
  public static enum BaseDataset {

    LOGS("Logs",
        "Contains the logs for jobs that have been run through the Hopsworks platform."),
    RESOURCES("Resources",
        "Contains resources used by jobs, for example, jar files.");
    private final String name;
    private final String description;

    private BaseDataset(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  public static enum ServiceDataset {
    ZEPPELIN("notebook", "Contains Zeppelin notebooks."),
    JUPYTER("Jupyter", "Contains Jupyter notebooks."),
    SERVING("Models", "Contains models to be used for serving.");

    private final String name;
    private final String description;

    private ServiceDataset(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  public int VAGRANT_ENABLED = 0;

  public synchronized boolean getVagrantEnabled() {
    checkCache();
    return VAGRANT_ENABLED == 1;
  }

  public static String JUPYTER_PIDS = "/tmp/jupyterNotebookServer.pids";
  public String RESOURCE_DIRS = ".sparkStaging;spark-warehouse";

  public synchronized String getResourceDirs() {
    checkCache();
    return RESOURCE_DIRS;
  }

  public Settings() {
  }
  
  
  private String ALERT_EMAIL_ADDRS = "";

  public synchronized String getAlertEmailAddrs() {
    checkCache();
    return ALERT_EMAIL_ADDRS;
  }
  

  /**
   * Get the variable value with the given name.
   *
   * @param id
   * @return The user with given email, or null if no such user exists.
   */
  public Variables findById(String id) {
    try {
      return em.createNamedQuery("Variables.findById", Variables.class
      ).setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Get all variables from the database.
   * @return List with all the variables
   */
  public List<Variables> getAllVariables() {
    TypedQuery<Variables> query = em.createNamedQuery("Variables.findAll", Variables.class);
    
    try {
      return query.getResultList();
    } catch (EntityNotFoundException ex) {
      logger.log(Level.SEVERE, ex.getMessage(), ex);
    } catch (NoResultException ex) {
    }
    return new ArrayList<>();
  }
  
  /**
   * Update a variable in the database.
   * @param variableName
   * @param variableValue
   */
  private void updateVariableInternal(String variableName, String variableValue) {
    Variables var = findById(variableName);
    if (var == null) {
      throw new NoResultException("Variable <" + variableName + "> does not exist in the database");
    }
    if (!var.getValue().equals(variableValue)) {
      var.setValue(variableValue);
      em.persist(var);
    }
  }
  
  public void detach(Variables variable) {
    em.detach(variable);
  }

  public static String getProjectPath(String projectname) {
    return File.separator + DIR_ROOT + File.separator + projectname;
  }

  Configuration conf;

  public Configuration getConfiguration() throws IllegalStateException {
    if (conf == null) {
      String hadoopDir = getHadoopSymbolicLinkDir();
      //Get the path to the Yarn configuration file from environment variables
      String yarnConfDir = System.getenv(Settings.ENV_KEY_YARN_CONF_DIR);
      //If not found in environment variables: warn and use default,
      if (yarnConfDir == null) {
        yarnConfDir = getYarnConfDir(hadoopDir);
      }

      Path confPath = new Path(yarnConfDir);
      File confFile = new File(confPath + File.separator
          + Settings.DEFAULT_YARN_CONFFILE_NAME);
      if (!confFile.exists()) {
        throw new IllegalStateException("No Yarn conf file");
      }

      //Also add the hadoop config
      String hadoopConfDir = System.getenv(Settings.ENV_KEY_HADOOP_CONF_DIR);
      //If not found in environment variables: warn and use default
      if (hadoopConfDir == null) {
        hadoopConfDir = hadoopDir + "/" + Settings.HADOOP_CONF_RELATIVE_DIR;
      }
      confPath = new Path(hadoopConfDir);
      File hadoopConf = new File(confPath + "/"
          + Settings.DEFAULT_HADOOP_CONFFILE_NAME);
      if (!hadoopConf.exists()) {
        throw new IllegalStateException("No Hadoop conf file");
      }

      File hdfsConf = new File(confPath + "/"
          + Settings.DEFAULT_HDFS_CONFFILE_NAME);
      if (!hdfsConf.exists()) {
        throw new IllegalStateException("No HDFS conf file");
      }

      //Set the Configuration object for the returned YarnClient
      conf = new Configuration();
      conf.addResource(new Path(confFile.getAbsolutePath()));
      conf.addResource(new Path(hadoopConf.getAbsolutePath()));
      conf.addResource(new Path(hdfsConf.getAbsolutePath()));

      addPathToConfig(conf, confFile);
      addPathToConfig(conf, hadoopConf);
      setDefaultConfValues(conf);
    }
    return conf;
  }

  private static void addPathToConfig(Configuration conf, File path) {
    // chain-in a new classloader
    URL fileUrl = null;
    try {
      fileUrl = path.toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Erroneous config file path", e);
    }
    URL[] urls = {fileUrl};
    ClassLoader cl = new URLClassLoader(urls, conf.getClassLoader());
    conf.setClassLoader(cl);
  }

  private static void setDefaultConfValues(Configuration conf) {
    if (conf.get("fs.hdfs.impl", null) == null) {
      conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
    }
    if (conf.get("fs.file.impl", null) == null) {
      conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
    }
  }

  public int MAX_STATUS_POLL_RETRY = 5;

  public synchronized int getMaxStatusPollRetry() {
    checkCache();
    return MAX_STATUS_POLL_RETRY;
  }

  /**
   * Returns aggregated log dir path for an application with the the given
   * appId.
   *
   * @param hdfsUser
   * @param appId
   * @return
   */
  public String getAggregatedLogPath(String hdfsUser, String appId) {
    boolean logPathsAreAggregated = conf.getBoolean(
        YarnConfiguration.LOG_AGGREGATION_ENABLED,
        YarnConfiguration.DEFAULT_LOG_AGGREGATION_ENABLED);
    String aggregatedLogPath = null;
    if (logPathsAreAggregated) {
      String[] nmRemoteLogDirs = conf.getStrings(
          YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
          YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);

      String[] nmRemoteLogDirSuffix = conf.getStrings(
          YarnConfiguration.NM_REMOTE_APP_LOG_DIR_SUFFIX,
          YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR_SUFFIX);
      aggregatedLogPath = nmRemoteLogDirs[0] + File.separator + hdfsUser
          + File.separator + nmRemoteLogDirSuffix[0] + File.separator
          + appId;
    }
    return aggregatedLogPath;
  }

  // For performance reasons, we have an in-memory cache of files being unzipped
  // Lazily remove them from the cache, when we check the FS and they aren't there.
  Set<String> unzippingFiles = new HashSet<>();

  public synchronized void addUnzippingState(String hdfsPath) {
    unzippingFiles.add(hdfsPath);
  }

  public synchronized String getUnzippingState(String hdfsPath) {

    if (!unzippingFiles.contains(hdfsPath)) {
      return "NONE";
    }
    String hashedPath = DigestUtils.sha256Hex(hdfsPath);
    String fsmPath = getStagingDir() + "/fsm-" + hashedPath + ".txt";
    String state = "NOT_FOUND";
    try {
      state = new String(java.nio.file.Files.readAllBytes(Paths.get(fsmPath)));
      state = state.trim();
    } catch (IOException ex) {
      state = "NONE";
      // lazily remove the file, probably because it has finished unzipping
      unzippingFiles.remove(hdfsPath);
    }
    // If a terminal state has been reached, removed the entry and the file.
    if (state == null || state.isEmpty() || state.compareTo("FAILED") == 0
        || state.compareTo("SUCCESS") == 0) {
      try {
        unzippingFiles.remove(hdfsPath);
        java.nio.file.Files.deleteIfExists(Paths.get(fsmPath));
      } catch (IOException ex) {
        Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (state == null || state.isEmpty()) {
      state = "NOT_FOUND";
    }

    return state;
  }

  private static boolean PYTHON_KERNEL = true;

  public synchronized boolean isPythonKernelEnabled() {
    checkCache();
    return PYTHON_KERNEL;
  }

  private static String HOPSUTIL_VERSION = "0.1.1";

  public synchronized String getHopsUtilHdfsPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/" + getHopsUtilFilename();
  }

  public synchronized String getHopsUtilFilename() {
    checkCache();
//    return "hops-util-" + HOPSUTIL_VERSION + ".jar";
    return "hops-util.jar";
  }

  public synchronized String getKafkaTourFilename() {
    checkCache();
//    return "hops-spark-" + HOPSUTIL_VERSION + ".jar";
    return "hops-spark.jar";

  }

  private String RECOVERY_PATH = "hopsworks-api/api/auth/recover";

  public synchronized String getRecoveryEndpoint() {
    checkCache();
    return HOPSWORKS_IP + ":" + HOPSWORKS_PORT + "/" + RECOVERY_PATH;
  }

  private String VERIFICATION_PATH = "hopsworks-api/api/auth/verify";

  public synchronized String getVerificationEndpoint() {
    checkCache();
    return HOPSWORKS_IP + ":" + HOPSWORKS_PORT + "/" + VERIFICATION_PATH;
  }

  //Dela START
  private static final String VARIABLE_HOPSSITE_BASE_URI = "hops_site_endpoint";
  private static final String VARIABLE_HOPSSITE_BASE_URI_HOST = "hops_site_host";
  private static final String VARIABLE_CLUSTER_CERT = "hopsworks_certificate";
  private static final String VARIABLE_DELA_ENABLED = "dela_enabled";
  private static final String VARIABLE_DELA_CLIENT_TYPE = "dela_client_type";
  private static final String VARIABLE_HOPSSITE_HEARTBEAT_INTERVAL = "hopssite_heartbeat_interval";

  public static final String VARIABLE_DELA_CLUSTER_ID = "cluster_id";
  private static final String VARIABLE_DELA_CLUSTER_IP = "dela_cluster_ip";
  private static final String VARIABLE_DELA_CLUSTER_HTTP_PORT = "dela_cluster_http_port";
  private static final String VARIABLE_DELA_PUBLIC_HOPSWORKS_PORT = "dela_hopsworks_public_port";
  private static final String VARIABLE_DELA_SEARCH_ENDPOINT = "dela_search_endpoint";
  private static final String VARIABLE_DELA_TRANSFER_ENDPOINT = "dela_transfer_endpoint";

  public static final Level DELA_DEBUG = Level.INFO;
  private String HOPSSITE_HOST = "hops.site";
  private String HOPSSITE = "http://hops.site:5081/hops-site/api";
  private Boolean DELA_ENABLED = false; // set to false if not found in variables table
  private DelaClientType DELA_CLIENT_TYPE = DelaClientType.FULL_CLIENT;

  private long HOPSSITE_HEARTBEAT_RETRY = 10 * 1000l; //10s
  private long HOPSSITE_HEARTBEAT_INTERVAL = 10 * 60 * 1000l;//10min

  private String DELA_TRANSFER_IP = "localhost";
  private String DELA_TRANSFER_HTTP_PORT = "8080";
  private String DELA_PUBLIC_HOPSWORK_PORT = "5081";

  //set on registration after Dela is contacted to detect public port
  private String DELA_SEARCH_ENDPOINT = "";
  private String DELA_TRANSFER_ENDPOINT = "";
  //set on cluster registration
  private String DELA_CLUSTER_ID = null;
  //
  private AddressJSON DELA_PUBLIC_ENDPOINT = null;
  //
  public static final String MANIFEST_FILE = "manifest.json";
  public static final String README_FILE = "README.md";

  private void populateDelaCache() {
    DELA_ENABLED = setBoolVar(VARIABLE_DELA_ENABLED, DELA_ENABLED);
    DELA_CLIENT_TYPE = DelaClientType.from(setVar(VARIABLE_DELA_CLIENT_TYPE, DELA_CLIENT_TYPE.type));
    HOPSSITE_CLUSTER_NAME = setVar(VARIABLE_HOPSSITE_CLUSTER_NAME, HOPSSITE_CLUSTER_NAME);
    HOPSSITE_CLUSTER_PSWD = setVar(VARIABLE_HOPSSITE_CLUSTER_PSWD, HOPSSITE_CLUSTER_PSWD);
    HOPSSITE_CLUSTER_PSWD_AUX = setVar(VARIABLE_HOPSSITE_CLUSTER_PSWD_AUX, HOPSSITE_CLUSTER_PSWD_AUX);
    HOPSSITE_HOST = setVar(VARIABLE_HOPSSITE_BASE_URI_HOST, HOPSSITE_HOST);
    HOPSSITE = setVar(VARIABLE_HOPSSITE_BASE_URI, HOPSSITE);
    HOPSSITE_HEARTBEAT_INTERVAL = setLongVar(VARIABLE_HOPSSITE_HEARTBEAT_INTERVAL, HOPSSITE_HEARTBEAT_INTERVAL);

    DELA_TRANSFER_IP = setStrVar(VARIABLE_DELA_CLUSTER_IP, DELA_TRANSFER_IP);
    DELA_TRANSFER_HTTP_PORT = setStrVar(VARIABLE_DELA_CLUSTER_HTTP_PORT, DELA_TRANSFER_HTTP_PORT);
    DELA_SEARCH_ENDPOINT = setStrVar(VARIABLE_DELA_SEARCH_ENDPOINT, DELA_SEARCH_ENDPOINT);
    DELA_TRANSFER_ENDPOINT = setStrVar(VARIABLE_DELA_TRANSFER_ENDPOINT, DELA_TRANSFER_ENDPOINT);
    DELA_PUBLIC_HOPSWORK_PORT = setStrVar(VARIABLE_DELA_PUBLIC_HOPSWORKS_PORT, DELA_PUBLIC_HOPSWORK_PORT);
    DELA_CLUSTER_ID = setStrVar(VARIABLE_DELA_CLUSTER_ID, DELA_CLUSTER_ID);
  }

  public synchronized Boolean isDelaEnabled() {
    checkCache();
    return DELA_ENABLED;
  }
  
  public synchronized DelaClientType getDelaClientType() {
    return DELA_CLIENT_TYPE;
  }

  public synchronized String getHOPSSITE_HOST() {
    checkCache();
    return HOPSSITE_HOST;
  }

  public synchronized String getHOPSSITE() {
    checkCache();
    return HOPSSITE;
  }

  public synchronized long getHOPSSITE_HEARTBEAT_RETRY() {
    checkCache();
    return HOPSSITE_HEARTBEAT_RETRY;
  }

  public synchronized long getHOPSSITE_HEARTBEAT_INTERVAL() {
    checkCache();
    return HOPSSITE_HEARTBEAT_INTERVAL;
  }

  public synchronized String getDELA_TRANSFER_IP() {
    checkCache();
    return DELA_TRANSFER_IP;
  }

  public synchronized String getDELA_TRANSFER_HTTP_PORT() {
    checkCache();
    return DELA_TRANSFER_HTTP_PORT;
  }

  public synchronized String getDELA_TRANSFER_HTTP_ENDPOINT() {
    checkCache();
    return "http://" + DELA_TRANSFER_IP + ":" + DELA_TRANSFER_HTTP_PORT + "/";
  }

  public synchronized String getDELA_HOPSWORKS_PORT() {
    checkCache();
    return DELA_PUBLIC_HOPSWORK_PORT;
  }

  public synchronized AddressJSON getDELA_PUBLIC_ENDPOINT() {
    return DELA_PUBLIC_ENDPOINT;
  }

  public synchronized String getDELA_SEARCH_ENDPOINT() {
    checkCache();
    if (DELA_SEARCH_ENDPOINT != null) {
      return DELA_SEARCH_ENDPOINT;
    }
    Variables v = findById(DELA_SEARCH_ENDPOINT);
    if (v != null) {
      return v.getValue();
    }
    return null;
  }

  public synchronized String getDELA_TRANSFER_ENDPOINT() {
    checkCache();
    if (DELA_TRANSFER_ENDPOINT != null) {
      return DELA_TRANSFER_ENDPOINT;
    }
    Variables v = findById(DELA_TRANSFER_ENDPOINT);
    if (v != null) {
      return v.getValue();
    }
    return null;
  }

  public synchronized void setDELA_PUBLIC_ENDPOINT(AddressJSON endpoint) {
    DELA_PUBLIC_ENDPOINT = endpoint;

    String delaSearchEndpoint = "http://" + endpoint.getIp() + ":"
        + getDELA_HOPSWORKS_PORT() + "/hopsworks-api/api";
    String delaTransferEndpoint = endpoint.getIp() + ":" + endpoint.getPort() + "/" + endpoint.getId();

    if (getDELA_SEARCH_ENDPOINT() == null) {
      em.persist(new Variables(VARIABLE_DELA_SEARCH_ENDPOINT, delaSearchEndpoint));
    } else {
      em.merge(new Variables(VARIABLE_DELA_SEARCH_ENDPOINT, delaSearchEndpoint));
    }
    DELA_SEARCH_ENDPOINT = delaSearchEndpoint;

    if (getDELA_TRANSFER_ENDPOINT() == null) {
      em.persist(new Variables(VARIABLE_DELA_TRANSFER_ENDPOINT, delaTransferEndpoint));
    } else {
      em.merge(new Variables(VARIABLE_DELA_TRANSFER_ENDPOINT, delaTransferEndpoint));
    }
    DELA_TRANSFER_ENDPOINT = delaTransferEndpoint;
  }

  public synchronized void setDELA_CLUSTER_ID(String id) {
    if (getDELA_CLUSTER_ID() == null) {
      em.persist(new Variables(VARIABLE_DELA_CLUSTER_ID, id));
    } else {
      em.merge(new Variables(VARIABLE_DELA_CLUSTER_ID, id));
    }
    DELA_CLUSTER_ID = id;
  }

  public synchronized String getDELA_CLUSTER_ID() {
    checkCache();
    if (DELA_CLUSTER_ID != null) {
      return DELA_CLUSTER_ID;
    } else {
      Variables v = findById(VARIABLE_DELA_CLUSTER_ID);
      if (v != null) {
        return v.getValue();
      }
      return null;
    }
  }

  public synchronized String getDELA_DOMAIN() {
    if (DELA_PUBLIC_ENDPOINT != null) {
      return DELA_PUBLIC_ENDPOINT.getIp();
    }
    return null;
  }
  //********************************************************************************************************************

  public final static String PROJECT_GENERIC_USER_SUFFIX = HdfsUsersController.USER_NAME_DELIMITER
      + "PROJECTGENERICUSER";

  //************************************************CERTIFICATES********************************************************
  private static final String HOPS_SITE_CA_DIR = "hops-site-certs";
  public final static String HOPS_SITE_CERTFILE = "/pub.pem";
  public final static String HOPS_SITE_CA_CERTFILE = "/ca_pub.pem";
  public final static String HOPS_SITE_INTERMEDIATE_CERTFILE = "/intermediate_ca_pub.pem";
  public final static String HOPS_SITE_KEY_STORE = "/keystores/keystore.jks";
  public final static String HOPS_SITE_TRUST_STORE = "/keystores/truststore.jks";

  private static final String VARIABLE_HOPSSITE_CLUSTER_NAME = "hops_site_cluster_name";
  private static final String VARIABLE_HOPSSITE_CLUSTER_PSWD = "hops_site_cluster_pswd";
  private static final String VARIABLE_HOPSSITE_CLUSTER_PSWD_AUX = "hops_site_cluster_pswd_aux";

  private String HOPSSITE_CLUSTER_NAME = null;
  private String HOPSSITE_CLUSTER_PSWD = null;
  private String HOPSSITE_CLUSTER_PSWD_AUX = "1234";

  public synchronized Optional<String> getHopsSiteClusterName() {
    checkCache();
    return Optional.ofNullable(HOPSSITE_CLUSTER_NAME);
  }

  public synchronized void setHopsSiteClusterName(String clusterName) {
    if (getHopsSiteClusterName().isPresent()) {
      em.merge(new Variables(VARIABLE_HOPSSITE_CLUSTER_NAME, clusterName));
    } else {
      em.persist(new Variables(VARIABLE_HOPSSITE_CLUSTER_NAME, clusterName));
    }
    HOPSSITE_CLUSTER_NAME = clusterName;
  }

  public synchronized void deleteHopsSiteClusterName() {
    if (getHopsSiteClusterName().isPresent()) {
      Variables v = findById(VARIABLE_HOPSSITE_CLUSTER_NAME);
      em.remove(v);
      HOPSSITE_CLUSTER_NAME = null;
    }
  }

  public synchronized String getHopsSiteClusterPswdAux() {
    checkCache();
    return HOPSSITE_CLUSTER_PSWD_AUX;
  }

  public synchronized Optional<String> getHopsSiteClusterPswd() {
    checkCache();
    return Optional.ofNullable(HOPSSITE_CLUSTER_PSWD);
  }

  public synchronized void setHopsSiteClusterPswd(String pswd) {
    if (getHopsSiteClusterPswd().isPresent()) {
      em.merge(new Variables(VARIABLE_HOPSSITE_CLUSTER_PSWD, pswd));
    } else {
      em.persist(new Variables(VARIABLE_HOPSSITE_CLUSTER_PSWD, pswd));
    }
    HOPSSITE_CLUSTER_PSWD = pswd;
  }

  public synchronized String getHopsSiteCaDir() {
    return getCertsDir() + File.separator + HOPS_SITE_CA_DIR;
  }

  public synchronized String getHopsSiteCaScript() {
    return getHopsworksInstallDir()
        + File.separator + "domain1"
        + File.separator + "bin"
        + File.separator + "ca-keystore.sh";
  }

  public synchronized String getHopsSiteCert() {
    return getHopsSiteCaDir() + HOPS_SITE_CERTFILE;
  }

  public synchronized String getHopsSiteCaCert() {
    return getHopsSiteCaDir() + HOPS_SITE_CA_CERTFILE;
  }

  public synchronized String getHopsSiteIntermediateCert() {
    return getHopsSiteCaDir() + HOPS_SITE_INTERMEDIATE_CERTFILE;
  }

  public synchronized String getHopsSiteKeyStorePath() {
    return getHopsSiteCaDir() + HOPS_SITE_KEY_STORE;
  }

  public synchronized String getHopsSiteTrustStorePath() {
    return getHopsSiteCaDir() + HOPS_SITE_TRUST_STORE;
  }
  //Dela END

  //************************************************ZOOKEEPER********************************************************
  public static final int ZOOKEEPER_SESSION_TIMEOUT_MS = 30 * 1000;//30 seconds
  public static final int ZOOKEEPER_CONNECTION_TIMEOUT_MS = 30 * 1000;// 30 seconds
  //Zookeeper END

  //************************************************KAFKA********************************************************
  public static final String KAFKA_ACL_WILDCARD = "*";
  public static final String KAFKA_DEFAULT_CONSUMER_GROUP = "default";
  public static final String KAFKA_BROKER_PROTOCOL = "INTERNAL";
  //These brokers are updated periodically by ZookeeperTimerThread
  public Set<String> kafkaBrokers = new HashSet<>();

  public synchronized Set<String> getKafkaBrokers() {
    return kafkaBrokers;
  }

  /**
   * Used when the application does not want all the brokers but mearly one to connect.
   *
   * @return
   */
  public synchronized String getRandomKafkaBroker() {
    Iterator<String> it = this.kafkaBrokers.iterator();
    if (it.hasNext()) {
      return it.next();
    }
    return null;
  }

  /**
   *
   * @return
   */
  public synchronized String getKafkaBrokersStr() {
    if (!kafkaBrokers.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (String addr : kafkaBrokers) {
        sb.append(addr).append(",");
      }
      return sb.substring(0, sb.length() - 1);
    }
    return null;
  }

  public synchronized void setKafkaBrokers(Set<String> kafkaBrokers) {
    this.kafkaBrokers.clear();
    this.kafkaBrokers.addAll(kafkaBrokers);
  }

  public Set<String> getBrokerEndpoints() throws AppException {

    Set<String> brokerList = new HashSet<>();
    ZooKeeper zk = null;
    try {
      zk = new ZooKeeper(getZkConnectStr(),
          Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, new ZookeeperWatcher());

      List<String> ids = zk.getChildren("/brokers/ids", false);
      for (String id : ids) {
        String brokerInfo = new String(zk.getData("/brokers/ids/" + id,
            false, null));
        String[] tokens = brokerInfo.split(DLIMITER);
        for (String str : tokens) {
          if (str.contains(SLASH_SEPARATOR) && str.startsWith(KAFKA_BROKER_PROTOCOL)) {
            brokerList.add(str);
          }
        }
      }
    } catch (IOException ex) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
          "Unable to find the zookeeper server: " + ex);
    } catch (KeeperException | InterruptedException ex) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
          "Unable to retrieve seed brokers from the kafka cluster: " + ex);
    } finally {
      if (zk != null) {
        try {
          zk.close();
        } catch (InterruptedException ex) {
          logger.log(Level.SEVERE, null, ex.getMessage());
        }
      }
    }

    return brokerList;
  }

  public class ZookeeperWatcher implements Watcher {

    @Override
    public void process(WatchedEvent we) {
    }
  }
  //Kafka END
  
  
  //-------------------------LDAP----------------------------
  private static final String VARIABLE_LDAP_AUTH = "ldap_auth";
  private static final String VARIABLE_LDAP_GROUP_MAPPING = "ldap_group_mapping";
  private static final String VARIABLE_LDAP_USER_ID = "ldap_user_id";
  private static final String VARIABLE_LDAP_USER_GIVEN_NAME = "ldap_user_givenName";
  private static final String VARIABLE_LDAP_USER_SURNAME = "ldap_user_surname";
  private static final String VARIABLE_LDAP_USER_EMAIL = "ldap_user_email";
  private static final String VARIABLE_LDAP_USER_SEARCH_FILTER = "ldap_user_search_filter";
  private static final String VARIABLE_LDAP_GROUP_SEARCH_FILTER = "ldap_group_search_filter";
  private static final String VARIABLE_LDAP_ATTR_BINARY = "ldap_attr_binary";
  private static final String VARIABLE_LDAP_GROUP_TARGET = "ldap_group_target";
  private static final String VARIABLE_LDAP_DYNAMIC_GROUP_TARGET = "ldap_dyn_group_target";
  private static final String VARIABLE_LDAP_USERDN = "ldap_user_dn";
  private static final String VARIABLE_LDAP_GROUPDN = "ldap_group_dn";
  private static final String VARIABLE_LDAP_ACCOUNT_STATUS = "ldap_account_status";

  private String LDAP_AUTH = "false";
  private String LDAP_GROUP_MAPPING = "";
  private String LDAP_USER_ID = "uid"; //login name
  private String LDAP_USER_GIVEN_NAME = "givenName";
  private String LDAP_USER_SURNAME = "sn";
  private String LDAP_USER_EMAIL = "mail";
  private String LDAP_USER_SEARCH_FILTER = "uid=%s";
  private String LDAP_GROUP_SEARCH_FILTER = "member=%d";
  private String LDAP_ATTR_BINARY = "java.naming.ldap.attributes.binary";
  private String LDAP_GROUP_TARGET = "cn";
  private String LDAP_DYNAMIC_GROUP_TARGET = "memberOf";
  private String LDAP_LDAP_USERDN = "";
  private String LDAP_LDAP_GROUPDN = "";
  private int LDAP_ACCOUNT_STATUS = 4;

  private void populateLDAPCache() {
    LDAP_AUTH = setVar(VARIABLE_LDAP_AUTH, LDAP_AUTH);
    LDAP_GROUP_MAPPING = setVar(VARIABLE_LDAP_GROUP_MAPPING, LDAP_GROUP_MAPPING);
    LDAP_USER_ID = setVar(VARIABLE_LDAP_USER_ID, LDAP_USER_ID);
    LDAP_USER_GIVEN_NAME = setVar(VARIABLE_LDAP_USER_GIVEN_NAME, LDAP_USER_GIVEN_NAME);
    LDAP_USER_SURNAME = setVar(VARIABLE_LDAP_USER_SURNAME, LDAP_USER_SURNAME);
    LDAP_USER_EMAIL = setVar(VARIABLE_LDAP_USER_EMAIL, LDAP_USER_EMAIL);
    LDAP_USER_SEARCH_FILTER = setVar(VARIABLE_LDAP_USER_SEARCH_FILTER, LDAP_USER_SEARCH_FILTER);
    LDAP_GROUP_SEARCH_FILTER = setVar(VARIABLE_LDAP_GROUP_SEARCH_FILTER, LDAP_GROUP_SEARCH_FILTER);
    LDAP_ATTR_BINARY = setVar(VARIABLE_LDAP_ATTR_BINARY, LDAP_ATTR_BINARY);
    LDAP_GROUP_TARGET = setVar(VARIABLE_LDAP_GROUP_TARGET, LDAP_GROUP_TARGET);
    LDAP_DYNAMIC_GROUP_TARGET = setVar(VARIABLE_LDAP_DYNAMIC_GROUP_TARGET, LDAP_DYNAMIC_GROUP_TARGET);
    LDAP_LDAP_USERDN = setVar(VARIABLE_LDAP_USERDN, LDAP_LDAP_USERDN);
    LDAP_LDAP_GROUPDN = setVar(VARIABLE_LDAP_GROUPDN, LDAP_LDAP_GROUPDN);
    LDAP_ACCOUNT_STATUS = setIntVar(VARIABLE_LDAP_ACCOUNT_STATUS, LDAP_ACCOUNT_STATUS);
  }

  public synchronized String getLDAPAuthStatus() {
    checkCache();
    return LDAP_AUTH;
  }

  public synchronized String getLdapGroupMapping() {
    checkCache();
    return LDAP_GROUP_MAPPING;
  }

  public synchronized String getLdapUserId() {
    checkCache();
    return LDAP_USER_ID;
  }

  public synchronized String getLdapUserGivenName() {
    checkCache();
    return LDAP_USER_GIVEN_NAME;
  }

  public synchronized String getLdapUserSurname() {
    checkCache();
    return LDAP_USER_SURNAME;
  }

  public synchronized String getLdapUserMail() {
    checkCache();
    return LDAP_USER_EMAIL;
  }

  public synchronized String getLdapUserSearchFilter() {
    checkCache();
    return LDAP_USER_SEARCH_FILTER;
  }

  public synchronized String getLdapGroupSearchFilter() {
    checkCache();
    return LDAP_GROUP_SEARCH_FILTER;
  }

  public synchronized String getLdapAttrBinary() {
    checkCache();
    return LDAP_ATTR_BINARY;
  }

  public synchronized String getLdapGroupTarget() {
    checkCache();
    return LDAP_GROUP_TARGET;
  }

  public synchronized String getLdapDynGroupTarget() {
    checkCache();
    return LDAP_DYNAMIC_GROUP_TARGET;
  }

  public synchronized String getLdapUserDN() {
    checkCache();
    return LDAP_LDAP_USERDN;
  }

  public synchronized String getLdapGroupDN() {
    checkCache();
    return LDAP_LDAP_GROUPDN;
  }

  public synchronized int getLdapAccountStatus() {
    checkCache();
    return LDAP_ACCOUNT_STATUS;
  }
  //----------------------------END LDAP------------------------------------
}
