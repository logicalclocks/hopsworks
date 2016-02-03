package se.kth.hopsworks.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.PostConstruct;
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

  @PostConstruct
  public void init() {
  }

  /**
   * Global Variables taken from the DB
   */
  private static final String VARIABLE_ELASTIC_IP = "elastic_ip";
  private static final String VARIABLE_SPARK_USER = "spark_user";
  private static final String VARIABLE_YARN_SUPERUSER = "yarn_user";
  private static final String VARIABLE_HDFS_SUPERUSER = "hdfs_user";
  private static final String VARIABLE_ZEPPELIN_DIR = "zeppelin_dir";
  private static final String VARIABLE_ZEPPELIN_USER = "zeppelin_user";
  private static final String VARIABLE_SPARK_DIR = "spark_dir";
  private static final String VARIABLE_FLINK_DIR = "flink_dir";
  private static final String VARIABLE_FLINK_USER = "flink_user";
  private static final String VARIABLE_NDB_DIR = "ndb_dir";
  private static final String VARIABLE_MYSQL_DIR = "mysql_dir";
  private static final String VARIABLE_HADOOP_DIR = "hadoop_dir";
  private static final String VARIABLE_CHARON_DIR = "charon_dir";
  private static final String VARIABLE_HIWAY_DIR = "hiway_dir";
  private static final String VARIABLE_YARN_DEFAULT_QUOTA = "yarn_default_quota";
  private static final String VARIABLE_HDFS_DEFAULT_QUOTA = "hdfs_default_quota";
  private static final String VARIABLE_MAX_NUM_PROJ_PER_USER = "max_num_proj_per_user";
  private static final String VARIABLE_ADAM_USER = "adam_user";
  private static final String VARIABLE_ADAM_DIR = "adam_dir";

  private String setUserVar(String varName, String defaultValue) {
    Variables userName = findById(varName);
    if (userName != null && userName.getValue() != null && (userName.getValue().isEmpty() == false)) {
      String user = userName.getValue();
      if (user != null && user.isEmpty() == false) {
        return user;
      }
    }
    return defaultValue;
  }

  private String setDirVar(String varName, String defaultValue) {
    Variables dirName = findById(varName);
    if (dirName != null && dirName.getValue() != null && (new File(dirName.getValue()).isDirectory())) {
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

  private boolean cached = false;

  private void populateCache() {
    if (!cached) {
      HDFS_SUPERUSER = setUserVar(VARIABLE_HDFS_SUPERUSER, HDFS_SUPERUSER);
      YARN_SUPERUSER = setUserVar(VARIABLE_YARN_SUPERUSER, YARN_SUPERUSER);
      SPARK_USER = setUserVar(VARIABLE_SPARK_USER, SPARK_USER);
      SPARK_DIR = setDirVar(VARIABLE_SPARK_DIR, SPARK_DIR);
      FLINK_USER = setUserVar(VARIABLE_FLINK_USER, FLINK_USER);
      FLINK_DIR = setDirVar(VARIABLE_FLINK_DIR, FLINK_DIR);
      ZEPPELIN_USER = setUserVar(VARIABLE_ZEPPELIN_USER, ZEPPELIN_USER);
      ZEPPELIN_DIR = setDirVar(VARIABLE_ZEPPELIN_DIR, ZEPPELIN_DIR);
      ADAM_USER = setUserVar(VARIABLE_ADAM_USER, ADAM_USER);
      ADAM_DIR = setDirVar(VARIABLE_ADAM_DIR, ADAM_DIR);
      MYSQL_DIR = setDirVar(VARIABLE_MYSQL_DIR, MYSQL_DIR);
      HADOOP_DIR = setDirVar(VARIABLE_HADOOP_DIR, HADOOP_DIR);
      NDB_DIR = setDirVar(VARIABLE_NDB_DIR, NDB_DIR);
      ELASTIC_IP = setIpVar(VARIABLE_ELASTIC_IP, ELASTIC_IP);
      CHARON_DIR = setDirVar(VARIABLE_CHARON_DIR, CHARON_DIR);
      HIWAY_DIR = setDirVar(VARIABLE_HIWAY_DIR, HIWAY_DIR);
      YARN_DEFAULT_QUOTA = setDirVar(VARIABLE_YARN_DEFAULT_QUOTA, YARN_DEFAULT_QUOTA);
      HDFS_DEFAULT_QUOTA = setDirVar(VARIABLE_HDFS_DEFAULT_QUOTA, HDFS_DEFAULT_QUOTA);
      MAX_NUM_PROJ_PER_USER = setDirVar(VARIABLE_MAX_NUM_PROJ_PER_USER, MAX_NUM_PROJ_PER_USER);
      cached = true;
    }
  }

  private void checkCache() {
    if (!cached) {
      populateCache();
    }
  }

  private String CHARON_DIR = "/srv/Charon";

  public synchronized String getCharonDir() {
    checkCache();
    return CHARON_DIR;
  }

  public synchronized String getCharonMountDir() {
    checkCache();
    return CHARON_DIR + "/charon_fs";
  }

  public synchronized String getCharonProjectDir(String projectName) {
    return getCharonMountDir() + "/" + projectName;
  }

  /**
   * Default Directory locations
   */
  private String SPARK_DIR = "/srv/spark";

  public synchronized String getSparkDir() {
    checkCache();
    return SPARK_DIR;
  }

  private String ADAM_USER = "glassfish";

  public synchronized String getAdamUser() {
    checkCache();
    return ADAM_USER;
  }

  private String FLINK_DIR = "/usr/local/flink";

  public synchronized String getFlinkDir() {
    checkCache();
    return FLINK_DIR;
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
  private String ZEPPELIN_DIR = "/srv/zeppelin";

  public synchronized String getZeppelinDir() {
    checkCache();
    return ZEPPELIN_DIR;
  }

  private String ADAM_DIR = "/srv/adam";

  public synchronized String getAdamDir() {
    checkCache();
    return ADAM_DIR;
  }
  
  
  private String HADOOP_DIR = "/srv/hadoop";

  public synchronized String getHadoopDir() {
    checkCache();
    return HADOOP_DIR;
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

  private String FLINK_USER = "flink";

  public synchronized String getFlinkUser() {
    checkCache();
    return FLINK_USER;
  }

  private String ZEPPELIN_USER = "glassfish";

  public synchronized String getZeppelinUser() {
    checkCache();
    return ZEPPELIN_USER;
  }

  private String HIWAY_DIR = "/home/glassfish";

  public synchronized String getHiwayDir() {
    checkCache();
    return HIWAY_DIR;
  }

  private String YARN_DEFAULT_QUOTA = "100";

  public synchronized String getYarnDefaultQuota() {
    checkCache();
    return YARN_DEFAULT_QUOTA;
  }

  private String HDFS_DEFAULT_QUOTA = "100";

  public synchronized String getHdfsDefaultQuota() {
    checkCache();
    return HDFS_DEFAULT_QUOTA;
  }

  private String MAX_NUM_PROJ_PER_USER = "10";

  public synchronized String getMaxNumProjPerUser() {
    checkCache();
    return MAX_NUM_PROJ_PER_USER;
  }

  public static String HIWAY_REL_JAR_PATH = "software/hiway/hiway-core.jar";

  //Local path to the hiway jar
//  public static final String HIWAY_JAR_PATH = "/home/glassfish/software/hiway";
  //Relative output path (within hdfs project folder) which to write cuneiform in-/output to
  public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Logs/Cuneiform/";

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

  //Environment variable keys
  public static final String ENV_KEY_YARN_CONF_DIR = "hdfs";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";

  //YARN constants
  public static final int YARN_DEFAULT_APP_MASTER_MEMORY = 512;
  public static final String YARN_DEFAULT_OUTPUT_PATH = "Logs/Yarn/";
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
//  private static String HADOOP_COMMON_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
//  private static final String HADOOP_HDFS_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
//  private static final String HADOOP_YARN_HOME_VALUE = HADOOP_DIR;
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";
//  public static final String HADOOP_CONF_DIR_VALUE = HADOOP_CONF_DIR;

  public static final String HADOOP_CONF_RELATIVE_DIR = "etc/hadoop";
  public static final String YARN_CONF_RELATIVE_DIR = HADOOP_CONF_RELATIVE_DIR;

  //Spark constants
  public static final String SPARK_STAGING_DIR = ".sparkStaging";
  public static final String SPARK_LOCRSC_SPARK_JAR = "__spark__.jar";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";
  public static final String SPARK_AM_MAIN = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Logs/Spark/";

  public synchronized String getLocalSparkJarPath() {
    return getSparkDir() + "/spark.jar";
  }

  public synchronized String getHdfsSparkJarPath() {
    return hdfsSparkJarPath(getSparkUser());
  }

  private static String hdfsSparkJarPath(String sparkUser) {
    return "hdfs:///user/" + sparkUser + "/spark.jar";
  }

  public static String getHdfsSparkJarPath(String sparkUser) {
    return hdfsSparkJarPath(sparkUser);
  }

  public synchronized String getSparkDefaultClasspath() {
    return sparkDefaultClasspath(getSparkDir());
  }

  private static String sparkDefaultClasspath(String sparkDir) {
    return sparkDir + "/conf:" + sparkDir + "/lib/*";
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
    return "hdfs:///user/" + getAdamUser() + "/repo/adam-cli.jar";
  }

  //Directory names in HDFS
  public static final String DIR_ROOT = "Projects";
  public static final String DIR_SAMPLES = "Samples";
  public static final String DIR_CUNEIFORM = "Cuneiform";
  public static final String DIR_RESULTS = "Results";
  public static final String DIR_CONSENTS = "consents";
  public static final String DIR_BAM = "bam";
  public static final String DIR_SAM = "sam";
  public static final String DIR_FASTQ = "fastq";
  public static final String DIR_FASTA = "fasta";
  public static final String DIR_VCF = "vcf";
  public static final String DIR_TEMPLATES = "Templates";
  public static final String PROJECT_STAGING_DIR = "resources";

  // Elasticsearch
  private String ELASTIC_IP = "127.0.0.1";

  public synchronized String getElasticIp() {
    checkCache();
    return ELASTIC_IP;
  }

  public static final int ELASTIC_PORT = 9300;

  // Hopsworks
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String HOPS_USERNAME_SEPARATOR = "__";
  public static final String HOPS_USERS_HOMEDIR = "/srv/users/";
  public static final int MAX_USERNME_LEN = 32;
  public static final int MAX_RETRIES = 500;
  public static final String META_NAME_FIELD = "name";
  public static final String META_DESCRIPTION_FIELD = "description";
  public static final String META_DATA_FIELD = "EXTENDED_METADATA";
  public static final String META_PROJECT_INDEX = "project";
  public static final String META_DATASET_INDEX = "dataset";
  public static final String META_PROJECT_PARENT_TYPE = "parent";
  public static final String META_PROJECT_CHILD_TYPE = "child";
  public static final String META_DATASET_PARENT_TYPE = "parent";
  public static final String META_DATASET_CHILD_TYPE = "child";
  public static final String META_INODE_SEARCHABLE_FIELD = "searchable";
  public static final String META_INODE_OPERATION_FIELD = "operation";

  public static final int META_INODE_OPERATION_ADD = 0;
  public static final int META_INODE_OPERATION_DELETE = 1;

  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#";
  public static final String PRINT_FILENAME_DISALLOWED_CHARS
      = "__, space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #";
  public static final String SHARED_FILE_SEPARATOR = "::";
  public static final String DOUBLE_UNDERSCORE = "__";

  //Project creation: default datasets
  public static enum DefaultDataset {

    LOGS("Logs", "Contains the logs for jobs that have been run through the Hopsworks platform."),
    RESOURCES("Resources", "Contains resources used by jobs, for example, jar files.");
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

  public Settings() {
  }

  /**
   * Get the variable value with the given name.
   * <p/>
   * @param id
   * @return The user with given email, or null if no such user exists.
   */
  public Variables
      findById(String id) {
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
