/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.jupyter;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.jupyter.JupyterJWT;
import io.hops.hopsworks.common.jupyter.JupyterJWTTokenWriter;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.jupyter.JupyterJWTManager.TOKEN_FILE_NAME;
import static io.hops.hopsworks.kube.jupyter.KubeJupyterManager.JWT_SUFFIX;
import static java.util.logging.Level.FINEST;

@Stateless
@KubeStereotype
public class KubeJupyterJWTTokenWriter implements JupyterJWTTokenWriter {
  
  private static final Logger logger = Logger.getLogger(KubeJupyterJWTTokenWriter.class.getName());
  
  @EJB
  KubeClientService kubeClientService;
  
  @Override
  public void writeToken(Settings settings, JupyterJWT jupyterJWT) throws IOException {
    JupyterJWTTokenWriter.super.writeToken(settings, jupyterJWT);
    logger.log(FINEST, "Creating JWT secret for project " + jupyterJWT.project.getName() + " and user "
      + jupyterJWT.user.getUsername());
    kubeClientService.createOrUpdateSecret(jupyterJWT.project, jupyterJWT.user, JWT_SUFFIX,
      ImmutableMap.of(TOKEN_FILE_NAME, jupyterJWT.token.getBytes()));
  }
  
  @Override
  public void deleteToken(JupyterJWT jupyterJWT) throws KubernetesClientException {
    logger.log(FINEST, "Deleting JWT secret for project " + jupyterJWT.project.getName() + " and user "
      + jupyterJWT.user.getUsername());
    JupyterJWTTokenWriter.super.deleteToken(jupyterJWT);
    kubeClientService.deleteSecret(jupyterJWT.project, jupyterJWT.user, JWT_SUFFIX);
  }
}
