/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.project;

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
  
  public void createSecrets(Project project) throws ApiKeyException, UserException, UnsupportedEncodingException {
    // copy serving api key secret of the project owner
    kubeApiKeyUtils.copyServingApiKeySecret(project, project.getOwner());
  }
  
  public void deleteSecrets(Project project) {
    // delete serving api key secret of the project owner
    kubeApiKeyUtils.deleteServingApiKeySecret(project, project.getOwner());
  }
}
