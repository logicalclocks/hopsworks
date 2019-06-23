/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.tensorflow.TfLibMappingUtil;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SparkConfigurationUtil;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to generate the Configuration Files for a Jupyter Notebook Server
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterConfigFilesGenerator {
  
  private static final Logger LOGGER = Logger.getLogger(JupyterConfigFilesGenerator.class.getName());
  private static final String JUPYTER_NOTEBOOK_CONFIG = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_KERNEL = "/kernel.json";
  private static final String JUPYTER_CUSTOM_JS = "/custom/custom.js";
  private static final String SPARKMAGIC_CONFIG = "/config.json";
  
  @EJB
  private Settings settings;
  @EJB
  private TfLibMappingUtil tfLibMappingUtil;
  @EJB
  private JWTController jwtController;
  
  public JupyterPaths generateConfiguration(Project project, String secretConfig, String hdfsUser, String usersFullName,
    String nameNodeEndpoint, JupyterSettings js, Integer port, String allowOrigin)
    throws ServiceException {
    boolean newDir = false;
    
    JupyterPaths jp = new JupyterPaths(settings.getJupyterDir(), project.getName(), hdfsUser, secretConfig);
    
    try {
      newDir = createJupyterDirs(jp);
      createConfigFiles(jp, hdfsUser, usersFullName, project, nameNodeEndpoint,
        port, js, allowOrigin);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectUserDirRecursive(jp);
      }
      LOGGER.log(Level.SEVERE,
        "Error in initializing JupyterConfig for project: {0}. {1}",
        new Object[]{project.getName(), e});
      
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_ADD_FAILURE, Level.SEVERE, null,
        e.getMessage(), e);
    }
    
    return jp;
  }
  
  //returns true if the project dir was created
  private boolean createJupyterDirs(JupyterPaths jp) throws IOException {
    File projectDir = new File(jp.getProjectUserPath());
    projectDir.mkdirs();
    File baseDir = new File(jp.getNotebookPath());
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
    perms.add(PosixFilePermission.OTHERS_EXECUTE);
    
    Files.setPosixFilePermissions(Paths.get(jp.getNotebookPath()), perms);
    Files.setPosixFilePermissions(Paths.get(jp.getProjectUserPath()), xOnly);
    
    new File(jp.getConfDirPath() + "/custom").mkdirs();
    new File(jp.getRunDirPath()).mkdirs();
    new File(jp.getLogDirPath()).mkdirs();
    new File(jp.getCertificatesDir()).mkdirs();
    new File(jp.getKernelsDir()).mkdirs();
    return true;
  }
  
  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(JupyterPaths jp, String hdfsUser,
    String usersFullName,
    Project project, String nameNodeEndpoint, Integer port, JupyterSettings js, String allowOrigin)
    throws IOException, ServiceException {
    String confDirPath = jp.getConfDirPath();
    String kernelsDir = jp.getKernelsDir();
    String certsDir = jp.getCertificatesDir();
    File jupyter_config_file = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    File sparkmagic_config_file = new File(confDirPath + SPARKMAGIC_CONFIG);
    File custom_js = new File(confDirPath + JUPYTER_CUSTOM_JS);
    boolean createdJupyter = false;
    boolean createdSparkmagic = false;
    boolean createdCustomJs = false;
    SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration) js.getJobConfig();
    
    if (!jupyter_config_file.exists()) {
      
      String[] nn = nameNodeEndpoint.split(":");
      String nameNodeIp = nn[0];
      String nameNodePort = nn[1];
      
      String pythonKernelName = "python-" + hdfsUser.toLowerCase();
      
      if (settings.isPythonKernelEnabled() && !project.getPythonVersion().contains("X")) {
        String pythonKernelPath = kernelsDir + File.separator + pythonKernelName;
        File pythonKernelFile = new File(pythonKernelPath + JUPYTER_CUSTOM_KERNEL);
        
        new File(pythonKernelPath).mkdir();
        // Create the python kernel
        StringBuilder jupyter_kernel_config = ConfigFileGenerator.
          instantiateFromTemplate(
            ConfigFileGenerator.JUPYTER_CUSTOM_KERNEL,
            "hdfs_user", hdfsUser,
            "hadoop_home", settings.getHadoopSymbolicLinkDir(),
            "hadoop_version", settings.getHadoopVersion(),
            "anaconda_home", settings.getAnacondaProjectDir(project),
            "secret_dir", settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret(),
            "project_name", project.getName(),
            "hive_endpoint", settings.getHiveServerHostName(false)
          );
        ConfigFileGenerator.createConfigFile(pythonKernelFile, jupyter_kernel_config.toString());
      }
      
      StringBuilder jupyter_notebook_config = ConfigFileGenerator.
        instantiateFromTemplate(
          ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
          "project", project.getName(),
          "namenode_ip", nameNodeIp,
          "namenode_port", nameNodePort,
          "hopsworks_endpoint", this.settings.getRestEndpoint(),
          "elastic_endpoint", this.settings.getElasticEndpoint(),
          "port", port.toString(),
          "base_dir", js.getBaseDir(),
          "hdfs_user", hdfsUser,
          "python-kernel", ", '" + pythonKernelName + "'",
          "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
          "hdfs_home", this.settings.getHadoopSymbolicLinkDir(),
          "jupyter_certs_dir", certsDir,
          "secret_dir", this.settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret(),
          "allow_origin", allowOrigin,
          "ws_ping_interval", String.valueOf(settings.getJupyterWSPingInterval()),
          "hopsworks_project_id", Integer.toString(project.getId())
        );
      createdJupyter = ConfigFileGenerator.createConfigFile(jupyter_config_file,
        jupyter_notebook_config.toString());
    }
    if (!sparkmagic_config_file.exists()) {
      
      // Get information about which version of TensorFlow the user is running
      String tfLdLibraryPath = tfLibMappingUtil.getTfLdLibraryPath(project);
      
      SparkConfigurationUtil sparkConfigurationUtil = new SparkConfigurationUtil();
      Map<String, String> extraJavaOptions = new HashMap<>();
      
      extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO, project.getName().toLowerCase() + ",jupyter,notebook,?");

      HashMap<String, String> finalSparkConfiguration = new HashMap<>();

      finalSparkConfiguration.put(Settings.SPARK_DRIVER_STAGINGDIR_ENV,
        "hdfs:///Projects/" + project.getName() + "/Resources");
      
      finalSparkConfiguration.putAll(sparkConfigurationUtil.setFrameworkProperties(project, sparkJobConfiguration,
        settings, hdfsUser, usersFullName, tfLdLibraryPath, extraJavaOptions));
      StringBuilder sparkConfBuilder = new StringBuilder();
      ArrayList<String> keys = new ArrayList<>(finalSparkConfiguration.keySet());
      Collections.sort(keys);
      
      for (String configKey : keys) {
        sparkConfBuilder.append("\t\"" + configKey + "\":\"" + finalSparkConfiguration.get(configKey) + "\"," + "\n");
      }
      sparkConfBuilder.deleteCharAt(sparkConfBuilder.lastIndexOf(","));
      
      HashMap<String, String> replacementMap = new HashMap<>();
      replacementMap.put("livy_ip", settings.getLivyIp());
      replacementMap.put("jupyter_home", confDirPath);
      replacementMap.put("driver_cores", finalSparkConfiguration.get(Settings.SPARK_DRIVER_CORES_ENV));
      replacementMap.put("driver_memory", finalSparkConfiguration.get(Settings.SPARK_DRIVER_MEMORY_ENV));
      if (sparkJobConfiguration.isDynamicAllocationEnabled() || sparkJobConfiguration.getExperimentType() != null) {
        replacementMap.put("num_executors", "1");
      } else {
        replacementMap.put("num_executors", finalSparkConfiguration.get(Settings.SPARK_NUMBER_EXECUTORS_ENV));
      }
      replacementMap.put("executor_cores", finalSparkConfiguration.get(Settings.SPARK_EXECUTOR_CORES_ENV));
      replacementMap.put("executor_memory", finalSparkConfiguration.get(Settings.SPARK_EXECUTOR_MEMORY_ENV));
      replacementMap.put("hdfs_user", hdfsUser);
      if (sparkJobConfiguration.getExperimentType() != null) {
        replacementMap.put("spark_magic_name", Settings.JUPYTER_SPARKMAGIC_PREFIX +
          sparkJobConfiguration.getExperimentType());
      } else if (sparkJobConfiguration.isDynamicAllocationEnabled()) {
        replacementMap.put("spark_magic_name", Settings.JUPYTER_SPARKMAGIC_PREFIX + "sparkDynamic");
      } else {
        replacementMap.put("spark_magic_name", Settings.JUPYTER_SPARKMAGIC_PREFIX + "sparkStatic");
      }
      replacementMap.put("yarn_queue", sparkJobConfiguration.getAmQueue());
      replacementMap.put("hadoop_home", this.settings.getHadoopSymbolicLinkDir());
      replacementMap.put("hadoop_version", this.settings.getHadoopVersion());
      replacementMap.put("spark_configuration", sparkConfBuilder.toString());
      
      StringBuilder sparkmagic_sb
        = ConfigFileGenerator.
        instantiateFromTemplate(
          ConfigFileGenerator.SPARKMAGIC_CONFIG_TEMPLATE,
          replacementMap);
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
  
  
  private void removeProjectUserDirRecursive(JupyterPaths jp) {
    try {
      FileUtils.deleteDirectory(new File(jp.getProjectUserPath()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not delete Jupyter directory: " + jp.getProjectUserPath(), e);
    }
  }
}
