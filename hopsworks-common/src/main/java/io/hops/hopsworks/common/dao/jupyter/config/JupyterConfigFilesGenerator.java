/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.templates.ConfigProperty;

import javax.ws.rs.core.Response;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to generate the Configuration Files for a Jupyter Notebook Server
 */
public class JupyterConfigFilesGenerator {

  private static final Logger LOGGER = Logger.getLogger(JupyterConfigFilesGenerator.class.
      getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String JUPYTER_NOTEBOOK_CONFIG = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_KERNEL = "/kernel.json";
  private static final String JUPYTER_CUSTOM_JS = "/custom/custom.js";
  private static final String SPARKMAGIC_CONFIG = "/config.json";
  private static final int DELETE_RETRY = 10;
  

  public static JupyterConfigFilesGenerator COMMON_CONF;

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
  private final String certificatesDir;
  private long pid;
  private String secret;
  private String token;
  private String nameNodeEndpoint;

  JupyterConfigFilesGenerator(Project project, String secretConfig, String hdfsUser,
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
    certificatesDir = notebookPath + File.separator + "certificates";
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
      if (e instanceof IllegalArgumentException) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Could not configure Jupyter, " + e.getMessage());
      }
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
    new File(certificatesDir).mkdirs();
    return true;
  }

  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String nameNodeEndpoint, Integer port, JupyterSettings js) throws IOException {
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
              "umask", js.getUmask(),
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
          .append("\"").append(settings.getSparkLog4JPath()).append("\",")
          // Glassfish domain truststore
          .append("\"").append(settings.getGlassfishTrustStoreHdfs()).append("#").append(Settings.DOMAIN_CA_TRUSTSTORE)
          .append("\",")
          // Add HopsUtil
          .append("\"").append(settings.getHopsUtilHdfsPath()).append("\"");

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

      if (!js.getFiles().equals("")) {
        sparkFiles.append("," + js.getFiles());
      }

      String sparkProps = js.getSparkParams();
      
      // Spark properties user has defined in the jupyter dashboard
      Map<String, String> userSparkProperties = HopsUtils.validateUserProperties(sparkProps, settings.getSparkDir());
      
      LOGGER.info("SparkProps are: " + System.lineSeparator() + sparkProps);

      boolean isTensorFlow = js.getMode().compareToIgnoreCase("tensorflow") == 0;
      boolean isTensorFlowOnSpark = js.getMode().compareToIgnoreCase("distributedtensorflow") == 0;
      boolean isHorovod = js.getMode().compareToIgnoreCase("horovod") == 0;
      boolean isSparkDynamic = js.getMode().compareToIgnoreCase("sparkDynamic") == 0;
      String extraJavaOptions = "-D" + Settings.LOGSTASH_JOB_INFO + "=" + project.getName().toLowerCase()
          + ",jupyter,notebook,?"
          + " -D" + Settings.HOPSWORKS_JOBTYPE_PROPERTY + "=" + JobType.SPARK
          + " -D" + Settings.KAFKA_BROKERADDR_PROPERTY + "=" + settings.getKafkaBrokersStr()
          + " -D" + Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY + "=" + settings.getRestEndpoint()
          + " -D" + Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY + "=" + settings.getElasticRESTEndpoint()
          + " -D" + Settings.HOPSWORKS_PROJECTID_PROPERTY + "=" + project.getId()
          + " -D" + Settings.HOPSWORKS_PROJECTNAME_PROPERTY + "=" + project.getName()
          + " -Dlog4j.configuration=./log4j.properties";
          
      String extraClassPath = settings.getHopsLeaderElectionJarPath()
          + File.pathSeparator
          +  settings.getHopsUtilFilename();
  
      // Map of default/system Spark(Magic) properties <Property_Name, ConfigProperty>
      // Property_Name should be either the SparkMagic property name or Spark property name
      // The replacement pattern is defined in ConfigProperty
      Map<String, ConfigProperty> sparkMagicParams = new HashMap<>();
      sparkMagicParams.put("livy_ip", new ConfigProperty("livy_ip", HopsUtils.IGNORE, settings.getLivyIp()));
      sparkMagicParams.put("jupyter_home", new ConfigProperty("jupyter_home", HopsUtils.IGNORE, this.confDirPath));
      sparkMagicParams.put("driverCores", new ConfigProperty("driver_cores", HopsUtils.IGNORE,
          (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "1" :
              Integer.toString(js.getAppmasterCores())));
      sparkMagicParams.put("driverMemory", new ConfigProperty("driver_memory", HopsUtils.IGNORE,
          Integer.toString(js.getAppmasterMemory()) + "m"));
      sparkMagicParams.put("numExecutors", new ConfigProperty("num_executors", HopsUtils.IGNORE,
          (isHorovod) ? "1":
              (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()):
                  (isSparkDynamic) ? Integer.toString(js.getDynamicMinExecutors()):
                      Integer.toString(js.getNumExecutors())));
      sparkMagicParams.put("executorCores", new ConfigProperty("executor_cores", HopsUtils.IGNORE,
          (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "1" :
              Integer.toString(js.getNumExecutorCores())));
      sparkMagicParams.put("executorMemory", new ConfigProperty("executor_memory", HopsUtils.IGNORE,
          Integer.toString(js.getExecutorMemory()) + "m"));
      sparkMagicParams.put("proxyUser", new ConfigProperty("hdfs_user", HopsUtils.IGNORE, this.hdfsUser));
      sparkMagicParams.put("name", new ConfigProperty("spark_magic_name", HopsUtils.IGNORE,
          "remotesparkmagics-jupyter-" + js.getMode()));
      sparkMagicParams.put("queue", new ConfigProperty("yarn_queue", HopsUtils.IGNORE, "default"));
      sparkMagicParams.put("archives", new ConfigProperty("archives", HopsUtils.IGNORE, js.getArchives()));
      sparkMagicParams.put("jars", new ConfigProperty("jars", HopsUtils.IGNORE, js.getJars()));
      sparkMagicParams.put("pyFiles", new ConfigProperty("pyFiles", HopsUtils.IGNORE, js.getPyFiles()));
      sparkMagicParams.put("files", new ConfigProperty("files", HopsUtils.IGNORE, sparkFiles.toString()));
      
      // Spark properties
      sparkMagicParams.put("spark.executorEnv.PATH", new ConfigProperty("spark_executorEnv_PATH",
          HopsUtils.APPEND, this.settings.getAnacondaProjectDir(project.getName())
          + "/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK_DRIVER_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK3_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.LD_LIBRARY_PATH", new ConfigProperty(
          "spark_yarn_appMaster_LD_LIBRARY_PATH", HopsUtils.APPEND,
          this.settings.getJavaHome() + "/jre/lib/amd64/server:" + this.settings.getCudaDir()
              + "/lib64:" + this.settings.getHadoopSymbolicLinkDir() + "/lib/native"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_HOME", new ConfigProperty("hadoop_home",
          HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.LIBHDFS_OPTS", new ConfigProperty(
          "spark_yarn_appMasterEnv_LIBHDFS_OPTS", HopsUtils.APPEND,
          "-Xmx96m -Dlog4j.configuration=" + this.settings.getHadoopSymbolicLinkDir()
              +"/etc/hadoop/log4j.properties -Dhadoop.root.logger=ERROR,RFA"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_HDFS_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_VERSION", new ConfigProperty(
          "hadoop_version", HopsUtils.IGNORE, this.settings.getHadoopVersion()));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_USER_NAME", new ConfigProperty(
          "hdfs_user", HopsUtils.IGNORE, this.hdfsUser));

      sparkMagicParams.put("spark.yarn.appMasterEnv.REST_ENDPOINT", new ConfigProperty(
              "rest_endpoint", HopsUtils.IGNORE, settings.getRestEndpoint()));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.HDFS_BASE_DIR", new ConfigProperty(
          "spark_yarn_appMasterEnv_HDFS_BASE_DIR", HopsUtils.IGNORE,
          "hdfs://Projects/" + this.project.getName() + js.getBaseDir()));
      
      sparkMagicParams.put("spark.yarn.stagingDir", new ConfigProperty(
          "spark_yarn_stagingDir", HopsUtils.IGNORE,
          "hdfs:///Projects/" + this.project.getName() + "/Resources"));
      
      sparkMagicParams.put("spark.driver.extraLibraryPath", new ConfigProperty(
          "spark_driver_extraLibraryPath", HopsUtils.APPEND,
          this.getSettings().getCudaDir() + "/lib64"));
      
      sparkMagicParams.put("spark.driver.extraJavaOptions", new ConfigProperty(
          "spark_driver_extraJavaOptions", HopsUtils.APPEND, extraJavaOptions));
      
      sparkMagicParams.put("spark.driver.extraClassPath", new ConfigProperty(
          "spark_driver_extraClassPath", HopsUtils.APPEND, extraClassPath));
  
      sparkMagicParams.put("spark.executor.extraClassPath", new ConfigProperty(
          "spark_executor_extraClassPath", HopsUtils.APPEND, extraClassPath));
      
      sparkMagicParams.put("spark.executorEnv.MPI_NP", new ConfigProperty(
          "spark_executorEnv_MPI_NP", HopsUtils.IGNORE, (isHorovod) ? Integer.toString(js.getNumMpiNp()) : ""));

      sparkMagicParams.put("spark.executorEnv.REST_ENDPOINT", new ConfigProperty(
              "rest_endpoint", HopsUtils.IGNORE, settings.getRestEndpoint()));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_USER_NAME", new ConfigProperty(
          "hdfs_user", HopsUtils.IGNORE, this.hdfsUser));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put("spark.executorEnv.LIBHDFS_OPTS", new ConfigProperty(
          "spark_executorEnv_LIBHDFS_OPTS", HopsUtils.APPEND,
          "-Xmx96m -Dlog4j.configuration=" + this.settings.getHadoopSymbolicLinkDir() +
              "/etc/hadoop/log4j.properties -Dhadoop.root.logger=ERROR,RFA"));
      
      sparkMagicParams.put("spark.executorEnv.PYSPARK_PYTHON", new ConfigProperty(
          "pyspark_bin", HopsUtils.IGNORE,
          this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.executorEnv.PYSPARK3_PYTHON", new ConfigProperty(
          "pyspark_bin", HopsUtils.IGNORE,
          this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
      
      sparkMagicParams.put("spark.executorEnv.LD_LIBRARY_PATH", new ConfigProperty(
          "spark_executorEnv_LD_LIBRARY_PATH", HopsUtils.APPEND,
          this.settings.getJavaHome() + "/jre/lib/amd64/server:" + this.settings
              .getCudaDir() + "/lib64:" + this.settings.getHadoopSymbolicLinkDir() + "/lib/native"));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_HDFS_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_VERSION", new ConfigProperty(
          "hadoop_version", HopsUtils.IGNORE, this.settings.getHadoopVersion()));
      
      sparkMagicParams.put("spark.executor.extraJavaOptions", new ConfigProperty(
          "spark_executor_extraJavaOptions", HopsUtils.APPEND, extraJavaOptions));
      
      sparkMagicParams.put("spark.executorEnv.HDFS_BASE_DIR", new ConfigProperty(
          "spark_executorEnv_HDFS_BASE_DIR", HopsUtils.IGNORE,
          "hdfs://Projects/" + this.project.getName() + js.getBaseDir()));
      
      sparkMagicParams.put("spark.pyspark.python", new ConfigProperty(
          "pyspark_bin", HopsUtils.IGNORE,
          this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
      
      sparkMagicParams.put("spark.shuffle.service.enabled", new ConfigProperty("", HopsUtils.IGNORE, "true"));
      
      sparkMagicParams.put("spark.submit.deployMode", new ConfigProperty("", HopsUtils.IGNORE, "cluster"));
  
      sparkMagicParams.put("spark.tensorflow.application", new ConfigProperty(
          "spark_tensorflow_application", HopsUtils.IGNORE,
          Boolean.toString(isTensorFlow || isTensorFlowOnSpark || isHorovod)));
  
      sparkMagicParams.put("spark.tensorflow.num.ps", new ConfigProperty(
          "spark_tensorflow_num_ps", HopsUtils.IGNORE,
          (isTensorFlowOnSpark) ? Integer.toString(js.getNumTfPs()) : "0"));
      
      sparkMagicParams.put("spark.executor.gpus", new ConfigProperty(
          "spark_executor_gpus", HopsUtils.IGNORE,
          (isTensorFlow || isTensorFlowOnSpark) ? Integer.toString(js.getNumTfGpus()):
              (isHorovod) ? Integer.toString(js.getNumMpiNp()*js.getNumTfGpus()): "0"));
      
      sparkMagicParams.put("spark.dynamicAllocation.enabled", new ConfigProperty(
          "spark_dynamicAllocation_enabled", HopsUtils.OVERWRITE,
          Boolean.toString(isSparkDynamic || isTensorFlow || isTensorFlowOnSpark || isHorovod)));
      
      sparkMagicParams.put("spark.dynamicAllocation.initialExecutors", new ConfigProperty(
          "spark_dynamicAllocation_initialExecutors", HopsUtils.OVERWRITE,
          (isTensorFlow) ? "0" :
              (isHorovod) ? "1" :
                  (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()) :
                  Integer.toString(js.getDynamicMinExecutors())));
      
      sparkMagicParams.put("spark.dynamicAllocation.minExecutors", new ConfigProperty(
          "spark_dynamicAllocation_minExecutors", HopsUtils.OVERWRITE,
          (isTensorFlow || isTensorFlowOnSpark || isHorovod) ? "0" :
              Integer.toString(js.getDynamicMinExecutors())));
      
      sparkMagicParams.put("spark.dynamicAllocation.maxExecutors", new ConfigProperty(
          "spark_dynamicAllocation_maxExecutors", HopsUtils.OVERWRITE,
          (isTensorFlow) ? Integer.toString(js.getNumExecutors()) :
              (isHorovod) ? "1" :
                  (isTensorFlowOnSpark) ? Integer.toString(js.getNumExecutors() + js.getNumTfPs()) :
                  Integer.toString(js.getDynamicMaxExecutors())));
      
      sparkMagicParams.put("spark.dynamicAllocation.executorIdleTimeout", new ConfigProperty(
          "spark_dynamicAllocation_executorIdleTimeout", HopsUtils.OVERWRITE,
          (isTensorFlowOnSpark) ? Integer.toString(((js.getNumExecutors() + js.getNumTfPs()) * 15) + 60 ) + "s" :
              "60s"));
      
      sparkMagicParams.put("spark.metrics.conf", new ConfigProperty(
          "spark_metrics_conf", HopsUtils.OVERWRITE, settings.getSparkMetricsPath()));
      
      sparkMagicParams.put("spark.task.maxFailures", new ConfigProperty(
          "spark_task_maxFailures", HopsUtils.OVERWRITE,
          (isHorovod || isTensorFlow || isTensorFlowOnSpark) ? "1": "4"));
      
      // Merge system and user defined properties
      Map<String, String> sparkParamsAfterMerge = HopsUtils.mergeHopsworksAndUserParams(sparkMagicParams,
          userSparkProperties, false);
      
      StringBuilder sparkmagic_sb
          = ConfigFileGenerator.
              instantiateFromTemplate(
                  ConfigFileGenerator.SPARKMAGIC_CONFIG_TEMPLATE,
                  sparkParamsAfterMerge);
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

  public String getCertificatesDir() {
    return certificatesDir;
  }
}
