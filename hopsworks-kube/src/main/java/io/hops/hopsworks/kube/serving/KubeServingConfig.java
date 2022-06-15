/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.kube.common.KubeInferenceEndpoints;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

@KubeStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeServingConfig implements ServingConfig {
  
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private KubeInferenceEndpoints kubeInferenceEndpoints;
  @EJB
  private Settings settings;
  
  @Override
  public Map<String, String> getEnvVars(Users user, boolean includeSecrets) throws ApiKeyException {
    return settings.getKubeKServeInstalled()
      ? getKServeEnvVars(user, includeSecrets) // kserve env vars
      : null;
  }
  
  @Override
  public String getClassName() {
    return KubeServingConfig.class.getName();
  }
  
  private Map<String, String> getKServeEnvVars(Users user, boolean includeSecrets) throws ApiKeyException {
    Map<String, String> envVars = new HashMap<>();
    
    if (includeSecrets) {
      // add serving api key
      Optional<ApiKey> apiKey = kubeApiKeyUtils.getServingApiKey(user);
      if (!apiKey.isPresent()) {
        throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.SEVERE,
          "Serving API key for user " + user.getUsername() + " not found");
      }
      String secretName = kubeApiKeyUtils.getServingApiKeySecretName(apiKey.get().getPrefix());
      String secret = kubeApiKeyUtils.getServingApiKeyValueFromKubeSecret(secretName);
      envVars.put("SERVING_API_KEY", secret);
    }
    
    return envVars;
  }
}
  