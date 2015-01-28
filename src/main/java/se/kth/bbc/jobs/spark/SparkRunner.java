package se.kth.bbc.jobs.spark;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.lims.Constants;

/**
 *
 * @author stig
 */
public class SparkRunner {

  private static final Logger logger = Logger.getLogger(SparkRunner.class.
          getName());

  private Configuration conf;

  public void startJob() {
//    setConfiguration();
//    System.setProperty("SPARK_YARN_MODE", "true");
//    SparkConf scon = new SparkConf();
//    ClientArguments cargs = new ClientArguments(null);
//    new Client(conf, cargs).run();
  }

  private void setConfiguration() throws IllegalStateException {
    //Get the path to the Yarn configuration file from environment variables
    String yarnConfDir = System.getenv(Constants.ENV_KEY_YARN_CONF_DIR);
    //If not found in environment variables: warn and use default
    if (yarnConfDir == null) {
      logger.log(Level.WARNING,
              "Environment variable " + Constants.ENV_KEY_YARN_CONF_DIR
              + " not found, using default "
              + Constants.DEFAULT_YARN_CONF_DIR);
      yarnConfDir = Constants.DEFAULT_YARN_CONF_DIR;
    }

    //Get the configuration file at found path
    Path confPath = new Path(yarnConfDir);
    File confFile = new File(confPath + File.separator
            + Constants.DEFAULT_YARN_CONFFILE_NAME);
    if (!confFile.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate Yarn configuration file in {0}. Aborting exectution.",
              confFile);
      throw new IllegalStateException("No Yarn conf file");
    }

    //Also add the hadoop config
    String hadoopConfDir = System.getenv(Constants.ENV_KEY_HADOOP_CONF_DIR);
    //If not found in environment variables: warn and use default
    if (hadoopConfDir == null) {
      logger.log(Level.WARNING,
              "Environment variable " + Constants.ENV_KEY_HADOOP_CONF_DIR
              + " not found, using default "
              + Constants.DEFAULT_HADOOP_CONF_DIR);
      hadoopConfDir = Constants.DEFAULT_HADOOP_CONF_DIR;
    }
    confPath = new Path(hadoopConfDir);
    File hadoopConf = new File(confPath + File.separator
            + Constants.DEFAULT_HADOOP_CONFFILE_NAME);
    if (!hadoopConf.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate Hadoop configuration file in {0}. Aborting exectution.",
              hadoopConf);
      throw new IllegalStateException("No Hadoop conf file");
    }

    //And the hdfs config
    File hdfsConf = new File(confPath + File.separator
            + Constants.DEFAULT_HDFS_CONFFILE_NAME);
    if (!hdfsConf.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate HDFS configuration file in {0}. Aborting exectution.",
              hdfsConf);
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
}
