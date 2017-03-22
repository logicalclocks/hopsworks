package io.hops.hopsworks.api.zeppelin.server;

import io.hops.hopsworks.api.zeppelin.socket.NotebookServer;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
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
import org.apache.zeppelin.helium.HeliumVisualizationFactory;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.NotebookAuthorization;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSync;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.apache.zeppelin.search.LuceneSearch;
import org.apache.zeppelin.search.SearchService;
import org.apache.zeppelin.user.Credentials;
import org.quartz.SchedulerException;
import org.sonatype.aether.RepositoryException;

public class ZeppelinConfig {

  private static final Logger LOGGGER = Logger.getLogger(ZeppelinConfig.class.
          getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String ZEPPELIN_SITE_XML = "/zeppelin-site.xml";
  private static final String ZEPPELIN_ENV_SH = "/zeppelin-env.sh";
  private static final String HIVE_SITE_XML = "/hive-site.xml";
  public static final String INTERPRETER_JSON = "/interpreter.json";
  public static final String METRICS_PROPERTIES = "/metrics.properties";
  private static final int DELETE_RETRY = 10;

  /**
   * A configuration that is common for all projects.
   */
  public static ZeppelinConfiguration COMMON_CONF;
  private ZeppelinConfiguration conf;
  private SchedulerFactory schedulerFactory;

  private Notebook notebook;
  private NotebookServer notebookServer;
  private InterpreterFactory replFactory;
  private NotebookRepo notebookRepo;
  private DependencyResolver depResolver;
  private NotebookAuthorization notebookAuthorization;
  private Helium helium;
  private HeliumApplicationFactory heliumApplicationFactory;
  private HeliumVisualizationFactory heliumVisualizationFactory;
  private Credentials credentials;
  private SearchService notebookIndex;
  private final Settings settings;
  private final String projectName;

  private final String projectDirPath;
  private final String confDirPath;
  private final String notebookDirPath;
  private final String runDirPath;
  private final String binDirPath;
  private final String logDirPath;
  private final String interpreterDirPath;
  private final String libDirPath;
  private final String repoDirPath;

  public ZeppelinConfig(String projectName, String owner, Settings settings,
          String interpreterConf) {
    this.projectName = projectName;
    this.settings = settings;
    boolean newDir = false;
    boolean newFile = false;
    boolean newBinDir = false;
    projectDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName;
    confDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + COMMON_CONF.getConfDir();
    notebookDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + COMMON_CONF.getNotebookDir();
    runDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "run";
    binDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "bin";
    logDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "logs";
    interpreterDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "interpreter";
    libDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "lib";
    repoDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + "local-repo";
    try {
      newDir = createZeppelinDirs();//creates the necessary folders for the project in /srv/zeppelin
      newBinDir = copyBinDir();
      createSymLinks();//interpreter and lib
      createVisCacheSymlink();//create a symlink to node and npm tar cache.
      newFile = createZeppelinConfFiles(interpreterConf);//create project specific configurations for zeppelin 
      this.conf = loadConfig();
      this.depResolver = new DependencyResolver(
              conf.getString(
                      ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_LOCALREPO));
      this.schedulerFactory = SchedulerFactory.singleton();
      this.heliumApplicationFactory = new HeliumApplicationFactory();
      this.heliumVisualizationFactory = new HeliumVisualizationFactory(
              new File(conf.getRelativeDir(
                      ZeppelinConfiguration.ConfVars.ZEPPELIN_DEP_LOCALREPO)),
              new File(conf.
                      getRelativeDir("lib/node_modules/zeppelin-tabledata")),
              new File(conf.getRelativeDir("lib/node_modules/zeppelin-vis")));
      this.helium = new Helium(conf.getHeliumConfPath(), conf.
              getHeliumDefaultLocalRegistryPath(), heliumVisualizationFactory,
              heliumApplicationFactory);
      // create visualization bundle
      this.heliumVisualizationFactory.bundle(helium.
              getVisualizationPackagesToBundle());
      LOGGGER.log(Level.INFO, "Using notebook Repo class {0}",
              conf.getString(
                      ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE));
      boolean notebookInHdfs = conf.getString(
              ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE).
              contains("HDFSNotebookRepo");
      if (notebookInHdfs) {
        this.notebookRepo = getNotebookRepo(owner);
      } else {
        this.notebookRepo = new NotebookRepoSync(conf);
      }
      this.notebookIndex = new LuceneSearch();
      this.notebookAuthorization = NotebookAuthorization.init(conf);
      this.credentials = new Credentials(conf.credentialsPersist(), conf.
              getCredentialsPath());
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGGER.log(Level.SEVERE,
              "Error in initializing ZeppelinConfig for project: {0}. {1}",
              new Object[]{this.projectName,
                e});
    }
  }

  public ZeppelinConfig(ZeppelinConfig zConf, NotebookServer nbs) {
    this.settings = zConf.getSettings();
    this.projectName = zConf.getProjectName();
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
    this.heliumApplicationFactory = zConf.getHeliumApplicationFactory();
    this.heliumVisualizationFactory = zConf.getHeliumVisualizationFactory();
    this.helium = zConf.getHelium();
    this.notebookRepo = zConf.getNotebookRepo();
    this.notebookIndex = zConf.getNotebookIndex();
    this.notebookAuthorization = zConf.getNotebookAuthorization();
    this.credentials = zConf.getCredentials();
    setNotebookServer(nbs);
  }

  private NotebookRepoSync getNotebookRepo(String owner) {
    NotebookRepoSync nbRepo = null;
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUser(owner, UserGroupInformation.
              getLoginUser());
      nbRepo = ugi.doAs((PrivilegedExceptionAction<NotebookRepoSync>) ()
              -> new NotebookRepoSync(conf));
    } catch (IOException ex) {
      LOGGGER.log(Level.SEVERE, "Could not create proxy user.", ex);
    } catch (InterruptedException ex) {
      LOGGGER.log(Level.SEVERE, "Could not create notebook repo.", ex);
    }
    if (nbRepo == null) {
      LOGGGER.log(Level.SEVERE, "Could not create notebook repo.");
      throw new IllegalStateException("Could not create notebook repo.");
    }
    return nbRepo;
  }

  public ZeppelinConfiguration getConf() {
    return this.conf;
  }

  private void setNotebookServer(NotebookServer nbs) {
    this.notebookServer = nbs;
    try {
      this.replFactory = new InterpreterFactory(this.conf, this.notebookServer,
              this.notebookServer, this.heliumApplicationFactory,
              this.depResolver, SecurityUtils.isAuthenticated());
      this.notebook = new Notebook(this.conf, this.notebookRepo,
              this.schedulerFactory, this.replFactory, this.notebookServer,
              this.notebookIndex, this.notebookAuthorization, this.credentials);
      // to update notebook from application event from remote process.
      this.heliumApplicationFactory.setNotebook(notebook);
      // to update fire websocket event on application event.
      this.heliumApplicationFactory.setApplicationEventListener(
              this.notebookServer);
      this.notebook.addNotebookEventListener(heliumApplicationFactory);
      this.notebook.addNotebookEventListener(this.notebookServer.
              getNotebookInformationListener());
    } catch (InterpreterException | IOException | RepositoryException |
            SchedulerException ex) {
      LOGGGER.log(Level.SEVERE, null, ex);
    }
  }

  public boolean isClosed() {
    return this.replFactory == null || this.notebook == null
            || this.notebookServer == null;
  }

  public Notebook getNotebook() {
    return notebook;
  }

  public NotebookRepo getNotebookRepo() {
    return notebookRepo;
  }

  public SchedulerFactory getSchedulerFactory() {
    return this.schedulerFactory;
  }

  public Helium getHelium() {
    return helium;
  }

  public HeliumApplicationFactory getHeliumApplicationFactory() {
    return heliumApplicationFactory;
  }

  public HeliumVisualizationFactory getHeliumVisualizationFactory() {
    return heliumVisualizationFactory;
  }
  
  public NotebookServer getNotebookServer() {
    return this.notebookServer;
  }

  public InterpreterFactory getReplFactory() {
    return this.replFactory;
  }

  public String getProjectName() {
    return projectName;
  }

  public String getProjectDirPath() {
    return projectDirPath;
  }

  public SearchService getNotebookIndex() {
    return notebookIndex;
  }

  public DependencyResolver getDepResolver() {
    return depResolver;
  }

  public void setDepResolver(DependencyResolver depResolver) {
    this.depResolver = depResolver;
  }

  public Settings getSettings() {
    return settings;
  }

  public String getInterpreterDirPath() {
    return interpreterDirPath;
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

  public String getRepoDirPath() {
    return repoDirPath;
  }

  public NotebookAuthorization getNotebookAuthorization() {
    return notebookAuthorization;
  }

  public Credentials getCredentials() {
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
      Files.copy(file.toPath(), destinationPath,
              StandardCopyOption.REPLACE_EXISTING);
    }

    return binDir.list().length == sourceDir.list().length;
  }

  //creates symlink to interpreters and libs
  private void createSymLinks() throws IOException {
    File target = new File(settings.getZeppelinDir() + File.separator
            + "interpreter");
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
    File target = new File(settings.getZeppelinDir() + File.separator
            + "local-repo/vis");
    if (!target.exists()) {
      LOGGGER.log(Level.SEVERE,
              "Node and npm not cached at {0}. Zeppelin will try to download "
            + "this for every project.",
              target.toURI());
      return;
    }
    File newLink = new File(repoDirPath + File.separator + "vis");
    if (!newLink.exists()) {
      Files.createSymbolicLink(newLink.toPath(), target.toPath());
    }
  }

  // returns true if one of the conf files were created anew 
  private boolean createZeppelinConfFiles(String interpreterConf) throws
          IOException {
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    File log4j_file = new File(confDirPath + LOG4J_PROPS);
    File hive_site_xml_file = new File(confDirPath + HIVE_SITE_XML);
    File interpreter_file = new File(confDirPath + INTERPRETER_JSON);
    String home = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName;
    String notebookDir = File.separator + Settings.DIR_ROOT + File.separator
            + this.projectName;
    String resourceDir = File.separator + Settings.DIR_ROOT + File.separator
            + this.projectName + File.separator
            + Settings.DefaultDataset.RESOURCES.getName();
    boolean createdSh = false;
    boolean createdLog4j = false;
    boolean createdXml = false;
    if (!log4j_file.exists()) {
      StringBuilder log4j = ConfigFileGenerator.instantiateFromTemplate(
              ConfigFileGenerator.LOG4J_TEMPLATE);
      createdLog4j = ConfigFileGenerator.createConfigFile(log4j_file, log4j.
              toString());
    }
    String metricsPath = Settings.getProjectSparkMetricsPath(this.projectName);
    String log4jPath = Settings.getProjectSparkLog4JPath(this.projectName);
    String zeppelinPythonPath = settings.getAnacondaDir() + "/envs/"+this.projectName+"/bin/python";
    if (!zeppelin_env_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String javaHome = Settings.JAVA_HOME;
      if (System.getenv().containsKey("JAVA_HOME")) {
        javaHome = System.getenv("JAVA_HOME");
      }

      StringBuilder zeppelin_env = ConfigFileGenerator.instantiateFromTemplate(
              ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
              "spark_dir", settings.getSparkDir(),
              "hadoop_dir", settings.getHadoopDir(),
              "anaconda_env_dir", settings.getAnacondaDir() + "/envs/"
              + this.projectName,
              // TODO: This should be the project__username, not just the projectname
              "hadoop_username", this.projectName,
              "java_home", javaHome,
              "ld_library_path", ldLibraryPath,
              "hadoop_classpath", HopsUtils.getHadoopClasspathGlob(settings.
                      getHadoopDir() + "/bin/hadoop", "classpath", "--glob"),
              "spark_options", "--files " + metricsPath + "," + log4jPath
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
                      "zeppelin_notebook_dir", notebookDir);
      createdXml = ConfigFileGenerator.createConfigFile(zeppelin_site_xml_file,
              zeppelin_site_xml.
              toString());
    }

    if (!hive_site_xml_file.exists()) {
      StringBuilder hive_site_xml = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.HIVE_SITE_TEMPLATE,
                      "metastore_dir", home);
      createdXml = ConfigFileGenerator.createConfigFile(hive_site_xml_file,
              hive_site_xml.toString());
    }

    //get interpreter string from db
    if (interpreter_file.exists()) {
      interpreter_file.delete();
    }

    String jobName = this.projectName.toLowerCase() + "-zeppelin";
    String logstashID = "-D" + Settings.LOGSTASH_JOB_INFO + "="
            + this.projectName.toLowerCase() + "," + jobName + "," + jobName;
    String extraSparkJavaOptions = " -Dlog4j.configuration=./log4j.properties " + logstashID;
    String hdfsResourceDir = "hdfs://" + resourceDir + File.separator;
    if (interpreterConf == null) {
      StringBuilder interpreter_json = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.INTERPRETER_TEMPLATE,
                      "projectName", this.projectName,
                      "zeppelin_home_dir", home,
                      "livy_url", settings.getLivyUrl(),
                      "metrics-properties_local_path", "./metrics.properties",
                      "metrics-properties_path", metricsPath + "," + log4jPath,
                      "extra_spark_java_options", extraSparkJavaOptions,
                      "spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
                      "spark.yarn.stagingDir", hdfsResourceDir,
                      "livy.spark.sql.warehouse.dir", hdfsResourceDir + "spark-warehouse",
                      "livy.spark.yarn.stagingDir", hdfsResourceDir,
                      "zeppelin.python_conda_path", zeppelinPythonPath
              );
      interpreterConf = interpreter_json.toString();
    }
    createdXml = ConfigFileGenerator.createConfigFile(interpreter_file,
            interpreterConf);

    return createdSh || createdXml || createdLog4j;
  }

  // loads configeration from project specific zeppelin-site.xml
  private ZeppelinConfiguration loadConfig() {
    URL url = null;
    File zeppelinConfig = new File(confDirPath + ZEPPELIN_SITE_XML);
    try {
      url = zeppelinConfig.toURI().toURL();
      LOGGGER.log(Level.INFO, "Load configuration from {0}", url);
      conf = new ZeppelinConfiguration(url);
    } catch (ConfigurationException e) {
      LOGGGER.log(Level.INFO, "Failed to load configuration from " + url
              + " proceeding with a default", e);
      conf = new ZeppelinConfiguration();
    } catch (MalformedURLException ex) {
      LOGGGER.log(Level.INFO, "Malformed URL failed to load configuration from "
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
  public boolean cleanAndRemoveConfDirs() {
    clean();
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
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    File hive_site_xml_file = new File(confDirPath + HIVE_SITE_XML);
    File interpreter_file = new File(confDirPath + INTERPRETER_JSON);
    boolean ret = false;
    if (zeppelin_env_file.exists()) {
      ret = zeppelin_env_file.delete();
    }
    if (zeppelin_site_xml_file.exists()) {
      ret = zeppelin_site_xml_file.delete();
    }
    if (hive_site_xml_file.exists()) {
      ret = hive_site_xml_file.delete();
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
  public void clean() {
    LOGGGER.log(Level.INFO, "Cleanup of zeppelin resources for project {0}",
            this.projectName);
    if (this.replFactory != null) {
      this.replFactory.close();
    }

    // will close repo and index
    if (this.notebook != null) {
      this.notebook.close();
    }
    if (this.notebookServer != null) {
      this.notebookServer.closeConnection();
    }
    this.schedulerFactory = null;
    //this.notebookServer = null;
    this.replFactory = null;
    this.notebook = null;
  }
}
