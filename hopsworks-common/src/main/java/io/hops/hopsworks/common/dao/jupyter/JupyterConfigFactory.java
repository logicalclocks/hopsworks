package io.hops.hopsworks.common.dao.jupyter;

import io.hops.hopsworks.api.zeppelin.socket.NotebookServer;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
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
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.ConfigurationException;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JupyterConfigFactory {

  private static final Logger LOGGGER = Logger.getLogger(
          JupyterConfigFactory.class.getName());
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "conf/jupyter_notebook_config.py";

  private final ConcurrentMap<String, JupyterConfig> userConfCache
          = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, Process> runningServers
          = new ConcurrentHashMap<>();

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsername;
  @EJB
  private JupyterFacade jupyterFacade;
//  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;

  @PostConstruct
  public void init() {
    JupyterConfig.COMMON_CONF = loadConfig();
  }

  @PreDestroy
  public void preDestroy() {
    for (JupyterConfig conf : userConfCache.values()) {
      conf.clean();
    }
    userConfCache.clear();
  }

  /**
   * If an existing process is running for this username, kill it.
   * Starts a new process with that username.
   *
   * @param hdfsUsername
   * @param process
   * @return
   */
  public void addNotebookServer(String hdfsUsername,
          Process process) throws AppException {
    if (!process.isAlive()) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Jupyter server died unexpectadly");
    }
    removeNotebookServer(hdfsUsername);
    runningServers.put(hdfsUsername, process);

    BufferedReader br = new BufferedReader(new InputStreamReader(
            process.getInputStream(), Charset.forName("UTF8")));

//    consoleOutput.put(hdfsUsername, br);
  }

  public boolean removeNotebookServer(String hdfsUsername) {
    if (runningServers.containsKey(hdfsUsername)) {
      Process oldProcess = runningServers.get(hdfsUsername);
      if (oldProcess != null) {
        oldProcess.destroyForcibly();
        return true;
      }
      runningServers.remove(hdfsUsername);
//      BufferedReader br = consoleOutput.get(hdfsUsername);
//      if (br != null) {
//        try {
//          br.close();
//        } catch (IOException ex) {
//          Logger.getLogger(JupyterConfig.class.getName()).
//                  log(Level.SEVERE, null, ex);
//        }
//        consoleOutput.remove(hdfsUsername);
//      }
    }
    return false;
  }

  /**
   * Returns a unique zeppelin configuration for the project user.
   *
   * @param projectName
   * @param username
   * @param nbs
   * @return null if the project does not exist.
   */
  public JupyterConfig getJupyterConfig(String username) {
    JupyterConfig config = getUserConf(username);
    Project project = projectBean.findByName(projectName);
    Users user = userFacade.findByEmail(username);
    if (project == null || user == null) {
      return null;
    }
    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    JupyterConfig userConfig = projectUserConfCache.get(hdfsUser);
    if (userConfig != null) {
      return userConfig;
    }
    userConfig = new JupyterConfig(config, nbs);
    projectUserConfCache.put(hdfsUser, userConfig);
    return userConfig;
  }

  /**
   * Returns a unique zeppelin configuration for the project user. Null is
   * returned when the user is not connected to web socket.
   *
   * @param projectName
   * @param username
   * @return null if there is no configuration for the user
   */
  public JupyterConfig getJupyterConfig(String projectName, String username) {
    Project project = projectBean.findByName(projectName);
    Users user = userFacade.findByEmail(username);
    if (project == null || user == null) {
      return null;
    }
    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    JupyterConfig userConfig = projectUserConfCache.get(hdfsUser);
    return userConfig;
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
  public JupyterConfig getUserConf(String username) {
    JupyterConfig config = userConfCache.get(username);
    if (config != null) {
      return config;
    }
//    ZeppelinInterpreterConfs interpreterConf = zeppelinInterpreterConfFacade.
//            findByName(projectName);
//    String conf = null;
//    if (interpreterConf != null) {
//      conf = interpreterConf.getIntrepeterConf();
//    }
    config = new JupyterConfig(username, settings);
    userConfCache.put(username, config);
    return userConfCache.get(username);
  }

  /**
   * Removes last restart time for projectName.
   *
   * @param projectName
   */
  public void removeFromCache(String projectName) {
    JupyterConfig config = userConfCache.remove(projectName);
    if (config != null) {
      config.clean();
      projectCacheLastRestart.put(projectName, System.currentTimeMillis());
    }
  }

  /**
   * Remove user configuration from cache.
   *
   * @param projectName
   * @param username
   */
  public void removeFromCache(String username) {
//    Project project = projectBean.findByName(projectName);
//    Users user = userFacade.findByEmail(username);
//    HdfsUsers user = hdfsUsername.get(username);
//    if (project == null || user == null) {
//      return;
//    }
//    if (project == null || user == null) {
//      return;
//    }
//    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    JupyterConfig config = projectUserConfCache.remove(hdfsUser);
    if (config != null) {
      config.clean();
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
  public boolean deleteJupyterConfDir(Project project) {
    JupyterConfig conf = userConfCache.remove(project.getName());
    if (conf != null) {
      return conf.cleanAndRemoveConfDirs();
    }
    String projectDirPath = settings.getZeppelinDir() + File.separator
            + Settings.DIR_ROOT + File.separator + project.getName();
    File projectDir = new File(projectDirPath);
    String hdfsUser = hdfsUsername.getHdfsUserName(project, project.getOwner());
    if (projectDir.exists()) {
      conf = new JupyterConfig(project.getName(), hdfsUser, settings, null);
      return conf.cleanAndRemoveConfDirs();
    }
    return false;
  }

  private JupyterConfiguration loadConfig() {
    JupyterConfiguration conf;
    URL url = null;
    File jupyterConfig
            = new File(settings.getZeppelinDir() + JUPYTER_NOTEBOOK_CONFIG);
    try {
      if (!jupyterConfig.exists()) {
        StringBuilder zeppelin_site_xml = ConfigFileGenerator.
                instantiateFromTemplate(
                        ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
                        "jupyter_home", settings.getJupyterDir()
                );
        ConfigFileGenerator.createConfigFile(jupyterConfig, zeppelin_site_xml.
                toString());
      }
    } catch (IOException e) {
      LOGGGER.log(Level.INFO, "Could not create zeppelin-site.xml at {0}", url);
    }
    try {
      url = jupyterConfig.toURI().toURL();
      LOGGGER.log(Level.INFO, "Load configuration from {0}", url);
      conf = new JupyterConfiguration(url);
    } catch (ConfigurationException e) {
      LOGGGER.log(Level.INFO, "Failed to load configuration from " + url
              + " proceeding with a default", e);
      conf = new JupyterConfiguration();
    } catch (MalformedURLException ex) {
      LOGGGER.log(Level.INFO, "Malformed URL failed to load configuration from "
              + url
              + " proceeding with a default", ex);
      conf = new JupyterConfiguration();
    }
    return conf;
  }

}
