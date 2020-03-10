/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.security;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.security.CertificateHandler;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.persistence.entity.certificates.UserCerts;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;

@Stateless
public class KubeCertificateHandler implements CertificateHandler {

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private CertificatesMgmService certificatesMgmService;

  @Override
  public void generate(Project project, Users user, UserCerts userCert) throws IOException {

    try {
      String decryptedPassword = HopsUtils.decrypt(user.getPassword(), userCert.getUserKeyPwd(),
          certificatesMgmService.getMasterEncryptionPassword());

      kubeClientService.createTLSSecret(project, user, userCert.getUserKey(),
          userCert.getUserCert(), decryptedPassword);
    } catch (Exception e) {
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