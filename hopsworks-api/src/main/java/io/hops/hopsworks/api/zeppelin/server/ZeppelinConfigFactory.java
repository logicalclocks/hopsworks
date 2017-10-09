package io.hops.hopsworks.api.zeppelin.server;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImpl;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImplFactory;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfFacade;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.sonatype.aether.RepositoryException;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ZeppelinConfigFactory {

  private static final Logger LOGGER = Logger.getLogger(ZeppelinConfigFactory.class.getName());
  private static final String ZEPPELIN_SITE_XML = "/conf/zeppelin-site.xml";
  private final ConcurrentMap<String, ZeppelinConfig> projectConfCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> projectCacheLastRestart = new ConcurrentHashMap<>();

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsername;
  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;
  @EJB
  private NotebookServerImplFactory notebookServerImplFactory;
  
  @PostConstruct
  public void init() {
    ZeppelinConfig.COMMON_CONF = loadConfig();
    checkInterpreterJsonValidity();
  }

  @PreDestroy
  public void preDestroy() {
    for (ZeppelinConfig conf : projectConfCache.values()) {
      conf.clean(notebookServerImplFactory);
    }
    projectConfCache.clear();
    projectCacheLastRestart.clear();
  }

  public ZeppelinConfig getProjectConf(String projectName) {
    return projectConfCache.get(projectName);
  }

  /**
   * Returns conf for the project. This can not be used to call
   * notebook server, replFactory or notebook b/c a user specific notebook
   * server
   * connection is needed to create those.
   *
   * @param projectName
   * @return
   */
  public ZeppelinConfig getZeppelinConfig(String projectName, NotebookServerImpl nbs) throws IOException,
      RepositoryException, TaskRunnerException {
    // User is null when Hopsworks checks for running interpreters
    ZeppelinConfig config = projectConfCache.get(projectName);
    if (config == null) {

      Project project = projectBean.findByName(projectName);
      if (project == null) {
        return null;
      }
      String owner = hdfsUsername.getHdfsUserName(project, project.getOwner());
      ZeppelinInterpreterConfs interpreterConf = zeppelinInterpreterConfFacade.findByName(projectName);
      String conf = null;
      if (interpreterConf != null) {
        conf = interpreterConf.getIntrepeterConf();
      }
      config = new ZeppelinConfig(projectName, project.getId(),
          owner, settings, conf, nbs);
      projectConfCache.put(projectName, config);
    }
    config.setNotebookServer(nbs);
    return config;
  }

  /**
   * Removes last restart time for projectName.
   *
   * @param projectName
   */
  public void removeFromCache(String projectName) {
    ZeppelinConfig config = projectConfCache.remove(projectName);
    if (config != null) {
      config.clean(notebookServerImplFactory);
      projectCacheLastRestart.put(projectName, System.currentTimeMillis());
    }
  }


  /**
   * Last restart time for the given project
   *
   * @param projectName
   * @return
   */
  public Long getLastRestartTime(String projectName) {
    return projectCacheLastRestart.get(projectName);
  }

  /**
   * Deletes zeppelin configuration dir for projectName.
   *
   * @param project
   * @return
   */
  public boolean deleteZeppelinConfDir(Project project) throws IOException, RepositoryException, TaskRunnerException {
    ZeppelinConfig conf = projectConfCache.remove(project.getName());
    if (conf != null) {
      return conf.cleanAndRemoveConfDirs(notebookServerImplFactory);
    }
    String projectDirPath = settings.getZeppelinDir() + File.separator
        + Settings.DIR_ROOT + File.separator + project.getName();
    File projectDir = new File(projectDirPath);
    String hdfsUser = hdfsUsername.getHdfsUserName(project, project.getOwner());
    if (projectDir.exists()) {
      conf = new ZeppelinConfig(project.getName(), project.getId(), hdfsUser, settings, null, null);
      return conf.cleanAndRemoveConfDirs(notebookServerImplFactory);
    }
    return false;
  }

  private ZeppelinConfiguration loadConfig() {
    ZeppelinConfiguration conf;
    URL url = null;
    File zeppelinConfig = new File(settings.getZeppelinDir() + ZEPPELIN_SITE_XML);
    try {
      if (!zeppelinConfig.exists()) {
        StringBuilder zeppelin_site_xml = ConfigFileGenerator.
            instantiateFromTemplate(ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
                "zeppelin_home", settings.getZeppelinDir(),
                "zeppelin_home_dir", settings.getZeppelinDir(),
                "livy_url", settings.getLivyUrl(),
                "livy_master", settings.getLivyYarnMode(),
                "zeppelin_notebook_dir", "");
        ConfigFileGenerator.createConfigFile(zeppelinConfig, zeppelin_site_xml.toString());
      }
    } catch (IOException e) {
      LOGGER.log(Level.INFO, "Could not create zeppelin-site.xml at {0}", url);
    }
    try {
      url = zeppelinConfig.toURI().toURL();
      LOGGER.log(Level.INFO, "Load configuration from {0}", url);
      conf = new ZeppelinConfiguration(url);
    } catch (ConfigurationException e) {
      LOGGER.log(Level.INFO, "Failed to load configuration from " + url + " proceeding with a default", e);
      conf = new ZeppelinConfiguration();
    } catch (MalformedURLException ex) {
      LOGGER.
          log(Level.INFO, "Malformed URL failed to load configuration from " + url + " proceeding with a default", ex);
      conf = new ZeppelinConfiguration();
    }
    return conf;
  }

  /**
   * Check if interpreter json is valid.
   *
   */
  public void checkInterpreterJsonValidity() {
    String json = ConfigFileGenerator.getZeppelinDefaultInterpreterJson();
    if (json == null || json.isEmpty()) {
      LOGGER.log(Level.SEVERE, "Could not read Zeppelin default interpreter json.");
      throw new IllegalStateException("Could not read default interpreter json.");
    }
    if (!zeppelinResource.isJSONValid(json)) {
      LOGGER.log(Level.SEVERE, "Zeppelin Default interpreter json not valid.");
      throw new IllegalStateException("Default interpreter json not valid.");
    }
  }

}
