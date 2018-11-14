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

package io.hops.hopsworks.api.zeppelin.server;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImpl;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImplFactory;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.dep.DependencyResolver;
import org.apache.zeppelin.helium.Helium;
import org.apache.zeppelin.helium.HeliumApplicationFactory;
import org.apache.zeppelin.helium.HeliumBundleFactory;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.interpreter.InterpreterOption;
import org.apache.zeppelin.interpreter.InterpreterOutput;
import org.apache.zeppelin.interpreter.InterpreterSettingManager;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.NotebookAuthorization;
import org.apache.zeppelin.notebook.repo.NotebookRepoSync;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.apache.zeppelin.search.LuceneSearch;
import org.apache.zeppelin.search.SearchService;
import org.apache.zeppelin.user.Credentials;
import org.quartz.SchedulerException;
import org.sonatype.aether.RepositoryException;

public class ZeppelinConfig {

  private static final Logger LOGGER = Logger.getLogger(ZeppelinConfig.class.getName());
  private static final String ZEPPELIN_SITE_XML = "/zeppelin-site.xml";
  private static final String ZEPPELIN_ENV_SH = "/zeppelin-env.sh";
  public static final String INTERPRETER_JSON = "/interpreter.json";
  private static final int DELETE_RETRY = 10;

  /**
   * A configuration that is common for all projects.
   */
  public static ZeppelinConfiguration COMMON_CONF;
  private ZeppelinConfiguration conf;
  private SchedulerFactory schedulerFactory;

  private Notebook notebook;
  private NotebookServerImpl notebookServer;
  private InterpreterFactory replFactory;
  private NotebookRepoSync notebookRepo;
  private DependencyResolver depResolver;
  private NotebookAuthorization notebookAuthorization;
  private InterpreterSettingManager interpreterSettingManager;
  private Helium helium;
  private HeliumApplicationFactory heliumApplicationFactory;
  private HeliumBundleFactory heliumBundleFactory;
  private Credentials credentials;
  private SearchService noteSearchService;
  private final Settings settings;
  private final String projectName;
  private final Integer projectId;
  private final String projectDirPath;
  private final String confDirPath;
  private final String notebookDirPath;
  private final String runDirPath;
  private final String binDirPath;
  private final String logDirPath;
  private final String interpreterDirPath;
  private final String libDirPath;
  private final String repoDirPath;
  private final String owner;

  public ZeppelinConfig(String projectName, Integer projectId, String owner, Settings settings,
      String interpreterConf, NotebookServerImpl nbs)
    throws IOException, RepositoryException, TaskRunnerException, InterruptedException {
    this.projectName = projectName;
    this.projectId = projectId;
    this.owner = owner;
    this.settings = settings;
    this.projectDirPath = settings.getZeppelinDir() + File.separator + Settings.DIR_ROOT + File.separator
        + this.projectName;
    this.runDirPath = this.projectDirPath + File.separator + "run";
    this.binDirPath = this.projectDirPath + File.separator + "bin";
    this.logDirPath = this.projectDirPath + File.separator + "logs";
    this.libDirPath = this.projectDirPath + File.separator + "lib";
    this.confDirPath = this.projectDirPath + File.separator + "conf";
    this.interpreterDirPath = this.projectDirPath + File.separator + "interpreter";
    this.notebookDirPath = this.projectDirPath + File.separator + COMMON_CONF.getNotebookDir();
    this.repoDirPath = this.projectDirPath + File.separator + COMMON_CONF.getInterpreterLocalRepoPath();
    
    boolean newDir = false;
    boolean newFile = false;
    boolean newBinDir = false;
    try {
      newDir = createZeppelinDirs();//creates the necessary folders for the project in /srv/hops/zeppelin
      newBinDir = copyBinDir();
      createSymLinks();//interpreter and lib
      createVisCacheSymlink();//create a symlink to node and npm tar cache.
      newFile = createZeppelinConfFiles(interpreterConf);//create project specific configurations for zeppelin
      this.conf = loadConfig();//load the newly created zeppelin-site.xml
      this.depResolver = new DependencyResolver(conf.getString(
          ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_LOCALREPO));
      InterpreterOutput.limit = conf.getInt(
          ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_OUTPUT_LIMIT);
      this.heliumApplicationFactory = new HeliumApplicationFactory();
      this.heliumBundleFactory = new HeliumBundleFactory(conf, null,
          new File(conf.getRelativeDir(ZeppelinConfiguration.ConfVars.ZEPPELIN_DEP_LOCALREPO)),
          new File(conf.getRelativeDir("lib/node_modules/zeppelin-tabledata")),
          new File(conf.getRelativeDir("lib/node_modules/zeppelin-vis")),
          new File(conf.getRelativeDir("lib/node_modules/zeppelin-spell")));
      
      this.helium = new Helium(conf.getHeliumConfPath(), conf.getHeliumRegistry(), new File(conf.getRelativeDir(
          ZeppelinConfiguration.ConfVars.ZEPPELIN_DEP_LOCALREPO), "helium-registry-cache"), heliumBundleFactory,
          heliumApplicationFactory);
      // create bundle
      try {
        this.heliumBundleFactory.buildAllPackages(helium.getBundlePackagesToBundle());
      } catch (Exception e) {
        LOGGER.log(Level.INFO, e.getMessage(), e);
      }
      
      this.schedulerFactory = SchedulerFactory.singleton();
      this.interpreterSettingManager = new InterpreterSettingManager(conf, depResolver, new InterpreterOption(true));
      this.notebookRepo = getNotebookRepo(owner);
      this.noteSearchService = new LuceneSearch();
      this.notebookAuthorization = NotebookAuthorization.init(conf);
      this.credentials = new Credentials(conf.credentialsPersist(), conf.getCredentialsPath());
      if(nbs!=null){
        setNotebookServer(nbs);
      }
    } catch (IOException | RepositoryException | TaskRunnerException | InterruptedException e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGER.log(Level.SEVERE, "Error in initializing ZeppelinConfig for project: {0}. {1}",
          new Object[]{this.projectName, e});
      throw e;
    }
  }
  
  public ZeppelinConfig(ZeppelinConfig zConf, NotebookServerImpl nbs) {
    this.settings = zConf.getSettings();
    this.projectName = zConf.getProjectName();
    this.projectId = zConf.getProjectId();
    this.owner = zConf.getOwner();
    this.projectDirPath = zConf.getProjectDirPath();
    this.confDirPath = zConf.getConfDirPath();
    this.notebookDirPath = zConf.getNotebookDirPath();
    this.runDirPath = zConf.getRunDirPath();
    this.binDirPath = zConf.getBinDirPath();
    this.logDirPath = zConf.getLogDirPath();
    this.interpreterDirPath = zConf.getInterpreterDirPath();
    this.libDirPath = zConf.getLibDirPath();
    this.repoDirPath = zConf.getRepoDirPath();
    this.conf = zConf.getConf();
    this.depResolver = zConf.getDepResolver();
    this.schedulerFactory = zConf.getSchedulerFactory();
    this.interpreterSettingManager = zConf.getInterpreterSettingManager();
    this.heliumApplicationFactory = zConf.getHeliumApplicationFactory();
    this.helium = zConf.getHelium();
    this.notebookRepo = zConf.getNotebookRepo();
    this.noteSearchService = zConf.getNotebookIndex();
    this.notebookAuthorization = zConf.getNotebookAuthorization();
    this.credentials = zConf.getCredentials();
    setNotebookServer(nbs);
  }
  
  private NotebookRepoSync getNotebookRepo(String owner) {
    if(owner==null){
      return null;
    }
    String notebookStorage = conf.getString(ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE);
    LOGGER.log(Level.INFO, "Using notebook Repo class {0}", notebookStorage);
    boolean notebookInHdfs = notebookStorage.contains("HDFSNotebookRepo");
    if (!notebookInHdfs) {
      return new NotebookRepoSync(conf);
    }
    NotebookRepoSync nbRepo = null;
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUser(owner, UserGroupInformation.getLoginUser());
      nbRepo = ugi.doAs((PrivilegedExceptionAction<NotebookRepoSync>) () -> new NotebookRepoSync(conf));
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Could not create proxy user.", ex);
    } catch (InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Could not create notebook repo.", ex);
    }
    if (nbRepo == null) {
      LOGGER.log(Level.SEVERE, "Could not create notebook repo.");
      throw new IllegalStateException("Could not create notebook repo.");
    }
    return nbRepo;
  }
  
  public ZeppelinConfiguration getConf() {
    return this.conf;
  }
  
  public void setNotebookServer(NotebookServerImpl nbs) {
    if(this.notebookServer!=null){
      return;
    }
    this.notebookServer = nbs;
    try {
      this.replFactory = new InterpreterFactory(this.conf, this.notebookServer,
          this.notebookServer, this.heliumApplicationFactory,
          this.depResolver, SecurityUtils.isAuthenticated(),
          this.interpreterSettingManager);
      this.notebook = new Notebook(this.conf, this.notebookRepo,
          this.schedulerFactory, this.replFactory, interpreterSettingManager,
          this.notebookServer,
          this.noteSearchService, this.notebookAuthorization,
          this.credentials);
      // to update notebook from application event from remote process.
      this.heliumApplicationFactory.setNotebook(notebook);
      // to update fire websocket event on application event.
      this.heliumApplicationFactory.setApplicationEventListener(this.notebookServer);
      this.notebook.addNotebookEventListener(heliumApplicationFactory);
      this.notebook.addNotebookEventListener(this.notebookServer.getNotebookInformationListener());
    } catch (InterpreterException | IOException | RepositoryException | SchedulerException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  public boolean isClosed() {
    return this.replFactory == null || this.notebook == null || this.notebookServer == null;
  }
  
  public InterpreterSettingManager getInterpreterSettingManager() {
    return interpreterSettingManager;
  }
  
  public Notebook getNotebook() {
    return notebook;
  }
  
  public NotebookRepoSync getNotebookRepo() {
    return notebookRepo;
  }
  
  private SchedulerFactory getSchedulerFactory() {
    return this.schedulerFactory;
  }
  
  public Helium getHelium() {
    return helium;
  }
  
  private HeliumApplicationFactory getHeliumApplicationFactory() {
    return heliumApplicationFactory;
  }
  
  public NotebookServerImpl getNotebookServer() {
    return this.notebookServer;
  }
  
  public InterpreterFactory getReplFactory() {
    return this.replFactory;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public String getOwner() {
    return owner;
  }
  
  private String getProjectDirPath() {
    return projectDirPath;
  }
  
  public SearchService getNotebookIndex() {
    return noteSearchService;
  }
  
  private DependencyResolver getDepResolver() {
    return depResolver;
  }
  
  public void setDepResolver(DependencyResolver depResolver) {
    this.depResolver = depResolver;
  }
  
  public Settings getSettings() {
    return settings;
  }
  
  private String getInterpreterDirPath() {
    return interpreterDirPath;
  }
  
  private String getLibDirPath() {
    return libDirPath;
  }
  
  public String getConfDirPath() {
    return confDirPath;
  }
  
  private String getNotebookDirPath() {
    return notebookDirPath;
  }
  
  private String getRunDirPath() {
    return runDirPath;
  }
  
  private String getBinDirPath() {
    return binDirPath;
  }
  
  private String getLogDirPath() {
    return logDirPath;
  }
  
  private String getRepoDirPath() {
    return repoDirPath;
  }
  
  private NotebookAuthorization getNotebookAuthorization() {
    return notebookAuthorization;
  }
  
  private Credentials getCredentials() {
    return credentials;
  }

  //returns true if the project dir was created
  private boolean createZeppelinDirs() {
    File projectDir = new File(projectDirPath);
    boolean newProjectDir = projectDir.mkdirs();
    new File(confDirPath).mkdirs();
    new File(notebookDirPath).mkdirs();
    new File(runDirPath).mkdirs();
    new File(binDirPath).mkdirs();
    new File(logDirPath).mkdirs();
    new File(repoDirPath).mkdirs();
    return newProjectDir;
  }

  //copies /srv/zeppelin/bin to /srv/zeppelin/this.project/bin
  private boolean copyBinDir() throws IOException {
    String source = settings.getZeppelinDir() + File.separator + "bin";
    File binDir = new File(binDirPath);
    File sourceDir = new File(source);
    if (binDir.list().length == sourceDir.list().length) {
      //should probably check if the files are the same
      return false;
    }
    Path destinationPath;
    for (File file : sourceDir.listFiles()) {
      destinationPath = Paths.get(binDirPath + File.separator + file.getName());
      Files.copy(file.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }
    return binDir.list().length == sourceDir.list().length;
  }

  //creates symlink to interpreters and libs
  private void createSymLinks() throws IOException {
    File target = new File(settings.getZeppelinDir() + File.separator + "interpreter");
    File newLink = new File(interpreterDirPath);
    if (!newLink.exists()) {
      Files.createSymbolicLink(newLink.toPath(), target.toPath());
    }
    target = new File(settings.getZeppelinDir() + File.separator + "lib");
    newLink = new File(libDirPath);
    if (!newLink.exists()) {
      Files.createSymbolicLink(newLink.toPath(), target.toPath());
    }
  }

  private void createVisCacheSymlink() throws IOException {
    File target = new File(settings.getZeppelinDir() + File.separator + "local-repo/vis");
    if (!target.exists()) {
      LOGGER.log(Level.SEVERE, "Node and npm not cached at {0}. Zeppelin will try to download this for every project.",
          target.toURI());
      return;
    }
    File newLink = new File(repoDirPath + File.separator + "vis");
    if (!newLink.exists()) {
      Files.createSymbolicLink(newLink.toPath(), target.toPath());
    }
  }

  // returns true if one of the conf files were created anew
  private boolean createZeppelinConfFiles(String interpreterConf) throws IOException, InterruptedException {
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    File interpreter_file = new File(confDirPath + INTERPRETER_JSON);
    String home = settings.getZeppelinDir() + File.separator + Settings.DIR_ROOT + File.separator + this.projectName;
    String notebookDir = File.separator + Settings.DIR_ROOT + File.separator + this.projectName;
    String resourceDir = File.separator + Settings.DIR_ROOT + File.separator + this.projectName + File.separator
        + Settings.BaseDataset.RESOURCES.getName();
    boolean createdSh = false;
    boolean createdLog4j = false;
    boolean createdXml = false;

    String log4jPath = settings.getSparkLog4JPath();
    String zeppelinPythonPath = settings.getAnacondaDir() + File.separator + "envs" + File.separator
        + "python27" + File.separator + "bin" + File.separator + "python";
    if (!zeppelin_env_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String javaHome = settings.getJavaHome();
      if (System.getenv().containsKey("JAVA_HOME")) {
        javaHome = System.getenv("JAVA_HOME");
      }

      StringBuilder zeppelin_env = ConfigFileGenerator.instantiateFromTemplate(
          ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
          "spark_dir", settings.getSparkDir(),
          "hadoop_dir", settings.getHadoopSymbolicLinkDir(),
          "anaconda_env_dir", settings.getAnacondaDir() + "/envs/" + this.projectName,
          "hadoop_username", this.projectName + Settings.PROJECT_GENERIC_USER_SUFFIX,
          "java_home", javaHome,
          "cuda_dir", settings.getCudaDir(),
          "ld_library_path", ldLibraryPath,
          "hadoop_classpath", HopsUtils.getHadoopClasspathGlob(settings.getHadoopSymbolicLinkDir() + "/bin/hadoop",
              "classpath", "--glob"),
          "spark_options", "--files " + log4jPath
      );
      createdSh = ConfigFileGenerator.createConfigFile(zeppelin_env_file,
          zeppelin_env.
              toString());
    }

    if (!zeppelin_site_xml_file.exists()) {
      StringBuilder zeppelin_site_xml = ConfigFileGenerator.
          instantiateFromTemplate(
              ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
              "zeppelin_home", home,
              "livy_url", settings.getLivyUrl(),
              "livy_master", settings.getLivyYarnMode(),
              "zeppelin_home_dir", home,
              "zeppelin_notebook_dir", notebookDir,
              "zeppelin_interpreters", settings.getZeppelinInterpreters());
      createdXml = ConfigFileGenerator.createConfigFile(zeppelin_site_xml_file,
          zeppelin_site_xml.
              toString());
    }

    //get interpreter string from db
    if (interpreter_file.exists()) {
      interpreter_file.delete();
    }

    //Set Hopsworks properties to be available in Zeppelin
    String logstashID = "-D" + Settings.LOGSTASH_JOB_INFO + "=" + this.projectName.toLowerCase() + ",zeppelin,"
        + "notebook,?";
    String restEndpointProp = " -D" + Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY + "=" + settings.getRestEndpoint();
    String elasticEndpointProp = " -D" + Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY + "="
        + settings.getElasticRESTEndpoint();
    String projectIdProp = " -D" + Settings.HOPSWORKS_PROJECTID_PROPERTY + "=" + this.projectId;
    String projectNameProp = " -D" + Settings.HOPSWORKS_PROJECTNAME_PROPERTY + "=" + this.projectName;
    String userProp = " -D" + Settings.HOPSWORKS_PROJECTUSER_PROPERTY + "=" + this.projectName
        + Settings.PROJECT_GENERIC_USER_SUFFIX;
    String jobType = " -D" + Settings.HOPSWORKS_JOBTYPE_PROPERTY + "=" + JobType.SPARK;
    String kafkaBrokers  = " -D" + Settings.KAFKA_BROKERADDR_PROPERTY + "=" + settings.getKafkaBrokersStr();
  
    String extraJavaOptions = " -Dlog4j.configuration=./log4j.properties "
        + logstashID + restEndpointProp + elasticEndpointProp + projectIdProp
        + projectNameProp + userProp + jobType + kafkaBrokers;
    String hdfsResourceDir = "hdfs://" + resourceDir + File.separator;
    // Comma-separated files to be added as local resources to Spark/Livy interpreter
    String driverExtraClassPath = settings.getHopsLeaderElectionJarPath()
        + File.pathSeparator
        +  settings.getHopsUtilFilename();
    String executorExtraClassPath = settings.getHopsLeaderElectionJarPath()
        + File.pathSeparator
        + settings.getHopsUtilFilename();
    
    StringBuilder distFiles = new StringBuilder();
    // When Hops RPC TLS is enabled, Yarn will take care of application certificate
    if (!settings.getHopsRpcTls()) {
      distFiles
          // KeyStore
          .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append("__kstore.jks#")
          .append(Settings.K_CERTIFICATE).append(",")
          // TrustStore
          .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append("__tstore.jks#")
          .append(Settings.T_CERTIFICATE)
          .append(",")
          // Material password
          .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append(File.separator)
          .append(projectName)
          .append(Settings.PROJECT_GENERIC_USER_SUFFIX)
          .append("__cert.key#")
          .append(Settings.CRYPTO_MATERIAL_PASSWORD)
          .append(",");
    }
    distFiles
        // Glassfish domain truststore
        .append(settings.getGlassfishTrustStoreHdfs())
        .append("#").append(Settings.DOMAIN_CA_TRUSTSTORE)
        .append(",")
        // Add HopsUtil
        .append(settings.getHopsUtilHdfsPath()).append("#").append(settings.getHopsUtilFilename());
    

    if (interpreterConf == null) {
      StringBuilder interpreter_json = ConfigFileGenerator.
          instantiateFromTemplate(
              ConfigFileGenerator.INTERPRETER_TEMPLATE,
              "projectName", this.projectName,
              "zeppelin_home_dir", home,
              "hdfs_user", this.projectName + Settings.PROJECT_GENERIC_USER_SUFFIX,
              "hadoop_home", settings.getHadoopSymbolicLinkDir(),
              "livy_url", settings.getLivyUrl(),
              "dist_files", log4jPath + "," + distFiles.toString(),
              "extra_spark_java_options", extraJavaOptions,
              "driver_extraClassPath", driverExtraClassPath,
              "executor_extraClassPath", executorExtraClassPath,
              "spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
              "spark.yarn.stagingDir", hdfsResourceDir,
              "livy.spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
              "livy.spark.yarn.stagingDir", hdfsResourceDir,
              "hadoop_version", settings.getHadoopVersion(),
              "zeppelin.python_conda_path", zeppelinPythonPath,
              "livy_session_timeout", settings.getLivyZeppelinSessionTimeout(),
              "spark.yarn.dist.files", distFiles.toString(),
              "hive.server", settings.getHiveServerHostName(false),
              "hive.db", this.projectName
          );
      interpreterConf = interpreter_json.toString();
    } 
    createdXml = ConfigFileGenerator.createConfigFile(interpreter_file, interpreterConf);

    return createdSh || createdXml || createdLog4j;
  }
  // loads configeration from project specific zeppelin-site.xml
  private ZeppelinConfiguration loadConfig() {
    URL url = null;
    File zeppelinConfig = new File(confDirPath + ZEPPELIN_SITE_XML);
    try {
      url = zeppelinConfig.toURI().toURL();
      LOGGER.log(Level.INFO, "Load configuration from {0}", url);
      conf = new ZeppelinConfiguration(url);
    } catch (ConfigurationException e) {
      LOGGER.log(Level.INFO, "Failed to load configuration from " + url
          + " proceeding with a default", e);
      conf = new ZeppelinConfiguration();
    } catch (MalformedURLException ex) {
      LOGGER.log(Level.INFO, "Malformed URL failed to load configuration from "
          + url
          + " proceeding with a default", ex);
      conf = new ZeppelinConfiguration();
    }
    return conf;
  }

  /**
   * Closes all resources and deletes project dir
   * /srv/zeppelin/Projects/this.projectName recursive.
   *
   * @return true if the dir is deleted
   */
  public boolean cleanAndRemoveConfDirs(NotebookServerImplFactory notebookServerImplFactory) {
    clean(notebookServerImplFactory);
    return removeProjectDirRecursive();
  }

  private boolean removeProjectDirRecursive() {
    File projectDir = new File(projectDirPath);
    if (!projectDir.exists()) {
      return true;
    }
    boolean ret = false;
    File interpreter = new File(interpreterDirPath);
    File lib = new File(libDirPath);
    File repo = new File(repoDirPath + File.separator + "vis");
    //symlinks must be deleted before we recursive delete the project dir.
    int retry = 0;
    while (interpreter.exists() || lib.exists() || repo.exists()) {
      if (interpreter.exists()) {
        interpreter.delete();
      }
      if (lib.exists()) {
        lib.delete();
      }
      if (repo.exists()) {
        repo.delete();
      }
      retry++;
      if (retry > DELETE_RETRY) {
        LOGGER.log(Level.SEVERE, "Could not delete zeppelin project folder.");
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
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    File interpreter_file = new File(confDirPath + INTERPRETER_JSON);
    boolean ret = false;
    if (zeppelin_env_file.exists()) {
      ret = zeppelin_env_file.delete();
    }
    if (zeppelin_site_xml_file.exists()) {
      ret = zeppelin_site_xml_file.delete();
    }
    if (interpreter_file.exists()) {
      ret = interpreter_file.delete();
    }
    return ret;
  }

  /**
   * closes notebook SearchService, Repo, InterpreterFactory, and
   * SchedulerFactory
   */
  public void clean(NotebookServerImplFactory notebookServerImplFactory) {
    LOGGER.log(Level.INFO, "Cleanup of zeppelin resources for project {0}",
        this.projectName);
    if (interpreterSettingManager != null) {
      interpreterSettingManager.close();
    }
    // will close repo and index
    if (this.notebook != null) {
      this.notebook.close();
    }
    if (this.notebookServer != null) {
      this.notebookServer.closeConnections(notebookServerImplFactory);
    }
    this.schedulerFactory = null;
    this.replFactory = null;
    this.notebook = null;
  }
}
