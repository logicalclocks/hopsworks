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
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @Inject
  private ServingConfig servingConfig;
  
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
    throws ServiceDiscoveryException, ApiKeyException {
    return kubePredictorPythonUtils.buildDeployment(project, user, serving);
  }
  
  @Override
  public Service buildServingService(Serving serving) { return kubePredictorPythonUtils.buildService(serving); }
  
  // KServe
  
  @Override
  public JSONObject buildInferenceServicePredictor(Project project, Users user, Serving serving, String artifactPath)
    throws ServiceDiscoveryException, ApiKeyException {
    
    // Containers
    List<Container> containers = new ArrayList<>();
    containers.add(buildPredictorContainer(project, user, serving));
    
    // Volumes
    List<Volume> volumes = kubePredictorUtils.buildVolumes(project, user);
  
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
    
    return kubeJsonUtils.buildPredictor(containers, volumes, serving.getInstances(), logging, loggerMode,
        serving.getBatchingConfiguration());
  }
  
  private Container buildPredictorContainer(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException, ApiKeyException {
    
    List<EnvVar> envVars = buildEnvironmentVariables(project, user, serving);
    List<VolumeMount> volumeMounts = kubePredictorUtils.buildVolumeMounts();
  
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
  
  private List<EnvVar> buildEnvironmentVariables(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException, ApiKeyException {
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
