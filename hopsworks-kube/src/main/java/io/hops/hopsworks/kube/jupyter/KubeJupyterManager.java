/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.jupyter;

import com.google.common.collect.ImmutableMap;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFilesGenerator;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterPaths;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jupyter.JupyterManager;
import io.hops.hopsworks.common.jupyter.JupyterManagerImpl;
import io.hops.hopsworks.common.jupyter.TokenGenerator;
import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.templates.jupyter.JupyterNotebookConfigTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.KernelTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.SparkMagicConfigTemplate;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.hops.hopsworks.restutils.RESTCodes.ServiceErrorCode.JUPYTER_STOP_ERROR;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;

@Stateless
@KubeStereotype
public class KubeJupyterManager extends JupyterManagerImpl implements JupyterManager {
  private static final String CERTS = "certs";
  private static final String CONF = "conf";
  private static final String HADOOP_CONF = "hadoopconf";
  private static final String JUPYTER = "jupyter";
  private static final String JWT = "jwt";
  private static final String KERNELS = "kernels";
  private static final String SPARK = "spark";
  private static final String SEPARATOR = "-";
  private static final String JUPYTER_PREFIX = JUPYTER + SEPARATOR;
  private static final String CONF_SUFFIX = SEPARATOR + "conf";
  public static final String JWT_SUFFIX = SEPARATOR + "jwt";
  private static final String KERNELS_SUFFIX = SEPARATOR + "kernels";
  private static final String READINESS_PATH = "/hopsworks-api/jupyter/%d/api/status?token=%s";
  
  private static final String CID = "no cid";
  private static final int TOKEN_LENGTH = 48;
  private static final int LOCAL_PORT = 8888;
  
  private static final Logger logger = Logger.getLogger(KubeJupyterManager.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterConfigFilesGenerator jupyterConfigFilesGenerator;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private JobController jobController;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @Inject
  private ServingConfig servingConfig;
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings jupyterSettings, String allowOrigin) throws ServiceException, JobException {

    JupyterPaths jupyterPaths = jupyterConfigFilesGenerator.generateJupyterPaths(project, hdfsUser, secretConfig);
    String kubeProjectUser = kubeClientService.getKubeDeploymentName(project, user);
    String pythonKernelName = jupyterConfigFilesGenerator.pythonKernelName
        (project.getPythonEnvironment().getPythonVersion());
    String secretDir = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
  
    String token = TokenGenerator.generateToken(TOKEN_LENGTH);
    
    logger.log(FINEST, "Starting Jupyter notebook for project " + project.getName() + " and user "
      + user.getUsername());
  
    Optional<Integer> nodePortOptional = Optional.empty();
    try {
      String serviceName = serviceAndDeploymentName(project, user);
      kubeClientService.createOrReplaceService(project, buildService(serviceName, kubeProjectUser));
  
      Service service = kubeClientService.getServiceInfo(project, serviceName, settings.getKubeAPIMaxAttempts())
        .orElseThrow(() -> new IOException("Service could not be retrieved"));
      
      nodePortOptional = jupyterPort(service.getSpec());
      int nodePort = nodePortOptional.orElseThrow(() -> new IOException("NodePort could not be retrieved"));
  
      // jupyter notebook config
      Writer jupyterNotebookConfig = new StringWriter();
      jupyterConfigFilesGenerator.createJupyterNotebookConfig(jupyterNotebookConfig, project, nodePort,
        jupyterSettings, hdfsUser, jupyterPaths.getCertificatesDir(), allowOrigin);
      
      // spark config
      Writer sparkMagicConfig = new StringWriter();
      jupyterConfigFilesGenerator.createSparkMagicConfig(sparkMagicConfig, project, jupyterSettings, hdfsUser, user,
        jupyterPaths.getConfDirPath());

      //If user selected Experiments or Spark we should use the default docker config for the Python kernel
      if(!jupyterSettings.isPythonKernel()) {
        jupyterSettings.setDockerConfig(
                (DockerJobConfiguration)jobController.getConfiguration(project, JobType.DOCKER, true));
      }
      
      kubeClientService.createOrUpdateConfigMap(project, user, CONF_SUFFIX,
        ImmutableMap.of(
            JupyterNotebookConfigTemplate.FILE_NAME, jupyterNotebookConfig.toString(),
            SparkMagicConfigTemplate.FILE_NAME, sparkMagicConfig.toString()));
  
      Writer kernelConfig = new StringWriter();
      jupyterConfigFilesGenerator.createJupyterKernelConfig(kernelConfig, project, jupyterSettings, hdfsUser);
      kubeClientService.createOrUpdateConfigMap(project, user, KERNELS_SUFFIX,
        ImmutableMap.of(
            KernelTemplate.FILE_NAME, kernelConfig.toString()));
      
      kubeProjectConfigMaps.reloadConfigMaps(project);
      
      String deploymentName = serviceAndDeploymentName(project, user);
      Deployment deployment = buildDeployment(
          deploymentName,
          kubeProjectUser,
          jupyterPaths,
          settings.getAnacondaProjectDir(),
          pythonKernelName,
          secretDir,
          jupyterPaths.getCertificatesDir(),
          hdfsUser,
          token,
          (DockerJobConfiguration)jupyterSettings.getDockerConfig(),
          nodePort,
          jupyterSettings.getMode().getValue(),
          project,
          user);
  
      kubeClientService.createOrReplaceDeployment(project, deployment);
  
      return new JupyterDTO(nodePort, token, CID, secretConfig, jupyterPaths.getCertificatesDir());
    } catch (KubernetesClientException | IOException | ServiceDiscoveryException | ApiKeyException e) {
      logger.log(SEVERE, "Failed to start Jupyter notebook on Kubernetes", e);
      nodePortOptional.ifPresent(nodePort -> {
        try {
          stopJupyterServer(project, user, hdfsUser, getJupyterHome(hdfsUser, project, secretDir),
            CID, nodePort);
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Could not stop jupyter server.", ex);
        }
      });
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, SEVERE,
          e.getMessage(), e.getMessage(), e);
    }
  }

  private Optional<Integer> jupyterPort(ServiceSpec spec) {
    return spec.getPorts()
      .stream()
      .filter(x -> JUPYTER.equals(x.getName()))
      .map(x -> x.getNodePort())
      .findAny();
  }

  @Override
  public void waitForStartup(Project project, Users user) throws TimeoutException {
    String deploymentName = serviceAndDeploymentName(project, user);
    String kubeProjectUser = kubeClientService.getKubeDeploymentName(project, user);
    kubeClientService.waitForDeployment(project, deploymentName, ImmutableMap.of(JUPYTER, kubeProjectUser),
        settings.getKubeAPIMaxAttempts());
  }
  
  public String serviceAndDeploymentName(Project project, Users user) {
    return serviceAndDeploymentName(kubeClientService.getKubeDeploymentName(project, user));
  }
  public String serviceAndDeploymentName(String kubeProjectUsername) {
    return JUPYTER_PREFIX + kubeProjectUsername;
  }

  private List<Container> buildContainer(JupyterPaths jupyterPaths, String anacondaEnv, String pythonKernelName,
      String secretDir, String certificatesDir, String hdfsUser, String token,
                                         ResourceRequirements resourceRequirements, Integer nodePort,
                                         String jupyterMode, Project project, Users user,
                                         Map<String, String> filebeatEnv)
      throws ServiceDiscoveryException, UnsupportedEncodingException, ApiKeyException {

    String jupyterHome = jupyterPaths.getNotebookPath();
    String hadoopHome = settings.getHadoopSymbolicLinkDir();
    String hadoopConfDir = hadoopHome + "/etc/hadoop";
    List<EnvVar> environment = new ArrayList<>();
    environment.add(new EnvVarBuilder().withName("JUPYTER_PATH").withValue(jupyterHome).build());
    environment.add(new EnvVarBuilder().withName("JUPYTER_DATA_DIR").withValue(jupyterHome).build());
    environment.add(new EnvVarBuilder().withName("PDIR").withValue(secretDir).build());
    environment.
        add(new EnvVarBuilder().withName("JUPYTER_CONFIG_DIR").withValue(jupyterPaths.getConfDirPath()).build());
    environment.
        add(new EnvVarBuilder().withName("JUPYTER_RUNTIME_DIR").withValue(jupyterPaths.getRunDirPath()).build());
    environment.
        add(new EnvVarBuilder().withName("SPARKMAGIC_CONF_DIR").withValue(jupyterPaths.getConfDirPath()).build());
    environment.add(new EnvVarBuilder().withName("PYSPARK_PYTHON").withValue(anacondaEnv + "/bin/python").build());
    environment.add(new EnvVarBuilder().withName("PYLIB").withValue(anacondaEnv + "/lib").build());
    environment.add(new EnvVarBuilder().withName("HADOOP_HDFS_HOME").withValue(hadoopHome).build());
    environment.add(new EnvVarBuilder().withName("HADOOP_CONF_DIR").withValue(hadoopConfDir).build());
    environment.add(new EnvVarBuilder().withName("HADOOP_CLIENT_OPTS").withValue("-Dfs.permissions.umask-mode=0002").
        build());
    environment.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue(certificatesDir).build());
    environment.add(new EnvVarBuilder().withName("HADOOP_USER_NAME").withValue(hdfsUser).build());
    environment.add(new EnvVarBuilder().withName("HADOOP_HOME").withValue(hadoopHome).build());
    environment.add(new EnvVarBuilder().withName("ANACONDA_ENV").withValue(anacondaEnv).build());
    environment.add(new EnvVarBuilder().withName("PYTHONHASHSEED").withValue("0").build());

    // serving env vars
    if (settings.getKubeKServeInstalled()) {
      environment.add(new EnvVarBuilder().withName("SERVING_API_KEY").withValueFrom(
        new EnvVarSourceBuilder().withNewSecretKeyRef(KubeApiKeyUtils.SERVING_API_KEY_SECRET_KEY,
          kubeApiKeyUtils.getProjectServingApiKeySecretName(user), false).build()).build());
      Map<String, String> servingEnvVars = servingConfig.getEnvVars(user, false);
      servingEnvVars.forEach((key, value) -> environment.add(
          new EnvVarBuilder().withName(key).withValue(value).build()));
    }
    
    List<Container> containers = new ArrayList<>();
    VolumeMount logMount = new VolumeMountBuilder()
      .withName("logs")
      .withMountPath("/logs")
      .build();
  
    containers.add(new ContainerBuilder()
      .withName(JUPYTER)
      .withImage(projectUtils.getFullDockerImageName(project, false))
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withReadinessProbe(
        new ProbeBuilder()
          .withHttpGet(
            new HTTPGetActionBuilder()
              .withPath(String.format(READINESS_PATH, nodePort, token))
              .withNewPort(LOCAL_PORT)
            .build())
          .build())
      .withResources(resourceRequirements)
      .withVolumeMounts(
        new VolumeMountBuilder()
          .withName(CERTS)
          .withReadOnly(true)
          .withMountPath("/srv/hops/jupyter/certificates")
          .build(),
        new VolumeMountBuilder()
          .withName(CONF)
          .withReadOnly(true)
          .withMountPath("/srv/hops/jupyter/conf")
          .build(),
        new VolumeMountBuilder()
          .withName(HADOOP_CONF)
          .withReadOnly(true)
          .withMountPath(hadoopConfDir)
          .build(),
        new VolumeMountBuilder()
          .withName(JWT)
          .withReadOnly(true)
          .withMountPath("/srv/hops/jupyter/secrets")
          .build(),
        new VolumeMountBuilder()
          .withName(KERNELS)
          .withReadOnly(true)
          .withMountPath("/srv/hops/jupyter/kernels/" + pythonKernelName)
          .build(),
        new VolumeMountBuilder()
          .withName(SPARK)
          .withReadOnly(true)
          .withMountPath(settings.getSparkConfDir())
          .build(),
        logMount)
      .withPorts(
        new ContainerPortBuilder()
          .withContainerPort(LOCAL_PORT)
          .build())
      .withEnv(environment)
      .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
      .withCommand("jupyter-launcher.sh")  
      .withArgs(Integer.toString(LOCAL_PORT), filebeatEnv.get("LOGPATH"), token, jupyterMode)
      .build());
  
    containers.add(new ContainerBuilder()
      .withName("filebeat")
      .withImage(ProjectUtils.getRegistryURL(settings,
          serviceDiscoveryController) +
          "/filebeat:" + settings.
          getHopsworksVersion())
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withEnv(kubeClientService.getEnvVars(filebeatEnv))
      .withVolumeMounts(logMount)
      .build());
    
    return containers;
  }
  
  private PodSpec buildPodSpec(Project project, String kubeProjectUser,
        List<Container> containers) {
    
    return new PodSpecBuilder()
      .withContainers(containers)
      .withVolumes(
          new VolumeBuilder()
          .withName(CERTS)
          .withSecret(
            new SecretVolumeSourceBuilder()
              .withSecretName(kubeProjectUser)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(CONF)
          .withConfigMap(
            new ConfigMapVolumeSourceBuilder()
              .withName(kubeProjectUser + CONF_SUFFIX)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(HADOOP_CONF)
          .withConfigMap(
            new ConfigMapVolumeSourceBuilder()
              .withName(kubeProjectConfigMaps.getHadoopConfigMapName(project))
              .build())
          .build(),
        new VolumeBuilder()
          .withName(JWT)
          .withSecret(
            new SecretVolumeSourceBuilder()
              .withSecretName(kubeProjectUser + JWT_SUFFIX)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(KERNELS)
          .withConfigMap(
            new ConfigMapVolumeSourceBuilder()
              .withName(kubeProjectUser + KERNELS_SUFFIX)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(SPARK)
          .withConfigMap(
            new ConfigMapVolumeSourceBuilder()
              .withName(kubeProjectConfigMaps.getSparkConfigMapName(project))
              .build())
          .build(),
        new VolumeBuilder()
          .withName("logs")
          .withEmptyDir(new EmptyDirVolumeSource())
          .build())
      .build();
  }
  
  private Deployment buildDeployment(String name, String kubeProjectUser, JupyterPaths jupyterPaths, String anacondaEnv,
    String pythonKernelName, String secretDir, String certificatesDir, String hadoopUser, String token,
    DockerJobConfiguration dockerConfig, Integer nodePort, String jupyterMode, Project project,
    Users user) throws ServiceDiscoveryException, UnsupportedEncodingException, ApiKeyException {

    ResourceRequirements resourceRequirements = kubeClientService.
      buildResourceRequirements(dockerConfig.getResourceConfig());
  
    com.logicalclocks.servicediscoverclient.service.Service logstash =
      serviceDiscoveryController
        .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.JUPYTER_LOGSTASH);
    String logstashAddr = logstash.getAddress() + ":" + logstash.getPort();
  
    Map<String, String> filebeatEnv = new HashMap<>();
    filebeatEnv.put("LOGPATH", "/logs/" + HopsUtils.getJupyterLogName(hdfsUsersController.getHdfsUserName(project
      , user), nodePort));
    filebeatEnv.put("LOGSTASH", logstashAddr);
  
    List<Container> containers = buildContainer(jupyterPaths, anacondaEnv, pythonKernelName, secretDir, certificatesDir,
      hadoopUser, token, resourceRequirements, nodePort, jupyterMode, project, user, filebeatEnv);
    
    return new DeploymentBuilder()
      .withMetadata(new ObjectMetaBuilder()
        .withName(name)
        .withLabels(ImmutableMap.of(JUPYTER, kubeProjectUser))
        .build())
      .withSpec(
        new DeploymentSpecBuilder()
          .withSelector(
            new LabelSelectorBuilder()
              .addToMatchLabels(JUPYTER, kubeProjectUser)
              .build())
                  .withReplicas(1)
                  .withTemplate(
                      new PodTemplateSpecBuilder()
                          .withMetadata(
                              new ObjectMetaBuilder()
                                  .withLabels(ImmutableMap.of(JUPYTER, kubeProjectUser))
                                  .build())
              .withSpec(buildPodSpec(project, kubeProjectUser, containers))
            .build())
          .build())
      .build();
  }
  
  private Service buildService(String serviceName, String kubeProjectUser) {
    return new ServiceBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withName(serviceName)
          .withLabels(ImmutableMap.of(JUPYTER, kubeProjectUser))
          .build())
      .withSpec(
        new ServiceSpecBuilder()
          .withSelector(ImmutableMap.of(JUPYTER, kubeProjectUser))
          .withPorts(
            new ServicePortBuilder()
              .withName(JUPYTER)
              .withProtocol("TCP")
              .withPort(LOCAL_PORT)
              .withTargetPort(new IntOrString(LOCAL_PORT))
              .build())
          .withType("NodePort")
          .build())
      .build();
  }

  @Override
  public void stopOrphanedJupyterServer(String cid, Integer port) throws ServiceException {
    Service orphan = kubeClientService.getServices(JUPYTER)
      .stream()
      .filter(s -> port.equals(jupyterPort(s.getSpec()).get()))
      .findAny()
      .orElseThrow(() -> new ServiceException(JUPYTER_STOP_ERROR, SEVERE, "Could find service for port " + port));

    deleteKubeResources(orphan.getMetadata().getNamespace(),
      orphan.getMetadata().getName().replaceFirst(JUPYTER_PREFIX, ""));
  }
  
  @Override
  public void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath, String cid,
      Integer port) throws ServiceException {
    
    if (port == null) {
      throw new IllegalArgumentException("Invalid arguments when stopping the Jupyter Server.");
    }
    
    String errorMessage = "Problem when removing jupyter notebook entry from jupyter_project table";
    runCatchAndLog(() -> jupyterFacade.remove(hdfsUsername, port), errorMessage, Optional.empty());

    deleteKubeResources(kubeClientService.getKubeProjectName(project),
      kubeClientService.getKubeDeploymentName(project, user));
  }

  private void deleteKubeResources(String kubeProjectName, String kubeProjectUser) throws ServiceException {
    String errorMessage = "Error when deleting jupyter resource";
    Optional<Exception> t = Optional.empty();
    t = runCatchAndLog(() -> kubeClientService.deleteDeployment(kubeProjectName,
      serviceAndDeploymentName(kubeProjectUser)), errorMessage, t);
    t = runCatchAndLog(() -> kubeClientService.deleteService(kubeProjectName,
      serviceAndDeploymentName(kubeProjectUser)), errorMessage, t);
    t = runCatchAndLog(() -> kubeClientService.deleteConfigMap(kubeProjectName, kubeProjectUser + CONF_SUFFIX),
      errorMessage, t);
    t = runCatchAndLog(() -> kubeClientService.deleteConfigMap(kubeProjectName, kubeProjectUser + KERNELS_SUFFIX),
      errorMessage, t);
    if (t.isPresent()) {
      throw new ServiceException(JUPYTER_STOP_ERROR, SEVERE,
        "Exception when deleting jupyter notebook", "Exception when deleting jupyter notebook", t.get());
    }
  }
  
  private Optional<Exception> runCatchAndLog(Runnable runnable, String errorMessage,
      Optional<Exception> previousError) {
    try {
      runnable.run();
    } catch (Exception e) {
      logger.log(SEVERE, errorMessage, e);
      return previousError.isPresent() ? previousError : Optional.of(e);
    }
    return previousError;
  }

  @Override
  public void projectCleanup(Project project) {
    projectCleanup(logger, project);
  }
  
  @Override
  public boolean ping(JupyterProject jupyterProject) {
    String hdfsUser = hdfsUsersFacade.find(jupyterProject.getHdfsUserId()).getName();
    Project project = projectFacade.findByName(hdfsUsersController.getProjectName(hdfsUser));
    Users user = userFacade.findByUsername(hdfsUsersController.getUserName(hdfsUser));
    boolean isUp = kubeClientService.getServiceInfo(project, serviceAndDeploymentName(project, user), 1).isPresent();
    logger.log(FINEST, "Pinging Jupyter for project " + project.getName() + " and user " + user.getUsername()
        + " with result " + isUp);
    return isUp;
  }
  
  @Override
  public List<JupyterProject> getAllNotebooks() {
    List<JupyterProject> allNotebooks = jupyterFacade.getAllNotebookServers();
    List<Service> notebookServices = kubeClientService.getServices(JUPYTER);

    Set<Integer> knownPorts = allNotebooks.stream().map(j -> j.getPort()).collect(Collectors.toSet());
    Stream<JupyterProject> orphaned = notebookServices
      .stream()
      .filter(s -> !knownPorts.contains(jupyterPort(s.getSpec()).get()))
      .map(s -> {
        JupyterProject jupyterProject = new JupyterProject();
        jupyterProject.setHdfsUserId(-1); // Orphaned
        jupyterProject.setCid(CID);
        jupyterProject.setPort(jupyterPort(s.getSpec()).get());
        return jupyterProject;
      });

    return Stream.concat(allNotebooks.stream(), orphaned).collect(Collectors.toList());
  }
  
  @Override
  public String getJupyterHost() {
    return kubeClientService.getRandomReadyNodeIp();
  }
}
