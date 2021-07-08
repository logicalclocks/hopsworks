/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
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
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.hops.hopsworks.common.util.Settings.HOPS_USERNAME_SEPARATOR;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeSkLearnServingUtils {
  
  private final static String SERVING_ID = "SERVING_ID";
  private final static String ARTIFACT_PATH = "ARTIFACT_PATH";
  private final static String DEFAULT_FS = "DEFAULT_FS";
  private final static String PROJECT_NAME = "PROJECT_NAME";
  private final static String MODEL_NAME = "MODEL_NAME";
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private ProjectUtils projectUtils;
  
  public String getDeploymentName(String servingId) {
    return "sklrn-serving-dep-" + servingId;
  }
  
  public String getServiceName(String servingId) {
    return "sklrn-serving-ser-" + servingId;
  }
  
  private ObjectMeta getDeploymentMetadata(String servingId) {
    return new ObjectMetaBuilder()
      .withName(getDeploymentName(servingId))
      .build();
  }
  
  public String getDeploymentPath(String verb) {
    StringBuilder pathBuilder = new StringBuilder().append("/").append(verb.replaceFirst(":", ""));
    return pathBuilder.toString();
  }
  
  private ObjectMeta getServiceMetadata(String servingId) {
    return new ObjectMetaBuilder()
      .withName(getServiceName(servingId))
      .build();
  }
  
  public Deployment buildServingDeployment(Project project, Users user,
    Serving serving) throws ServiceDiscoveryException {
    
    String servingIdStr = String.valueOf(serving.getId());
    String projectUser = project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();
    String hadoopHome = settings.getHadoopSymbolicLinkDir();
    String hadoopConfDir = hadoopHome + "/etc/hadoop";

    ResourceRequirements resourceRequirements = kubeClientService.
      buildResourceRequirements(serving.getDockerResourcesConfig());
    
    List<EnvVar> servingEnv = new ArrayList<>();
    servingEnv.add(new EnvVarBuilder().withName(SERVING_ID).withValue(servingIdStr).build());
    servingEnv.add(new EnvVarBuilder().withName(PROJECT_NAME).withValue(project.getName()).build());
    servingEnv.add(new EnvVarBuilder().withName(MODEL_NAME).withValue(serving.getName().toLowerCase()).build());
    servingEnv.add(new EnvVarBuilder().withName(ARTIFACT_PATH)
      .withValue("hdfs://" + serviceDiscoveryController.constructServiceFQDN(
        ServiceDiscoveryController.HopsworksService.RPC_NAMENODE) + "/" + serving.getModelPath()).build());
    servingEnv.add(new EnvVarBuilder().withName(DEFAULT_FS)
      .withValue("hdfs://" + serviceDiscoveryController.constructServiceFQDN(
        ServiceDiscoveryController.HopsworksService.RPC_NAMENODE)).build());
    servingEnv.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    servingEnv.add(new EnvVarBuilder().withName("TLS").withValue(String.valueOf(settings.getHopsRpcTls())).build());
    servingEnv.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER").withValue(projectUser).build());
    servingEnv.add(new EnvVarBuilder().withName("HDFS_USER").withValue(projectUser).build());
    servingEnv.add(new EnvVarBuilder().withName("PYTHONPATH").withValue(
      settings.getAnacondaProjectDir() + "/bin/python").build());
    servingEnv.add(new EnvVarBuilder().withName("SCRIPT_NAME").withValue("predict.py").build());
    servingEnv.add(new EnvVarBuilder().withName("IS_KUBE").withValue("true").build());
    SecretVolumeSource secretVolume = new SecretVolumeSourceBuilder()
      .withSecretName(kubeClientService.getKubeDeploymentName(project, user))
      .build();
    
    Volume secretVol = new VolumeBuilder()
      .withName("certs")
      .withSecret(secretVolume)
      .build();
    
    Volume pythonEnv = new VolumeBuilder()
      .withName("hadoopconf")
      .withConfigMap(
        new ConfigMapVolumeSourceBuilder()
          .withName(kubeProjectConfigMaps.getHadoopConfigMapName(project))
          .build())
      .build();
    
    VolumeMount secretMount = new VolumeMountBuilder()
      .withName("certs")
      .withReadOnly(true)
      .withMountPath("/certs")
      .build();
    
    VolumeMount hadoopConfEnvMount = new VolumeMountBuilder()
      .withName("hadoopconf")
      .withReadOnly(true)
      .withMountPath(hadoopConfDir)
      .build();
    
    Container skLeanContainer = new ContainerBuilder()
      .withName("sklearn")
      .withImage(projectUtils.getFullDockerImageName(project, false))
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withEnv(servingEnv)
      .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
      .withCommand("sklearn_serving-launcher.sh")
      .withVolumeMounts(secretMount, hadoopConfEnvMount)
      .withResources(resourceRequirements)
      .build();
    
    List<Container> containerList = Arrays.asList(skLeanContainer);
    
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
      .withVolumes(secretVol, pythonEnv)
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
  
  public Service buildServingService(Serving serving) {
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
}
