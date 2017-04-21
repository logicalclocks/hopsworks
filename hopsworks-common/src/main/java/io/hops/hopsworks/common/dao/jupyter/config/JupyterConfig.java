package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JupyterConfig {

  private static final Logger LOGGGER = Logger.getLogger(JupyterConfig.class.
          getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_JS = "/custom.js";
  private static final int DELETE_RETRY = 10;

  public static JupyterConfig COMMON_CONF;

  /**
   * A configuration that is common for all projects.
   */
  private final Settings settings;
  private final String projectName;
  private final String hdfsUser;
  private final String projectUserDirPath;
  private final String confDirPath;
  private final String notebookDirPath;
  private final String runDirPath;
  private final String binDirPath;
  private final String logDirPath;
  private final String libDirPath;

  JupyterConfig(String projectName, String hdfsUser, String nameNodeIp,
          Settings settings) {
    this.projectName = projectName;
    this.hdfsUser = hdfsUser;
    boolean newDir = false;
    boolean newFile = false;
    this.settings = settings;
    // settings.getJupyterProjectsDir()
    projectUserDirPath = settings.getJupyterProjectsDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + hdfsUser;
    confDirPath = projectUserDirPath + File.separator + "conf";
    notebookDirPath = projectUserDirPath + File.separator + "notebooks";
    runDirPath = projectUserDirPath + File.separator + "run";
    binDirPath = projectUserDirPath + File.separator + "bin";
    logDirPath = projectUserDirPath + File.separator + "logs";
    libDirPath = projectUserDirPath + File.separator + "lib";
    try {
      newDir = createJupyterDirs();//creates the necessary folders for the project in /srv/zeppelin
      createConfigFiles(nameNodeIp);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGGER.log(Level.SEVERE,
              "Error in initializing JupyterConfig for project: {0}. {1}",
              new Object[]{this.projectName, e});
    }
  }

  public JupyterConfig(JupyterConfig jConf) {
    this.projectName = jConf.getProjectName();
    this.settings = jConf.getSettings();
    this.hdfsUser = jConf.getHdfsUser();
    this.projectUserDirPath = jConf.getProjectDirPath();
    this.confDirPath = jConf.getConfDirPath();
    this.notebookDirPath = jConf.getNotebookDirPath();
    this.runDirPath = jConf.getRunDirPath();
    this.binDirPath = jConf.getBinDirPath();
    this.logDirPath = jConf.getLogDirPath();
    this.libDirPath = jConf.getLibDirPath();
  }

  public String getHdfsUser() {
    return hdfsUser;
  }

  public Settings getSettings() {
    return settings;
  }

  /**
   * No synchronization here, as one slow notebook server could kill all clients
   *
   * @param hdfsUsername
   * @param port
   * @return
   * @throws IOException
   */
  public StringBuffer getConsoleOutput(String hdfsUsername, int port) throws
          IOException {

    // Read the whole log file and pass it as a StringBuffer. 
    String fname = this.getLogDirPath() + "/" + hdfsUsername + "-" + port
            + ".log";
    BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(fname), Charset.forName("UTF8")));
    String line;
    StringBuffer sb = new StringBuffer();
    while (((line = br.readLine()) != null)) {
      sb.append(line);
    }
    return sb;

//    StringBuilder sb = new StringBuilder();
//    BufferedReader br = consoleOutput.get(hdfsUsername);
//    if (br != null) {
//      // This could block if jupyter doesn't output a complete line, but blocks while
//      // waiting for the line terminating character
//      while (br.ready())  {
//        sb.append(br.readLine()).append("\n");
//      }
//    }
//    return sb;
  }

  /**
   * This only works on Linux systems. From Java 9, you can just call
   * p.getPid();
   * http://stackoverflow.com/questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
   *
   * @param p
   * @return
   */
  public static synchronized long getPidOfProcess(Process p) {
    long pid = -1;

    try {
      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getLong(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      pid = -1;
    }
    return pid;
  }

  public void clean() {
    cleanAndRemoveConfDirs();

    // TODO: jim
    // kill all the processes
  }

  public String getProjectName() {
    return projectName;
  }

  public String getProjectDirPath() {
    return projectUserDirPath;
  }

  public String getLibDirPath() {
    return libDirPath;
  }

  public String getConfDirPath() {
    return confDirPath;
  }

  public String getNotebookDirPath() {
    return notebookDirPath;
  }

  public String getRunDirPath() {
    return runDirPath;
  }

  public String getBinDirPath() {
    return binDirPath;
  }

  public String getLogDirPath() {
    return logDirPath;
  }

  //returns true if the project dir was created 
  private boolean createJupyterDirs() {
    File projectDir = new File(projectUserDirPath);
    boolean newProjectDir = projectDir.mkdirs();
    new File(confDirPath).mkdirs();
    new File(notebookDirPath).mkdirs();
    new File(runDirPath).mkdirs();
    new File(binDirPath).mkdirs();
    new File(logDirPath).mkdirs();
    return newProjectDir;
  }

//  //creates symlink to interpreters and libs
//  private void createSymLinks() throws IOException {
//    File target = new File(settings.getJupyterDir() + File.separator
//            + "interpreter");
//    target = new File(settings.getJupyterDir() + File.separator + "lib");
//    File newLink = new File(libDirPath);
//    if (!newLink.exists()) {
//      Files.createSymbolicLink(newLink.toPath(), target.toPath());
//    }
//  }
  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String nameNodeIp) throws
          IOException {
    File jupyter_custom_js_file = new File(confDirPath + JUPYTER_CUSTOM_JS);
    boolean createdSh = false;
    boolean createdXml = false;
    if (!jupyter_custom_js_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String javaHome = Settings.JAVA_HOME;
      if (System.getenv().containsKey("JAVA_HOME")) {
        javaHome = System.getenv("JAVA_HOME");
      }

      StringBuilder jupyter_notebook_config = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
                      "project", this.projectName,
                      "namenode_ip", nameNodeIp,
                      "hopsworks_ip", settings.getHopsworksIp(),
                      "hdfs_user", this.hdfsUser,
                      "hdfs_home", this.settings.getHadoopDir()
              );
      createdSh = ConfigFileGenerator.createConfigFile(jupyter_custom_js_file,
              jupyter_notebook_config.
              toString());
    }

//    if (!jupyter_notebook_config_file.exists()) {
//      StringBuilder zeppelin_site_xml = ConfigFileGenerator.
//              instantiateFromTemplate(
//                      ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
//                      "zeppelin_home", home,
//                      "livy_url", settings.getLivyUrl(),
//                      "livy_master", settings.getLivyYarnMode(),
//                      "zeppelin_home_dir", home,
//                      "zeppelin_notebook_dir", notebookDir);
//      createdXml = ConfigFileGenerator.createConfigFile(jupyter_notebook_config_file,
//              zeppelin_site_xml.
//              toString());
//    }
//
//
//    String jobName = this.projectName.toLowerCase() + "-zeppelin";
//    String logstashID = "-D" + Settings.LOGSTASH_JOB_INFO + "="
//            + this.projectName.toLowerCase() + "," + jobName + "," + jobName;
//    String extraSparkJavaOptions = " -Dlog4j.configuration=./log4j.properties " + logstashID;
//    String hdfsResourceDir = "hdfs://" + resourceDir + File.separator;
//    if (interpreterConf == null) {
//      StringBuilder interpreter_json = ConfigFileGenerator.
//              instantiateFromTemplate(
//                      ConfigFileGenerator.INTERPRETER_TEMPLATE,
//                      "projectName", this.projectName,
//                      "zeppelin_home_dir", home,
//                      "livy_url", settings.getLivyUrl(),
//                      "metrics-properties_local_path", "./metrics.properties",
//                      "metrics-properties_path", metricsPath + "," + log4jPath,
//                      "extra_spark_java_options", extraSparkJavaOptions,
//                      "spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
//                      "spark.yarn.stagingDir", hdfsResourceDir,
//                      "livy.spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
//                      "livy.spark.yarn.stagingDir", hdfsResourceDir
//              );
//      interpreterConf = interpreter_json.toString();
//    }
    return createdSh || createdXml;
  }

//  // loads configeration from project specific zeppelin-site.xml
//  private ZeppelinConfiguration loadConfig() {
//    URL url = null;
//    File zeppelinConfig = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
//    try {
//      url = zeppelinConfig.toURI().toURL();
//      LOGGGER.log(Level.INFO, "Load configuration from {0}", url);
//      conf = new ZeppelinConfiguration(url);
//    } catch (ConfigurationException e) {
//      LOGGGER.log(Level.INFO, "Failed to load configuration from " + url
//              + " proceeding with a default", e);
//      conf = new ZeppelinConfiguration();
//    } catch (MalformedURLException ex) {
//      LOGGGER.log(Level.INFO, "Malformed URL failed to load configuration from "
//              + url
//              + " proceeding with a default", ex);
//      conf = new ZeppelinConfiguration();
//    }
//    return conf;
//  }
  /**
   * Closes all resources and deletes project dir
   * /srv/zeppelin/Projects/this.projectName recursive.
   *
   * @return true if the dir is deleted
   */
  public boolean cleanAndRemoveConfDirs() {
//    clean();
    return removeProjectDirRecursive();
  }

  private boolean removeProjectDirRecursive() {
    File projectDir = new File(projectUserDirPath);
    if (!projectDir.exists()) {
      return true;
    }
    boolean ret = false;
    File lib = new File(libDirPath);
    //symlinks must be deleted before we recursive delete the project dir.
    int retry = 0;
    while (lib.exists()) {
      if (lib.exists()) {
        lib.delete();
      }
      retry++;
      if (retry > DELETE_RETRY) {
        LOGGGER.log(Level.SEVERE, "Could not delete zeppelin project folder.");
        return false;
      }
    }
    try {
      ret = ConfigFileGenerator.deleteRecursive(projectDir);
    } catch (FileNotFoundException ex) {
    }
    return ret;
  }

  private boolean removeProjectConfFiles() {
    File jupyter_js_file = new File(confDirPath + JUPYTER_CUSTOM_JS);
    File zeppelin_site_xml_file
            = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    boolean ret = false;
    if (jupyter_js_file.exists()) {
      ret = jupyter_js_file.delete();
    }
    if (zeppelin_site_xml_file.exists()) {
      ret = zeppelin_site_xml_file.delete();
    }
    return ret;
  }

}
