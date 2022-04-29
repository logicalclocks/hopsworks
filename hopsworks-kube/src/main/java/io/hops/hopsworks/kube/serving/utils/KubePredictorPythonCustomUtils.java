/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.KeyToPathBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.DeployableComponentResources;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for creating deployments for custom Python models on Kubernetes.
 *
 * It implements methods for KServe deployments and reuses existing utils methods for default deployments.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubePredictorPythonCustomUtils extends KubePredictorServerUtils {
  
  private final String ContainerModelNameArgName = "--model_name";
  
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private KubePredictorUtils predictorUtils;
  @EJB
  private KubePredictorPythonUtils kubePredictorPythonUtils;
  @EJB
  private KubeJsonUtils kubeJsonUtils;
  
  // Default
  
  @Override
  public String getDeploymentName(String servingId) { return kubePredictorPythonUtils.getDeploymentName(servingId); }
  
  @Override
  public String getDeploymentPath(String servingName, Integer modelVersion, InferenceVerb verb) {
    return kubePredictorPythonUtils.getDeploymentPath(verb);
  }
  
  @Override
  public String getServiceName(String servingId) { return kubePredictorPythonUtils.getServiceName(servingId); }
  
  @Override
  public Deployment buildServingDeployment(Project project, Users user, Serving serving)
      throws ServiceDiscoveryException {
    return kubePredictorPythonUtils.buildDeployment(project, user, serving);
  }
  
  @Override
  public Service buildServingService(Serving serving) { return kubePredictorPythonUtils.buildService(serving); }
  
  // KServe
  
  @Override
  public JSONObject buildInferenceServicePredictor(Project project, Users user, Serving serving, String artifactPath)
    throws ServiceDiscoveryException {
    
    // Containers
    List<Container> containers = new ArrayList<>();
    containers.add(buildPredictorContainer(project, user, serving));
    
    // Volumes
    List<Volume> volumes = buildVolumes(project, user);
  
    // Inference logging
    InferenceLogging inferenceLogging = serving.getInferenceLogging();
    boolean logging = inferenceLogging != null;
    String loggerMode;
    if (logging) {
      if (inferenceLogging == InferenceLogging.ALL) {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_ALL;
      } else if (inferenceLogging == InferenceLogging.PREDICTIONS) {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_RESPONSE;
      } else {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_REQUEST;
      }
    } else {
      loggerMode = null;
    }
    
    return kubeJsonUtils.buildPredictor(containers, volumes, serving.getInstances(), logging, loggerMode);
  }
  
  private Container buildPredictorContainer(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException {
    
    List<EnvVar> envVars = buildEnvironmentVariables(project, user, serving);
    List<VolumeMount> volumeMounts = buildVolumeMounts();
  
    DeployableComponentResources predictorResources = serving.getPredictorResources();
    ResourceRequirements resourceRequirements = kubeClientService.
      buildResourceRequirements(predictorResources.getLimits(), predictorResources.getRequests());
    
    return new ContainerBuilder()
      .withName("predictor")
      .withImage(projectUtils.getFullDockerImageName(project, false))
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withCommand("kserve-component-launcher.sh")
      .withArgs(ContainerModelNameArgName, serving.getName())
      .withEnv(envVars)
      .withVolumeMounts(volumeMounts)
      .withResources(resourceRequirements)
      .build();
  }
  
  private List<Volume> buildVolumes(Project project, Users user) {
    List<Volume> volumes = new ArrayList<>();
    volumes.add(new VolumeBuilder().withName("certs").withSecret(
      new SecretVolumeSourceBuilder().withSecretName(kubeClientService.getKubeDeploymentName(project, user)).build())
      .build());
    volumes.add(new VolumeBuilder().withName("hadoopconf").withConfigMap(
      new ConfigMapVolumeSourceBuilder().withName(kubeProjectConfigMaps.getHadoopConfigMapName(project)).build())
      .build());
    volumes.add(new VolumeBuilder().withName("keys").withSecret(
      new SecretVolumeSourceBuilder()
        .withSecretName(kubeApiKeyUtils.getProjectServingApiKeySecretName(user))
        .withItems(new KeyToPathBuilder()
          .withKey(kubeApiKeyUtils.getServingApiKeySecretKeyName())
          .withPath("api.key").build())
        .build())
      .build());
    return volumes;
  }
  
  private List<VolumeMount> buildVolumeMounts() {
    List<VolumeMount> volumeMounts = new ArrayList<>();
    volumeMounts.add(new VolumeMountBuilder().withName("certs").withReadOnly(true).withMountPath("/certs").build());
    volumeMounts.add(new VolumeMountBuilder().withName("hadoopconf").withReadOnly(true)
      .withMountPath(settings.getHadoopSymbolicLinkDir() + "/etc/hadoop").build());
    volumeMounts.add(new VolumeMountBuilder().withName("keys").withReadOnly(true).withMountPath("/keys").build());
    return volumeMounts;
  }
  
  private List<EnvVar> buildEnvironmentVariables(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException {
    List<EnvVar> envVars = new ArrayList<>();
    
    // Predictor launcher
    envVars.add(new EnvVarBuilder()
      .withName("SCRIPT_PATH").withValue(predictorUtils.getPredictorFileName(serving, true)).build());
    envVars.add(new EnvVarBuilder().withName("IS_KUBE").withValue("true").build());
    envVars.add(new EnvVarBuilder().withName("IS_PREDICTOR").withValue("true").build());
    envVars.add(new EnvVarBuilder()
      .withName("PYTHONPATH").withValue(settings.getAnacondaProjectDir() + "/bin/python").build());
    
    // HSFS and HOPS
    envVars.add(new EnvVarBuilder()
      .withName("HADOOP_USER_NAME").withValue(hdfsUsersController.getHdfsUserName(project, user)).build());
    envVars.add(new EnvVarBuilder().withName("REST_ENDPOINT").withValue("https://" + serviceDiscoveryController
      .constructServiceFQDNWithPort(ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP)).build());
    envVars.add(new EnvVarBuilder()
      .withName("REQUESTS_VERIFY").withValue(String.valueOf(settings.getRequestsVerify())).build());
    envVars.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    
    // HSFS
    envVars.add(new EnvVarBuilder()
      .withName("PROJECT_ID").withValue(String.valueOf(serving.getProject().getId())).build());
    envVars.add(new EnvVarBuilder().withName("PROJECT_NAME").withValue(serving.getProject().getName()).build());
    envVars.add(new EnvVarBuilder().withName("SECRETS_DIR").withValue("/keys").build());
    
    // HOPS
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
    
    return envVars;
  }
}
