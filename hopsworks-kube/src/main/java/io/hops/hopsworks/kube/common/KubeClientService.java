/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import com.google.common.base.Suppliers;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobCondition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.hops.common.Pair;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.DockerResourcesConfiguration;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.StringJoiner;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.hops.hopsworks.common.util.Settings.CERT_PASS_SUFFIX;
import static io.hops.hopsworks.common.util.Settings.HOPS_USERNAME_SEPARATOR;
import static io.hops.hopsworks.common.util.Settings.KEYSTORE_SUFFIX;
import static io.hops.hopsworks.common.util.Settings.TRUSTSTORE_SUFFIX;
import static java.util.logging.Level.INFO;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeClientService.class.getName());
  
  @EJB
  private Settings settings;

  /*
   * When using this bean *DO NOT* use the client object directly as it might not be initialized
   * instead call the handleClientOp method which, besides checking for authentication exceptions
   * initialize the client
   */
  private KubernetesClient client = null;
  private com.google.common.base.Supplier<List<String>> nodeIPListCache;

  // Namespaces
  
  public void createProjectNamespace(Project project)
      throws KubernetesClientException {
    handleClientOp((client) ->
        client.namespaces().createNew()
            .withNewMetadata()
            .withName(getKubeProjectName(project))
            .endMetadata()
            .done());
  }
  
  public void deleteProjectNamespace(Project project)
      throws KubernetesClientException {
    handleClientOp((client) ->
        client.namespaces().withName(getKubeProjectName(project)).delete());
  }
  
  // Config maps
  
  @Asynchronous
  public void createOrUpdateConfigMap(Execution execution, String suffix, Map<String, String> filenameToContent)
    throws KubernetesClientException {
  
    String kubeProjectNS = getKubeProjectName(execution.getJob().getProject());
    String kubeProjectDeployment = getKubeDeploymentName(execution) + suffix;
    createOrUpdateConfigMap(kubeProjectNS, kubeProjectDeployment, filenameToContent, null);
  }
  
  @Asynchronous
  public void createOrUpdateConfigMap(Project project, Users user, String suffix, Map<String, String> filenameToContent)
    throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectDeployment = getKubeDeploymentName(kubeProjectNS, user) + suffix;
    createOrUpdateConfigMap(kubeProjectNS, kubeProjectDeployment, filenameToContent, null);
  }
  
  @Asynchronous
  public void createOrUpdateConfigMap(Project project, String suffix, Map<String, String> filenameToContent) {
    createOrUpdateConfigMap(project, suffix, filenameToContent, null);
  }
  @Asynchronous
  public void createOrUpdateConfigMap(Project project, String suffix, Map<String, String> filenameToContent,
    Map<String, String> labels) throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectDeployment = kubeProjectNS + suffix;
    createOrUpdateConfigMap(kubeProjectNS, kubeProjectDeployment, filenameToContent, labels);
  }
  
  @Asynchronous
  public void createOrUpdateConfigMap(String kubeProjectNS,
      String kubeProjectDeployment,
      Map<String, String> filenameToContent,
      Map<String, String> labels) throws KubernetesClientException {
    
    ConfigMap configMap = new ConfigMapBuilder()
        .withMetadata(
            new ObjectMetaBuilder()
                .withName(kubeProjectDeployment)
                .withLabels(labels)
                .build())
        .withData(filenameToContent)
        .build();
    handleClientOp((client) -> client.configMaps().inNamespace(kubeProjectNS)
        .createOrReplace(configMap));
  }
  
  @Asynchronous
  public void patchConfigMap(String kubeProjectNS, String kubeProjectConfigMap, Map<String, String> data,
    Map<String, String> labels) throws KubernetesClientException {
    // TODO: (Javier) Update fabric8 to v5 and use PatchContext(Strategy.MergePatch)
    ConfigMap cm = getConfigMap(kubeProjectNS, kubeProjectConfigMap);
    if (cm == null) {
      data.values().removeAll(Collections.singleton(null));
      createOrUpdateConfigMap(kubeProjectNS, kubeProjectConfigMap, data, labels);
      return; // config map was just created
    }
    if (labels != null) {
      // patch labels
      cm.getMetadata().getLabels().putAll(labels);
    }
    // patch config map data
    Map<String, String> cmData = cm.getData();
    if (cmData != null) {
      cmData.putAll(data);
      cmData.values().removeAll(Collections.singleton(null));
    } else {
      data.values().removeAll(Collections.singleton(null));
      if (data.size() == 0) {
        return; // config map doesn't exist and there is no data to patch
      }
      cmData = data;
    }
    cm.setData(cmData);
    LOGGER.log(INFO, "Patching config map of project " + kubeProjectNS + " with members: " + String.join(", ",
      cmData.keySet()));
    handleClientOp((client) -> client.configMaps().inNamespace(kubeProjectNS).withName(kubeProjectConfigMap).patch(cm));
  }
  
  public ConfigMap getConfigMap(String namespace, String name) {
    return handleClientOp((client) -> client.configMaps().inNamespace(namespace).withName(name).get());
  }
  
  @Asynchronous
  public void deleteConfigMap(String namespace, String secretName)
      throws KubernetesClientException {
    handleClientOp((client) -> client.configMaps().inNamespace(namespace).delete(
            new ConfigMapBuilder()
                .withMetadata(
                    new ObjectMetaBuilder()
                        .withName(secretName)
                        .build())
                .build()));
  }
  
  // Secrets
  
  @Asynchronous
  public void createOrUpdateSecret(Project project, Users user, String suffix, Map<String, byte[]> filenameToContent)
      throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeDeploymentName(kubeProjectNS, user) + suffix;
    
    createOrUpdateSecret(kubeProjectNS, kubeProjectSecretName, filenameToContent, null);
  }
  
  @Asynchronous
  public void createOrUpdateSecret(Execution execution, String suffix,
    Map<String, byte[]> filenameToContent, Map<String, String> labels)
    throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(execution.getJob().getProject());
    String kubeProjectSecretName =
      getKubeDeploymentName(execution) + suffix;
    createOrUpdateSecret(kubeProjectNS, kubeProjectSecretName, filenameToContent, labels);
  }
  
  @Asynchronous
  public void createOrUpdateSecret(String projectNamespace, String name,
      Map<String, byte[]> filenameToContent, Map<String, String> labels)
      throws KubernetesClientException {
    
    Secret secret = new SecretBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withName(name)
          .withLabels(labels)
          .build())
      .withData(filenameToContent
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Base64.getEncoder().encodeToString(e.getValue()))))
      .build();
    
    handleClientOp((client) -> client.secrets().inNamespace(projectNamespace)
        .createOrReplace(secret));
  }
  
  @Asynchronous
  public void deleteSecret(Project project, Users user, String suffix) throws KubernetesClientException {
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeDeploymentName(kubeProjectNS, user) + suffix;
    deleteSecret(kubeProjectNS, kubeProjectSecretName);
  }
  
  @Asynchronous
  public void deleteSecret(Execution execution, String suffix) throws KubernetesClientException {
    String kubeProjectNS = getKubeProjectName(execution.getJob().getProject());
    String kubeProjectSecretName = getKubeDeploymentName(execution) + suffix;
    deleteSecret(kubeProjectNS, kubeProjectSecretName);
  }
  
  @Asynchronous
  public void deleteSecret(String namespace, String secretName) throws KubernetesClientException {
    handleClientOp((client) -> client.secrets().inNamespace(namespace).delete(
        new SecretBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(secretName)
                    .build())
            .build()));
  }
  
  @Asynchronous
  public void deleteSecrets(Project project, Map<String, String> labels)
      throws KubernetesClientException {
    String kubeProjectNS = getKubeProjectName(project);
    deleteSecrets(kubeProjectNS, labels, null);
  }
  
  @Asynchronous
  public void deleteSecrets(Map<String, String> labels) throws KubernetesClientException {
    deleteSecrets(null, labels, null);
  }
  
  public void deleteSecrets(String namespace, Map<String, String> labels, Pair<String, String[]> labelIn) {
    handleClientOp((client) -> {
      FilterWatchListMultiDeletable<Secret, SecretList, Boolean, Watch, Watcher<Secret>> secrets = (namespace != null)
        ? client.secrets().inNamespace(namespace)
        : client.secrets().inAnyNamespace();
      FilterWatchListDeletable<Secret, SecretList, Boolean, Watch, Watcher<Secret>> secretsWithLabels = null;
      if (labels != null) {
        secretsWithLabels = secrets.withLabels(labels);
        if (labelIn != null)
          secretsWithLabels = secretsWithLabels.withLabelIn(labelIn.getL(), labelIn.getR());
      } else {
        if (labelIn != null) {
          secretsWithLabels = secrets.withLabelIn(labelIn.getL(), labelIn.getR());
        }
      }
      return (secretsWithLabels != null) ? secretsWithLabels.delete() : secrets.delete();
    });
  }
  
  public void createTLSSecret(Project project, Users user, byte[] keyStore, byte[] trustStore,
                              String keyPassword) throws KubernetesClientException {
    
    String projectUsername = getProjectUsername(project, user);

    Map<String, byte[]> secretData = new HashMap<>();
    secretData.put(projectUsername + CERT_PASS_SUFFIX, keyPassword.getBytes());
    secretData.put(projectUsername + KEYSTORE_SUFFIX, keyStore);
    secretData.put(projectUsername + TRUSTSTORE_SUFFIX, trustStore);

    createOrUpdateSecret(project, user, "", secretData);
  }

  @Asynchronous
  public void deleteTLSSecret(Project project, Users user) throws KubernetesClientException {
    deleteSecret(project, user, "");
  }
  
  public Secret getSecret(Project project, String name) {
    String kubeProjectNs = getKubeProjectName(project);
    return getSecret(kubeProjectNs, name);
  }
  
  public Secret getSecret(String namespace, String name) {
    return handleClientOp((client) -> client.secrets().inNamespace(namespace).withName(name).get());
  }
  
  public List<Secret> getSecrets(Map<String, String> labels) {
    return getSecrets(null, labels, null);
  }
  
  public List<Secret> getSecrets(Project project, Map<String, String> labels) {
    String kubeProjectNs = getKubeProjectName(project);
    return getSecrets(kubeProjectNs, labels, null);
  }
  public List<Secret> getSecrets(String namespace, Map<String, String> labels) {
    return getSecrets(namespace, labels, null);
  }
  
  public List<Secret> getSecrets(Map<String, String> labels, Pair<String, String[]> labelIn) {
    return getSecrets(null, labels, labelIn);
  }
  
  public List<Secret> getSecrets(String namespace, Map<String, String> labels, Pair<String, String[]> labelIn) {
    return handleClientOp((client) -> {
      FilterWatchListMultiDeletable<Secret, SecretList, Boolean, Watch, Watcher<Secret>> secrets = (namespace != null)
        ? client.secrets().inNamespace(namespace)
        : client.secrets().inAnyNamespace();
      FilterWatchListDeletable<Secret, SecretList, Boolean, Watch, Watcher<Secret>> secretsWithLabels = null;
      if (labels != null) {
        secretsWithLabels = secrets.withLabels(labels);
        if (labelIn != null)
          secretsWithLabels = secretsWithLabels.withLabelIn(labelIn.getL(), labelIn.getR());
      } else {
        if (labelIn != null)
          secretsWithLabels = secrets.withLabelIn(labelIn.getL(), labelIn.getR());
      }
      return (secretsWithLabels != null) ? secretsWithLabels.list().getItems() : secrets.list().getItems();
    });
  }
  
  // Deployments
  
  @Asynchronous
  public void createOrReplaceDeployment(Project project, Deployment deployment)
    throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    handleClientOp((client) -> client.apps().deployments().inNamespace(kubeProjectNs).createOrReplace(deployment));
  }
  
  @Asynchronous
  public void deleteDeployment(String namespace, String deploymentName)
    throws KubernetesClientException{
    deleteDeployment(
      namespace,
      new ObjectMetaBuilder()
        .withName(deploymentName)
        .build());
  }

  @Asynchronous
  public void deleteDeployment(Project project, ObjectMeta deploymentMetadata)
      throws KubernetesClientException{
    deleteDeployment(getKubeProjectName(project), deploymentMetadata);
  }
  
  @Asynchronous
  public void deleteDeployment(String namespace, ObjectMeta deploymentMetadata)
      throws KubernetesClientException {
    handleClientOp((client) -> client.apps().deployments().inNamespace(namespace)
            .delete(new DeploymentBuilder().withMetadata(deploymentMetadata).build()));
  }

  public void waitForDeployment(Project project, String deploymentName, Map<String, String> podLabels,
                                int maxAttempts) throws TimeoutException {
    RetryConfig retryConfig = RetryConfig.<Optional<Integer>>custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction.ofExponentialBackoff(400L, 1.3D))
      .retryOnResult(o -> o.map(replicas -> replicas < 1).orElse(true))
      .build();
    Retry retry = Retry.of("waitForDeployment: " + deploymentName, retryConfig);
    Optional<Integer> availableReplicas = Retry.decorateSupplier(retry, () -> getDeploymentStatus(project,
            deploymentName, 1)
      .map(o -> o.getAvailableReplicas()))
      .get();
    if (!availableReplicas.isPresent()) {
      //Get the pods
      List<Pod> podsList = getPodList(project, podLabels);
      StringJoiner joiner = new StringJoiner(",");
      if (!podsList.isEmpty()) {
        //Can there be more than one jupyter pod??
        Pod pod = podsList.get(0);
        PodStatus podStatus = pod.getStatus();
        for (PodCondition condition: podStatus.getConditions()) {
          //Since we have already timed out, all current pod conditions will be considered unhealthy
          joiner.add(condition.getType() + ":" + condition.getMessage());
        }
      }
      throw new TimeoutException("Timed out waiting for Jupyter pod startup. " + joiner.toString());
    }
  }

  public Optional<DeploymentStatus> getDeploymentStatus(Project project, String deploymentName, int maxAttempts) {
    RetryConfig retryConfig = RetryConfig.<DeploymentStatus>custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction.ofExponentialBackoff())
      .retryOnResult(x -> x == null)
      .build();
    Retry retry = Retry.of("getDeploymentStatus: " + deploymentName, retryConfig);
    Supplier<DeploymentStatus> supplier = Retry.decorateSupplier(retry, () ->
      this.getDeploymentStatus(project, deploymentName));
    return Optional.ofNullable(supplier.get());
  }
  
  public DeploymentStatus getDeploymentStatus(Project project, String deploymentName) {
    String kubeProjectNs = getKubeProjectName(project);
    
    Deployment deployment = handleClientOp((client) -> client.apps().deployments().inNamespace(kubeProjectNs)
      .withName(deploymentName).get());
    return deployment == null ? null : deployment.getStatus();
  }
  
  public DeploymentStatus getDeploymentStatus(Project project, Map<String, String> podLabels)
    throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    
    List<Deployment> deployments = handleClientOp((client) -> client.apps().deployments().inNamespace(kubeProjectNs)
      .withLabels(podLabels).list().getItems());
    
    return deployments.size() == 0 ? null : deployments.get(0).getStatus();
  }
  
  public Deployment rolloutDeployment(String namespace, String name) {
    return handleClientOp((client) -> client.apps().deployments().inNamespace(namespace).withName(name).
      rolling().restart());
  }
  
  // Services
  
  @Asynchronous
  public void createOrReplaceService(Project project, Service service) {
    String kubeProjectNs = getKubeProjectName(project);
    handleClientOp((client) -> client.services().inNamespace(kubeProjectNs)
      .createOrReplace(service));
  }
  
  @Asynchronous
  public void deleteService(String namespace, String serviceName)
    throws KubernetesClientException{
    deleteService(
      namespace,
      new ObjectMetaBuilder()
        .withName(serviceName)
        .build());
  }

  @Asynchronous
  public void deleteService(Project project, ObjectMeta serviceMetadata)
      throws KubernetesClientException{
    deleteService(getKubeProjectName(project), serviceMetadata);
  }
  
  @Asynchronous
  public void deleteService(String namespace, ObjectMeta serviceMetadata)
      throws KubernetesClientException {
    handleClientOp((client) -> client.services().inNamespace(namespace)
        .delete(new ServiceBuilder().withMetadata(serviceMetadata).build()));
  }
  
  public Optional<Service> getServiceInfo(Project project, String serviceName, int maxAttempts) {
    RetryConfig retryConfig = RetryConfig.<Service>custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction.ofExponentialBackoff())
      .retryOnResult(x -> x == null)
      .build();
    Retry retry = Retry.of("getServiceInfo: " + serviceName, retryConfig);
    Supplier<Service> supplier = Retry.decorateSupplier(retry, () -> this.getServiceInfo(project, serviceName));
    return Optional.ofNullable(supplier.get());
  }
  
  public Service getServiceInfo(Project project, String serviceName)
    throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    return handleClientOp((client) -> client.services().inNamespace(kubeProjectNs).withName(serviceName).get());
  }

  public List<Service> getServices(String label)
    throws KubernetesClientException {
    return handleClientOp((client) -> client.services().inAnyNamespace().withLabel(label).list().getItems());
  }
  
  // Jobs
  
  @Asynchronous
  public void deleteJob(String namespace, String kubeJobName)
      throws KubernetesClientException {
    handleClientOp((client) -> client.batch().jobs().inNamespace(namespace).withName(kubeJobName).delete());
  }
  
  @Asynchronous
  public void stopExecution(String prefix, Execution execution) throws KubernetesClientException{
    stopJob(getKubeProjectName(execution.getJob().getProject()), prefix + getKubeDeploymentName(execution));
  }
  
  @Asynchronous
  private void stopJob(String namespace, String kubeJobName)
      throws KubernetesClientException {
    Job job = handleClientOp((client) -> client.batch().jobs().inNamespace(namespace).withName(kubeJobName).get());
    if (job.getStatus().getConditions().isEmpty()) {
      JobCondition condition = new JobCondition();
      condition.setType("Failed");
      job.getStatus().getConditions().add(condition);
    }
    job.getStatus().getConditions().get(0).setType("Failed");
    handleClientOp((client) -> client.batch().jobs().inNamespace(namespace).withName(kubeJobName).replace(job));
    LOGGER.info("Stopped job: " + kubeJobName);
  }
  
  public List<Job> getJobs() {
    return handleClientOp((client) -> client.batch().jobs().inAnyNamespace().list().getItems());
  }

  public void createJob(Project project, Job job)
      throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    handleClientOp((client) -> client.batch().jobs().inNamespace(kubeProjectNs).create(job));
  }

  // Pods
  
  public List<Pod> getPodList(Project project, Map<String, String> podLabels)
    throws KubernetesClientException {
    return getPodList(getKubeProjectName(project), podLabels);
  }
  
  public List<Pod> getPodList(final String kubeProjectNs,
      final Map<String, String> podLabels) throws KubernetesClientException {
    return handleClientOp((client) -> client.pods().inNamespace(kubeProjectNs)
        .withLabels(podLabels).list().getItems());
  }
  
  public List<Pod> getPodList(Map<String, String> podLabels) throws KubernetesClientException {
    return handleClientOp((client) -> client.pods().inAnyNamespace().withLabels(podLabels).list().getItems());
  }
  
  public String getLogs(String namespace, String podName, Integer tailingLines,
    Integer limitBytes) {
    return handleClientOp((client) -> client.pods().inNamespace(namespace).withName(podName).limitBytes(limitBytes)
      .tailingLines(tailingLines).getLog());
  }

  public String getLogs(String namespace, String podName, String containerName, Integer tailingLines,
    Integer limitBytes) {
    return handleClientOp((client) -> client.pods().inNamespace(namespace).withName(podName).inContainer(containerName)
      .limitBytes(limitBytes).tailingLines(tailingLines).getLog());
  }
  
  // Names
  
  /* In Kubernetes, most of the regex to validate names do not allow the _.
   * For this reason we replace _ with - which is allowed.
   * Hopsworks projects cannot contain -.
   * All chars should be lowercase
   */
  public String getKubeProjectName(Project project) {
    return project.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
  }
  
  public String getKubeDeploymentName(Project project, Users user) {
    return this.getKubeDeploymentName(this.getKubeProjectName(project), user.getUsername());
  }
  
  public String getKubeDeploymentName(String kubeProjectNS, Users user) {
    return this.getKubeDeploymentName(kubeProjectNS, user.getUsername());
  }
  
  public String getKubeDeploymentName(Execution execution) {
    return this.getKubeDeploymentName(this.getKubeProjectName(execution.getJob().getProject()),
      execution.getUser().getUsername(),
      execution.getJob().getName(), Integer.toString(execution.getId()));
  }
  
  /**
   * From 0.6.0 usernames are alphanumeric only
   * Previous versions contain _ and -, to maintain compatibility we convert the _ to -
   * There might be some usernames that end with -, this is not allowed. In this case we add a 0 at the end
   * @param kubeProjectName
   * @param params username, jobName, executionId
   * @return kubernetes deployment name for Jupyter and Jobs
   */
  private String getKubeDeploymentName(String kubeProjectName, String... params) {
    StringBuilder kubeName = new StringBuilder(kubeProjectName).append("--");
    for (String param : params) {
      kubeName.append(param.toLowerCase().replaceAll("[^a-z0-9-]", "-"));
      if (kubeName.lastIndexOf("-") == kubeName.length() - 1) {
        kubeName.replace(kubeName.length() - 1, kubeName.length(), "0");
      }
      kubeName.append("--");
    }
    
    return kubeName.toString().replaceAll("--$","");
  }

  private String getProjectUsername(Project project, Users user) {
    return project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();
  }

  // Nodes
  
  public List<Node> getNodeList() throws KubernetesClientException {
    return handleClientOp((client) -> client.nodes().list().getItems());
  }

  private List<String> getReadyNodeIpListInternal() throws KubernetesClientException {
    // Extract node IPs
    return getNodeList().stream()
        // filter only ready nodes
        .filter(node -> node.getStatus().getConditions().stream().anyMatch(
          nodeCondition -> nodeCondition.getType().equalsIgnoreCase("ready")
            && nodeCondition.getStatus().equalsIgnoreCase("true")
        ))
        .flatMap(node -> node.getStatus().getAddresses().stream())
        .filter(nodeAddress -> nodeAddress.getType().equals("InternalIP"))
        .flatMap(nodeAddress -> Stream.of(nodeAddress.getAddress()))
        .collect(Collectors.toList());
  }

  public List<String> getReadyNodeList() {
    return nodeIPListCache.get();
  }

  public String getRandomReadyNodeIp() {
    List<String> nodeIPs = nodeIPListCache.get();
    return nodeIPs.get((int) (System.currentTimeMillis() % nodeIPs.size()));
  }
  
  public ResourceRequirements buildResourceRequirements(DockerResourcesConfiguration limitResources) {
    // only limit resources, used for jobs and jupyter containers as fixed resources
    return buildResourceRequirements(limitResources, null);
  }
  public ResourceRequirements buildResourceRequirements(DockerResourcesConfiguration limitResources,
    DockerResourcesConfiguration requestedResources) {
    // hard limits can be set using either docker max resources or serving max resources specified by the Hopsworks
    // variables: kube_docker_max... and kube_serving_max..., respectively.
    // we can use requestedResources to decide which resource configuration to use.
    Double maxCores;
    Integer maxMemory, maxGpus;
    if (requestedResources != null) {
      maxMemory = settings.getKubeServingMaxMemoryAllocation();
      maxCores = settings.getKubeServingMaxCoresAllocation();
      maxGpus = settings.getKubeServingMaxGpusAllocation();
    } else {
      maxMemory = settings.getKubeDockerMaxMemoryAllocation();
      maxCores = settings.getKubeDockerMaxCoresAllocation();
      maxGpus = settings.getKubeDockerMaxGpusAllocation();
    }
    
    // validate limits: in jobs, jupyter and deployments
    validateResources(limitResources, maxCores, maxMemory, maxGpus);
  
    // add limits
    ResourceRequirementsBuilder resources = new ResourceRequirementsBuilder();
    Integer memoryAmount = limitResources.getMemory() == -1 ? maxMemory : limitResources.getMemory();
    if (memoryAmount > -1) {
      resources.addToLimits("memory", new Quantity(memoryAmount + "Mi"));
    }
    Double coresAmount = limitResources.getCores() == -1 ? maxCores : limitResources.getCores();
    if (coresAmount > -1) {
      resources.addToLimits("cpu", new QuantityBuilder().withAmount(Double.toString(coresAmount)).build());
    }
    if(limitResources.getGpus() > 0) {
      resources.addToLimits("nvidia.com/gpu", new QuantityBuilder()
          .withAmount(Double.toString(limitResources.getGpus())).build());
    }
    
    // requests: only in deployments
    if (requestedResources != null) {
      // validate requested resources
      validateResources(requestedResources, limitResources, maxCores, maxMemory, maxGpus);
      
      // add requested resources
      resources
        .addToRequests("memory", new Quantity(requestedResources.getMemory() + "Mi"))
        .addToRequests("cpu", new QuantityBuilder().withAmount(
          Double.toString(requestedResources.getCores())).build());
      if(requestedResources.getGpus() > 0) {
        resources.addToRequests("nvidia.com/gpu", new QuantityBuilder()
          .withAmount(Double.toString(requestedResources.getGpus())).build());
      }
    }
    
    return resources.build();
  }
  
  private void validateResources(DockerResourcesConfiguration resources, Double maxCores, Integer maxMemory,
    Integer maxGpus) {
    validateResources(resources, null, maxCores, maxMemory, maxGpus);
  }
  private void validateResources(DockerResourcesConfiguration resources, DockerResourcesConfiguration limitResources,
    Double maxCores, Integer maxMemory, Integer maxGpus ) {
    
    // validate resources
    if(maxMemory > -1 && resources.getMemory() > maxMemory) {
      throw new IllegalArgumentException(String.format("Configured memory allocation of %s MB exceeds maximum memory " +
        "allocation allowed of %s MB", resources.getMemory(), maxMemory));
    } else if(maxCores > -1 && resources.getCores() > maxCores) {
      throw new IllegalArgumentException(String.format("Configured cores allocation of %s exceeds maximum core " +
        "allocation allowed of %s cores", resources.getCores(), maxCores));
    } else if(maxGpus > -1 && resources.getGpus() > maxGpus) {
      throw new IllegalArgumentException(String.format("Configured GPU allocation of %s exceeds maximum GPU " +
        "allocation allowed of %s GPUs", resources.getGpus(), maxGpus));
    }
    
    if (limitResources != null) {
      // validate memory limits
      if (maxMemory > -1) {
        if(limitResources.getMemory() > maxMemory) {
          throw new IllegalArgumentException(String.format("Configured limit memory allocation of %s MB exceeds maximum"
            + " memory allocation allowed of %s MB", limitResources.getMemory(), maxMemory));
        }
        if(resources.getMemory() > limitResources.getMemory()) {
          throw new IllegalArgumentException(String.format("Configured memory allocation request of %s MB exceeds " +
            "limit memory allocation limit of %s MB", resources.getMemory(), limitResources.getMemory()));
        }
      }
      // validate cores limits
      if (maxCores > -1) {
        if(limitResources.getCores() > maxCores) {
          throw new IllegalArgumentException(String.format("Configured limit cores allocation of %s exceeds maximum " +
            "core allocation allowed of %s cores", limitResources.getCores(), maxCores));
        }
        if(resources.getCores() > limitResources.getCores()) {
          throw new IllegalArgumentException(String.format("Configured cores allocation request of %s exceeds " +
            "limit core allocation limit of %s cores", resources.getCores(), limitResources.getCores()));
        }
      }
      // validate gpus limits
      if (maxGpus > -1) {
        if(limitResources.getGpus() > maxGpus) {
          throw new IllegalArgumentException(String.format("Configured limit GPU allocation of %s exceeds maximum GPU "
            + "allocation allowed of %s GPUs", limitResources.getGpus(), maxGpus));
        }
        if(resources.getGpus() > limitResources.getGpus()) {
          throw new IllegalArgumentException(String.format("Configured GPU allocation request of %s exceeds " +
            "limit GPU allocation limit of %s GPUs", resources.getGpus(), limitResources.getGpus()));
        }
      }
    }
  }
  
  public Pair<Integer, Integer> getNumReplicasRange(Integer minReplicas) {
    if (settings.getKubeServingMinNumInstances() == 0 && minReplicas != 0) {
      // scale-to-zero must be enabled if kube_serving_min_num_instances is 0
      throw new IllegalArgumentException(String.format("Scale-to-zero is required in this " +
        "cluster. Please, set the number of instances to 0."));
    }
    Integer maxReplicas = settings.getKubeServingMaxNumInstances();
    if (maxReplicas != -1 && minReplicas > maxReplicas) {
      throw new IllegalArgumentException(String.format("Configured number of instances %s exceeds the " +
        "maximum number of instances allowed of %s instances", minReplicas, maxReplicas));
    }
    return new Pair<>(minReplicas, maxReplicas);
  }
  
  public List<EnvVar> getEnvVars(Map<String, String> envVarsMap) {
    if(envVarsMap == null || envVarsMap.isEmpty()){
      return new ArrayList<>();
    }
    //Convert envVars map to List of envVars
    return envVarsMap.keySet().stream()
      .map(name -> new EnvVarBuilder().withName(name).withValue(envVarsMap.get(name)).build())
      .collect(Collectors.toList());
  }
  
  public interface KubeRunner<T> {
    T run(KubernetesClient client) throws KubernetesClientException;
  }

  public <T> T handleClientOp(KubeRunner<T> op) throws KubernetesClientException {
    if (client == null) {
      // initialize client
      initClient(false);
    }

    try{
      return op.run(client);
    } catch (KubernetesClientException ex){
      // TODO(Fabio): here in case of timeout exception we should probably retry a couple of times.
      if(ex.getCode() == 401 && ex.getMessage().contains("Token may have expired!")
          && (settings.getKubeType() == Settings.KubeType.EKS || settings.getKubeType() == Settings.KubeType.AKS)){
        LOGGER.info("Token may have expired! Reinitialize EKS Kube client.");
        initClient(true);
        return op.run(client);
      }
      LOGGER.log(Level.WARNING, "Failed to execute k8s commands", ex);
      throw ex;
    }
  }

  private synchronized void initClient(boolean forceInitialization) throws KubernetesClientException {
    if (!forceInitialization && client != null) {
      return;
    }

    LOGGER.info("Initialize " + settings.getKubeType() + " kube client");
    switch (settings.getKubeType()){
      case Local:
        Config config = new ConfigBuilder()
            .withUsername(settings.getKubeHopsworksUser())
            .withMasterUrl(settings.getKubeMasterUrl())
            .withCaCertFile(settings.getKubeCaCertfile())
            .withTrustStoreFile(settings.getKubeTruststorePath())
            .withTrustStorePassphrase(settings.getKubeTruststoreKey())
            .withKeyStoreFile(settings.getKubeKeystorePath())
            .withKeyStorePassphrase(settings.getKubeKeystoreKey())
            .withClientCertFile(settings.getKubeClientCertfile())
            .withClientKeyFile(settings.getKubeClientKeyfile())
            .withClientKeyPassphrase(settings.getKubeClientKeypass())
            .build();
        client = new DefaultKubernetesClient(config);
        break;
      case EKS:
        client = new DefaultKubernetesClient();
        break;
      case AKS:
        Config aksConfig = new ConfigBuilder()
            .withKeyStorePassphrase(settings.getKubeKeystoreKey())
            .withKeyStoreFile(settings.getKubeKeystorePath()).build();
        client = new DefaultKubernetesClient(aksConfig);
        break;
      default:
    }

    nodeIPListCache = Suppliers.memoizeWithExpiration(this::getReadyNodeIpListInternal, 10, TimeUnit.SECONDS);
  }
}
