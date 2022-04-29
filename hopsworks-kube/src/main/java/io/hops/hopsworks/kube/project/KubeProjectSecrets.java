/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.project;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.UnsupportedEncodingException;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeProjectSecrets {
  
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private Settings settings;
  
  public void createSecrets(Project project) throws ApiKeyException, UserException, UnsupportedEncodingException {
    if (settings.getKubeKServeInstalled()) {
      // copy serving api key secret of the project owner
      kubeApiKeyUtils.copyServingApiKeySecret(project, project.getOwner());
    }
  }
  
  public void deleteSecrets(Project project) {
    if (settings.getKubeKServeInstalled()) {
      // delete serving api key secret of the project owner
      kubeApiKeyUtils.deleteServingApiKeySecret(project, project.getOwner());
    }
  }
}
