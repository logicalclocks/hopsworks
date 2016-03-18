package se.kth.hopsworks.zeppelin.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterFactory;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSync;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import se.kth.hopsworks.util.ConfigFileGenerator;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.zeppelin.socket.NotebookServer;

public class ZeppelinConfig {

  private static final Logger LOGGGER = Logger.getLogger(ZeppelinConfig.class.
          getName());
  private static final String ZEPPELIN_SITE_XML = "/zeppelin-site.xml";
  private static final String ZEPPELIN_ENV_SH = "/zeppelin_env.sh";

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

  public ZeppelinConfig(String projectName, Settings settings) {
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
    try {
      newDir = createZeppelinDirs();//creates the necessary folders for the project in /srv/zeppelin
      newBinDir = copyBinDir();
      createSymLinks();//interpreter and lib
      newFile = createZeppelinConfFiles();//create project specific configurations for zeppelin 
      this.conf = loadConfig();
      this.notebookServer = setupNotebookServer();
      this.schedulerFactory = new SchedulerFactory();
      this.replFactory = new InterpreterFactory(conf, notebookServer);
      this.notebookRepo = new NotebookRepoSync(conf);
      this.notebook = new Notebook(conf, notebookRepo, schedulerFactory,
              replFactory, notebookServer);
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

  public ZeppelinConfiguration getConf() {
    return this.conf;
  }

  private NotebookServer setupNotebookServer() throws Exception {
    NotebookServer server = new NotebookServer();
    return server;
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
  private boolean createZeppelinDirs() {
    File projectDir = new File(projectDirPath);
    boolean newProjectDir = projectDir.mkdirs();
    new File(confDirPath).mkdirs();
    new File(notebookDirPath).mkdirs();
    new File(runDirPath).mkdirs();
    new File(binDirPath).mkdirs();
    new File(logDirPath).mkdirs();
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
  
  //creates sym link to interpreters and libs
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


  // returns true if one of the conf files were created anew 
  private boolean createZeppelinConfFiles() throws IOException {
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    boolean createdSh = false;
    boolean createdXml = false;
    if (!zeppelin_env_file.exists()) {
      StringBuilder zeppelin_env = ConfigFileGenerator.instantiateFromTemplate(
              ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
              "zeppelin_dir", settings.getZeppelinDir(),
              "project_dir", Settings.DIR_ROOT + File.separator + projectName,
              "spark_dir", settings.getSparkDir(),
              "hadoop_dir", settings.getHadoopDir());
      createdSh = ConfigFileGenerator.createConfigFile(zeppelin_env_file,
              zeppelin_env.
              toString());
    }

    if (!zeppelin_site_xml_file.exists()) {
      StringBuilder zeppelin_site_xml = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
                      "zeppelin_dir", settings.getZeppelinDir(),
                      "project_dir", Settings.DIR_ROOT + File.separator
                      + projectName);
      createdXml = ConfigFileGenerator.createConfigFile(zeppelin_site_xml_file,
              zeppelin_site_xml.
              toString());
    }
    return createdSh || createdXml;
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
      //conf = new ZeppelinConfiguration();
    } catch (MalformedURLException ex) {
      LOGGGER.log(Level.INFO, "Malformed URL failed to load configuration from "
              + url
              + " proceeding with a default", ex);
      //conf = new ZeppelinConfiguration();
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
    try {
      ret = ConfigFileGenerator.deleteRecursive(projectDir);
    } catch (FileNotFoundException ex) {
    }
    return ret;
  }

  private boolean removeProjectConfFiles() {
    File zeppelin_env_file = new File(confDirPath + ZEPPELIN_ENV_SH);
    File zeppelin_site_xml_file = new File(confDirPath + ZEPPELIN_SITE_XML);
    boolean ret = false;
    if (zeppelin_env_file.exists()) {
      ret = zeppelin_env_file.delete();
    }
    if (zeppelin_site_xml_file.exists()) {
      ret = zeppelin_site_xml_file.delete();
    }
    return ret;
  }

  /**
   * closes notebook SearchService, Repo, InterpreterFactory, and
   * SchedulerFactory
   */
  public void clean() {
    LOGGGER.log(Level.INFO, "Cleanup of zeppelin resources for project ==> {0}",
            this.projectName);
    if (this.replFactory != null) {
      this.replFactory.close();
    }
    if (this.schedulerFactory != null) {
      this.schedulerFactory.destroy();
    }

  }
}
