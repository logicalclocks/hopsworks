package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

public class JupyterConfig {

  private static final Logger LOGGER = Logger.getLogger(JupyterConfig.class.
      getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String JUPYTER_NOTEBOOK_CONFIG = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_KERNEL = "/kernel.json";
  private static final String JUPYTER_CUSTOM_JS = "/custom/custom.js";
  private static final String SPARKMAGIC_CONFIG = "/config.json";
  private static final int DELETE_RETRY = 10;

  public static JupyterConfig COMMON_CONF;

  /**
   * A configuration that is common for all projects.
   */
  private final Settings settings;
  private final Project project;
  private final String hdfsUser;
  private final String projectUserPath;
  private final String notebookPath;
  private final String confDirPath;
  private final String runDirPath;
  private final String logDirPath;
  private final int port;
  private long pid;
  private String secret;
  private String token;
  private String nameNodeEndpoint;

  JupyterConfig(Project project, String secretConfig, String hdfsUser,
      String nameNodeEndpoint, Settings settings, int port, String token,
      JupyterSettings js)
      throws AppException {
    this.project = project;
    this.hdfsUser = hdfsUser;
    this.nameNodeEndpoint = nameNodeEndpoint;
    boolean newDir = false;
    boolean newFile = false;
    this.settings = settings;
    this.port = port;
    projectUserPath = settings.getJupyterDir() + File.separator
        + Settings.DIR_ROOT + File.separator + this.project.getName()
        + File.separator + hdfsUser;
    notebookPath = projectUserPath + File.separator + secretConfig;
    confDirPath = notebookPath + File.separator + "conf";
    logDirPath = notebookPath + File.separator + "logs";
    runDirPath = notebookPath + File.separator + "run";
    this.token = token;
    try {
      newDir = createJupyterDirs();
      createConfigFiles(nameNodeEndpoint, port, js);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectUserDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGER.log(Level.SEVERE,
          "Error in initializing JupyterConfig for project: {0}. {1}",
          new Object[]{this.project.getName(), e});
      throw new AppException(
          Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "Could not configure Jupyter. Report a bug.");

    }
  }

  public String getProjectUserPath() {
    return projectUserPath;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
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

  }

  public void clean() {
    cleanAndRemoveConfDirs();
  }

  public String getProjectName() {
    return project.getName();
  }

  public String getConfDirPath() {
    return confDirPath;
  }

  public String getRunDirPath() {
    return runDirPath;
  }

  public String getLogDirPath() {
    return logDirPath;
  }

  //returns true if the project dir was created 
  private boolean createJupyterDirs() throws IOException {
    File projectDir = new File(projectUserPath);
    projectDir.mkdirs();
    File baseDir = new File(notebookPath);
    baseDir.mkdirs();
    // Set owner persmissions
    Set<PosixFilePermission> xOnly = new HashSet<>();
    xOnly.add(PosixFilePermission.OWNER_WRITE);
    xOnly.add(PosixFilePermission.OWNER_READ);
    xOnly.add(PosixFilePermission.OWNER_EXECUTE);
    xOnly.add(PosixFilePermission.GROUP_WRITE);
    xOnly.add(PosixFilePermission.GROUP_EXECUTE);

    Set<PosixFilePermission> perms = new HashSet<>();
    //add owners permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_WRITE);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    perms.add(PosixFilePermission.OTHERS_READ);
//    perms.add(PosixFilePermission.OTHERS_WRITE);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);

    Files.setPosixFilePermissions(Paths.get(notebookPath), perms);
    Files.setPosixFilePermissions(Paths.get(projectUserPath), xOnly);

    new File(confDirPath + "/custom").mkdirs();
    new File(runDirPath).mkdirs();
    new File(logDirPath).mkdirs();
    return true;
  }

  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String nameNodeEndpoint, Integer port,
      JupyterSettings js)
      throws
      IOException {
    File jupyter_config_file = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    File jupyter_kernel_file = new File(confDirPath + JUPYTER_CUSTOM_KERNEL);
    File sparkmagic_config_file = new File(confDirPath + SPARKMAGIC_CONFIG);
    File custom_js = new File(confDirPath + JUPYTER_CUSTOM_JS);
    File log4j_file = new File(confDirPath + LOG4J_PROPS);
    boolean createdJupyter = false;
    boolean createdKernel = false;
    boolean createdSparkmagic = false;
    boolean createdCustomJs = false;

    if (!jupyter_config_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String[] nn = nameNodeEndpoint.split(":");
      String nameNodeIp = nn[0];
      String nameNodePort = nn[1];

      String pythonKernel = "";

      if (settings.isPythonKernelEnabled() && project.getPythonVersion().contains("X") == false) {
        pythonKernel = ", 'python-" + hdfsUser + "'";
        StringBuilder jupyter_kernel_config = ConfigFileGenerator.
            instantiateFromTemplate(
                ConfigFileGenerator.JUPYTER_CUSTOM_KERNEL,
                "hdfs_user", this.hdfsUser,
                "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
                "hadoop_version", this.settings.getHadoopVersion(),
                "anaconda_home", this.settings.getAnacondaProjectDir(this.project.getName()),
                "secret_dir", this.settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret()
            );
        createdKernel = ConfigFileGenerator.createConfigFile(jupyter_kernel_file, jupyter_kernel_config.toString());
      }

      StringBuilder jupyter_notebook_config = ConfigFileGenerator.
          instantiateFromTemplate(
              ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
              "project", this.project.getName(),
              "namenode_ip", nameNodeIp,
              "namenode_port", nameNodePort,
              "hopsworks_ip", settings.getHopsworksIp(),
              "base_dir", js.getBaseDir(),
              "hdfs_user", this.hdfsUser,
              "port", port.toString(),
              "python-kernel", pythonKernel,
              "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
              "hdfs_home", this.settings.getHadoopSymbolicLinkDir(),
              "secret_dir", this.settings.getStagingDir()
              + Settings.PRIVATE_DIRS + js.getSecret()
          );
      createdJupyter = ConfigFileGenerator.createConfigFile(jupyter_config_file,
          jupyter_notebook_config.toString());
    }
    if (!sparkmagic_config_file.exists()) {

      // TODO: Add this local file to 'spark: file' to copy it to hdfs and localize it.
      StringBuilder log4j_sb
          = ConfigFileGenerator.instantiateFromTemplate(
              ConfigFileGenerator.LOG4J_TEMPLATE_JUPYTER,
              "logstash_ip", settings.getLogstashIp(),
              "logstash_port", settings.getLogstashPort().toString(),
              "log_level", js.getLogLevel().toUpperCase()
          );
      ConfigFileGenerator.createConfigFile(log4j_file, log4j_sb.toString());

      StringBuilder sparkFiles = new StringBuilder();
      sparkFiles
          // Keystore
          .append("\"hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
          .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
          .append("__kstore.jks#").append(Settings.K_CERTIFICATE).append("\",")
          // TrustStore
          .append("\"hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
          .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
          .append("__tstore.jks#").append(Settings.T_CERTIFICATE).append("\",")
          .append("\""+Settings.getSparkLog4JPath(settings.getSparkUser()) + "\"");
      
      // If RPC TLS is enabled, password file would be injected by the
      // NodeManagers. We don't need to add it as LocalResource
      if (!settings.getHopsRpcTls()) {
        sparkFiles
            .append(",")
            // File with crypto material password
            .append("\"hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
            .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
            .append("__cert.key#").append(Settings.CRYPTO_MATERIAL_PASSWORD)
            .append("\"");
      }
      
      if(!js.getFiles().equals("")) {
        sparkFiles.append("," + js.getFiles());
      }

      String sparkProps = js.getSparkParams();
      if (sparkProps != null && !sparkProps.isEmpty()) {
        String lines[] = sparkProps.split("\\r?\\n");
        StringBuffer sb = new StringBuffer();
        for (String l : lines) {
          // Trim white-spaces on the left and the right of each line
          String leftRemoved = l.replaceAll("^\\s+", "");
          String trimmedLine = leftRemoved.replaceAll("\\s+$", "");
          String[] props = trimmedLine.split(" +");
          for (int x = 0; x < props.length; x++) {
            if (x == 0) {
              sb.append("\"").append(props[x]).append("\": ");
            } else {
              sb.append("\"").append(props[x]).append("\",").append(System.lineSeparator());
              x = props.length; // ignore any more properties on the same line
            }
          }
        }
        sparkProps = sb.toString();
      }
      LOGGER.info("SparkProps are: " + System.lineSeparator() + sparkProps);

      boolean isTensorFlow = js.getMode().compareToIgnoreCase("tensorflow") == 0;
      boolean isTensorFlowOnSpark = js.getMode().compareToIgnoreCase("distributedtensorflow") == 0;
      boolean isHorovod = js.getMode().compareToIgnoreCase("horovod") == 0;
      boolean isSparkDynamic = js.getMode().compareToIgnoreCase("sparkDynamic") == 0;
      String extraJavaOptions = "-D" + Settings.LOGSTASH_JOB_INFO + "=" + project.getName().toLowerCase()
          + ",jupyter,notebook,?";
      StringBuilder sparkmagic_sb
          = ConfigFileGenerator.
              instantiateFromTemplate(
                  ConfigFileGenerator.SPARKMAGIC_CONFIG_TEMPLATE,
                  "spark_params", sparkProps,
                  "livy_ip", settings.getLivyIp(),
                  "hdfs_user", this.hdfsUser,
                  "driver_cores", (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "1" :
                              Integer.toString(js.getAppmasterCores()),
                  "driver_memory", Integer.toString(js.getAppmasterMemory()) + "m",
                  "num_executors", (isHorovod) ? "1":
                                   (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()):
                                   (isSparkDynamic) ? Integer.toString(js.getDynamicMinExecutors()):
                                   Integer.toString(js.getNumExecutors()),
                  "executor_cores", (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "1" :
                              Integer.toString(js.getNumExecutorCores()),
                  "executor_memory", Integer.toString(js.getExecutorMemory()) + "m",
                  "dynamic_executors", Boolean.toString(isSparkDynamic || isTensorFlow || isTensorFlowOnSpark ||
                              isHorovod),
                  "min_executors", (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "0" :
                                   Integer.toString(js.getDynamicMinExecutors()),
                  "initial_executors", (isTensorFlow) ? "0" :
                                   (isHorovod) ? "1" :
                                   (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()):
                                   Integer.toString(js.getDynamicMinExecutors()),
                  "max_executors", (isTensorFlow) ? Integer.toString(js.getNumExecutors()):
                                   (isHorovod) ? "1" :
                                   (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()):
                                   Integer.toString(js.getDynamicMaxExecutors()),
                  "archives", js.getArchives(),
                  "jars", js.getJars(),
                  "files", sparkFiles.toString(),
                  "pyFiles", js.getPyFiles(),
                  "yarn_queue", "default",
                  "num_ps", (isTensorFlowOnSpark) ? Integer.toString(js.getNumTfPs()) : "0",
                  "num_gpus", (isTensorFlow || isTensorFlowOnSpark) ? Integer.toString(js.getNumTfGpus()):
                              (isHorovod) ? Integer.toString(js.getNumMpiNp()*js.getNumTfGpus()): "0",
                  "mpi_np", (isHorovod) ? Integer.toString(js.getNumMpiNp()) : "",
                  "tensorflow", Boolean.toString(isTensorFlow || isTensorFlowOnSpark || isHorovod),
                  "jupyter_home", this.confDirPath,
                  "project", this.project.getName(),
                  "mode", js.getMode(),
                  "nn_endpoint", this.nameNodeEndpoint,
                  "spark_user", this.settings.getSparkUser(),
                  "java_home", this.settings.getJavaHome(),
                  "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
                  "hadoop_version", this.settings.getHadoopVersion(),
                  "pyspark_bin", this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python",
                  "anaconda_dir", this.settings.getAnacondaDir(),
                  "cuda_dir", this.settings.getCudaDir(),
                  "anaconda_env", this.settings.getAnacondaProjectDir(project.getName()),
                  "sparkhistoryserver_ip", this.settings.getSparkHistoryServerIp(),
                  "metrics_path", settings.getSparkMetricsPath(),
                  "exec_timeout", (isTensorFlowOnSpark) ?
                                  Integer.toString(((js.getNumExecutors() + js.getNumTfPs()) * 15) + 60 ) + "s":
                                  "60s",
                  "extra_java_options", extraJavaOptions
              );
      createdSparkmagic = ConfigFileGenerator.createConfigFile(
          sparkmagic_config_file,
          sparkmagic_sb.toString());
    }
    if (!custom_js.exists()) {

      StringBuilder custom_js_sb = ConfigFileGenerator.
          instantiateFromTemplate(
              ConfigFileGenerator.JUPYTER_CUSTOM_TEMPLATE,
              "hadoop_home", this.settings.getHadoopSymbolicLinkDir()
          );
      createdCustomJs = ConfigFileGenerator.createConfigFile(
          custom_js, custom_js_sb.toString());
    }

    // Add this local file to 'spark: file' to copy it to hdfs and localize it.
    return createdJupyter || createdSparkmagic || createdCustomJs;
  }

  /**
   * Closes all resources and deletes project dir
   * /srv/zeppelin/Projects/this.projectName recursive.
   *
   * @return true if the dir is deleted
   */
  public boolean cleanAndRemoveConfDirs() {
    return removeProjectUserDirRecursive();
  }

  private boolean removeProjectUserDirRecursive() {
    File projectUserDir = new File(projectUserPath);
    if (!projectUserDir.exists()) {
      return true;
    }
    boolean ret = false;
    try {
      ret = ConfigFileGenerator.deleteRecursive(projectUserDir);
    } catch (FileNotFoundException ex) {
      // do nothing
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

  public String getNotebookPath() {
    return notebookPath;
  }

}
