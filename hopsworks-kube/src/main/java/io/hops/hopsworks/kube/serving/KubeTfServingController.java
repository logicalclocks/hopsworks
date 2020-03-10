/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
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
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
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
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
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
public class KubeTfServingController {

  private final static String SERVING_ID = "SERVING_ID";
  private final static String MODEL_NAME = "MODEL_NAME";
  private final static String MODEL_DIR = "MODEL_DIR";
  private final static String MODEL_VERSION = "MODEL_VERSION";

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLEFacade;
  @EJB
  private Settings settings;

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

  public String getDeploymentName(String servingId) {
    return "tf-serving-dep-" + servingId;
  }

  public String getServiceName(String servingId) {
    return "tf-serving-ser-" + servingId;
  }

  public Deployment buildServingDeployment(Project project, Users user, Serving serving) {

    String servingIdStr = String.valueOf(serving.getId());

    List<EnvVar> tfServingEnv = new ArrayList<>();
    tfServingEnv.add(new EnvVarBuilder().withName(SERVING_ID).withValue(servingIdStr).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_NAME).withValue(serving.getName()).build());
    tfServingEnv.add(new EnvVarBuilder().withName("PROJECT_NAME").withValue(project.getName().toLowerCase()).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_DIR)
        .withValue("hdfs://" + hdfsLEFacade.getRPCEndpoint() + serving.getArtifactPath()).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_VERSION)
        .withValue(String.valueOf(serving.getVersion())).build());
    tfServingEnv.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER")
        .withValue(project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername()).build());
    tfServingEnv.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    tfServingEnv.add(new EnvVarBuilder().withName("HDFS_USER")
        .withValue(settings.getHdfsSuperUser()).build());
    tfServingEnv.add(new EnvVarBuilder().withName("TLS")
        .withValue(String.valueOf(settings.getHopsRpcTls())).build());
    tfServingEnv.add(new EnvVarBuilder().withName("ENABLE_BATCHING")
        .withValue(serving.isBatchingEnabled() ? "1" : "0").build());

    List<EnvVar> fileBeatEnv = new ArrayList<>();
    fileBeatEnv.add(new EnvVarBuilder().withName("LOGPATH").withValue("/logs/*").build());
    fileBeatEnv.add(new EnvVarBuilder().withName("LOGSTASH").withValue(settings.getLogstashIp() + ":" +
        settings.getLogstashPortTfServing()).build());

    SecretVolumeSource secretVolume = new SecretVolumeSourceBuilder()
        .withSecretName(kubeClientService.getKubeProjectUsername(kubeClientService.getKubeProjectName(project), user))
        .build();

    Volume secretVol = new VolumeBuilder()
        .withName("certs")
        .withSecret(secretVolume)
        .build();

    Volume logs = new VolumeBuilder()
        .withName("logs")
        .withEmptyDir(new EmptyDirVolumeSource())
        .build();

    VolumeMount secretMount = new VolumeMountBuilder()
        .withName("certs")
        .withReadOnly(true)
        .withMountPath("/certs")
        .build();

    VolumeMount logMount = new VolumeMountBuilder()
        .withName("logs")
        .withMountPath("/logs")
        .build();

    Container tfContainer = new ContainerBuilder()
        .withName("tf-serving")
        .withImage(settings.getKubeRegistry() + "/tf:" + settings.getKubeTfImgVersion())
        .withImagePullPolicy(settings.getKubeImagePullPolicy())
        .withEnv(tfServingEnv)
        .withVolumeMounts(secretMount, logMount)
        .build();

    Container fileBeatContainer = new ContainerBuilder()
        .withName("filebeat")
        .withImage(settings.getKubeRegistry() + "/filebeat:" + settings.getKubeFilebeatImgVersion())
        .withImagePullPolicy(settings.getKubeImagePullPolicy())
        .withEnv(fileBeatEnv)
        .withVolumeMounts(logMount)
        .build();

    List<Container> containerList = Arrays.asList(tfContainer, fileBeatContainer);

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
        .withVolumes(secretVol, logs)
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
        .withPort(9999)
        .withTargetPort(new IntOrString(1234))
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
