/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
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
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.serving.TfServingFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.serving.tf.TfServingCommands;
import io.hops.hopsworks.common.serving.tf.TfServingController;
import io.hops.hopsworks.common.serving.tf.TfServingException;
import io.hops.hopsworks.common.serving.tf.TfServingStatusEnum;
import io.hops.hopsworks.common.serving.tf.TfServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.serving.tf.TfServingCommands.START;
import static io.hops.hopsworks.common.serving.tf.TfServingCommands.STOP;
import static io.hops.hopsworks.common.util.Settings.HOPS_USERNAME_SEPARATOR;

@Alternative
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeTfServingController implements TfServingController {

  private final static Logger logger = Logger.getLogger(KubeTfServingController.class.getName());

  public final static String SERVING_ID = "SERVING_ID";
  public final static String MODEL_NAME = "MODEL_NAME";
  public final static String MODEL_DIR = "MODEL_DIR";
  public final static String MODEL_VERSION = "MODEL_VERSION";

  @EJB
  private TfServingFacade tfServingFacade;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLEFacade;
  @EJB
  private Settings settings;

  @Override
  public List<TfServingWrapper> getTfServings(Project project) throws TfServingException {
    List<TfServing> tfServingList = tfServingFacade.findForProject(project);

    List<TfServingWrapper> tfServingWrapperList = new ArrayList<>();
    for (TfServing tfServing : tfServingList) {
      tfServingWrapperList.add(getTfServingInternal(project, tfServing));
    }

    return tfServingWrapperList;
  }

  @Override
  public TfServingWrapper getTfServing(Project project, Integer id) throws TfServingException {
    TfServing tfServing = tfServingFacade.findByProjectAndId(project, id);
    if (tfServing == null) {
      return null;
    }

    return getTfServingInternal(project, tfServing);
  }

  @Override
  public void deleteTfServings(Project project) throws TfServingException {
    // Nothing to do here. This function is called when a project is deleted.
    // During the project deletion, the namespace is deleted. Kubernetes takes care of removing
    // pods, namespaces and services
    return;
  }

  @Override
  public void deleteTfServing(Project project, Integer id) throws TfServingException{
    TfServing tfServing = tfServingFacade.acquireLock(project, id);

    String servingIdStr = String.valueOf(tfServing.getId());
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr));

      // If pods are currently running for this tfServing instance, kill them
      if (deploymentStatus != null) {
        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingIdStr));
      }

      Service serviceInfo = kubeClientService.getServiceInfo(project, getServiceName(servingIdStr));

      // if there is a service, delete it
      if (serviceInfo != null) {
        kubeClientService.deleteService(project, getServiceMetadata(servingIdStr));
      }
    } catch (KubernetesClientException e) {
      throw new TfServingException(RESTCodes.TfServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    // If the call to Kubernetes succeeded, then Kubernetes is taking care of terminating the pods.
    // Safe to remove the entry from the db
    tfServingFacade.delete(tfServing);
  }

  @Override
  public void createOrUpdate(Project project, Users user, TfServing newTfServing) throws TfServingException {
    if (newTfServing.getId() == null) {
      // Create request
      newTfServing.setCreated(new Date());
      newTfServing.setCreator(user);
      newTfServing.setProject(project);
      tfServingFacade.merge(newTfServing);
    } else {
      TfServing dbTfServing = tfServingFacade.updateDbObject(newTfServing, project);
      String servingIdStr = String.valueOf(dbTfServing.getId());
      // If pods are currently running for this tfServing instance, submit a new deployment to update them
      try {
        DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
            getDeploymentName(servingIdStr));
        if (deploymentStatus != null) {
          kubeClientService.createOrReplaceDeployment(project,
              buildTfServingDeployment(project, user, dbTfServing));
        }
      } catch (KubernetesClientException e) {
        throw new TfServingException(RESTCodes.TfServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
      } finally {
        tfServingFacade.releaseLock(project, newTfServing.getId());
      }
    }
  }

  @Override
  public void startOrStop(Project project, Users user, Integer tfServingId, TfServingCommands command)
      throws TfServingException {
    TfServing tfServing = tfServingFacade.acquireLock(project, tfServingId);

    String servingIdStr = String.valueOf(tfServing.getId());
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr));

      Service instanceService = kubeClientService.getServiceInfo(project,
          getServiceName(servingIdStr));

      Map<String, String> labelMap = new HashMap<>();
      labelMap.put("model", servingIdStr);
      List<Pod> podList = kubeClientService.getPodList(project, labelMap);

      TfServingStatusEnum status = getInstanceStatus(tfServing, deploymentStatus,
        instanceService, podList);


      if ((status == TfServingStatusEnum.RUNNING ||
          status == TfServingStatusEnum.STARTING ||
          // Maybe something went wrong during the first stopping, give the opportunity to the user to fix it.
          status == TfServingStatusEnum.STOPPING ||
          status == TfServingStatusEnum.UPDATING) && command == STOP) {

        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingIdStr));
        kubeClientService.deleteService(project, getDeploymentMetadata(servingIdStr));

      } else if (status == TfServingStatusEnum.STOPPED && command == START) {

        kubeClientService.createOrReplaceDeployment(project, buildTfServingDeployment(project, user, tfServing));
        kubeClientService.createOrReplaceService(project, buildTfServingService(tfServing));

      } else {
        throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERROR, Level.FINE,
            "Instance is already: " + status.toString());
      }
    } catch (KubernetesClientException e) {
      throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE,
          null, e.getMessage(), e);
    } finally {
      tfServingFacade.releaseLock(project, tfServingId);
    }
  }

  @Override
  public int getMaxNumInstances() {
    return 10; // TODO(Fabio) make this a parameter
  }

  @Override
  public String getClassName() {
    return KubeTfServingController.class.getName();
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

  private String getDeploymentName(String servingId) {
    return "tf-serving-dep-" + servingId;
  }

  private String getServiceName(String servingId) {
    return "tf-serving-ser-" + servingId;
  }

  private TfServingWrapper getTfServingInternal(Project project, TfServing tfServing)
      throws TfServingException {
    TfServingWrapper tfServingWrapper = new TfServingWrapper(tfServing);

    String servingIdStr = String.valueOf(tfServing.getId());

    DeploymentStatus deploymentStatus = null;
    Service instanceService = null;
    List<Pod> podList = null;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr));
      instanceService = kubeClientService.getServiceInfo(project,
          getServiceName(servingIdStr));

      Map<String, String> labelMap = new HashMap<>();
      labelMap.put("model", servingIdStr);
      podList = kubeClientService.getPodList(project, labelMap);
    } catch (KubernetesClientException ex) {
      throw new TfServingException(RESTCodes.TfServingErrorCode.STATUSERROR, Level.SEVERE, null, ex.getMessage(), ex);
    }

    TfServingStatusEnum status = getInstanceStatus(tfServing, deploymentStatus,
        instanceService, podList);

    tfServingWrapper.setStatus(status);

    switch (status) {
      case STARTING:
      case RUNNING:
      case UPDATING:
        tfServingWrapper.setNodePort(instanceService.getSpec().getPorts().get(0).getNodePort());
        tfServingWrapper.setAvailableReplicas(deploymentStatus.getAvailableReplicas() == null ? 0
            : deploymentStatus.getAvailableReplicas() );
        break;
      default:
        tfServingWrapper.setNodePort(null);
        tfServingWrapper.setAvailableReplicas(0);
    }

    return tfServingWrapper;
  }

  private TfServingStatusEnum getInstanceStatus(TfServing tfServing, DeploymentStatus deploymentStatus,
                                                Service instanceService, List<Pod> podList) {

    if (deploymentStatus != null && instanceService != null) {
      Integer availableReplicas = deploymentStatus.getAvailableReplicas();

      if (availableReplicas == null ||
          (!availableReplicas.equals(tfServing.getInstances()) && deploymentStatus.getObservedGeneration() == 1)) {
        // if there is a mismatch between the requested number of instances and the number actually active
        // in Kubernetes, and it's the 1st generation, the tfServing cluster is starting
        return  TfServingStatusEnum.STARTING;
      } else if (availableReplicas.equals(tfServing.getInstances())) {
        return  TfServingStatusEnum.RUNNING;
      } else {
        return TfServingStatusEnum.UPDATING;
      }
    } else {
      if (podList.isEmpty()) {
        return TfServingStatusEnum.STOPPED;
      } else {
        // If there are still Pod running, we are still in the stopping phase.
        return TfServingStatusEnum.STOPPING;
      }
    }
  }

  private Deployment buildTfServingDeployment(Project project, Users user, TfServing tfServing) {

    String servingIdStr = String.valueOf(tfServing.getId());

    List<EnvVar> envVarList = new ArrayList<>();
    envVarList.add(new EnvVarBuilder().withName(SERVING_ID).withValue(servingIdStr).build());
    envVarList.add(new EnvVarBuilder().withName(MODEL_NAME).withValue(tfServing.getModelName()).build());
    envVarList.add(new EnvVarBuilder().withName(MODEL_DIR)
        .withValue("hdfs://" + hdfsLEFacade.getSingleEndpoint() + tfServing.getModelPath()).build());
    envVarList.add(new EnvVarBuilder().withName(MODEL_VERSION)
        .withValue(String.valueOf(tfServing.getVersion())).build());
    envVarList.add(new EnvVarBuilder().withName("HADOOP_PROXY_USER")
        .withValue(project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername()).build());
    envVarList.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue("/certs").build());
    envVarList.add(new EnvVarBuilder().withName("HDFS_USER")
        .withValue(settings.getHdfsSuperUser()).build());
    envVarList.add(new EnvVarBuilder().withName("TLS")
        .withValue(String.valueOf(settings.getHopsRpcTls())).build());


    SecretVolumeSource secretVolume = new SecretVolumeSourceBuilder()
        .withSecretName(kubeClientService.getKubeProjectUsername(project.getName(), user))
        .build();

    Volume volume = new VolumeBuilder()
        .withName("certs")
        .withSecret(secretVolume)
        .build();

    VolumeMount volumeMount = new VolumeMountBuilder()
        .withName("certs")
        .withReadOnly(true)
        .withMountPath("/certs")
        .build();

    Container container = new ContainerBuilder()
        .withName("tf-serving")
        .withImage(settings.getKubeRegistry() + "/tf")
        .withImagePullPolicy("Always")
        .withEnv(envVarList)
        .withVolumeMounts(volumeMount)
        .build();

    LabelSelector labelSelector = new LabelSelectorBuilder()
        .addToMatchLabels("model", servingIdStr)
        .build();

    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("model", servingIdStr);

    ObjectMeta podMetadata = new ObjectMetaBuilder()
        .withLabels(labelMap)
        .build();

    PodSpec podSpec = new PodSpecBuilder()
        .withContainers(container)
        .withVolumes(volume)
        .build();

    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
        .withMetadata(podMetadata)
        .withSpec(podSpec)
        .build();

    DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
        .withReplicas(tfServing.getInstances())
        .withSelector(labelSelector)
        .withTemplate(podTemplateSpec)
        .build();

    return new DeploymentBuilder()
        .withMetadata(getDeploymentMetadata(servingIdStr))
        .withSpec(deploymentSpec)
        .build();
  }

  private Service buildTfServingService(TfServing tfServing) {
    String servingIdStr = String.valueOf(tfServing.getId());

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
