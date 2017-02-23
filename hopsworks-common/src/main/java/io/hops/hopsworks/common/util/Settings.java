package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.dao.util.Variables;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Settings {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public static String AGENT_EMAIL = "kagent@hops.io";
  public static final String SITE_EMAIL = "admin@kth.se";
  /**
   * Global Variables taken from the DB
   */
  private static final String VARIABLE_KIBANA_IP = "kibana_ip";
  private static final String VARIABLE_LIVY_IP = "livy_ip";
  private static final String VARIABLE_JHS_IP = "jhs_ip";
  private static final String VARIABLE_OOZIE_IP = "oozie_ip";
  private static final String VARIABLE_SPARK_HISTORY_SERVER_IP
          = "spark_history_server_ip";
  private static final String VARIABLE_ELASTIC_IP = "elastic_ip";
  private static final String VARIABLE_ELASTIC_PORT = "elastic_port";
  private static final String VARIABLE_SPARK_USER = "spark_user";
  private static final String VARIABLE_YARN_SUPERUSER = "yarn_user";
  private static final String VARIABLE_HDFS_SUPERUSER = "hdfs_user";
  private static final String VARIABLE_ZEPPELIN_DIR = "zeppelin_dir";
  private static final String VARIABLE_ZEPPELIN_PROJECTS_DIR
          = "zeppelin_projects_dir";
  private static final String VARIABLE_ZEPPELIN_SYNC_INTERVAL
          = "zeppelin_sync_interval";
  private static final String VARIABLE_ZEPPELIN_USER = "zeppelin_user";
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
  private static final String VARIABLE_ADAM_USER = "adam_user";
  private static final String VARIABLE_ADAM_DIR = "adam_dir";
  private static final String VARIABLE_TWOFACTOR_AUTH = "twofactor_auth";
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
  private static final String VARIABLE_YARN_WEB_UI_PORT = "yarn_ui_port";
  private static final String VARIABLE_FILE_PREVIEW_IMAGE_SIZE
          = "file_preview_image_size";
  private static final String VARIABLE_FILE_PREVIEW_TXT_SIZE
          = "file_preview_txt_size";
  private static final String VARIABLE_GVOD_REST_ENDPOINT = "gvod_rest_endpoint";
  private static final String VARIABLE_PUBLIC_SEARCH_ENDPOINT
          = "public_search_endpoint";
  private static final String VARIABLE_REST_PORT = "rest_port";

  public static final String ERASURE_CODING_CONFIG = "erasure-coding-site.xml";

  private static final String VARIABLE_KAFKA_NUM_PARTITIONS
          = "kafka_num_partitions";
  private static final String VARIABLE_KAFKA_NUM_REPLICAS = "kafka_num_replicas";
  private static final String VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD
          = "hopsworks_master_password";
  private static final String VARIABLE_GLASSFISH_CERT_CENERATED
          = "glassfish_cert";
  private static final String VARIABLE_ANACONDA_DIR = "anaconda_dir";
  private static final String VARIABLE_ANACONDA_INSTALLED = "anaconda_installed";

  private static final String VARIABLE_INFLUXDB_IP = "influxdb_ip";
  private static final String VARIABLE_INFLUXDB_PORT = "influxdb_port";
  private static final String VARIABLE_INFLUXDB_USER = "influxdb_user";
  private static final String VARIABLE_INFLUXDB_PW = "influxdb_pw";
  private static final String VARIABLE_ANACONDA_ENV = "anaconda_env";
  private static final String VARIABLE_GRAPHITE_PORT = "graphite_port";
  private static final String VARIABLE_RESOURCE_DIRS = "resources";

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
    Variables ip = findById(varName);
    if (ip != null && ip.getValue() != null && Ip.validIp(ip.getValue())) {
      String val = ip.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setDbVar(String varName, String defaultValue) {
    Variables ip = findById(varName);
    if (ip != null && ip.getValue() != null) {
      // TODO - check this is a valid DB name
      String val = ip.getValue();
      if (val != null && val.isEmpty() == false) {
        return val;
      }
    }
    return defaultValue;
  }

  private int setIntVar(String varName, int defaultValue) {
    Variables ip = findById(varName);
    if (ip != null && ip.getValue() != null) {
      String val = ip.getValue();
      if (val != null && val.isEmpty() == false) {
        return Integer.parseInt(val);
      }
    }
    return defaultValue;
  }

  private long setLongVar(String varName, long defaultValue) {
    Variables ip = findById(varName);
    if (ip != null && ip.getValue() != null) {
      String val = ip.getValue();
      if (val != null && val.isEmpty() == false) {
        return Long.parseLong(val);
      }
    }
    return defaultValue;
  }

  private boolean cached = false;

  private void populateCache() {
    if (!cached) {
      TWOFACTOR_AUTH = setVar(VARIABLE_TWOFACTOR_AUTH, TWOFACTOR_AUTH);
      HDFS_SUPERUSER = setVar(VARIABLE_HDFS_SUPERUSER, HDFS_SUPERUSER);
      YARN_SUPERUSER = setVar(VARIABLE_YARN_SUPERUSER, YARN_SUPERUSER);
      SPARK_USER = setVar(VARIABLE_SPARK_USER, SPARK_USER);
      SPARK_DIR = setDirVar(VARIABLE_SPARK_DIR, SPARK_DIR);
      FLINK_USER = setVar(VARIABLE_FLINK_USER, FLINK_USER);
      FLINK_DIR = setDirVar(VARIABLE_FLINK_DIR, FLINK_DIR);
      ZEPPELIN_USER = setVar(VARIABLE_ZEPPELIN_USER, ZEPPELIN_USER);
      ZEPPELIN_DIR = setDirVar(VARIABLE_ZEPPELIN_DIR, ZEPPELIN_DIR);
      ZEPPELIN_PROJECTS_DIR = setDirVar(VARIABLE_ZEPPELIN_PROJECTS_DIR,
              ZEPPELIN_PROJECTS_DIR);
      ZEPPELIN_SYNC_INTERVAL = setLongVar(VARIABLE_ZEPPELIN_SYNC_INTERVAL,
              ZEPPELIN_SYNC_INTERVAL);
      ADAM_USER = setVar(VARIABLE_ADAM_USER, ADAM_USER);
      ADAM_DIR = setDirVar(VARIABLE_ADAM_DIR, ADAM_DIR);
      MYSQL_DIR = setDirVar(VARIABLE_MYSQL_DIR, MYSQL_DIR);
      HADOOP_DIR = setDirVar(VARIABLE_HADOOP_DIR, HADOOP_DIR);
      HOPSWORKS_INSTALL_DIR = setDirVar(VARIABLE_HOPSWORKS_DIR,
              HOPSWORKS_INSTALL_DIR);
      NDB_DIR = setDirVar(VARIABLE_NDB_DIR, NDB_DIR);
      ELASTIC_IP = setIpVar(VARIABLE_ELASTIC_IP, ELASTIC_IP);
      ELASTIC_PORT = setIntVar(VARIABLE_ELASTIC_PORT, ELASTIC_PORT);
      JHS_IP = setIpVar(VARIABLE_JHS_IP, JHS_IP);
      LIVY_IP = setIpVar(VARIABLE_LIVY_IP, LIVY_IP);
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
      KAFKA_IP = setIpVar(VARIABLE_KAFKA_IP, KAFKA_IP);
      KAFKA_USER = setVar(VARIABLE_KAFKA_USER, KAFKA_USER);
      KAFKA_DIR = setDirVar(VARIABLE_KAFKA_DIR, KAFKA_DIR);
      KAFKA_DEFAULT_NUM_PARTITIONS = setDirVar(VARIABLE_KAFKA_NUM_PARTITIONS,
              KAFKA_DEFAULT_NUM_PARTITIONS);
      KAFKA_DEFAULT_NUM_REPLICAS = setDirVar(VARIABLE_KAFKA_NUM_REPLICAS,
              KAFKA_DEFAULT_NUM_REPLICAS);
      YARN_DEFAULT_QUOTA = setDirVar(VARIABLE_YARN_DEFAULT_QUOTA,
              YARN_DEFAULT_QUOTA);
      YARN_WEB_UI_IP = setIpVar(VARIABLE_YARN_WEB_UI_IP, YARN_WEB_UI_IP);
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
      FILE_PREVIEW_IMAGE_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_IMAGE_SIZE,
              10000000);
      FILE_PREVIEW_TXT_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_TXT_SIZE, 100);
      GVOD_REST_ENDPOINT = setStrVar(VARIABLE_GVOD_REST_ENDPOINT,
              GVOD_REST_ENDPOINT);
      PUBLIC_SEARCH_ENDPOINT = setStrVar(VARIABLE_PUBLIC_SEARCH_ENDPOINT,
              PUBLIC_SEARCH_ENDPOINT);
      REST_PORT = setIntVar(VARIABLE_REST_PORT, REST_PORT);
      ANACONDA_DIR = setDirVar(VARIABLE_ANACONDA_DIR, ANACONDA_DIR);
      ANACONDA_ENV = setStrVar(VARIABLE_ANACONDA_ENV, ANACONDA_ENV);
      ANACONDA_INSTALLED = Boolean.parseBoolean(setStrVar(
              VARIABLE_ANACONDA_INSTALLED, ANACONDA_INSTALLED.toString()));
      INFLUXDB_IP = setStrVar(VARIABLE_INFLUXDB_IP, INFLUXDB_IP);
      INFLUXDB_PORT = setStrVar(VARIABLE_INFLUXDB_PORT, INFLUXDB_PORT);
      INFLUXDB_USER = setStrVar(VARIABLE_INFLUXDB_USER, INFLUXDB_USER);
      INFLUXDB_PW = setStrVar(VARIABLE_INFLUXDB_PW, INFLUXDB_PW);
      RESOURCE_DIRS = setStrVar(VARIABLE_RESOURCE_DIRS, RESOURCE_DIRS);
      cached = true;
    }
  }

  private void checkCache() {
    if (!cached) {
      populateCache();
    }
  }

  private static final String GLASSFISH_DIR = "/srv/hops/glassfish";

  public static synchronized String getGlassfishDir() {
    return GLASSFISH_DIR;
  }

  private String TWOFACTOR_AUTH = "false";

  public synchronized String getTwoFactorAuth() {
    checkCache();
    return TWOFACTOR_AUTH;
  }

  /**
   * Default Directory locations
   */
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
  public static final String SPARK_EXECUTOR_MEMORY_ENV = "spark.executor.memory";
  public static final String SPARK_EXECUTOR_CORES_ENV = "spark.executor.cores";
  public static final String SPARK_EXECUTOR_EXTRACLASSPATH
          = "spark.executor.extraClassPath";
  public static final String SPARK_DRIVER_STAGINGDIR_ENV
          = "spark.yarn.stagingDir";
  public static final String SPARK_JAVA_LIBRARY_PROP = "java.library.path";
  public static final String SPARK_METRICS_ENV = "spark.metrics.conf";

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

  public synchronized String getSparkDir() {
    checkCache();
    return SPARK_DIR;
  }

  private final String SPARK_CONF_DIR = SPARK_DIR + "/conf";

  public synchronized String getSparkConfDir() {
    checkCache();
    return SPARK_CONF_DIR;
  }

  private final String SPARK_CONF_FILE = SPARK_CONF_DIR + "/spark-defaults.conf";

  public synchronized String getSparkConfFile() {
    //checkCache();
    return SPARK_CONF_FILE;
  }

  private String ADAM_USER = "glassfish";

  public synchronized String getAdamUser() {
    checkCache();
    return ADAM_USER;
  }

  private String FLINK_DIR = "/srv/hops/flink";

  public synchronized String getFlinkDir() {
    checkCache();
    return FLINK_DIR;
  }
  private final String FLINK_CONF_DIR = "conf";

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

  public synchronized String getHadoopDir() {
    checkCache();
    return HADOOP_DIR;
  }

  private static String HOPSWORKS_INSTALL_DIR = "/srv/hops";

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
    return getHopsworksDomainDir() + Settings.CA_DIR;
  }

  //User under which yarn is run
  private String YARN_SUPERUSER = "glassfish";

  public synchronized String getYarnSuperUser() {
    checkCache();
    return YARN_SUPERUSER;
  }
  private String HDFS_SUPERUSER = "glassfish";

  public synchronized String getHdfsSuperUser() {
    checkCache();
    return HDFS_SUPERUSER;
  }
  private String SPARK_USER = "glassfish";

  public synchronized String getSparkUser() {
    checkCache();
    return SPARK_USER;
  }

  private String FLINK_USER = "glassfish";

  public synchronized String getFlinkUser() {
    checkCache();
    return FLINK_USER;
  }

  private String ZEPPELIN_USER = "glassfish";

  public synchronized String getZeppelinUser() {
    checkCache();
    return ZEPPELIN_USER;
  }

  private final String HIWAY_DIR = "/home/glassfish";

  public synchronized String getHiwayDir() {
    checkCache();
    return HIWAY_DIR;
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

  //Hadoop locations
  public synchronized String getHadoopConfDir() {
    return hadoopConfDir(getHadoopDir());
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
  //public static final String SPARK_LOCRSC_SPARK_JAR = "__spark__.jar";
  public static final String SPARK_JARS = "spark.yarn.jars";
  public static final String SPARK_ARCHIVE = "spark.yarn.archive";
  // Subdirectory where Spark libraries will be placed.
  public static final String LOCALIZED_LIB_DIR = "__spark_libs__";
  public static final String LOCALIZED_CONF_DIR = "__spark_conf__";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";
  public static final String HOPSUTIL_JAR = "hops-util-0.1.jar";
  // Distribution-defined classpath to add to processes
  public static final String ENV_DIST_CLASSPATH = "SPARK_DIST_CLASSPATH";
  public static final String SPARK_AM_MAIN
          = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Logs/Spark/";
  public static final String SPARK_CONFIG_FILE = "conf/spark-defaults.conf";
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
  public static final String FLINK_KAFKA_CERTS_DIR
          = "/srv/hops/domain1/config";

  //Zeppelin constants
  public static final String JAVA_HOME = "/usr/lib/jvm/default-java";

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

  public static String getSparkLog4JPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/log4j.properties";
  }

  public static String getSparkMetricsPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/metrics.properties";
  }

  //TODO put the spark metrics in each project and take it from there
  public static String getProjectSparkMetricsPath(String projectName) {
    return "hdfs:///user/glassfish/metrics.properties";
  }

  public static String getProjectSparkLog4JPath(String sparkUser) {
    return "hdfs:///user/glassfish/log4j.properties";
  }

  public static String getHopsutilPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/" + HOPSUTIL_JAR;
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

  public static String getHdfsRootPath(String projectname) {
    return "/" + DIR_ROOT + "/" + projectname + "/";
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
  public static final String DIR_BAM = "bam";
  public static final String DIR_SAM = "sam";
  public static final String DIR_FASTQ = "fastq";
  public static final String DIR_FASTA = "fasta";
  public static final String DIR_VCF = "vcf";
  public static final String DIR_TEMPLATES = "Templates";
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
  
  public synchronized String getElasticEndpoint() {
    return getElasticIp() + ":" + getElasticPort();
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

  // Livy Server
  private String LIVY_IP = "127.0.0.1";
  private final String LIVY_YARN_MODE = "yarn";

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

  // Zeppelin
  private String ZEPPELIN_DIR = "/srv/zeppelin";

  public synchronized String getZeppelinDir() {
    checkCache();
    return ZEPPELIN_DIR;
  }

  private String ZEPPELIN_PROJECTS_DIR = "/srv/zeppelin/Projects";

  public synchronized String getZeppelinProjectsDir() {
    checkCache();
    return ZEPPELIN_PROJECTS_DIR;
  }

  private long ZEPPELIN_SYNC_INTERVAL = 24 * 60 * 60 * 1000;

  public synchronized long getZeppelinSyncInterval() {
    checkCache();
    return ZEPPELIN_SYNC_INTERVAL;
  }

  private long CONDA_SYNC_INTERVAL = 24 * 60 * 60 * 1000;

  public synchronized long getCondaSyncInterval() {
    checkCache();
    return CONDA_SYNC_INTERVAL;
  }

  // Kafka
  private String KAFKA_IP = "10.0.2.15";
  public static final int KAFKA_PORT = 9091;

  public synchronized String getKafkaConnectStr() {
    checkCache();
    return KAFKA_IP + ":" + KAFKA_PORT;
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

  private String ANACONDA_DIR = "/opt/anaconda/anaconda/envs";

  public synchronized String getAnacondaDir() {
    checkCache();
    return ANACONDA_DIR;
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

  private String GVOD_REST_ENDPOINT = "http://10.0.2.15:42000";

  public synchronized String getGVodRestEndpoint() {
    checkCache();
    return GVOD_REST_ENDPOINT;
  }

  private String PUBLIC_SEARCH_ENDPOINT
          = "http://10.0.2.15:8080/hopsworks-web-api/api/elastic/publicdatasets/";

  public synchronized String getPublicSearchEndpoint() {
    checkCache();
    return PUBLIC_SEARCH_ENDPOINT;
  }

  private int REST_PORT = 8080;

  public synchronized int getRestPort() {
    checkCache();
    return REST_PORT;
  }

  /**
   * Generates the Endpoint for kafka.
   *
   * @return
   */
  public String getRestEndpoint() {
    String gvod_endpoint = getGVodRestEndpoint();
    String ip = getGVodRestEndpoint().substring(0, gvod_endpoint.
            lastIndexOf(":"));
    int port = getRestPort();
    return ip + ":" + port;
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

  // Hopsworks
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String HOPS_USERS_HOMEDIR = "/home/";
  public static final String HOPS_USERNAME_SEPARATOR = "__";
  private static final String CA_DIR = "/config/ca/intermediate";
  public static final String SSL_CREATE_CERT_SCRIPTNAME = "createusercerts.sh";
  public static final String SSL_DELETE_CERT_SCRIPTNAME = "deleteusercerts.sh";
  public static final String SSL_DELETE_PROJECT_CERTS_SCRIPTNAME
          = "deleteprojectcerts.sh";
  public static final int USERNAME_LEN = 8;
  public static final int MAX_USERNAME_SUFFIX = 99;
  public static final int MAX_RETRIES = 500;
  public static final String META_NAME_FIELD = "name";
  public static final String META_DESCRIPTION_FIELD = "description";
  public static final String META_INDEX = "projects";
  public static final String META_PROJECT_TYPE = "proj";
  public static final String META_DATASET_TYPE = "ds";
  public static final String META_INODE_TYPE = "inode";
  public static final String META_PROJECT_ID_FIELD = "project_id";
  public static final String META_ID = "_id";
  public static final String META_DATA_NESTED_FIELD = "xattr";
  public static final String META_DATA_FIELDS = META_DATA_NESTED_FIELD + ".*";

  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS
          = " /\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String PRINT_FILENAME_DISALLOWED_CHARS
          = "__, space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #";
  public static final String SHARED_FILE_SEPARATOR = "::";
  public static final String DOUBLE_UNDERSCORE = "__";

  public static final String KAFKA_ACL_WILDCARD = "*";
  public static final String KAFKA_DEFAULT_CONSUMER_GROUP = "default";
  public static final String KAFKA_K_CERTIFICATE = "kafka_k_certificate";
  public static final String KAFKA_T_CERTIFICATE = "kafka_t_certificate";

  public static final String TMP_CERT_STORE_REMOTE
          = "/user/glassfish/kafkacerts";

  //Used to retrieve schema by HopsUtil
  public static final String KAFKA_SESSIONID_ENV_VAR = "hopsworks.sessionid";
  public static final String KAFKA_PROJECTID_ENV_VAR = "hopsworks.projectid";
  public static final String KAFKA_PROJECTNAME_ENV_VAR = "hopsworks.projectname";
  public static final String HOPSUTIL_JOBNAME_ENV_VAR = "hopsworks.jobname";
  public static final String KAFKA_BROKERADDR_ENV_VAR
          = "hopsworks.kafka.brokeraddress";
  public static final String KAFKA_JOB_ENV_VAR = "hopsworks.kafka.job";
  public static final String KAFKA_JOB_TOPICS_ENV_VAR
          = "hopsworks.kafka.job.topics";
  public static final String KEYSTORE_PASSWORD_ENV_VAR
          = "hopsworks.keystore.password";
  public static final String TRUSTSTORE_PASSWORD_ENV_VAR
          = "hopsworks.truststore.password";
  public static final String KAFKA_CONSUMER_GROUPS
          = "hopsworks.kafka.consumergroups";
  public static final String KAFKA_REST_ENDPOINT_ENV_VAR
          = "hopsworks.kafka.restendpoint";
  
  public static final String ELASTIC_ENDPOINT_ENV_VAR = "hopsworks.elastic.endpoint";

  public static int FILE_PREVIEW_IMAGE_SIZE = 10000000;
  public static int FILE_PREVIEW_TXT_SIZE = 100;
  public static int FILE_PREVIEW_TXT_SIZE_BYTES = 1024 * 128;
  public static int FILE_PREVIEW_TXT_SIZE_BYTES_README = 1024 * 512;
  public static String README_TEMPLATE = "*This is an auto-generated README.md"
          + " file for your Dataset!*\n"
          + "To replace it, go into your DataSet and edit the README.md file.\n"
          + "\n" + "*%s* DataSet\n" + "===\n" + "\n"
          + "## %s";

  public String getHopsworksTmpCertDir() {
    return getHopsworksDomainDir() + File.separator + "kafkacerts";
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
  public static enum DefaultDataset {

    LOGS("Logs",
            "Contains the logs for jobs that have been run through the Hopsworks platform."),
    RESOURCES("Resources",
            "Contains resources used by jobs, for example, jar files."),
    NOTEBOOKS("notebook", "Contains zeppelin notebooks.");
    private final String name;
    private final String description;

    private DefaultDataset(String name, String description) {
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
  
  public String RESOURCE_DIRS = ".sparkStaging;spark-warehouse";
  
  public synchronized String getResourceDirs() {
    checkCache();
    return RESOURCE_DIRS;
  }
  
  public Settings() {
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

//  public void setIdValue(String id, String value) {
//    Variables v = new Variables(id, value);
//    try {
//      em.persist(v)
//    } catch (EntityExistsException ex) {
//    }
//  }
  public void detach(Variables variable) {
    em.detach(variable);
  }

  public static String getProjectPath(String projectname) {
    return File.separator + DIR_ROOT + File.separator + projectname;
  }

}
