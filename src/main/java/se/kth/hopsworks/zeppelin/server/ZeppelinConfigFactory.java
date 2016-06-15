package se.kth.hopsworks.zeppelin.server;

import java.io.File;
import java.io.FileNotFoundException;
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
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.ConfigFileGenerator;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.zeppelin.socket.NotebookServer;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ZeppelinConfigFactory {

  private static final Logger LOGGGER = Logger.getLogger(
          ZeppelinConfigFactory.class.getName());
  private static final String ZEPPELIN_SITE_XML = "/conf/zeppelin-site.xml";
  private final ConcurrentMap<String, ZeppelinConfig> projectConfCache
          = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ZeppelinConfig> projectUserConfCache
          = new ConcurrentHashMap<>();
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsername;

  @PostConstruct
  public void init() {
    ZeppelinConfig.COMMON_CONF = loadConfig();
  }

  @PreDestroy
  public void preDestroy() {
    for (ZeppelinConfig conf : projectUserConfCache.values()) {
      conf.clean();
    }
    for (ZeppelinConfig conf : projectConfCache.values()) {
      conf.clean();
    }
    projectConfCache.clear();
    projectUserConfCache.clear();
  }

  /**
   * Returns a unique zeppelin configuration for the project user.
   *
   * @param projectName
   * @param username
   * @param nbs
   * @return null if the project does not exist.
   */
  public ZeppelinConfig getZeppelinConfig(String projectName, String username,
          NotebookServer nbs) {
    ZeppelinConfig config = getprojectConf(projectName);
    Project project = projectBean.findByName(projectName);
    Users user = userFacade.findByEmail(username);
    if (project == null || user == null) {
      return null;
    }
    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    ZeppelinConfig userConfig = projectUserConfCache.get(hdfsUser);
    if (userConfig != null) {
      return userConfig;
    }
    userConfig = new ZeppelinConfig(config, nbs);
    projectUserConfCache.put(hdfsUser, userConfig);
    return userConfig;
  }
  
  /**
   * Returns a unique zeppelin configuration for the project user. Null is 
   * returned when the user is not connected to web socket.
   * @param projectName
   * @param username
   * @return null if there is no configuration for the user
   */
  public ZeppelinConfig getZeppelinConfig(String projectName, String username) {
    Project project = projectBean.findByName(projectName);
    Users user = userFacade.findByEmail(username);
    if (project == null || user == null) {
      return null;
    }
    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    ZeppelinConfig userConfig = projectUserConfCache.get(hdfsUser);
    return userConfig;
  }

  /**
   * Returns conf for the project. This can not be used to call 
   * notebook server, replFactory or notebook b/c a user specific notebook server
   * connection is needed to create those.
   * @param projectName
   * @return 
   */
  public ZeppelinConfig getprojectConf(String projectName) {
    ZeppelinConfig config = projectConfCache.get(projectName);
    if (config != null) {
      return config;
    }
    Project project = projectBean.findByName(projectName);
    if (project == null) {
      return null;
    }
    config = new ZeppelinConfig(projectName, settings);
    projectConfCache.put(projectName, config);
    return projectConfCache.get(projectName);
  }

  /**
   * Removes configuration for projectName. This will force it to be recreated
   * next time it is accessed.
   *
   * @param projectName
   */
  public void removeFromCache(String projectName) {
    projectConfCache.remove(projectName);
  }
  
  /**
   * Remove user configuration from cache. 
   * @param projectName
   * @param username 
   */
  public void removeFromCache(String projectName, String username) {
    Project project = projectBean.findByName(projectName);
    Users user = userFacade.findByEmail(username);
    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    projectUserConfCache.remove(hdfsUser);
  }

  /**
   * Deletes zeppelin configuration dir for projectName.
   *
   * @param projectName
   * @return
   */
  public boolean deleteZeppelinConfDir(String projectName) {
    ZeppelinConfig conf = projectConfCache.remove(projectName);
    if (conf != null) {
      return conf.cleanAndRemoveConfDirs();
    }
    String projectDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + projectName;
    File projectDir = new File(projectDirPath);
    boolean ret = true;
    try {
      ret = ConfigFileGenerator.deleteRecursive(projectDir);
    } catch (FileNotFoundException ex) {
    }
    return ret;
  }

  private ZeppelinConfiguration loadConfig() {
    ZeppelinConfiguration conf;
    URL url = null;
    File zeppelinConfig
            = new File(settings.getZeppelinDir() + ZEPPELIN_SITE_XML);
    try {
      if (!zeppelinConfig.exists()) {
        StringBuilder zeppelin_site_xml = ConfigFileGenerator.
                instantiateFromTemplate(
                        ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
                        "zeppelin_home", settings.getZeppelinDir(),
                        "livy_url", settings.getLivyUrl(),
                        "livy_master", settings.getLivyYarnMode(),
                        "zeppelin_home_dir", "");
        ConfigFileGenerator.createConfigFile(zeppelinConfig, zeppelin_site_xml.
                toString());
      }
    } catch (IOException e) {
      LOGGGER.log(Level.INFO, "Could not create zeppelin-site.xml at {0}", url);
    }
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

}
