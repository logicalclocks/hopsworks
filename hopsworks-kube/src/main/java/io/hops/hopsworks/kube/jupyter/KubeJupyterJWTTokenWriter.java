/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.jupyter;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.jupyter.JupyterJWT;
import io.hops.hopsworks.common.jupyter.JupyterJWTTokenWriter;
import io.hops.hopsworks.common.jwt.JWTTokenWriter;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;

@Stateless
@KubeStereotype
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeJupyterJWTTokenWriter implements JupyterJWTTokenWriter {
  
  @Inject
  private JWTTokenWriter kubeJWTTokenWriter;
  
  @Override
  public String readToken(Project project, Users user) throws IOException {
    return kubeJWTTokenWriter.readToken(project, user);
  }
  
  @Override
  public void writeToken(Settings settings, JupyterJWT jupyterJWT) throws IOException {
    kubeJWTTokenWriter.writeToken(jupyterJWT);
  }
  
  @Override
  public void deleteToken(JupyterJWT jupyterJWT) throws KubernetesClientException {
    kubeJWTTokenWriter.deleteToken(jupyterJWT);
  }
}
