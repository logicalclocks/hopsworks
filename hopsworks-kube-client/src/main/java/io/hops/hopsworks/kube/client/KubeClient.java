/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.kube.client;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.kube.client.utils.KubeSettings;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeClient {
  private static final Logger LOGGER = Logger.getLogger(KubeClient.class.getName());
  private KubernetesClient client = null;
  
  @EJB
  private KubeSettings settings;
  
  @PostConstruct
  public void init() {
    initClient(false);
  }
  
  private void initClient(boolean forceInitialization) throws KubernetesClientException {
    if (!forceInitialization && client != null) {
      return;
    }
    client = new KubernetesClientBuilder().build();
  }
  
  public interface KubeRunner<T> {
    T run(KubernetesClient client) throws KubernetesClientException;
  }
  
  public <T> T handleClientOp(KubeRunner<T> op) throws KubernetesClientException {
    if (client == null) {
      // initialize client
      initClient(false);
    }
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount <= maxRetries) {
      try {
        return op.run(client);
      } catch (KubernetesClientException ex) {
        if (retryCount < maxRetries && ex.getCode() == 504) { // Check for timeout error (504 gateway timeout){
          LOGGER.log(Level.INFO, "Timeout occurred while handling operation. Retrying... {0}/{1}",
            new Object[]{retryCount, maxRetries});
          retryCount++;
          continue;
        }
        if (retryCount < maxRetries && ex.getCode() == 401 && ex.getMessage().contains("Token may have expired!")) {
          LOGGER.log(Level.INFO,"Token may have expired! Reinitialize Kube client. Retrying... {0}/{1}",
            new Object[]{retryCount, maxRetries});
          initClient(true);
          retryCount = maxRetries; //retry only once
          continue;
        }
        LOGGER.log(Level.WARNING, "Failed to execute k8s commands", ex);
        throw ex;
      }
    }
    throw new KubernetesClientException("Max retry reached. Failed to handle operation.");
  }
  
  public ConfigMap getConfigMap(String namespace, String name)  {
    return handleClientOp((client) -> client.configMaps().inNamespace(namespace).withName(name).get());
  }
  
  public ConfigMap getConfigMap(String name)  {
    return getConfigMap(settings.get(KubeSettings.KubeSettingKeys.NAMESPACE), name);
  }
  
  public void patchConfigMap(String namespace, String name, ConfigMap configMap) {
    handleClientOp((client) -> client.configMaps()
      .inNamespace(namespace)
      .withName(name)
      .patch(configMap));
  }
  
  public void patchConfigMap(String name, ConfigMap configMap) {
    patchConfigMap(settings.get(KubeSettings.KubeSettingKeys.NAMESPACE), name, configMap);
  }
  
  public Secret getSecret(String namespace, String name) {
    return handleClientOp((client) -> client.secrets().inNamespace(namespace).withName(name).get());
  }
  
  public Secret getSecret(String name) {
    return getSecret(settings.get(KubeSettings.KubeSettingKeys.NAMESPACE), name);
  }
  
  public void patchSecret(String namespace, String name, Secret secret) {
    handleClientOp((client) -> client.secrets()
      .inNamespace(namespace)
      .withName(name)
      .patch(secret));
  }
  
  public void patchSecret(String name, Secret secret) {
    patchSecret(settings.get(KubeSettings.KubeSettingKeys.NAMESPACE), name, secret);
  }
}