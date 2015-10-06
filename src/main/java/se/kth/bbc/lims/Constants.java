package se.kth.bbc.lims;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants class to facilitate deployment on different servers. TODO: move this to configuration file to be read!
 *
 * @author stig
 */
public class Constants {

  //Local path to the hiway jar
  public static final String HIWAY_JAR_PATH
      = "/srv/hiway-1.0.1-SNAPSHOT/hiway-core-1.0.1-SNAPSHOT.jar";

  //User under which yarn is run
  public static final String DEFAULT_YARN_USER = "glassfish";
  public static final String DEFAULT_SPARK_USER = "spark";

  //Relative output path (within hdfs project folder) which to write cuneiform in-/output to
  public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Logs/Cuneiform/";

  //Default configuration locations
  public static final String DEFAULT_HADOOP_CONF_DIR = "/srv/hadoop/etc/hadoop/";
  public static final String DEFAULT_YARN_CONF_DIR = DEFAULT_HADOOP_CONF_DIR;

  //Default configuration file names
  public static final String DEFAULT_YARN_CONFFILE_NAME = "yarn-site.xml";
  public static final String DEFAULT_HADOOP_CONFFILE_NAME = "core-site.xml";
  public static final String DEFAULT_HDFS_CONFFILE_NAME = "hdfs-site.xml";

  //Environment variable keys
  public static final String ENV_KEY_YARN_CONF_DIR = "YARN_CONF_DIR";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";

  //YARN constants
  public static final String YARN_DEFAULT_OUTPUT_PATH = "Logs/Yarn/";
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
  public static final String HADOOP_COMMON_HOME_VALUE = "/srv/hadoop";
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
  public static final String HADOOP_HDFS_HOME_VALUE = "/srv/hadoop";
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
  public static final String HADOOP_YARN_HOME_VALUE = "/srv/hadoop";
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";
  public static final String HADOOP_CONF_DIR_VALUE = "/srv/hadoop/etc/hadoop";

  //Spark constants
  public static final String SPARK_STAGING_DIR = ".sparkStaging";
  public static final String SPARK_LOCRSC_SPARK_JAR = "__spark__.jar";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";
  public static final String SPARK_AM_MAIN
      = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String DEFAULT_SPARK_JAR_PATH = "/srv/spark/spark.jar";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Logs/Spark/";
  public static String DEFAULT_SPARK_JAR_HDFS_PATH
          = "hdfs:///user/" + DEFAULT_SPARK_USER + "/spark.jar";
  public static final String SPARK_DEFAULT_CLASSPATH
      = "/srv/spark/conf:/srv/spark/lib/*";

  //ADAM constants
  public static final String ADAM_MAINCLASS = "org.bdgenomics.adam.cli.ADAMMain";
  public static final String ADAM_DEFAULT_JAR_HDFS_PATH
      = "hdfs:///user/adam/repo/adam-cli.jar";
  //Or: "adam-cli/target/appassembler/repo/org/bdgenomics/adam/adam-cli/0.15.1-SNAPSHOT/adam-cli-0.15.1-SNAPSHOT.jar"
  public static final String ADAM_DEFAULT_OUTPUT_PATH = "Logs/Adam/";
  public static final String ADAM_DEFAULT_HDFS_REPO = "/user/adam/repo/";

  //Directory names in HDFS
  public static final String DIR_ROOT = "Projects";
  public static final String DIR_SAMPLES = "Samples";
  public static final String DIR_CUNEIFORM = "Cuneiform";
  public static final String DIR_RESULTS = "Results";
  public static final String DIR_BAM = "bam";
  public static final String DIR_SAM = "sam";
  public static final String DIR_FASTQ = "fastq";
  public static final String DIR_FASTA = "fasta";
  public static final String DIR_VCF = "vcf";
  public static final String DIR_TEMPLATES = "Templates";

  // Hopsworks
  public final static Charset ENCODING = StandardCharsets.UTF_8;
  public static final String HOPS_USERNAME_SEPARATOR = "__";
  public static final String HOPS_USERS_HOMEDIR = "/srv/users/";
  public static final int MAX_USERNME_LEN = 16;
  public static final int MAX_RETRIES = 50;
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
  public static final String VARIABLE_ELASTIC_ADDR = "elastic_addr";
  public static final String VARIABLE_SPARK_USER = "spark_user";
  public static final int META_INODE_OPERATION_ADD = 0;
  public static final int META_INODE_OPERATION_DELETE = 1;

  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#";
  public static final String PRINT_FILENAME_DISALLOWED_CHARS
      = "space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #";
  public static final String SHARED_FILE_SEPARATOR = "::";

  //Project creation: default datasets
  public static enum DefaultDataset {

    LOGS("Logs",
        "Contains the logs for jobs that have been run through the Hopsworks platform."),
    RESOURCES("Resources",
        "Contains resources for job running, for instance jar files.");
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
}
