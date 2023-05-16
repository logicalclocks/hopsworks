/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.DeployableComponentResources;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeTransformerUtils {
  
  private final static String TRANSFORMER_NAME_PREFIX = "transformer-";
  private final static String IPYNB_EXTENSION = ".ipynb";
  private final static String PY_EXTENSION = ".py";
  
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private KubeJsonUtils kubeJsonUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @Inject
  private ServingConfig servingConfig;
  
  public void copyTransformerToArtifactDir(Project project, Users user, Serving serving)
    throws DatasetException, ServiceException {
    
    String sourceFilePath = serving.getTransformer();
    DatasetPath sourceFileDatasetPath = datasetHelper.getDatasetPath(project, sourceFilePath, DatasetType.DATASET);
    String destFilePath = getTransformerFilePath(serving);
    if (serving.getTransformer().endsWith(IPYNB_EXTENSION)) {
      // If transformer file is ipynb, convert it to python script
      jupyterController.convertIPythonNotebook(project, user, sourceFilePath, destFilePath,
          JupyterController.NotebookConversion.PY_JOB);
    } else {
      // Otherwise, copy transformer file into model artifact
      DatasetPath destFileDatasetPath = datasetHelper.getDatasetPath(project, destFilePath, DatasetType.DATASET);
      datasetController.copy(project, user, sourceFileDatasetPath.getFullPath(), destFileDatasetPath.getFullPath(),
        sourceFileDatasetPath.getDataset(), destFileDatasetPath.getDataset());
    }
  }
  
  public String getTransformerFileName(Serving serving, Boolean prefix) {
    String transformerPath = serving.getTransformer();
    if (transformerPath == null) {
      return null;
    }
    String name = transformerPath;
    if (transformerPath.contains("/")) {
      String[] splits = transformerPath.split("/");
      name = splits[splits.length - 1];
    }
    if (transformerPath.endsWith(IPYNB_EXTENSION)) {
      name = name.replace(IPYNB_EXTENSION, PY_EXTENSION);
    }
    return prefix ? TRANSFORMER_NAME_PREFIX + name : name;
  }
  
  public String getTransformerFilePath(Serving serving) {
    String artifactDirPath = kubeArtifactUtils.getArtifactDirPath(serving);
    String transformerFileName = getTransformerFileName(serving, true);
    return artifactDirPath + "/" + transformerFileName;
  }
  
  public JSONObject buildInferenceServiceTransformer(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException, ApiKeyException {

    List<Container> containers = new ArrayList<>();
    containers.add(buildTransformerContainer(project, user, serving));
    
    List<Volume> volumes = kubePredictorUtils.buildVolumes(project, user);
    
    return kubeJsonUtils.buildPredictor(containers, volumes, serving.getTransformerInstances(),
        serving.getBatchingConfiguration());
  }
  
  public Boolean checkTransformerExists(Serving serving)
    throws UnsupportedEncodingException {
    String hdfsPath = Utils.prepPath(getTransformerFilePath(serving));
    return inodeController.existsPath(hdfsPath);
  }
  
  private Container buildTransformerContainer(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException, ApiKeyException {
    
    List<VolumeMount> volumeMounts = kubePredictorUtils.buildVolumeMounts();
  
    DeployableComponentResources transformerResources = serving.getTransformerResources();
    ResourceRequirements resourceRequirements = kubeClientService.
      buildResourceRequirements(transformerResources.getLimits(), transformerResources.getRequests());
    
    List<EnvVar> envVars = buildEnvironmentVariables(project, user, serving, resourceRequirements);
    
    return new ContainerBuilder()
      .withName("transformer")
      .withImage(projectUtils.getFullDockerImageName(project, false))
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withCommand("kserve-component-launcher.sh")
      .withEnv(envVars)
      .withVolumeMounts(volumeMounts)
      .withResources(resourceRequirements)
      .build();
  }
  
  private List<EnvVar> buildEnvironmentVariables(Project project, Users user, Serving serving,
    ResourceRequirements resourceRequirements) throws ServiceDiscoveryException, ApiKeyException {
    List<EnvVar> envVars = new ArrayList<>();
    
    // Transformer launcher
    envVars.add(new EnvVarBuilder()
      .withName("SCRIPT_PATH").withValue(getTransformerFileName(serving, true)).build());
    envVars.add(new EnvVarBuilder().withName("IS_KUBE").withValue("true").build());
    envVars.add(new EnvVarBuilder().withName("IS_TRANSFORMER").withValue("true").build());
    envVars.add(new EnvVarBuilder()
      .withName("PYTHONPATH").withValue(settings.getAnacondaProjectDir() + "/bin/python").build());
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
