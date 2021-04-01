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

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import freemarker.template.TemplateException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.kafka.KafkaBrokers;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jupyter.JupyterContentsManager;
import io.hops.hopsworks.common.jupyter.JupyterNbVCSController;
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
  @Inject
  private JupyterNbVCSController jupyterNbVCSController;
  @EJB
  private TemplateEngine templateEngine;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private KafkaBrokers kafkaBrokers;
  @EJB
  private HiveController hiveController;

  public JupyterPaths generateJupyterPaths(Project project, String hdfsUser, String secretConfig) {
    return new JupyterPaths(settings.getJupyterDir(), project.getName(), hdfsUser, secretConfig);
  }
  
  public JupyterPaths generateConfiguration(Project project, String secretConfig, String hdfsUser,
    JupyterSettings js, Integer port, String allowOrigin)
    throws ServiceException, JobException {
    boolean newDir = false;
    
    JupyterPaths jp = generateJupyterPaths(project, hdfsUser, secretConfig);
    
    try {
      newDir = createJupyterDirs(jp);
      createConfigFiles(jp, hdfsUser, project, port, js, allowOrigin);
    } catch (IOException | ServiceException | ServiceDiscoveryException e) {
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
    try {
      KernelTemplate kernelTemplate = KernelTemplateBuilder.newBuilder()
          .setHdfsUser(hdfsUser)
          .setHadoopHome(settings.getHadoopSymbolicLinkDir())
          .setHadoopVersion(settings.getHadoopVersion())
          .setAnacondaHome(settings.getAnacondaProjectDir())
          .setSecretDirectory(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret())
          .setProject(project)
          .setHiveEndpoints(hiveController.getHiveServerInternalEndpoint())
          .setLibHdfsOpts("-Xmx512m")
          .build();

      Map<String, Object> dataModel = new HashMap<>(1);
      dataModel.put("kernel", kernelTemplate);
      templateEngine.template(KernelTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException | ServiceDiscoveryException ex) {
      throw new IOException(ex);
    }
  }
  
  public void createJupyterNotebookConfig(Writer out, Project project, int port,
      JupyterSettings js, String hdfsUser, String certsDir, String allowOrigin)
        throws IOException, ServiceException, ServiceDiscoveryException {
    Service namenode = serviceDiscoveryController
        .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.RPC_NAMENODE);
    String hopsworksRestEndpoint = "https://" + serviceDiscoveryController
        .constructServiceFQDNWithPort(ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);

    String remoteGitURL = "";
    String apiKey = "";
    String gitBackend = "";
    if (js.isGitBackend() && js.getGitConfig() != null) {
      remoteGitURL = js.getGitConfig().getRemoteGitURL();
      gitBackend = js.getGitConfig().getGitBackend().name();
      if(!Strings.isNullOrEmpty(js.getGitConfig().getApiKeyName())) {
        apiKey = jupyterNbVCSController.getGitApiKey(hdfsUser, js.getGitConfig().getApiKeyName());
      }
    }
    JupyterContentsManager jcm = jupyterNbVCSController.getJupyterContentsManagerClass(remoteGitURL);
    JupyterNotebookConfigTemplate template = JupyterNotebookConfigTemplateBuilder.newBuilder()
        .setProject(project)
        .setNamenodeIp(namenode.getAddress())
        .setNamenodePort(String.valueOf(namenode.getPort()))
        .setContentsManager(jcm.getClassName())
        .setHopsworksEndpoint(hopsworksRestEndpoint)
        .setElasticEndpoint(settings.getElasticEndpoint())
        .setPort(port)
        .setBaseDirectory(js.getBaseDir())
        .setHdfsUser(hdfsUser)
        .setWhiteListedKernels("'" + pythonKernelName(js.getProject().getPythonEnvironment().getPythonVersion()) +
            "', 'pysparkkernel', 'sparkkernel', 'sparkrkernel'")
        .setHadoopHome(settings.getHadoopSymbolicLinkDir())
        .setJupyterCertsDirectory(certsDir)
        .setSecretDirectory(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret())
        .setAllowOrigin(allowOrigin)
        .setWsPingInterval(settings.getJupyterWSPingInterval())
        .setApiKey(apiKey)
        .setGitBackend(gitBackend)
        .setFlinkConfDirectory(settings.getFlinkConfDir())
        .setFlinkLibDirectory(settings.getFlinkLibDir())
        .setHadoopClasspathGlob(settings.getHadoopClasspathGlob())
        .setRequestsVerify(settings.getRequestsVerify())
        .setDomainCATruststore(Paths.get(certsDir, hdfsUser + Settings.TRUSTSTORE_SUFFIX).toString())
        .setServiceDiscoveryDomain(settings.getServiceDiscoveryDomain())
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
      String confDirPath) throws IOException, ServiceDiscoveryException, JobException {
    
    SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration) js.getJobConfig();
  
    SparkConfigurationUtil sparkConfigurationUtil = new SparkConfigurationUtil();
    Map<String, String> extraJavaOptions = new HashMap<>();
  
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO, project.getName().toLowerCase() + ",jupyter,notebook,?");
  
    HashMap<String, String> finalSparkConfiguration = new HashMap<>();
  
    finalSparkConfiguration.put(Settings.SPARK_DRIVER_STAGINGDIR_ENV,
      "hdfs:///Projects/" + project.getName() + "/Resources");

    // Set Hopsworks consul service domain, don't use the address, use the name
    String hopsworksRestEndpoint = "https://" + serviceDiscoveryController.
        constructServiceFQDNWithPort(ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);

    finalSparkConfiguration.putAll(
        sparkConfigurationUtil.setFrameworkProperties(project, sparkJobConfiguration, settings, hdfsUser,
            extraJavaOptions, kafkaBrokers.getKafkaBrokersString(), hopsworksRestEndpoint, serviceDiscoveryController));
    
    StringBuilder sparkConfBuilder = new StringBuilder();
    ArrayList<String> keys = new ArrayList<>(finalSparkConfiguration.keySet());
    Collections.sort(keys);
  
    for (String configKey : keys) {
      sparkConfBuilder.append("\t\"" + configKey + "\":\"" + finalSparkConfiguration.get(configKey) + "\"," + "\n");
    }
    sparkConfBuilder.deleteCharAt(sparkConfBuilder.lastIndexOf(","));
  
    try {
      Service livyService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          ServiceDiscoveryController.HopsworksService.LIVY);
      SparkMagicConfigTemplateBuilder templateBuilder = SparkMagicConfigTemplateBuilder.newBuilder()
          .setLivyIp(livyService.getAddress())
          .setJupyterHome(confDirPath)
          .setDriverCores(Integer.parseInt(finalSparkConfiguration.get(Settings.SPARK_DRIVER_CORES_ENV)))
          .setDriverMemory(finalSparkConfiguration.get(Settings.SPARK_DRIVER_MEMORY_ENV))
          .setLivyStartupTimeout(settings.getLivyStartupTimeout());
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

      templateEngine.template(SparkMagicConfigTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException | ServiceDiscoveryException ex) {
      throw new IOException(ex);
    }
  }

  // returns true if one of the conf files were created anew 
  private void createConfigFiles(JupyterPaths jp, String hdfsUser, Project project,
      Integer port, JupyterSettings js, String allowOrigin)
          throws IOException, ServiceException, ServiceDiscoveryException, JobException {
    String confDirPath = jp.getConfDirPath();
    String kernelsDir = jp.getKernelsDir();
    String certsDir = jp.getCertificatesDir();
    File jupyter_config_file = new File(confDirPath, JupyterNotebookConfigTemplate.FILE_NAME);
    File sparkmagic_config_file = new File(confDirPath, SparkMagicConfigTemplate.FILE_NAME);
    
    if (!jupyter_config_file.exists()) {
      String pythonKernelName = pythonKernelName(project.getPythonEnvironment().getPythonVersion());
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
        createJupyterNotebookConfig(out, project, port, js, hdfsUser, certsDir, allowOrigin);
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
