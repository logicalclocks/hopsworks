/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
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
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeServingUtils;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;

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
public class KubeTfServingUtils {

  private final static String SERVING_ID = "SERVING_ID";
  private final static String MODEL_NAME = "MODEL_NAME";
  private final static String MODEL_DIR = "MODEL_DIR";
  private final static String MODEL_VERSION = "MODEL_VERSION";

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  
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
  
  public String getDeploymentPath(String servingName, Integer modelVersion, String verb) {
    StringBuilder pathBuilder = new StringBuilder()
      .append("/v1/models/")
      .append(servingName);
    
    // Append the version if the user specified it.
    if (modelVersion != null) {
      pathBuilder.append("/versions").append(modelVersion);
    }
    
    pathBuilder.append(verb);
    return pathBuilder.toString();
  }
  
  public String getServiceName(String servingId) {
    return "tf-serving-ser-" + servingId;
  }

  public Deployment buildServingDeployment(Project project, Users user,
      Serving serving) throws ServiceDiscoveryException {

    String servingIdStr = String.valueOf(serving.getId());
    String projectUser = project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();
    String hadoopHome = settings.getHadoopSymbolicLinkDir();
    String hadoopConfDir = hadoopHome + "/etc/hadoop";

    List<EnvVar> tfServingEnv = new ArrayList<>();
    tfServingEnv.add(new EnvVarBuilder().withName(SERVING_ID).withValue(servingIdStr).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_NAME).withValue(serving.getName()).build());
    tfServingEnv.add(new EnvVarBuilder().withName("PROJECT_NAME").withValue(project.getName().toLowerCase()).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_DIR)
        .withValue("hdfs://" + serviceDiscoveryController.constructServiceFQDN(
            ServiceDiscoveryController.HopsworksService.RPC_NAMENODE) + "/" + serving.getArtifactPath()).build());
    tfServingEnv.add(new EnvVarBuilder().withName(MODEL_VERSION)
        .withValue(String.valueOf(serving.getVersion())).build());
    tfServingEnv.add(new EnvVarBuilder().withName("TLS")
        .withValue(String.valueOf(settings.getHopsRpcTls())).build());
    tfServingEnv.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER")
        .withValue(projectUser).build());
    tfServingEnv.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    tfServingEnv.add(new EnvVarBuilder().withName("HADOOP_CONF_DIR").
        withValue(hadoopConfDir).build());
    tfServingEnv.add(new EnvVarBuilder().withName("HDFS_USER")
        .withValue(projectUser).build());
    tfServingEnv.add(new EnvVarBuilder().withName("ENABLE_BATCHING")
        .withValue(serving.isBatchingEnabled() ? "1" : "0").build());
    tfServingEnv.add(new EnvVarBuilder().withName("IS_KUBE")
        .withValue("true").build());
    tfServingEnv.add(new EnvVarBuilder().withName("FILE_SYSTEM_POLLING_INTERVAL_SECS")
        .withValue("10").build());
    tfServingEnv.add(new EnvVarBuilder().withName("GRPCPORT")
        .withValue("1233").build());
    tfServingEnv.add(new EnvVarBuilder().withName("RESTPORT")
        .withValue("1234").build());

    SecretVolumeSource secretVolume = new SecretVolumeSourceBuilder()
        .withSecretName(kubeClientService.getKubeDeploymentName(project, user))
        .build();

    Volume secretVol = new VolumeBuilder()
        .withName("certs")
        .withSecret(secretVolume)
        .build();

    Volume hadoopConf = new VolumeBuilder()
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

    VolumeMount hadoopConfMount = new VolumeMountBuilder()
        .withName("hadoopconf")
        .withReadOnly(true)
        .withMountPath(hadoopConfDir)
        .build();

    Container tfContainer = new ContainerBuilder()
        .withName("tf-serving")
        .withImage(projectUtils.getFullDockerImageName(project, true))
        .withImagePullPolicy(settings.getKubeImagePullPolicy())
        .withEnv(tfServingEnv)
        .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
        .withCommand("tfserving-launcher.sh")
        .withVolumeMounts(secretMount, hadoopConfMount)
        .build();

    List<Container> containerList = Arrays.asList(tfContainer);

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
        .withVolumes(secretVol, hadoopConf)
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
  
  public JSONObject buildInferenceServicePredictor(String artifactPath, Integer minReplicas, Boolean logger) {
    return new JSONObject() {
      {
        put("predictor", new JSONObject() {
          {
            put("minReplicas", minReplicas);
            put("logger", !logger ? null : new JSONObject() {
              {
                put("mode", KubeServingUtils.INFERENCE_LOGGER_MODE);
                put("url", String.format("http://%s:%s", KubeServingUtils.INFERENCE_LOGGER_HOST,
                  KubeServingUtils.INFERENCE_LOGGER_PORT));
              }
            });
            put("tensorflow", new JSONObject() {
              {
                put("storageUri", artifactPath);
              }
            });
          }
        });
      }
    };
  }
}
