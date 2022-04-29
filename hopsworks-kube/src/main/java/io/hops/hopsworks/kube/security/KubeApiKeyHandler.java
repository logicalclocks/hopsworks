/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.security;

import io.hops.hopsworks.common.project.ProjectTeamRoleHandler;
import io.hops.hopsworks.common.user.UserAccountHandler;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyHandler;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKeyScope;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.INFO;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeApiKeyHandler implements ApiKeyHandler, UserAccountHandler, ProjectTeamRoleHandler {
  
  private static final Logger logger = Logger.getLogger(KubeApiKeyHandler.class.getName());
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private Settings settings;
  
  // ApiKey handler
  //
  // Replicate api keys to hops-system namespace in Kubernetes for Istio authentication
  
  @Override
  public void create(ApiKey apiKey) {
    if (settings.getKubeKServeInstalled()) {
      createServingApiKeySecret(apiKey);
    }
  }
  
  @Override
  public void delete(ApiKey apiKey) {
    if (settings.getKubeKServeInstalled()) {
      logger.log(INFO, "Deleting serving API key secret for user " + apiKey.getUser().getUsername());
      kubeClientService.deleteSecret(KubeServingUtils.HOPS_SYSTEM_NAMESPACE,
        kubeApiKeyUtils.getServingApiKeySecretName(apiKey.getPrefix()));
    }
  }
  
  @Override
  public boolean match(Collection<ApiKeyScope> scopes) {
    return scopes.stream().anyMatch(scope -> scope.getScope() == ApiScope.SERVING);
  }
  
  private void createServingApiKeySecret(ApiKey apiKey) {
    // TODO: (Javier) Update fabric8 to v5 and use PatchContext(Strategy.MergePatch)
    String secretName = kubeApiKeyUtils.getServingApiKeySecretName(apiKey.getPrefix());
    if (apiKey.getName().startsWith(KubeApiKeyUtils.SERVING_API_KEY_NAME) && apiKey.getReserved()) {
      // if serving api key, noop. The kube secret is created with the raw secret later on
      logger.log(INFO, "Postponing creation of serving API key secret with name " + secretName + " for user " +
        apiKey.getUser().getUsername());
      return;
    }
    
    kubeClientService.createOrUpdateSecret(
      KubeServingUtils.HOPS_SYSTEM_NAMESPACE,
      secretName,
      kubeApiKeyUtils.getApiKeySecretData(apiKey),
      kubeApiKeyUtils.getApiKeySecretLabels(apiKey)
    );
    logger.log(INFO,
      "Created serving API key secret with name " + secretName + " for user " + apiKey.getUser().getUsername());
  }
  
  // User account handler
  //
  // When an user is created/deleted, create or delete the serving api key in the hops-system namespace in Kubernetes.
  // This api key contains the raw secret used by KServe (download artifacts, connect to the Feature Store) and
  // HSML (inference requests)
  
  @Override
  public void create(Users user) throws Exception {
    if (settings.getKubeKServeInstalled()) {
      replicateUserServingApiKey(user);
    }
  }
  
  @Override
  public void update(Users user) throws Exception {
    if (settings.getKubeKServeInstalled()) {
      if (user.getStatus() != UserAccountStatus.ACTIVATED_ACCOUNT) {
        // if user is not activated, delete serving api key
        remove(user);
        return;
      }
      // if user is activated, ensure serving api key exists
      create(user);
    }
  }
  
  @Override
  public void remove(Users user) throws Exception {
    if (settings.getKubeKServeInstalled()) {
      // if an user is removed, remove the serving api key
      logger.log(INFO, "Delete serving API key secret for user " + user.getUsername());
      kubeApiKeyUtils.deleteServingApiKey(user);
    }
  }
  
  private void replicateUserServingApiKey(Users user) throws UserException, ApiKeyException {
    if (user.getStatus() != UserAccountStatus.ACTIVATED_ACCOUNT) {
      return; // only create serving api keys for activated users
    }
  
    // if an user account is created or activated, create a serving api key
    Optional<ApiKey> servingApiKey = kubeApiKeyUtils.getServingApiKey(user);
    if (servingApiKey.isPresent()) {
      logger.log(INFO, "Serving API key already created for user " + user.getUsername());
      return; // serving api key already created for this user
    }
    logger.log(INFO, "Create serving API key secret for user " + user.getUsername());
    kubeApiKeyUtils.createServingApiKey(user);
  }
  
  // Project team handler
  //
  // When an user is added/removed from a project, copy or delete the serving api key in the project namespace in
  // Kubernetes. This is needed because secrets cannot be shared across namespaces.
  
  @Override
  public void addMembers(Project project, List<Users> members, ProjectRoleTypes teamRole, boolean serviceUsers)
      throws Exception {
    if (settings.getKubeKServeInstalled()) {
      replicateMembersServingApiKeys(project, members, teamRole, serviceUsers);
    }
  }
  
  @Override
  public void updateMembers(Project project, List<Users> members, ProjectRoleTypes teamRole) {
    // noop
  }
  
  @Override
  public void removeMembers(Project project, List<Users> members) {
    if (settings.getKubeKServeInstalled()) {
      // remove serving api keys from project namespace,
      kubeApiKeyUtils.deleteServingApiKeySecrets(project, members);
    }
  }
  
  @Override
  public String getClassName() {
    return KubeApiKeyHandler.class.getName();
  }
  
  private void replicateMembersServingApiKeys(Project project, List<Users> members, ProjectRoleTypes teamRole,
    boolean serviceUsers) throws UserException, ApiKeyException, UnsupportedEncodingException {
    if (serviceUsers) {
      return; /* ignore service users */
    }
    if (teamRole == ProjectRoleTypes.UNDER_REMOVAL) {
      // if cleaning up, remove existing secrets
      removeMembers(project, members);
      return;
    }
  
    // if an user project role is updated, check the existence of a copy of the serving api key
    HashSet<String> users = kubeApiKeyUtils.getServingApiKeySecrets(project, members).stream()
      .map(s -> s.getMetadata().getLabels().get(KubeApiKeyUtils.API_KEY_USER_LABEL_NAME))
      .collect(Collectors.toCollection(HashSet::new));
    for (Users member : members) {
      if (!users.contains(member.getUsername())) {
        logger
          .log(INFO, "Copy serving API key for user " + member.getUsername() + " and project " + project.getName());
        // if the serving api key of a given user is not already in the project, copy it.
        kubeApiKeyUtils.copyServingApiKeySecret(project, member);
      }
    }
  }
}
