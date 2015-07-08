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
          = "hdfs:///user/adam/adam-cli-0.16.0.jar";
  //Or: "adam-cli/target/appassembler/repo/org/bdgenomics/adam/adam-cli/0.15.1-SNAPSHOT/adam-cli-0.15.1-SNAPSHOT.jar"
  public static final String ADAM_DEFAULT_OUTPUT_PATH = "Adam/Output/";
  public static final String[] ADAM_HDFS_JARS = {
    "hdfs:///user/adam/args4j-2.0.23.jar",
    "hdfs:///user/adam/snappy-java-1.1.1.6.jar",
    "hdfs:///user/adam/commons-jexl-2.1.1.jar",
    "hdfs:///user/adam/commons-compress-1.4.1.jar",
    "hdfs:///user/adam/bcel-5.2.jar",
    "hdfs:///user/adam/ant-1.8.2.jar",
    "hdfs:///user/adam/ant-launcher-1.8.2.jar",
    "hdfs:///user/adam/avro-1.7.6.jar",
    "hdfs:///user/adam/httpclient-4.3.2.jar",
    "hdfs:///user/adam/httpcore-4.3.1.jar",
    "hdfs:///user/adam/joda-convert-1.6.jar",
    "hdfs:///user/adam/xz-1.0.jar",
    "hdfs:///user/adam/bsh-2.0b4.jar",
    "hdfs:///user/adam/jackson-core-asl-1.9.13.jar",
    "hdfs:///user/adam/jackson-mapper-asl-1.9.13.jar",
    "hdfs:///user/adam/scalatra-common_2.10-2.3.0.jar",
    "hdfs:///user/adam/rl_2.10-0.4.10.jar",
    "hdfs:///user/adam/scalatra-json_2.10-2.3.0.jar",
    "hdfs:///user/adam/scalatra_2.10-2.3.0.jar",
    "hdfs:///user/adam/bdg-formats-0.4.0.jar",
    "hdfs:///user/adam/bdg-utils-parquet-0.1.1.jar",
    "hdfs:///user/adam/bdg-utils-misc-0.1.1.jar",
    "hdfs:///user/adam/bdg-utils-metrics-0.1.1.jar",
    "hdfs:///user/adam/adam-apis-0.16.0.jar",
    "hdfs:///user/adam/adam-core-0.16.0.jar",
    "hdfs:///user/adam/adam-cli-0.16.0.jar",
    "hdfs:///user/adam/testng-6.8.8.jar",
    "hdfs:///user/adam/json4s-ast_2.10-3.2.10.jar",
    "hdfs:///user/adam/json4s-core_2.10-3.2.10.jar",
    "hdfs:///user/adam/slf4j-api-1.7.5.jar",
    "hdfs:///user/adam/slf4j-log4j12-1.7.5.jar",
    "hdfs:///user/adam/asm-4.0.jar",
    "hdfs:///user/adam/scalap-2.10.0.jar",
    "hdfs:///user/adam/scala-library-2.10.4.jar",
    "hdfs:///user/adam/scala-reflect-2.10.4.jar",
    "hdfs:///user/adam/scala-compiler-2.10.0.jar",
    "hdfs:///user/adam/objenesis-1.2.jar",
    "hdfs:///user/adam/scalate-util_2.10-1.6.1.jar",
    "hdfs:///user/adam/scalate-core_2.10-1.6.1.jar",
    "hdfs:///user/adam/grizzled-slf4j_2.10-1.0.2.jar",
    "hdfs:///user/adam/cofoja-1.1-r150.jar",
    "hdfs:///user/adam/htsjdk-1.118.jar",
    "hdfs:///user/adam/hadoop-bam-7.0.0.jar",
    "hdfs:///user/adam/scalac-scoverage-plugin_2.10-0.99.2.jar",
    "hdfs:///user/adam/commons-logging-1.1.1.jar",
    "hdfs:///user/adam/log4j-1.2.17.jar",
    "hdfs:///user/adam/commons-cli-1.2.jar",
    "hdfs:///user/adam/joda-time-2.3.jar",
    "hdfs:///user/adam/commons-codec-1.4.jar",
    "hdfs:///user/adam/commons-io-1.3.2.jar",
    "hdfs:///user/adam/mime-util-2.1.3.jar",
    "hdfs:///user/adam/jakarta-regexp-1.4.jar",
    "hdfs:///user/adam/minlog-1.2.jar",
    "hdfs:///user/adam/reflectasm-1.07-shaded.jar",
    "hdfs:///user/adam/kryo-2.21.jar",
    "hdfs:///user/adam/servo-core-0.5.5.jar",
    "hdfs:///user/adam/paranamer-2.3.jar",
    "hdfs:///user/adam/guava-14.0.1.jar",
    "hdfs:///user/adam/annotations-2.0.0.jar",
    "hdfs:///user/adam/parquet-column-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-generator-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-hadoop-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-jackson-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-common-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-format-2.2.0-rc1.jar",
    "hdfs:///user/adam/parquet-avro-1.6.0rc4.jar",
    "hdfs:///user/adam/parquet-encoding-1.6.0rc4.jar",
    "hdfs:///user/adam/jcommander-1.27.jar",
    "hdfs:///user/adam/juniversalchardet-1.0.3.jar",
    "hdfs:///user/adam/commons-httpclient-3.1.jar",
    "hdfs:///user/adam/fastutil-6.4.4.jar"
  };

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

}
