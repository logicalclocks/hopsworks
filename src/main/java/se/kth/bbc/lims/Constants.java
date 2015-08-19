package se.kth.bbc.lims;

/**
 * Constants class to facilitate deployment on different servers. TODO: move
 * this to configuration file to be read!
 *
 * @author stig
 */
public class Constants {

  //TODO: check entire project for correct closing of resources!
  //Local path to the hiway jar
  public static final String HIWAY_JAR_PATH
          = "/srv/hiway-1.0.1-SNAPSHOT/hiway-core-1.0.1-SNAPSHOT.jar";

  //User under which yarn is run
  public static final String DEFAULT_YARN_USER = "glassfish";

  //Relative output path (within hdfs project folder) which to write cuneiform in-/output to
  public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Cuneiform/Output/";
  public static final String CUNEIFORM_DEFAULT_INPUT_PATH = "Cuneiform/Input/";

  //Default configuration locations
  public static final String DEFAULT_HADOOP_CONF_DIR
          = "/srv/hadoop/etc/hadoop/";
  public static final String DEFAULT_YARN_CONF_DIR
          = "/srv/hadoop/etc/hadoop/";

  //Default configuration file names
  public static final String DEFAULT_YARN_CONFFILE_NAME = "yarn-site.xml";
  public static final String DEFAULT_HADOOP_CONFFILE_NAME = "core-site.xml";
  public static final String DEFAULT_HDFS_CONFFILE_NAME = "hdfs-site.xml";

  //Environment variable keys
  public static final String ENV_KEY_YARN_CONF_DIR = "YARN_CONF_DIR";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";

  //YARN constants
  public static final String YARN_DEFAULT_OUTPUT_PATH = "Yarn/Output/";
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
  public static final String HADOOP_COMMON_HOME_VALUE
          = "/srv/hadoop";
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
  public static final String HADOOP_HDFS_HOME_VALUE
          = "/srv/hadoop";
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
  public static final String HADOOP_YARN_HOME_VALUE
          = "/srv/hadoop";
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";
  public static final String HADOOP_CONF_DIR_VALUE
          = "/srv/hadoop/etc/hadoop";

  //Spark constants
  public static final String SPARK_VERSION = "1.2.0";
  public static final String SPARK_STAGING_DIR = ".sparkStaging";
  public static final String SPARK_LOCRSC_SPARK_JAR = "__spark__.jar";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";
  public static final String SPARK_AM_MAIN
          = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String DEFAULT_SPARK_JAR_PATH = "/srv/spark/spark.jar";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Spark/Output/";
  public static final String DEFAULT_SPARK_JAR_HDFS_PATH
          = "hdfs:///user/spark/spark.jar";
  public static final String SPARK_DEFAULT_CLASSPATH = "/srv/spark/conf:"
          + "/srv/spark/lib/spark-assembly-" + SPARK_VERSION
          + "-hadoop2.4.0.jar:"
          + "/srv/spark/lib/datanucleus-core-3.2.10.jar:"
          + "/srv/spark/lib/datanucleus-api-jdo-3.2.6.jar:"
          + "/srv/spark/lib/datanucleus-rdbms-3.2.9.jar";

  //ADAM constants
  public static final String ADAM_MAINCLASS = "org.bdgenomics.adam.cli.ADAMMain";
  public static final String ADAM_DEFAULT_JAR_HDFS_PATH
          = "hdfs:///user/adam/repo/adam-cli.jar";
  //Or: "adam-cli/target/appassembler/repo/org/bdgenomics/adam/adam-cli/0.15.1-SNAPSHOT/adam-cli-0.15.1-SNAPSHOT.jar"
  public static final String ADAM_DEFAULT_OUTPUT_PATH = "Adam/Output/";
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

  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#";
  public static final String PRINT_FILENAME_DISALLOWED_CHARS
          = "space, /, \\, ?, *, :, |, ', \", <, >, %, (, ), &, ;, #";
  public static final String SHARED_FILE_SEPARATOR = "::";
}
