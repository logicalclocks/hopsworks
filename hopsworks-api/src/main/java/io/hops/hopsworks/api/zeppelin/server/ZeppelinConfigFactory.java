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
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfFacade;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
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
   * @param projectName project name
   * @param nbs NotebookServer
   * @return ZeppelinConfig object.
   * @throws java.io.IOException IOException
   * @throws org.sonatype.aether.RepositoryException RepositoryException
   * @throws com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException TaskRunnerException
   */
  public ZeppelinConfig getZeppelinConfig(String projectName, NotebookServerImpl nbs) throws IOException,
    RepositoryException, TaskRunnerException, InterruptedException {
    // User is null when Hopsworks checks for running interpreters
    ZeppelinConfig config = projectConfCache.get(projectName);
    if (config == null) {

      Project project = projectBean.findByName(projectName);
      if (project == null) {
        return null;
      }
      String owner = hdfsUsername.getHdfsUserName(project, project.getOwner());
      ZeppelinInterpreterConfs interpreterConf = zeppelinInterpreterConfFacade.findByProject(project);
      String conf = null;
      if (interpreterConf != null) {
        conf = interpreterConf.getInterpreterConf();
      }
      config = new ZeppelinConfig(projectName, project.getId(), owner, settings, conf, nbs);
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
  public boolean deleteZeppelinConfDir(Project project)
    throws IOException, RepositoryException, TaskRunnerException, InterruptedException {
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
