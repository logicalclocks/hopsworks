/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.jupyter;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFilesGenerator;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterManager;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterPaths;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jupyter.DockerJobConfiguration;
import io.hops.hopsworks.common.jupyter.TokenGenerator;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.templates.jupyter.JupyterNotebookConfigTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.KernelTemplate;
import io.hops.hopsworks.common.util.templates.jupyter.SparkMagicConfigTemplate;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
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
public class KubeJupyterManager implements JupyterManager {
  public static final String ANACONDA = "anaconda";
  public static final String CERTS = "certs";
  public static final String CONF = "conf";
  public static final String HADOOP = "hadoop";
  public static final String JUPYTER = "jupyter";
  public static final String JWT = "jwt";
  public static final String KERNELS = "kernels";
  public static final String FLINK = "flink";
  public static final String SPARK = "spark";
  public static final String SEPARATOR = "-";
  public static final String JUPYTER_PREFIX = JUPYTER + SEPARATOR;
  public static final String CONF_SUFFIX = SEPARATOR + "conf";
  public static final String JWT_SUFFIX = SEPARATOR + "jwt";
  public static final String KERNELS_SUFFIX = SEPARATOR + "kernels";
  private static final String READINESS_PATH = "/hopsworks-api/jupyter/%d/api/status?token=%s";
  
  private static final long PID = -1;
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
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterConfigFilesGenerator jupyterConfigFilesGenerator;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private OSProcessExecutor osProcessExecutor;


  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings jupyterSettings, String allowOrigin) throws ServiceException {

    JupyterPaths jupyterPaths = jupyterConfigFilesGenerator.generateJupyterPaths(project, hdfsUser, secretConfig);
    String kubeProjectUser = kubeClientService.getKubeProjectUsername(project, user);
    String pythonKernelName = jupyterConfigFilesGenerator.pythonKernelName(project.getPythonVersion());
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
  
      Writer jupyterNotebookConfig = new StringWriter();
      jupyterConfigFilesGenerator.createJupyterNotebookConfig(jupyterNotebookConfig, project,
          hdfsLeFacade.getRPCEndpoint(), nodePort, jupyterSettings, hdfsUser, pythonKernelName,
          jupyterPaths.getCertificatesDir(), allowOrigin);
      Writer sparkMagicConfig = new StringWriter();
      jupyterConfigFilesGenerator.createSparkMagicConfig(sparkMagicConfig,project, jupyterSettings, hdfsUser,
          jupyterPaths.getConfDirPath(), user.getFname() + " " + user.getLname());
      
      kubeClientService.createOrUpdateConfigMap(project, user, CONF_SUFFIX,
        ImmutableMap.of(
            JupyterNotebookConfigTemplate.FILE_NAME, jupyterNotebookConfig.toString(),
            SparkMagicConfigTemplate.FILE_NAME, sparkMagicConfig.toString()));
  
      Writer kernelConfig = new StringWriter();
      jupyterConfigFilesGenerator.createJupyterKernelConfig(kernelConfig, project, jupyterSettings, hdfsUser);
      kubeClientService.createOrUpdateConfigMap(project, user, KERNELS_SUFFIX,
        ImmutableMap.of(
            KernelTemplate.FILE_NAME, kernelConfig.toString()));
      
      String deploymentName = serviceAndDeploymentName(project, user);
      kubeClientService.createOrReplaceDeployment(project,
        buildDeployment(
          deploymentName,
          kubeProjectUser,
          jupyterPaths.getNotebookPath(),
          settings.getAnacondaProjectDir(project),
          pythonKernelName,
          secretDir,
          jupyterPaths.getCertificatesDir(),
          hdfsUser,
          token,
          settings.getFlinkConfDir(),
          settings.getSparkConfDir(),
          (DockerJobConfiguration)jupyterSettings.getDockerConfig(),
          nodePort, jupyterSettings.getMode().getValue()));
      
      return new JupyterDTO(nodePort, token, PID, secretConfig, jupyterPaths.getCertificatesDir());
    } catch (KubernetesClientException | IOException e) {
      logger.log(SEVERE, "Failed to start Jupyter notebook on Kubernetes", e);
      nodePortOptional.ifPresent(nodePort -> {
        try {
          stopJupyterServer(project, user, hdfsUser, getJupyterHome(settings, hdfsUser, project, secretDir),
            PID, nodePort);
        } catch (Exception ex) {
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
    kubeClientService.waitForDeployment(project, deploymentName, settings.getKubeAPIMaxAttempts());
  }
  
  private String serviceAndDeploymentName(Project project, Users user) {
    return serviceAndDeploymentName(kubeClientService.getKubeProjectUsername(project, user));
  }

  private String serviceAndDeploymentName(String kubeProjectUsername) {
    return JUPYTER_PREFIX + kubeProjectUsername;
  }

  private ResourceRequirements buildResourceRequirements(DockerJobConfiguration dockerConfig) throws ServiceException {

    if(dockerConfig.getMemory() > settings.getKubeDockerMaxMemoryAllocation()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.FINE, "Exceeded maximum memory "
          + "allocation allowed for Jupyter Notebook server: " + settings.getKubeDockerMaxMemoryAllocation() + "MB");
    } else if(dockerConfig.getCores() > settings.getKubeDockerMaxCoresAllocation()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.FINE, "Exceeded maximum cores "
          + "allocation allowed for Jupyter Notebook server: " + settings.getKubeDockerMaxCoresAllocation() + " cores");
    }
    return new ResourceRequirementsBuilder()
        .addToLimits("memory", new QuantityBuilder()
            .withAmount(dockerConfig.getMemory() + "Mi").build())
        .addToLimits("cpu", new QuantityBuilder()
            .withAmount(Double.toString(dockerConfig.getCores() * settings.getKubeDockerCoresFraction())).build())
        .build();
  }


  private Container buildContainer(String jupyterHome, String anacondaEnv, String pythonKernelName, String secretDir,
                                   String certificatesDir, String hadoopUser, String token, String flinkConfDir,
                                   String sparkConfDir, ResourceRequirements resourceRequirements,
                                   Integer nodePort, String jupyterMode) {
    return new ContainerBuilder()
      .withName(JUPYTER)
      .withImage(settings.getKubeRegistry() + "/jupyter:" + settings.getJupyterImgVersion())
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
          .withName(ANACONDA)
          .withReadOnly(true)
          .withMountPath("/srv/hops/anaconda/env")
          .build(),
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
          .withName(HADOOP)
          .withReadOnly(true)
          .withMountPath("/srv/hops/hadoop")
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
          .withName(FLINK)
          .withReadOnly(true)
          .withMountPath("/srv/hops/flink/conf")
          .build(),
        new VolumeMountBuilder()
          .withName(SPARK)
          .withReadOnly(true)
          .withMountPath("/srv/hops/spark/conf")
          .build())
      .withPorts(
        new ContainerPortBuilder()
          .withContainerPort(LOCAL_PORT)
          .build())
      .withCommand("jupyter-launch.sh")
      .withArgs(jupyterHome, anacondaEnv, secretDir, certificatesDir, hadoopUser, token, flinkConfDir, sparkConfDir,
          jupyterMode)
      .build();
  }
  
  private PodSpec buildPodSpec(String kubeProjectUser, String anacondaEnv, Container container) {
    
    return new PodSpecBuilder()
      .withContainers(container)
      .withVolumes(
        new VolumeBuilder()
          .withName(ANACONDA)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(anacondaEnv)
              .build())
          .build(),
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
          .withName(HADOOP)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getHadoopSymbolicLinkDir())
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
          .withName(FLINK)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getFlinkConfDir())
              .build())
          .build(),
        new VolumeBuilder()
          .withName(SPARK)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getSparkConfDir())
              .build())
          .build())
      .build();
  }
  
  private Deployment buildDeployment(String name, String kubeProjectUser, String jupyterHome, String anacondaEnv,
    String pythonKernelName, String secretDir, String certificatesDir, String hadoopUser, String token,
    String flinkConfDir, String sparkConfDir, DockerJobConfiguration dockerConfig, Integer nodePort,
    String jupyterMode)
      throws ServiceException {

    ResourceRequirements resourceRequirements = buildResourceRequirements(dockerConfig);
    Container container = buildContainer(jupyterHome, anacondaEnv, pythonKernelName, secretDir, certificatesDir,
      hadoopUser, token, flinkConfDir, sparkConfDir, resourceRequirements, nodePort, jupyterMode);
    
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
              .withSpec(buildPodSpec(kubeProjectUser, anacondaEnv, container))
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

  public void stopOrphanedJupyterServer(Long pid, Integer port) throws ServiceException {
    Service orphan = kubeClientService.getServices(JUPYTER)
      .stream()
      .filter(s -> port.equals(jupyterPort(s.getSpec()).get()))
      .findAny()
      .orElseThrow(() -> new ServiceException(JUPYTER_STOP_ERROR, SEVERE, "Could find service for port " + port));

    deleteKubeResources(orphan.getMetadata().getNamespace(),
      orphan.getMetadata().getName().replaceFirst(JUPYTER_PREFIX, ""));
  }
  
  @Override
  public void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath, Long pid,
      Integer port) throws ServiceException {
    
    if (port == null) {
      throw new IllegalArgumentException("Invalid arguments when stopping the Jupyter Server.");
    }
    
    String errorMessage = "Problem when removing jupyter notebook entry from jupyter_project table";
    runCatchAndLog(() -> jupyterFacade.remove(hdfsUsername, port), errorMessage, Optional.empty());

    deleteKubeResources(kubeClientService.getKubeProjectName(project),
      kubeClientService.getKubeProjectUsername(project, user));
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
    projectCleanup(settings, logger, osProcessExecutor, project);
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
        jupyterProject.setPid(PID);
        jupyterProject.setPort(jupyterPort(s.getSpec()).get());
        return jupyterProject;
      });

    return Stream.concat(allNotebooks.stream(), orphaned).collect(Collectors.toList());
  }
}
