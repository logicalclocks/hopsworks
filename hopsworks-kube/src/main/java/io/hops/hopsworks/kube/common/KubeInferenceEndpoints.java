/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint.InferenceEndpointType;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeInferenceEndpoints {
  
  @EJB
  private Settings settings;
  @EJB
  private KubeIstioClientService kubeIstioClientService;
  @EJB
  private KubeClientService kubeClientService;
  
  private final HashMap<InferenceEndpointType, InferenceEndpoint> endpointsMap = new HashMap<>();
  
  public InferenceEndpoint getEndpoint(InferenceEndpointType endpointType) {
    InferenceEndpoint endpoint = endpointsMap.getOrDefault(endpointType, null);
    if (endpoint == null) {
      endpoint = findEndpoint(endpointType);
      cacheEndpoint(endpoint);
    }
    return endpoint;
  }
  
  private void cacheEndpoint(InferenceEndpoint endpoint) {
    endpointsMap.put(endpoint.getType(), endpoint);
  }
  
  private InferenceEndpoint findEndpoint(InferenceEndpointType endpointType) {
    if (settings.getKubeKServeInstalled()) {
      // if kserve, return istio endpoint
      return kubeIstioClientService.getIstioIngressEndpoint(endpointType);
    }
    switch (endpointType) {
      case NODE:
        List<String> hosts = kubeClientService.getReadyNodeList(); // random kubernetes node IP
        return new InferenceEndpoint(endpointType, hosts, null); // ports to be defined by the k8s service
      case LOAD_BALANCER:
        // get custom load balancer endpoint
      case KUBE_CLUSTER:
        // get custom kubernetes cluster ip
      default:
        return null;
    }
  }
}
