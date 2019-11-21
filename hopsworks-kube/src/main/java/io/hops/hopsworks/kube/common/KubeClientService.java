/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.hops.hopsworks.common.util.Settings.CERT_PASS_SUFFIX;
import static io.hops.hopsworks.common.util.Settings.HOPS_USERNAME_SEPARATOR;
import static io.hops.hopsworks.common.util.Settings.KEYSTORE_SUFFIX;
import static io.hops.hopsworks.common.util.Settings.TRUSTSTORE_SUFFIX;


@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeClientService {

  @EJB
  private Settings settings;

  private KubernetesClient client = null;

  @PostConstruct
  private void initClient() {
    Config config = new ConfigBuilder()
        .withUsername(settings.getKubeUser())
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
  }

  public void createProjectNamespace(Project project) throws KubernetesClientException {
    client.namespaces().createNew()
        .withNewMetadata()
        .withName(getKubeProjectName(project))
        .endMetadata()
        .done();
  }

  public void deleteProjectNamespace(Project project) throws KubernetesClientException {
    client.namespaces().withName(getKubeProjectName(project)).delete();
  }
  
  @Asynchronous
  public void createOrUpdateConfigMap(Project project, Users user, String suffix, Map<String, String> filenameToContent)
      throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeProjectUsername(kubeProjectNS, user);
    
    ConfigMap configMap = new ConfigMapBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withName(kubeProjectSecretName + suffix)
          .build())
      .withData(filenameToContent)
      .build();
    
    client.configMaps().inNamespace(kubeProjectNS).createOrReplace(configMap);
  }

  @Asynchronous
  public void deleteConfigMap(String namespace, String secretName) throws KubernetesClientException {
    client.configMaps().inNamespace(namespace).delete(
      new ConfigMapBuilder()
        .withMetadata(
          new ObjectMetaBuilder()
            .withName(secretName)
            .build())
        .build());
  }
  
  @Asynchronous
  public void createOrUpdateSecret(Project project, Users user, String suffix, Map<String, byte[]> filenameToContent)
      throws KubernetesClientException {
    
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeProjectUsername(kubeProjectNS, user) + suffix;
    
    Secret secret = new SecretBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withName(kubeProjectSecretName)
          .build())
      .withData(filenameToContent
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Base64.getEncoder().encodeToString(e.getValue()))))
      .build();
    
    client.secrets().inNamespace(kubeProjectNS).createOrReplace(secret);
  }
  
  @Asynchronous
  public void deleteSecret(Project project, Users user, String suffix) throws KubernetesClientException {
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeProjectUsername(kubeProjectNS, user) + suffix;
    deleteSecret(kubeProjectNS, kubeProjectSecretName);
  }

  @Asynchronous
  public void deleteSecret(String namespace, String secretName) throws KubernetesClientException {
    client.secrets().inNamespace(namespace).delete(
      new SecretBuilder()
        .withMetadata(
          new ObjectMetaBuilder()
            .withName(secretName)
            .build())
        .build());
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
    throws KubernetesClientException{
    client.extensions().deployments().inNamespace(namespace)
      .delete(new DeploymentBuilder().withMetadata(deploymentMetadata).build());
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
    throws KubernetesClientException{
    client.services().inNamespace(namespace)
      .delete(new ServiceBuilder().withMetadata(serviceMetadata).build());
  }

  @Asynchronous
  public void createOrReplaceDeployment(Project project, Deployment deployment)
      throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    client.extensions().deployments().inNamespace(kubeProjectNs).createOrReplace(deployment);
  }

  @Asynchronous
  public void createOrReplaceService(Project project, Service service) {
    String kubeProjectNs = getKubeProjectName(project);
    client.services().inNamespace(kubeProjectNs).createOrReplace(service);
  }
  
  public void waitForDeployment(Project project, String deploymentName, int maxAttempts) throws TimeoutException {
    RetryConfig retryConfig = RetryConfig.<Optional<Integer>>custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction.ofExponentialBackoff(400L, 1.3D))
      .retryOnResult(o -> o.map(replicas -> replicas < 1).orElse(true))
      .build();
    Retry retry = Retry.of("waitForDeployment: " + deploymentName, retryConfig);
    Retry.decorateSupplier(retry, () -> getDeploymentStatus(project, deploymentName, 1)
        .map(o -> o.getAvailableReplicas()))
      .get()
      .orElseThrow(() -> new TimeoutException("Timed out waiting for Jupyter pod startup"));
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

  public DeploymentStatus getDeploymentStatus(Project project, String deploymentName)
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project);

    Deployment deployment = client.extensions().deployments().inNamespace(kubeProjectNs)
        .withName(deploymentName).get();
    return deployment == null ? null : deployment.getStatus();
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
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project);
    return client.services().inNamespace(kubeProjectNs).withName(serviceName).get();
  }

  public List<Service> getServices(String label) throws KubernetesClientException{
    return client.services().inAnyNamespace().withLabel(label).list().getItems();
  }

  public List<Pod> getPodList(Project project, Map<String, String> podLabels)
      throws KubernetesClientException {
    String kubeProjectNs = getKubeProjectName(project);
    return client.pods().inNamespace(kubeProjectNs)
            .withLabels(podLabels).list().getItems();
  }

  /* In Kubernetes, most of the regex to validate names do not allow the _.
   * For this reason we replace _ with - which is allowed.
   * Hopsworks projects cannot contain -.
   * All chars should be lowercase
   */
  public String getKubeProjectName(Project project) {
    return project.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
  }
  
  public String getKubeProjectUsername(Project project, Users user) {
    return this.getKubeProjectUsername(this.getKubeProjectName(project), user);
  }

  // From 0.6.0 usernames are alfanumeric only
  // Previous versions contain _ and -, to maintain compatibility we convert the _ to -
  // There might be some usernames that end with -, this is not allowed. In this case we add a 0 at the end
  public String getKubeProjectUsername(String kubeProjectName, Users user) {
    String kubeProjectUsername =
        kubeProjectName + "--" + user.getUsername().toLowerCase().replaceAll("[^a-z0-9-]", "-");
    if (kubeProjectUsername.endsWith("-")) {
      return kubeProjectUsername + "0";
    } else {
      return kubeProjectUsername;
    }
  }

  private String getProjectUsername(Project project, Users user) {
    return project.getName() + HOPS_USERNAME_SEPARATOR + user.getUsername();
  }

  public List<Node> getNodeList() throws KubernetesClientException {
    return client.nodes().list().getItems();

  }

  public List<String> getNodeIpList() throws KubernetesClientException {
    // Extract node IPs
    List<String> nodeIPList = getNodeList().stream()
        .flatMap(node -> node.getStatus().getAddresses().stream())
        .filter(nodeAddress -> nodeAddress.getType().equals("InternalIP"))
        .flatMap(nodeAddress -> Stream.of(nodeAddress.getAddress()))
        .collect(Collectors.toList());

    Collections.shuffle(nodeIPList);

    return nodeIPList;
  }
}
