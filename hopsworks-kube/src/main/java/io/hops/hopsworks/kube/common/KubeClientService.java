/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public void createTLSSecret(Project project, Users user, byte[] keyStore, byte[] trustStore,
                              String keyPassword) throws KubernetesClientException {

    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeProjectUsername(kubeProjectNS, user);
    String projectUsername = getProjectUsername(project, user);

    Map<String, String> secretData = new HashMap<>();
    secretData.put(projectUsername + CERT_PASS_SUFFIX, Base64.getEncoder().encodeToString(keyPassword.getBytes()));
    secretData.put(projectUsername + KEYSTORE_SUFFIX, Base64.getEncoder().encodeToString(keyStore));
    secretData.put(projectUsername + TRUSTSTORE_SUFFIX, Base64.getEncoder().encodeToString(trustStore));

    Secret secret = new SecretBuilder()
        .withMetadata(getSecretMetadata(kubeProjectSecretName))
        .withData(secretData)
        .build();

    // Send request
    client.secrets().inNamespace(kubeProjectNS).createOrReplace(secret);
  }

  @Asynchronous
  public void deleteTLSSecret(Project project, Users user) throws KubernetesClientException {
    String kubeProjectNS = getKubeProjectName(project);
    String kubeProjectSecretName = getKubeProjectUsername(kubeProjectNS, user);

    client.secrets().inNamespace(kubeProjectNS).delete(
        new SecretBuilder().withMetadata(getSecretMetadata(kubeProjectSecretName)).build());
  }

  private ObjectMeta getSecretMetadata(String kubeProjectSecretName) {
    return new ObjectMetaBuilder()
        .withName(kubeProjectSecretName)
        .build();
  }

  @Asynchronous
  public void deleteDeployment(Project project, ObjectMeta deploymentMetadata)
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project) ;
    client.extensions().deployments().inNamespace(kubeProjectNs)
        .delete(new DeploymentBuilder().withMetadata(deploymentMetadata).build());
  }

  @Asynchronous
  public void deleteService(Project project, ObjectMeta serviceMetadata)
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project) ;
    client.services().inNamespace(kubeProjectNs)
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

  public DeploymentStatus getDeploymentStatus(Project project, String deploymentName)
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project);

    Deployment deployment = client.extensions().deployments().inNamespace(kubeProjectNs)
        .withName(deploymentName).get();
    return deployment == null ? null : deployment.getStatus();
  }

  public Service getServiceInfo(Project project, String serviceName)
      throws KubernetesClientException{
    String kubeProjectNs = getKubeProjectName(project);
    return client.services().inNamespace(kubeProjectNs).withName(serviceName).get();
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
}
