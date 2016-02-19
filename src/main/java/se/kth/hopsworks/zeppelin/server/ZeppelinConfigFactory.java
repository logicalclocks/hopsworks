package se.kth.hopsworks.zeppelin.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.util.ConfigFileGenerator;
import se.kth.hopsworks.util.Settings;

@Singleton
public class ZeppelinConfigFactory {

  private static final Logger LOGGGER = Logger.getLogger(
          ZeppelinConfigFactory.class.getName());
  private static final String ZEPPELIN_SITE_XML = "/conf/zeppelin-site.xml";
  private final ConcurrentMap<String, ZeppelinConfig> cache
          = new ConcurrentHashMap<>();
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectBean;

  @PostConstruct
  public void init() {
    ZeppelinConfig.COMMON_CONF = loadConfig();
  }

  @PreDestroy
  public void preDestroy() {
    for (ZeppelinConfig conf : cache.values()) {
      conf.clean();
    }
    cache.clear();
  }

  /**
   * Returns a unique zeppelin configuration for the projectName.
   * @param projectName
   * @return null if the project does not exist.
   */
  public ZeppelinConfig getZeppelinConfig(String projectName) {
    ZeppelinConfig config = cache.get(projectName);
    if (config != null) {
      return config;
    }
    Project project = projectBean.findByName(projectName);
    if (project == null) {
      return null;
    }
    config = new ZeppelinConfig(projectName, settings);
    cache.put(projectName, config);
    return cache.get(projectName);
  }
  
  /**
   * Returns a unique zeppelin configuration for the projectId.
   * @param projectId
   * @return null if the project does not exist.
   */
  public ZeppelinConfig getZeppelinConfig(Integer projectId) {
    Project project;
    project = projectBean.find(projectId);
    if (project == null) {
      return null;
    }
    return getZeppelinConfig(project.getName());
  }

  /**
   * Deletes zeppelin configuration dir for projectName.
   * @param projectName
   * @return 
   */
  public boolean deleteZeppelinConfDir(String projectName) {
    ZeppelinConfig conf = cache.remove(projectName);
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
