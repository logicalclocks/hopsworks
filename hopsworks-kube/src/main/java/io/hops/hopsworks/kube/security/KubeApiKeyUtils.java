/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.security;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Secret;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyController;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.binary.Base64;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeApiKeyUtils {
  
  private static final Logger logger = Logger.getLogger(KubeApiKeyUtils.class.getName());
  
  @EJB
  private ApiKeyController apiKeyController;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private KubeServingUtils kubeServingUtils;
  
  public final static String AUTH_HEADER_API_KEY_PREFIX = "ApiKey ";
  public final static String AUTH_HEADER_BEARER_PREFIX = "Bearer ";
  // secrets
  public final static String API_KEY_SECRET_KEY = "secret";
  public final static String API_KEY_SALT_KEY = "salt";
  public final static String API_KEY_USER_KEY = "user";
  public final static String SERVING_API_KEY_SECRET_KEY = "apiKey";
  public final static String SERVING_API_KEY_NAME = "serving";
  private final static String SERVING_API_KEY_SECRET_SUFFIX = "--serving";
  private final static String SERVING_API_KEY_SECRET_PREFIX = "api-key";
  // labels
  public final static String API_KEY_NAME_LABEL_NAME = KubeServingUtils.LABEL_PREFIX + "/name";
  public final static String API_KEY_USER_LABEL_NAME = KubeServingUtils.LABEL_PREFIX + "/user";
  public final static String API_KEY_MODIFIED_LABEL_NAME = KubeServingUtils.LABEL_PREFIX + "/modified";
  
  public Optional<ApiKey> getServingApiKey(Users user) {
    // get user's serving api key stored in Hopsworks
    List<ApiKey> apiKeys = apiKeyController.getKeys(user);
    String servingApiKeyName = getServingApiKeyName(user);
    return apiKeys.stream().filter(key -> key.getName().equals(servingApiKeyName)).findAny();
  }
  
  public List<Secret> getServingApiKeySecrets(Project project, List<Users> members) {
    // get multiple users' serving api key secrets stored in a project namespace
    Map<String, String> labels = getApiKeySecretLabels(true, null, null, null);
    Pair<String, String[]> labelIn = getApiKeySecretUserLabelInSelector(members);
    String namespace = kubeClientService.getKubeProjectName(project);
    return kubeClientService.getSecrets(namespace, labels, labelIn);
  }
  
  public Pair<ApiKey, String> createServingApiKey(Users user) throws ApiKeyException, UserException {
    // One reserved apikey per user is created for model serving with name "serving". This apikey is stored in a
    // kubernetes secret in the hops-system namespace and removed when the user is deleted.
    return createServingApiKeyAndSecrets(user);
  }
  
  public void deleteServingApiKey(Users user) throws ApiKeyException {
    // delete api key
    apiKeyController.deleteKey(user, getServingApiKeyName(user));
    // delete every secret copy in any namespace
    deleteServingApiKeySecrets(user);
  }
  
  public void deleteServingApiKeySecrets(Project project, List<Users> members) {
    // delete users' serving api key secrets from a given project namespace
    Map<String, String> labels = getApiKeySecretLabels(true, null, null, null);
    Pair<String, String[]> labelIn = getApiKeySecretUserLabelInSelector(members);
    String namespace = kubeClientService.getKubeProjectName(project);
    kubeClientService.deleteSecrets(namespace, labels, labelIn);
  }
  
  public void deleteServingApiKeySecret(Project project, Users user) {
    // delete user's serving api key secret from project namespace
    String secretName = getProjectServingApiKeySecretName(user);
    String namespace = kubeClientService.getKubeProjectName(project);
    kubeClientService.deleteSecret(namespace, secretName);
  }
  
  public void copyServingApiKeySecret(Project project, Users user)
    throws ApiKeyException, UserException {
    // Since secrets cannot be shared across namespaces, the serving apikey is copied to each project the user is
    // member of. The apikey is used for downloading the artifact, connecting to the Feature Store from the
    // transformer and sending request to Istio from HSML when running within the Hopsworks cluster, among other things.
    logger.log(INFO,
      "Copying serving API key secret for user " + user.getUsername() + " into project " + project.getName());
    String rawSecret = null;
    Optional<ApiKey> apiKey = getServingApiKey(user);
    if (!apiKey.isPresent()) {
      // if the serving api key is not present, create it
      Pair<ApiKey, String> pair = createServingApiKey(user);
      apiKey = Optional.of(pair.getL());
      rawSecret = pair.getR();
    }
    if (rawSecret == null) {
      String secretName = getServingApiKeySecretName(apiKey.get().getPrefix());
      rawSecret = getServingApiKeyValueFromKubeSecret(user.getUsername(), secretName);
    }
    
    // copy it to the project namespace
    createProjectServingApiKeySecret(project, user, apiKey.get(), rawSecret);
  }
  
  public String getServingApiKeyName(Users user) {
    return SERVING_API_KEY_NAME + "_" + user.getUsername() + "_" + user.getUid();
  }
  
  public String getServingApiKeySecretName(String apiKeyPrefix) {
    // Kubernetes secret names are DNS-1123 subdomain names and must consist of lower case
    // alphanumeric characters, '-' or '.', and must start and end with an alphanumeric character
    StringBuilder prefixWithMask = new StringBuilder("api-key-" + apiKeyPrefix.toLowerCase() + "-");
    for (char c: apiKeyPrefix.toCharArray()) {
      prefixWithMask.append(Character.isUpperCase(c) ? '1' : '0');
    }
    return prefixWithMask.toString();
  }
  
  public String getServingApiKeySecretKeyName() {
    return SERVING_API_KEY_SECRET_KEY;
  }
  
  public String getProjectServingApiKeySecretName(Users user) {
    // get user's serving api key secret name (stored in hops-system namespace)
    return SERVING_API_KEY_SECRET_PREFIX + "-" + user.getUsername() + SERVING_API_KEY_SECRET_SUFFIX;
  }
  
  public Map<String, byte[]> getApiKeySecretData(ApiKey apiKey) {
    return getApiKeySecretData(apiKey, null);
  }
  public Map<String, byte[]> getApiKeySecretData(ApiKey apiKey, String secret) {
    return new HashMap<String, byte[]>(){
      {
        put(API_KEY_SECRET_KEY, apiKey.getSecret().getBytes());
        put(API_KEY_SALT_KEY, apiKey.getSalt().getBytes());
        put(API_KEY_USER_KEY, apiKey.getUser().getUsername().getBytes());
        if (secret != null) { put(SERVING_API_KEY_SECRET_KEY, secret.getBytes()); }
      }
    };
  }
  
  public Map<String, String> getApiKeySecretLabels(ApiKey apiKey) {
    return getApiKeySecretLabels(apiKey.getReserved(), apiKey.getName(), apiKey.getUser().getUsername(),
      apiKey.getModified());
  }
  
  public String getServingApiKeyValueFromKubeSecret(String username, String secretName) throws ApiKeyException {
    Secret secret = null;
    int retry = 15;
    do {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.log(INFO,
          "Cannot wait before retrieving the serving API key secret for user " + username);
        // ignore it, and try fetching the secret anyways
      }
      // get secret from hops-system
      secret = kubeClientService.getSecret(KubeServingUtils.HOPS_SYSTEM_NAMESPACE, secretName);
      if (secret != null) {
        break;
      }
      logger.info("Cannot find serving API key secret for user " + username + " (retries left: " + retry + ").");
    } while (retry-- > 0);
    
    if (secret == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.SEVERE,
        "Serving API key secret with name " + secretName + " not found in hops-system");
    }
    if (secret.getData() == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.SEVERE, "Serving API key secret " +
        "with name " + secretName + " in hops-system is empty");
    }
    if (!secret.getData().containsKey(SERVING_API_KEY_SECRET_KEY)) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.SEVERE, "Serving API key not found " +
        "in secret " + secretName + " in hops-system");
    }
    String encodedSecret = secret.getData().get(SERVING_API_KEY_SECRET_KEY);
    if (Strings.isNullOrEmpty(encodedSecret)) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.SEVERE, "Serving API key secret " +
        "value is null or empty for secret " + secretName + " in hops-system");
    }
    // decode secret
    byte[] secretBytes = Base64.decodeBase64(encodedSecret);
    return new String(secretBytes, StandardCharsets.UTF_8);
  }
  
  private Pair<ApiKey, String> createServingApiKeyAndSecrets(Users user) throws ApiKeyException, UserException {
    // One reserved apikey per user is created for model serving with name "serving". This apikey is stored in a
    // kubernetes secret in the hops-system namespace and removed when the user is deleted.
    Set<ApiScope> scopes = getServingApiKeyScopes();
    String servingApiKeyName = getServingApiKeyName(user);
    String secret = apiKeyController.createNewKey(user, servingApiKeyName, scopes, true);
    logger.log(INFO, "Created new serving API key for user " + user.getUsername());
    
    // create Kubernetes secrets
    ApiKey apiKey = apiKeyController.getApiKey(secret);
    // -- in hops-system
    createServingApiKeySecret(user, apiKey, secret);
    // -- in all project namespaces
    for(ProjectTeam projectTeam : projectTeamFacade.findActiveByMember(user)) {
      createProjectServingApiKeySecret(projectTeam.getProject(), user, apiKey, secret);
    }
    
    return new Pair(apiKey, secret);
  }
  
  private void createServingApiKeySecret(Users user, ApiKey apiKey, String secret) {
    // Create the serving api key secret in hops-system namespace
    String secretName = getServingApiKeySecretName(apiKey.getPrefix());
    Map<String, String> labels = getApiKeySecretLabels(true, apiKey.getName(), user.getUsername(),
      apiKey.getModified());
    Map<String, byte[]> data = getApiKeySecretData(apiKey, secret);
    kubeClientService.createOrUpdateSecret(KubeServingUtils.HOPS_SYSTEM_NAMESPACE, secretName, data, labels);
    logger.log(INFO, "Created serving API key secret in hops-system for user " + user.getUsername());
    
    // Wait until secret is available
    int retry = 3;
    do {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.log(INFO,
          "Cannot wait for the creation of the serving API key secret for user " + user.getUsername());
        e.printStackTrace();
        break;
      }
      Secret kubeSecret = kubeClientService.getSecret(KubeServingUtils.HOPS_SYSTEM_NAMESPACE, secretName);
      if (kubeSecret != null) return; // kube secret is available
    } while(retry-- > 0);
    
    logger.log(INFO,
      "Created serving API key secret is not available yet in hops-system for user " + user.getUsername());
  }
  
  private void createProjectServingApiKeySecret(Project project, Users user, ApiKey apiKey, String secret) {
    // Create user's serving api key secret in a project namespace
    Map<String, String> labels = getApiKeySecretLabels(true, apiKey.getName(), user.getUsername(),
      apiKey.getModified());
    String secretName = getProjectServingApiKeySecretName(user);
    HashMap<String, byte[]> data = new HashMap<>();
    data.put(SERVING_API_KEY_SECRET_KEY, secret.getBytes());
    String namespace = kubeClientService.getKubeProjectName(project);
    kubeClientService.createOrUpdateSecret(namespace, secretName, data, labels);
    logger.log(INFO,
      "Created serving API key secret in project " + project.getName() + " for user " + user.getUsername());
  }
  
  private void deleteServingApiKeySecrets(Users user) {
    // delete user's serving api key secrets from any namespace
    String servingApiKeyName = getServingApiKeyName(user);
    Map<String, String> labels = getApiKeySecretLabels(true, servingApiKeyName, user.getUsername(), null);
    kubeClientService.deleteSecrets(labels);
    logger.log(INFO, "Deleted serving API key secrets for user " + user.getUsername());
  }
  
  private Map<String, String> getApiKeySecretLabels(Boolean reserved, String apiKeyName, String username,
      Date modified) {
    Map<String, String> labels = kubeServingUtils.getServingScopeLabels(reserved);
    if (apiKeyName != null) { labels.put(API_KEY_NAME_LABEL_NAME, apiKeyName); }
    if (username != null) { labels.put(API_KEY_USER_LABEL_NAME, username); }
    if (modified != null) { labels.put(API_KEY_MODIFIED_LABEL_NAME, String.valueOf(modified.getTime())); }
    return labels;
  }
  
  private Pair<String, String[]> getApiKeySecretUserLabelInSelector(List<Users> users) {
    return new Pair<>(API_KEY_USER_LABEL_NAME, users.stream().map(Users::getUsername).toArray(String[]::new));
  }
  
  private Set<ApiScope> getServingApiKeyScopes(){
    return new HashSet<ApiScope>() {
      {
        add(ApiScope.DATASET_VIEW); // storage-initializer: download the model artifact
        add(ApiScope.KAFKA); // inference-logger: get the topic schema
        add(ApiScope.PROJECT); // transformer: get project details
        add(ApiScope.FEATURESTORE); // transformer: get feature vector and transformations from the feature store
        add(ApiScope.MODELREGISTRY); // predictor and transformer: download model artifact using hsml
        add(ApiScope.SERVING); // hops-util-py/hsml: inference requests through Istio
        add(ApiScope.PYTHON_LIBRARIES); // hopsworks sdk: opensearch client
      }
    };
  }
}
