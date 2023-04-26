/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.DeployableComponentResources;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;
import io.hops.hopsworks.servicediscovery.tags.NamenodeTags;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.hops.hopsworks.common.util.Settings.HOPS_USERNAME_SEPARATOR;

/**
 * Utils for creating default deployments for Python models on Kubernetes.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubePredictorPythonUtils {
  
  private final String ARTIFACT_VERSION = "ARTIFACT_VERSION";
  private final String ARTIFACT_PATH = "ARTIFACT_PATH";
  private final String DEFAULT_FS = "DEFAULT_FS";
  private final String PROJECT_NAME = "PROJECT_NAME";
  
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @Inject
  private ServingConfig servingConfig;
  
  // Default
  
  public String getDeploymentName(String servingId) { return "python-server-" + servingId; }
  
  public String getDeploymentPath(InferenceVerb verb) {
    StringBuilder pathBuilder = new StringBuilder().append("/");
    if (verb != null) {
      pathBuilder.append(verb.toString(false));
    }
    return pathBuilder.toString();
  }
  
  public String getServiceName(String servingId) {
    return "python-server-" + servingId;
  }
  
  public Deployment buildDeployment(Project project, Users user,
    Serving serving) throws ServiceDiscoveryException, ApiKeyException {
    
    String servingIdStr = String.valueOf(serving.getId());
    DeployableComponentResources predictorResources = serving.getPredictorResources();
    ResourceRequirements resourceRequirements = kubeClientService.
      buildResourceRequirements(predictorResources.getLimits(), predictorResources.getRequests());
    
    List<EnvVar> envVars = buildEnvironmentVariables(project, user, serving, resourceRequirements);

    List<Volume> volumes = kubePredictorUtils.buildVolumes(project, user);
    
    List<VolumeMount> volumeMounts = kubePredictorUtils.buildVolumeMounts();
    
    Container pythonServerContainer = new ContainerBuilder()
      .withName("python-server")
      .withImage(projectUtils.getFullDockerImageName(project, false))
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withEnv(envVars)
      .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
      .withCommand("python-server-launcher.sh")
      .withVolumeMounts(volumeMounts)
      .withResources(resourceRequirements)
      .build();
    
    List<Container> containerList = Arrays.asList(pythonServerContainer);
    
    LabelSelector labelSelector = new LabelSelectorBuilder()
      .addToMatchLabels("model", servingIdStr)
      .build();
    
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("model", servingIdStr);
    
    ObjectMeta podMetadata = new ObjectMetaBuilder()
      .withLabels(labelMap)
      .build();
    
    PodSpec podSpec = new PodSpecBuilder()
      .withContainers(containerList)
      .withVolumes(volumes)
      .build();
    
    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
      .withMetadata(podMetadata)
      .withSpec(podSpec)
      .build();
    
    DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
      .withReplicas(serving.getInstances())
      .withSelector(labelSelector)
      .withTemplate(podTemplateSpec)
      .build();
    
    return new DeploymentBuilder()
      .withMetadata(getDeploymentMetadata(servingIdStr))
      .withSpec(deploymentSpec)
      .build();
  }
  
  public Service buildService(Serving serving) {
    String servingIdStr = String.valueOf(serving.getId());
    
    Map<String, String> selector = new HashMap<>();
    selector.put("model", servingIdStr);
    
    ServicePort tfServingServicePorts = new ServicePortBuilder()
      .withProtocol("TCP")
      .withPort(9998)
      .withTargetPort(new IntOrString(5000))
      .build();
    
    ServiceSpec tfServingServiceSpec = new ServiceSpecBuilder()
      .withSelector(selector)
      .withPorts(tfServingServicePorts)
      .withType("NodePort")
      .build();
    
    return new ServiceBuilder()
      .withMetadata(getServiceMetadata(servingIdStr))
      .withSpec(tfServingServiceSpec)
      .build();
  }
  
  private ObjectMeta getDeploymentMetadata(String servingId) {
    return new ObjectMetaBuilder()
      .withName(getDeploymentName(servingId))
      .build();
  }
  
  private ObjectMeta getServiceMetadata(String servingId) {
    return new ObjectMetaBuilder()
      .withName(getServiceName(servingId))
      .build();
  }
  
  private List<EnvVar> buildEnvironmentVariables(Project project, Users user, Serving serving,
                                                 ResourceRequirements resourceRequirements)
    throws ServiceDiscoveryException, ApiKeyException {
    String projectUser = project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();
    String servingIdStr = String.valueOf(serving.getId());
    
    List<EnvVar> envVars = new ArrayList<>();
    
    envVars.add(new EnvVarBuilder().withName(KubePredictorServerUtils.SERVING_ID).withValue(servingIdStr).build());
    envVars.add(new EnvVarBuilder().withName(PROJECT_NAME).withValue(project.getName()).build());
    envVars.add(new EnvVarBuilder().withName(KubePredictorServerUtils.MODEL_NAME)
      .withValue(serving.getName().toLowerCase()).build());
    envVars.add(new EnvVarBuilder().withName(ARTIFACT_VERSION)
      .withValue(String.valueOf(serving.getArtifactVersion())).build());
    envVars.add(new EnvVarBuilder().withName(ARTIFACT_PATH)
      .withValue("hdfs://" + serviceDiscoveryController.constructServiceFQDN(
        HopsworksService.NAMENODE.getNameWithTag(NamenodeTags.rpc)) +"/"
          + kubeArtifactUtils.getArtifactFilePath(serving))
      .build());
    envVars.add(new EnvVarBuilder().withName(DEFAULT_FS)
      .withValue("hdfs://" + serviceDiscoveryController.constructServiceFQDN(
        HopsworksService.NAMENODE.getNameWithTag(NamenodeTags.rpc))).build());
    envVars.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    envVars.add(new EnvVarBuilder().withName("TLS").withValue(String.valueOf(settings.getHopsRpcTls())).build());
    envVars.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER").withValue(projectUser).build());
    envVars.add(new EnvVarBuilder().withName("HDFS_USER").withValue(projectUser).build());
    envVars.add(new EnvVarBuilder().withName("CONDAPATH").withValue(settings.getAnacondaProjectDir()).build());
    envVars.add(new EnvVarBuilder().withName("PYTHONPATH")
      .withValue(settings.getAnacondaProjectDir() + "/bin/python").build());
    envVars.add(new EnvVarBuilder().withName("SCRIPT_NAME")
      .withValue(kubePredictorUtils.getPredictorFileName(serving, true)).build());
    envVars.add(new EnvVarBuilder().withName("IS_KUBE").withValue("true").build());
    envVars.add(new EnvVarBuilder().withName("NVIDIA_VISIBLE_DEVICES")
      .withValue(kubeClientService.getNvidiaVisibleDevices(resourceRequirements)).build());
    
    // HSFS and HOPS
    envVars.add(new EnvVarBuilder()
      .withName("HADOOP_USER_NAME").withValue(hdfsUsersController.getHdfsUserName(project, user)).build());
    envVars.add(new EnvVarBuilder().withName("REST_ENDPOINT").withValue("https://" + serviceDiscoveryController
      .constructServiceFQDNWithPort(HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks)))
        .build());
    envVars.add(new EnvVarBuilder()
      .withName("REQUESTS_VERIFY").withValue(String.valueOf(settings.getRequestsVerify())).build());
    envVars.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
  
    // HSFS
    envVars.add(new EnvVarBuilder()
      .withName("PROJECT_ID").withValue(String.valueOf(serving.getProject().getId())).build());
    envVars.add(new EnvVarBuilder().withName("PROJECT_NAME").withValue(serving.getProject().getName()).build());
    envVars.add(new EnvVarBuilder().withName("SECRETS_DIR").withValue("/keys").build());
  
    // HOPS / HSML
    envVars.add(new EnvVarBuilder()
      .withName("HOPSWORKS_PROJECT_ID").withValue(String.valueOf(serving.getProject().getId())).build());
    envVars.add(new EnvVarBuilder()
      .withName("HOPSWORKS_PROJECT_NAME").withValue(serving.getProject().getName()).build());
    envVars.add(new EnvVarBuilder().withName("API_KEY").withValueFrom(
      new EnvVarSourceBuilder().withNewSecretKeyRef(
        kubeApiKeyUtils.getServingApiKeySecretKeyName(),
        kubeApiKeyUtils.getProjectServingApiKeySecretName(user),
        false)
        .build())
      .build());
  
    // HSML Serving
    envVars.add(new EnvVarBuilder().withName("SERVING_API_KEY").withValueFrom(
      new EnvVarSourceBuilder().withNewSecretKeyRef(KubeApiKeyUtils.SERVING_API_KEY_SECRET_KEY,
        kubeApiKeyUtils.getProjectServingApiKeySecretName(user), false).build()).build());
    Map<String, String> servingEnvVars = servingConfig.getEnvVars(user, false);
    servingEnvVars.forEach((key, value) -> envVars.add(
      new EnvVarBuilder().withName(key).withValue(value).build()));
    
    // HOPSWORKS PYTHON API
    envVars.add(new EnvVarBuilder().withName("ELASTIC_ENDPOINT").withValue(settings.getOpenSearchRESTEndpoint())
      .build());
  
    // DEPLOYMENT INFO
    envVars.add(new EnvVarBuilder().withName("DEPLOYMENT_NAME").withValue(serving.getName()).build());
    envVars.add(new EnvVarBuilder().withName("MODEL_NAME").withValue(serving.getModelName()).build());
    envVars.add(new EnvVarBuilder().withName("MODEL_VERSION")
      .withValue(String.valueOf(serving.getModelVersion())).build());
    envVars.add(new EnvVarBuilder().withName("ARTIFACT_VERSION")
      .withValue(String.valueOf(serving.getArtifactVersion())).build());
    
    return envVars;
  }
}
