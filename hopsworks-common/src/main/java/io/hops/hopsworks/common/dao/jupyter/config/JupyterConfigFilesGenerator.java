/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.dao.jupyter.config;

import com.google.common.base.Strings;
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
//    File log4j_file = new File(confDirPath + LOG4J_PROPS);
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
  
//      // TODO: Add this local file to 'spark: file' to copy it to hdfs and localize it.
//      StringBuilder log4j_sb
//          = ConfigFileGenerator.instantiateFromTemplate(
//          ConfigFileGenerator.LOG4J_TEMPLATE_JUPYTER,
//          "logstash_ip", settings.getLogstashIp(),
//          "logstash_port", settings.getLogstashPort().toString(),
//          "log_level", js.getLogLevel().toUpperCase()
//      );
//      ConfigFileGenerator.createConfigFile(log4j_file, log4j_sb.toString());
//  
      StringBuilder sparkFiles = new StringBuilder();
      sparkFiles
          //Log4j.properties
          .append(settings.getSparkLog4JPath())
          .append(",")
          // Glassfish domain truststore
          .append(settings.getGlassfishTrustStoreHdfs()).append("#").append(Settings.DOMAIN_CA_TRUSTSTORE)
          .append(",")
          // Add HopsUtil
          .append(settings.getHopsUtilHdfsPath());
  
      if (!js.getFiles().equals("")) {
        //Split the comma-separated string and append it to sparkFiles
        for (String file : js.getFiles().split(",")) {
          sparkFiles.append(",").append(file);
        }
      }
  
      String extraClassPath = settings.getHopsLeaderElectionJarPath()
          + File.pathSeparator
          + settings.getHopsUtilFilename();
  
      if (!js.getJars().equals("")) {
        //Split the comma-separated string and append the names to the driver and executor classpath
        for (String jar : js.getJars().split(",")) {
          sparkFiles.append(",").append(jar);
          //Get jar name
          String name = jar.substring(jar.lastIndexOf("/") + 1);
          extraClassPath += File.pathSeparator + name;
        }
      }
  
      // If Hops RPC TLS is enabled, password file would be injected by the
      // NodeManagers. We don't need to add it as LocalResource
      if (!settings.getHopsRpcTls()) {
        sparkFiles
            // Keystore
            .append(",hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
            .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
            .append("__kstore.jks#").append(Settings.K_CERTIFICATE)
            .append(",")
            // TrustStore
            .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
            .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
            .append("__tstore.jks#").append(Settings.T_CERTIFICATE)
            .append(",")
            // File with crypto material password
            .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
            .append(this.hdfsUser).append(File.separator).append(this.hdfsUser)
            .append("__cert.key#").append(Settings.CRYPTO_MATERIAL_PASSWORD);
      }
      
      //Prepare pyfiles
      StringBuilder pyFilesBuilder = new StringBuilder();
      if (!Strings.isNullOrEmpty(js.getPyFiles())) {
        pyFilesBuilder= new StringBuilder();
        for (String file : js.getPyFiles().split(",")) {
          file += "#" + file.substring(file.lastIndexOf("/")+1);
          pyFilesBuilder.append(file).append(",");
        }
        //Remove last comma character
        pyFilesBuilder.deleteCharAt(pyFilesBuilder.length()-1);
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

      // Export versions of software

      sparkMagicParams.put("spark.yarn.appMasterEnv.LIVY_VERSION", new ConfigProperty("livy_version",
              HopsUtils.IGNORE, this.settings.getLivyVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.SPARK_VERSION", new ConfigProperty("spark_version",
              HopsUtils.IGNORE, this.settings.getSparkVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.KAFKA_VERSION", new ConfigProperty("kafka_version",
              HopsUtils.IGNORE, this.settings.getKafkaVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.TENSORFLOW_VERSION", new ConfigProperty("tensorflow_version",
              HopsUtils.IGNORE, this.settings.getTensorflowVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.CUDA_VERSION", new ConfigProperty("cuda_version",
              HopsUtils.IGNORE, this.settings.getCudaVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.HOPSWORKS_VERSION", new ConfigProperty("hopsworks_version",
              HopsUtils.IGNORE, this.settings.getHopsworksVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_VERSION", new ConfigProperty("hadoop_version",
              HopsUtils.IGNORE, this.settings.getHadoopVersion()));

      sparkMagicParams.put("spark.yarn.appMasterEnv.KAFKA_BROKERS", new ConfigProperty("kafka_brokers",
              HopsUtils.IGNORE, this.settings.getKafkaBrokersStr()));
      // Spark properties
      sparkMagicParams.put(this.settings.SPARK_EXECUTORENV_PATH, new ConfigProperty("spark_executorEnv_PATH",
          HopsUtils.APPEND_PATH, this.settings.getAnacondaProjectDir(project.getName())
          + "/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK_DRIVER_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.PYSPARK3_PYTHON", new ConfigProperty("pyspark_bin",
          HopsUtils.IGNORE, this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put(this.settings.SPARK_YARN_APPMASTERENV_LD_LIBRARY_PATH, new ConfigProperty(
          "spark_yarn_appMaster_LD_LIBRARY_PATH", HopsUtils.APPEND_PATH,
          this.settings.getJavaHome() + "/jre/lib/amd64/server:" + this.settings.getCudaDir()
              + "/lib64:" + this.settings.getHadoopSymbolicLinkDir() + "/lib/native"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_HOME", new ConfigProperty("hadoop_home",
          HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put(this.settings.SPARK_YARN_APPMASTERENV_LIBHDFS_OPTS, new ConfigProperty(
          "spark_yarn_appMasterEnv_LIBHDFS_OPTS", HopsUtils.APPEND_SPACE,
          "-Xmx96m -Dlog4j.configuration=" + this.settings.getHadoopSymbolicLinkDir()
              +"/etc/hadoop/log4j.properties -Dhadoop.root.logger=ERROR,RFA"));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_HDFS_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put("spark.yarn.appMasterEnv.HADOOP_USER_NAME", new ConfigProperty(
          "hdfs_user", HopsUtils.IGNORE, this.hdfsUser));

      sparkMagicParams.put("spark.yarn.appMasterEnv.REST_ENDPOINT", new ConfigProperty(
              "rest_endpoint", HopsUtils.IGNORE, settings.getRestEndpoint()));
  
      sparkMagicParams.put("spark.yarn.appMasterEnv.HDFS_BASE_DIR", new ConfigProperty(
          "spark_yarn_appMasterEnv_HDFS_BASE_DIR", HopsUtils.IGNORE,
          "hdfs://Projects/" + this.project.getName() + js.getBaseDir()));
      
      sparkMagicParams.put(this.settings.SPARK_DRIVER_STAGINGDIR_ENV, new ConfigProperty(
          "spark_yarn_stagingDir", HopsUtils.IGNORE,
          "hdfs:///Projects/" + this.project.getName() + "/Resources"));
  
      sparkMagicParams.put("spark.yarn.dist.files", new ConfigProperty(
          "spark_yarn_dist_files", HopsUtils.IGNORE,
          sparkFiles.toString()));
  
      sparkMagicParams.put("spark.yarn.dist.archives", new ConfigProperty(
          "spark_yarn_dist_archives", HopsUtils.IGNORE,
          js.getArchives()));
  
      sparkMagicParams.put("spark.yarn.dist.pyFiles", new ConfigProperty(
          "spark_yarn_dist_pyFiles", HopsUtils.IGNORE,
          pyFilesBuilder.toString()));
    
      sparkMagicParams.put(this.settings.SPARK_DRIVER_EXTRALIBRARYPATH, new ConfigProperty(
          "spark_driver_extraLibraryPath", HopsUtils.APPEND_PATH,
          this.getSettings().getCudaDir() + "/lib64"));
      
      sparkMagicParams.put(this.settings.SPARK_DRIVER_EXTRAJAVAOPTIONS, new ConfigProperty(
          "spark_driver_extraJavaOptions", HopsUtils.APPEND_SPACE, extraJavaOptions));
      
      sparkMagicParams.put(this.settings.SPARK_DRIVER_EXTRACLASSPATH, new ConfigProperty(
          "spark_driver_extraClassPath", HopsUtils.APPEND_PATH, extraClassPath));
  
      sparkMagicParams.put(this.settings.SPARK_EXECUTOR_EXTRACLASSPATH, new ConfigProperty(
          "spark_executor_extraClassPath", HopsUtils.APPEND_PATH, extraClassPath));
      
      sparkMagicParams.put("spark.executorEnv.MPI_NP", new ConfigProperty(
          "spark_executorEnv_MPI_NP", HopsUtils.IGNORE, (isHorovod) ? Integer.toString(js.getNumMpiNp()) : ""));

      sparkMagicParams.put("spark.executorEnv.REST_ENDPOINT", new ConfigProperty(
              "rest_endpoint", HopsUtils.IGNORE, settings.getRestEndpoint()));
      
      sparkMagicParams.put(this.settings.SPARK_EXECUTORENV_HADOOP_USER_NAME, new ConfigProperty(
          "hdfs_user", HopsUtils.IGNORE, this.hdfsUser));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));
      
      sparkMagicParams.put(this.settings.SPARK_EXECUTORENV_LIBHDFS_OPTS, new ConfigProperty(
          "spark_executorEnv_LIBHDFS_OPTS", HopsUtils.APPEND_SPACE,
          "-Xmx96m -Dlog4j.configuration=" + this.settings.getHadoopSymbolicLinkDir() +
              "/etc/hadoop/log4j.properties -Dhadoop.root.logger=ERROR,RFA"));
      
      sparkMagicParams.put("spark.executorEnv.PYSPARK_PYTHON", new ConfigProperty(
          "pyspark_bin", HopsUtils.IGNORE,
          this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
  
      sparkMagicParams.put("spark.executorEnv.PYSPARK3_PYTHON", new ConfigProperty(
          "pyspark_bin", HopsUtils.IGNORE,
          this.settings.getAnacondaProjectDir(project.getName()) + "/bin/python"));
      
      sparkMagicParams.put(this.settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH, new ConfigProperty(
          "spark_executorEnv_LD_LIBRARY_PATH", HopsUtils.APPEND_PATH,
          this.settings.getJavaHome() + "/jre/lib/amd64/server:" + this.settings
              .getCudaDir() + "/lib64:" + this.settings.getHadoopSymbolicLinkDir() + "/lib/native"));
      
      sparkMagicParams.put("spark.executorEnv.HADOOP_HDFS_HOME", new ConfigProperty(
          "hadoop_home", HopsUtils.IGNORE, this.settings.getHadoopSymbolicLinkDir()));

      // Export versions of software

      sparkMagicParams.put("spark.executorEnv.LIVY_VERSION", new ConfigProperty("livy_version",
              HopsUtils.IGNORE, this.settings.getLivyVersion()));

      sparkMagicParams.put("spark.executorEnv.SPARK_VERSION", new ConfigProperty("spark_version",
              HopsUtils.IGNORE, this.settings.getSparkVersion()));

      sparkMagicParams.put("spark.executorEnv.KAFKA_VERSION", new ConfigProperty("kafka_version",
              HopsUtils.IGNORE, this.settings.getKafkaVersion()));

      sparkMagicParams.put("spark.executorEnv.TENSORFLOW_VERSION", new ConfigProperty("tensorflow_version",
              HopsUtils.IGNORE, this.settings.getTensorflowVersion()));

      sparkMagicParams.put("spark.executorEnv.CUDA_VERSION", new ConfigProperty("cuda_version",
              HopsUtils.IGNORE, this.settings.getCudaVersion()));

      sparkMagicParams.put("spark.executorEnv.HOPSWORKS_VERSION", new ConfigProperty("hopsworks_version",
              HopsUtils.IGNORE, this.settings.getHopsworksVersion()));

      sparkMagicParams.put("spark.executorEnv.HADOOP_VERSION", new ConfigProperty("hadoop_version",
              HopsUtils.IGNORE, this.settings.getHadoopVersion()));

      sparkMagicParams.put("spark.executorEnv.KAFKA_BROKERS", new ConfigProperty("kafka_brokers",
              HopsUtils.IGNORE, this.settings.getKafkaBrokersStr()));
      
      sparkMagicParams.put(this.settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS, new ConfigProperty(
          "spark_executor_extraJavaOptions", HopsUtils.APPEND_SPACE, extraJavaOptions));
      
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


      // Blacklisting behaviour for TensorFlow on Spark (e.g. Hyperparameter search) to make it robust
      // Allow many failures on a particular node before blacklisting the node
      // Blacklist executor instantly

      sparkMagicParams.put("spark.blacklist.enabled", new ConfigProperty(
              "spark_blacklist_enabled", HopsUtils.OVERWRITE,
              (isTensorFlow && js.getFaultTolerant()) ? "true": "false"));

      // If any task fails on an executor - kill it instantly (need fresh working directory for each task)
      sparkMagicParams.put("spark.blacklist.task.maxTaskAttemptsPerExecutor", new ConfigProperty(
              "spark_max_task_attempts_per_executor", HopsUtils.OVERWRITE, "1"));

      // Blacklist node after 2 tasks fails on it
      sparkMagicParams.put("spark.blacklist.task.maxTaskAttemptsPerNode", new ConfigProperty(
              "spark_max_task_attempts_per_node", HopsUtils.OVERWRITE, "2"));

      // If any task fails on an executor within a stage - blacklist it
      sparkMagicParams.put("spark.blacklist.stage.maxFailedTasksPerExecutor", new ConfigProperty(
              "spark_stage_max_failed_tasks_per_executor", HopsUtils.OVERWRITE, "1"));

      // Blacklist node after 2 tasks within a stage fails on it
      sparkMagicParams.put("spark.blacklist.stage.maxFailedExecutorsPerNode", new ConfigProperty(
              "spark_stage_max_failed_executors_per_node", HopsUtils.OVERWRITE, "2"));

      // If any task fails on an executor within an application - blacklist it
      sparkMagicParams.put("spark.blacklist.application.maxFailedTasksPerExecutor", new ConfigProperty(
              "spark_application_max_failed_tasks_per_executor", HopsUtils.OVERWRITE, "1"));

      // If 2 task fails on a node within an application - blacklist it
      sparkMagicParams.put("spark.blacklist.application.maxFailedExecutorsPerNode", new ConfigProperty(
              "spark_application_max_failed_executors_per_node", HopsUtils.OVERWRITE, "2"));

      sparkMagicParams.put("spark.task.maxFailures", new ConfigProperty(
              "spark_task_max_failures", HopsUtils.OVERWRITE,
              (isTensorFlow && js.getFaultTolerant()) ? "3" :
                      ((isHorovod || isTensorFlowOnSpark || isTensorFlow) ? "1" : "4")));

      // Always kill the blacklisted executors (further failures could be results of local files from the failed task)
      sparkMagicParams.put("spark.blacklist.killBlacklistedExecutors", new ConfigProperty(
              "spark_kill_blacklisted_executors", HopsUtils.OVERWRITE,
              (isTensorFlow) ? "true": "false"));
      
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
