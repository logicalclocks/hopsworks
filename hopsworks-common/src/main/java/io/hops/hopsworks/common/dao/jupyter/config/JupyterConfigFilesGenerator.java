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

import freemarker.template.TemplateException;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jupyter.JupyterContentsManager;
import io.hops.hopsworks.common.jupyter.JupyterNbVCSController;
import io.hops.hopsworks.common.tensorflow.TfLibMappingUtil;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SparkConfigurationUtil;
import io.hops.hopsworks.common.util.TemplateEngine;
import io.hops.hopsworks.common.util.templates.jupyter.JupyterNotebookConfigTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.JupyterNotebookConfigTemplateBuilder;
import io.hops.hopsworks.common.util.templates.jupyter.KernelTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.KernelTemplateBuilder;
import io.hops.hopsworks.common.util.templates.jupyter.SparkMagicConfigTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.SparkMagicConfigTemplateBuilder;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
  
  @EJB
  private Settings settings;
  @EJB
  private TfLibMappingUtil tfLibMappingUtil;
  @Inject
  private JupyterNbVCSController jupyterNbVCSController;
  @EJB
  private TemplateEngine templateEngine;
  
  public JupyterPaths generateJupyterPaths(Project project, String hdfsUser, String secretConfig) {
    return new JupyterPaths(settings.getJupyterDir(), project.getName(), hdfsUser, secretConfig);
  }
  
  public JupyterPaths generateConfiguration(Project project, String secretConfig, String hdfsUser,
    String nameNodeEndpoint, JupyterSettings js, Integer port, String allowOrigin)
    throws ServiceException {
    boolean newDir = false;
    
    JupyterPaths jp = generateJupyterPaths(project, hdfsUser, secretConfig);
    
    try {
      newDir = createJupyterDirs(jp);
      createConfigFiles(jp, hdfsUser, project, nameNodeEndpoint,
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
    
    new File(jp.getConfDirPath()).mkdirs();
    new File(jp.getRunDirPath()).mkdirs();
    new File(jp.getLogDirPath()).mkdirs();
    new File(jp.getCertificatesDir()).mkdirs();
    new File(jp.getKernelsDir()).mkdirs();
    return true;
  }
  
  public String pythonKernelName(String pythonVersion) {
    return "python" + pythonVersion.charAt(0);
  }

  
  public String pythonKernelPath(String kernelsDir, String pythonKernelName) {
    return kernelsDir + File.separator + pythonKernelName;
  }
  
  public void createJupyterKernelConfig(Writer out, Project project, JupyterSettings js, String hdfsUser)
      throws IOException {
    KernelTemplate kernelTemplate = KernelTemplateBuilder.newBuilder()
        .setHdfsUser(hdfsUser)
        .setHadoopHome(settings.getHadoopSymbolicLinkDir())
        .setHadoopVersion(settings.getHadoopVersion())
        .setAnacondaHome(settings.getAnacondaProjectDir(project))
        .setSecretDirectory(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret())
        .setProject(project)
        .setHiveEndpoints(settings.getHiveServerHostName(false))
        .setLibHdfsOpts("-Xmx512m")
        .build();
    
    Map<String, Object> dataModel = new HashMap<>(1);
    dataModel.put("kernel", kernelTemplate);
    
    try {
      templateEngine.template(KernelTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException ex) {
      throw new IOException(ex);
    }
  }
  
  public void createJupyterNotebookConfig(Writer out, Project project, String nameNodeEndpoint, int port,
      JupyterSettings js, String hdfsUser, String pythonKernelName, String certsDir, String allowOrigin)
        throws IOException, ServiceException {
    String[] nn = nameNodeEndpoint.split(":");
    String nameNodeIp = nn[0];
    String nameNodePort = nn[1];
  
    String remoteGitURL = "";
    String apiKey = "";
    if (js.isGitBackend() && js.getGitConfig() != null) {
      remoteGitURL = js.getGitConfig().getRemoteGitURL();
      apiKey = jupyterNbVCSController.getGitApiKey(hdfsUser, js.getGitConfig().getApiKeyName());
    }
    JupyterContentsManager jcm = jupyterNbVCSController.getJupyterContentsManagerClass(remoteGitURL);
    JupyterNotebookConfigTemplate template = JupyterNotebookConfigTemplateBuilder.newBuilder()
        .setProject(project)
        .setNamenodeIp(nameNodeIp)
        .setNamenodePort(nameNodePort)
        .setContentsManager(jcm.getClassName())
        .setHopsworksEndpoint(settings.getRestEndpoint())
        .setElasticEndpoint(settings.getElasticEndpoint())
        .setPort(port)
        .setBaseDirectory(js.getBaseDir())
        .setHdfsUser(hdfsUser)
        .setWhiteListedKernels("'" + pythonKernelName(js.getProject().getPythonVersion()) +
            "', 'pysparkkernel', 'sparkkernel', 'sparkrkernel'")
        .setHadoopHome(settings.getHadoopSymbolicLinkDir())
        .setJupyterCertsDirectory(certsDir)
        .setSecretDirectory(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret())
        .setAllowOrigin(allowOrigin)
        .setWsPingInterval(settings.getJupyterWSPingInterval())
        .setApiKey(apiKey)
        .setFlinkConfDirectory(settings.getFlinkConfDir())
        .setRequestsVerify(settings.getRequestsVerify())
        .setDomainCATruststorePem(settings.getSparkConfDir() + File.separator + Settings.DOMAIN_CA_TRUSTSTORE_PEM)
        .build();
    Map<String, Object> dataModel = new HashMap<>(1);
    dataModel.put("conf", template);
    try {
      templateEngine.template(JupyterNotebookConfigTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException ex) {
      throw new IOException(ex);
    }
  }

  public void createSparkMagicConfig(Writer out, Project project, JupyterSettings js, String hdfsUser,
      String confDirPath) throws IOException {
    
    SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration) js.getJobConfig();
    
    // Get information about which version of TensorFlow the user is running
    String tfLdLibraryPath = tfLibMappingUtil.getTfLdLibraryPath(project);
  
    SparkConfigurationUtil sparkConfigurationUtil = new SparkConfigurationUtil();
    Map<String, String> extraJavaOptions = new HashMap<>();
  
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO, project.getName().toLowerCase() + ",jupyter,notebook,?");
  
    HashMap<String, String> finalSparkConfiguration = new HashMap<>();
  
    finalSparkConfiguration.put(Settings.SPARK_DRIVER_STAGINGDIR_ENV,
      "hdfs:///Projects/" + project.getName() + "/Resources");

    finalSparkConfiguration.putAll(sparkConfigurationUtil.setFrameworkProperties(project, sparkJobConfiguration,
      settings, hdfsUser, tfLdLibraryPath, extraJavaOptions));
    
    StringBuilder sparkConfBuilder = new StringBuilder();
    ArrayList<String> keys = new ArrayList<>(finalSparkConfiguration.keySet());
    Collections.sort(keys);
  
    for (String configKey : keys) {
      sparkConfBuilder.append("\t\"" + configKey + "\":\"" + finalSparkConfiguration.get(configKey) + "\"," + "\n");
    }
    sparkConfBuilder.deleteCharAt(sparkConfBuilder.lastIndexOf(","));
  
    SparkMagicConfigTemplateBuilder templateBuilder = SparkMagicConfigTemplateBuilder.newBuilder()
        .setLivyIp(settings.getLivyIp())
        .setJupyterHome(confDirPath)
        .setDriverCores(Integer.parseInt(finalSparkConfiguration.get(Settings.SPARK_DRIVER_CORES_ENV)))
        .setDriverMemory(finalSparkConfiguration.get(Settings.SPARK_DRIVER_MEMORY_ENV));
    if (sparkJobConfiguration.isDynamicAllocationEnabled() || sparkJobConfiguration.getExperimentType() != null) {
      templateBuilder.setNumExecutors(1);
    } else {
      templateBuilder.setNumExecutors(Integer.parseInt(finalSparkConfiguration
          .get(Settings.SPARK_NUMBER_EXECUTORS_ENV)));
    }
    templateBuilder
        .setExecutorCores(Integer.parseInt(finalSparkConfiguration.get(Settings.SPARK_EXECUTOR_CORES_ENV)))
        .setExecutorMemory(finalSparkConfiguration.get(Settings.SPARK_EXECUTOR_MEMORY_ENV))
        .setHdfsUser(hdfsUser)
        .setYarnQueue(sparkJobConfiguration.getAmQueue())
        .setHadoopHome(settings.getHadoopSymbolicLinkDir())
        .setHadoopVersion(settings.getHadoopVersion())
        .setSparkConfiguration(sparkConfBuilder.toString());
    Map<String, Object> dataModel = new HashMap<>(1);
    dataModel.put("conf", templateBuilder.build());
    try {
      templateEngine.template(SparkMagicConfigTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException ex) {
      throw new IOException(ex);
    }
  }

  // returns true if one of the conf files were created anew 
  private void createConfigFiles(JupyterPaths jp, String hdfsUser, Project project,
      String nameNodeEndpoint, Integer port, JupyterSettings js, String allowOrigin)
      throws IOException, ServiceException {
    String confDirPath = jp.getConfDirPath();
    String kernelsDir = jp.getKernelsDir();
    String certsDir = jp.getCertificatesDir();
    File jupyter_config_file = new File(confDirPath, JupyterNotebookConfigTemplate.FILE_NAME);
    File sparkmagic_config_file = new File(confDirPath, SparkMagicConfigTemplate.FILE_NAME);
    
    if (!jupyter_config_file.exists()) {
      String pythonKernelName = pythonKernelName(project.getPythonVersion());
      if (settings.isPythonKernelEnabled()) {
        String pythonKernelPath = pythonKernelPath(kernelsDir, pythonKernelName);
        File pythonKernelFile = new File(pythonKernelPath, KernelTemplate.FILE_NAME);
        
        new File(pythonKernelPath).mkdir();
        // Create the python kernel
        try (Writer out = new FileWriter(pythonKernelFile, false)) {
          createJupyterKernelConfig(out, project, js, hdfsUser);
        }
      }
  
      try (Writer out = new FileWriter(jupyter_config_file, false)) {
        createJupyterNotebookConfig(out, project, nameNodeEndpoint, port, js, hdfsUser,
            pythonKernelName, certsDir, allowOrigin);
      }
    }
    
    if (!sparkmagic_config_file.exists()) {
      try (Writer out = new FileWriter(sparkmagic_config_file, false)) {
        createSparkMagicConfig(out, project, js, hdfsUser, confDirPath);
      }
    }
  }
  
  
  private void removeProjectUserDirRecursive(JupyterPaths jp) {
    try {
      FileUtils.deleteDirectory(new File(jp.getProjectUserPath()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not delete Jupyter directory: " + jp.getProjectUserPath(), e);
    }
  }
}
