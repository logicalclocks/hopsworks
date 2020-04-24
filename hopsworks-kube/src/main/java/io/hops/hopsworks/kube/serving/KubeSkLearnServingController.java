/*
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
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
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
public class KubeSkLearnServingController {

  private final static String SERVING_ID = "SERVING_ID";
  private final static String ARTIFACT_PATH = "ARTIFACT_PATH";
  private final static String DEFAULT_FS = "DEFAULT_FS";
  private final static String PROJECT_NAME = "PROJECT_NAME";
  private final static String MODEL_NAME = "MODEL_NAME";

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLEFacade;
  @EJB
  private Settings settings;

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

  private ObjectMeta getServiceMetadata(String servingId) {
    return new ObjectMetaBuilder()
        .withName(getServiceName(servingId))
        .build();
  }

  public Deployment buildServingDeployment(Project project, Users user, Serving serving) {

    String servingIdStr = String.valueOf(serving.getId());
    String projectUser = project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();

    List<EnvVar> servingEnv = new ArrayList<>();
    servingEnv.add(new EnvVarBuilder().withName(SERVING_ID).withValue(servingIdStr).build());
    servingEnv.add(new EnvVarBuilder().withName(PROJECT_NAME).withValue(project.getName()).build());
    servingEnv.add(new EnvVarBuilder().withName(MODEL_NAME).withValue(serving.getName().toLowerCase()).build());
    servingEnv.add(new EnvVarBuilder().withName(ARTIFACT_PATH)
        .withValue("hdfs://" + hdfsLEFacade.getRPCEndpoint() + serving.getArtifactPath()).build());
    servingEnv.add(new EnvVarBuilder().withName(DEFAULT_FS)
        .withValue("hdfs://" + hdfsLEFacade.getRPCEndpoint()).build());
    servingEnv.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    servingEnv.add(new EnvVarBuilder().withName("TLS").withValue(String.valueOf(settings.getHopsRpcTls())).build());
    servingEnv.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER").withValue(projectUser).build());
    servingEnv.add(new EnvVarBuilder().withName("HDFS_USER").withValue(projectUser).build());

    List<EnvVar> fileBeatEnv = new ArrayList<>();
    fileBeatEnv.add(new EnvVarBuilder().withName("LOGPATH").withValue("/logs/*").build());
    fileBeatEnv.add(new EnvVarBuilder().withName("LOGSTASH").withValue(settings.getLogstashIp() + ":" +
        settings.getLogstashPortSkLearnServing()).build());

    SecretVolumeSource secretVolume = new SecretVolumeSourceBuilder()
        .withSecretName(kubeClientService.getKubeDeploymentName(project, user))
        .build();

    Volume secretVol = new VolumeBuilder()
        .withName("certs")
        .withSecret(secretVolume)
        .build();

    Volume logs = new VolumeBuilder()
        .withName("logs")
        .withEmptyDir(new EmptyDirVolumeSource())
        .build();

    HostPathVolumeSource pythonEnvHPSource = new HostPathVolumeSourceBuilder()
        .withPath(settings.getAnacondaProjectDir(project))
        .build();

    Volume pythonEnv = new VolumeBuilder()
        .withName("pythonenv")
        .withHostPath(pythonEnvHPSource)
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

    VolumeMount pythonEnvMount = new VolumeMountBuilder()
        .withName("pythonenv")
        .withMountPath("/pythonenv")
        .build();

    Container skLeanContainer = new ContainerBuilder()
        .withName("sklearn")
        .withImage(settings.getKubeRegistry() + "/sklearn:" + settings.getKubeSKLearnImgVersion())
        .withImagePullPolicy(settings.getKubeImagePullPolicy())
        .withEnv(servingEnv)
        .withVolumeMounts(secretMount, logMount, pythonEnvMount)
        .build();

    Container fileBeatContainer = new ContainerBuilder()
        .withName("filebeat")
        .withImage(settings.getKubeRegistry() + "/filebeat:" + settings.getKubeFilebeatImgVersion())
        .withImagePullPolicy(settings.getKubeImagePullPolicy())
        .withEnv(fileBeatEnv)
        .withVolumeMounts(logMount)
        .build();

    List<Container> containerList = Arrays.asList(skLeanContainer, fileBeatContainer);

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
        .withVolumes(secretVol, logs, pythonEnv)
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
    /*
    apiVersion: v1
    kind: Service
    metadata:
      name: skleaern
    spec:
      selector:
        model: id
      ports:
      - protocol: TCP
        port: 5000
        targetPort: 5000
      type: NodePort
     */
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
