/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.jobs.ExecutionJWT;
import io.hops.hopsworks.common.jwt.JWTTokenWriter;
import io.hops.hopsworks.common.jwt.ServiceJWT;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.jupyter.JupyterJWTManager.TOKEN_FILE_NAME;
import static io.hops.hopsworks.kube.jupyter.KubeJupyterManager.JWT_SUFFIX;
import static java.util.logging.Level.INFO;

@Stateless
@KubeStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeJWTTokenWriter implements JWTTokenWriter {
  
  private static final Logger logger = Logger.getLogger(KubeJWTTokenWriter.class.getName());
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JWTController jwtController;
  @EJB
  private Settings settings;
  
  
  @Override
  public String readToken(Project project, Users user) {
    logger.log(INFO, "Getting JWT secret for project " + project.getName() + " and user " + user.getUsername());
    String kubeProjectNS = kubeClientService.getKubeProjectName(project);
    String kubeProjectSecretName = kubeClientService.getKubeDeploymentName(kubeProjectNS, user) + JWT_SUFFIX;
    Secret secret = kubeClientService.getSecret(kubeProjectNS, kubeProjectSecretName);
    return new String(Base64.getDecoder().decode(secret.getData().get(TOKEN_FILE_NAME)), Charset.defaultCharset());
  }
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void writeToken(ServiceJWT serviceJWT) {
    logger.log(INFO, "Creating JWT secret for project " + serviceJWT.project.getName() + " and user "
      + serviceJWT.user.getUsername());
    kubeClientService.createOrUpdateSecret(serviceJWT.project, serviceJWT.user, JWT_SUFFIX,
      ImmutableMap.of(TOKEN_FILE_NAME, serviceJWT.token.getBytes()));
  }
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void writeToken(ExecutionJWT executionJWT) {
    logger.log(INFO, "Creating JWT secret for project " + executionJWT.project.getName() + " and user "
      + executionJWT.user.getUsername() + " and job " + executionJWT.execution.getJob().getName() + " and execution " +
      executionJWT.execution.getId());
    Map<String, String> labels = new HashMap<>(2);
    labels.put("type", "jwt");
    labels.put("deployment", "execution");
    labels.put("job", executionJWT.execution.getJob().getName());
    labels.put("execution", String.valueOf(executionJWT.execution.getId()));
    labels.put("user", executionJWT.user.getUsername());
  
    kubeClientService.createOrUpdateSecret(executionJWT.execution, JWT_SUFFIX,
      ImmutableMap.of(TOKEN_FILE_NAME, executionJWT.token.getBytes()), labels);
  }
  
  @Override
  public void deleteToken(ServiceJWT serviceJWT) throws KubernetesClientException {
    logger.log(INFO, "Deleting JWT secret for project " + serviceJWT.project.getName() + " and user "
      + serviceJWT.user.getUsername());
    kubeClientService.deleteSecret(serviceJWT.project, serviceJWT.user, JWT_SUFFIX);
  }
  
  @Override
  public void deleteToken(ExecutionJWT executionJWT) {
    logger.log(INFO, "Deleting JWT secret for execution" + executionJWT.execution);
    kubeClientService.deleteSecret(executionJWT.execution, JWT_SUFFIX);
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Set<ExecutionJWT> getJWTs(Map<String, String> labels) throws SigningKeyNotFoundException {
    List<Secret> secrets = kubeClientService.getSecrets(labels);
    Set<ExecutionJWT> jwts = new HashSet<>();
    
    for (Secret secret : secrets) {
      String token = new String(Base64.getDecoder().decode(secret.getData().get("token.jwt")));
      //Get project
      if (executionFacade.findById(Integer.parseInt(secret.getMetadata().getLabels().get("execution"))).isPresent()
          && !JobState.getFinalStates().contains(
                  executionFacade.findById(Integer.parseInt(secret.getMetadata().getLabels().get("execution")))
                                                                                            .get().getState())) {
        Execution execution = executionFacade.findById(Integer.parseInt(secret.getMetadata().getLabels().get(
          "execution"))).get();
        ExecutionJWT jwt = new ExecutionJWT(execution, token);
        try {
          DecodedJWT decodedJWT = jwtController.verifyToken(token, settings.getJWTIssuer());
          jwt.expiration = DateUtils.date2LocalDateTime(decodedJWT.getExpiresAt());
          jwts.add(jwt);
        } catch (VerificationException e) {
          logger.log(Level.INFO,
            "JWT has expired but was not removed. Cleaning up now for execution:" + execution.getJob() + ", " +
              execution.getId());
          logger.info("Deleting secret: " + secret.getMetadata().getName());
          kubeClientService.deleteSecret(secret.getMetadata().getNamespace(), secret.getMetadata().getName());
        }
      } else {
        logger.info("Deleting secret: " + secret.getMetadata().getName());
        kubeClientService.deleteSecret(secret.getMetadata().getNamespace(), secret.getMetadata().getName());
      }
    }
    return jwts;
  }
}
