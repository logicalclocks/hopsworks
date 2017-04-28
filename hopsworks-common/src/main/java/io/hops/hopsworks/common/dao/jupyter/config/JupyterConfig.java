package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

public class JupyterConfig {

  private static final Logger LOGGGER = Logger.getLogger(JupyterConfig.class.
          getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_JS = "/custom/custom.js";
  private static final String SPARKMAGIC_CONFIG = "/config.json";
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
  private final int port;
  private long pid;
  private String token;

  JupyterConfig(String projectName, String hdfsUser, String nameNodeHostname,
          Settings settings, int port) throws AppException {
    this.projectName = projectName;
    this.hdfsUser = hdfsUser;
    boolean newDir = false;
    boolean newFile = false;
    this.settings = settings;
    this.port = port;
    projectUserDirPath = settings.getJupyterDir() + File.separator
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
      createConfigFiles(nameNodeHostname, port);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGGER.log(Level.SEVERE,
              "Error in initializing JupyterConfig for project: {0}. {1}",
              new Object[]{this.projectName, e});
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not configure Jupyter. Report a bug.");

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
    this.port = jConf.getPort();
    this.pid = jConf.getPid();
    this.token = jConf.getToken();
  }

  public String getHdfsUser() {
    return hdfsUser;
  }

  public Settings getSettings() {
    return settings;
  }

  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }

  public int getPort() {
    return port;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
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
    new File(confDirPath + "/custom").mkdirs();
    new File(notebookDirPath).mkdirs();
    new File(runDirPath).mkdirs();
    new File(binDirPath).mkdirs();
    new File(logDirPath).mkdirs();
    return newProjectDir;
  }

  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String nameNodeHostname, Integer port)
          throws
          IOException {
    File jupyter_config_file = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    File sparkmagic_config_file = new File(confDirPath + SPARKMAGIC_CONFIG);
    File custom_js = new File(confDirPath + JUPYTER_CUSTOM_JS);
    boolean createdJupyter = false;
    boolean createdSparkmagic = false;
    boolean createdCustomJs = false;

    if (!jupyter_config_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String[] nn = nameNodeHostname.split(":");
      String nameNodeIp = nn[0];
      String nameNodePort = nn[1];

      StringBuilder jupyter_notebook_config = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
                      "project", this.projectName,
                      "namenode_ip", nameNodeIp,
                      "namenode_port", nameNodePort,
                      "hopsworks_ip", settings.getHopsworksIp(),
                      "hdfs_user", this.hdfsUser,
                      "port", port.toString(),
                      "hadoop_home", this.settings.getHadoopDir(),
                      "hdfs_home", this.settings.getHadoopDir()
              );
      createdJupyter = ConfigFileGenerator.createConfigFile(jupyter_config_file,
              jupyter_notebook_config.toString());
    }
    if (!sparkmagic_config_file.exists()) {

      StringBuilder sparkmagic_sb = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.SPARKMAGIC_CONFIG_TEMPLATE,
                      "livy_ip", settings.getLivyIp(),
                      "hdfs_user", this.hdfsUser,
                      "jupyter_home", confDirPath,
                      "hadoop_home", this.settings.getHadoopDir()
              );
      createdSparkmagic = ConfigFileGenerator.createConfigFile(
              sparkmagic_config_file,
              sparkmagic_sb.toString());
    }
    if (!custom_js.exists()) {

      StringBuilder custom_js_sb = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.JUPYTER_CUSTOM_TEMPLATE,
                      "hadoop_home", this.settings.getHadoopDir()
              );
      createdCustomJs = ConfigFileGenerator.createConfigFile(
              custom_js, custom_js_sb.toString());
    }

    return createdJupyter || createdSparkmagic || createdCustomJs;
  }

  /**
   * Closes all resources and deletes project dir
   * /srv/zeppelin/Projects/this.projectName recursive.
   *
   * @return true if the dir is deleted
   */
  public boolean cleanAndRemoveConfDirs() {
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
//    File jupyter_js_file = new File(confDirPath + JUPYTER_CUSTOM_JS);
    File jupyter_config_file
            = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    boolean ret = false;
//    if (jupyter_js_file.exists()) {
//      ret = jupyter_js_file.delete();
//    }
    if (jupyter_config_file.exists()) {
      ret = jupyter_config_file.delete();
    }
    return ret;
  }

  public String getProjectUserDirPath() {
    return projectUserDirPath;
  }

}
