/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.security;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.security.CertificateHandler;
import io.hops.hopsworks.kube.common.KubeClientService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;

@Stateless
public class KubeCertificateHandler implements CertificateHandler {

  @EJB
  private KubeClientService kubeClientService;

  @Override
  public void generate(Project project, Users user, UserCerts userCerts) throws IOException {
    try {
      kubeClientService.createTLSSecret(project, user, userCerts.getUserKey(),
          userCerts.getUserCert(), userCerts.getUserKeyPwd());
    } catch (KubernetesClientException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void revoke(Project project, Users user) throws IOException {
    try {
      kubeClientService.deleteTLSSecret(project, user);
    } catch (KubernetesClientException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getClassName() {
    return KubeCertificateHandler.class.getName();
  }
}