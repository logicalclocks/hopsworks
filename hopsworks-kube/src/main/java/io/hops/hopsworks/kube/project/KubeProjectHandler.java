/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.project;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.project.ProjectHandler;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeProjectHandler implements ProjectHandler {

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private KubeProjectSecrets kubeProjectSecrets;
  
  @Override
  public void preCreate(Project project) throws EJBException {
    try {
      kubeClientService.createProjectNamespace(project);
      kubeProjectConfigMaps.createConfigMaps(project);
      kubeProjectSecrets.createSecrets(project);
    } catch (Exception e) {
      String usrMsg = "";
      if (e instanceof EJBException && ((EJBException) e).getCausedByException() instanceof KubernetesClientException) {
        if (((KubernetesClientException) ((EJBException) e).getCausedByException()).getCode() == 409) {
          usrMsg = "Environment is not cleaned up yet. Please retry in a few seconds. If error persists, contact an " +
            "administrator. Reason: " +
            ((KubernetesClientException) ((EJBException) e).getCausedByException()).getStatus().getMessage();
        }
      }
      throw new EJBException(usrMsg, e);
    }
  }

  @Override
  public void postCreate(Project project) throws Exception {

  }

  @Override
  public void preDelete(Project project) throws Exception {

  }

  @Override
  public void postDelete(Project project) throws Exception {
    try {
      kubeClientService.deleteProjectNamespace(project);
      kubeProjectConfigMaps.deleteConfigMaps(project);
      kubeProjectSecrets.deleteSecrets(project);
    } catch (KubernetesClientException e) {
      throw new Exception(e);
    }
  }

  @Override
  public String getClassName() {
    return KubeProjectHandler.class.getName();
  }
}
