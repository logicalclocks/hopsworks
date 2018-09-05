/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.project;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.project.ProjectHandler;
import io.hops.hopsworks.kube.common.KubeClientService;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class KubeProjectHandler implements ProjectHandler {

  @EJB
  private KubeClientService kubeClientService;

  @Override
  public void preCreate(Project project) throws Exception {
    try {
      kubeClientService.createProjectNamespace(project);
    } catch (KubernetesClientException e) {
      throw new Exception(e);
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
    } catch (KubernetesClientException e) {
      throw new Exception(e);
    }
  }

  @Override
  public String getClassName() {
    return KubeProjectHandler.class.getName();
  }
}
