/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.hops.common.Pair;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClientException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeIstioClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeIstioClientService.class.getName());

  
  public static final String ISTIOSYSTEM_NS = "istio-system";
  public static final String ISTIOSYSTEM_INGRESSSERVICE = "istio-ingressgateway";
  
  @EJB
  private KubeClientService kubeClientService;
  
  public Pair<String, Integer> getIstioIngressHostPort() {
    return getIstioIngressHostPort("http2");
  }
  
  public Pair<String, Integer> getIstioIngressHostPort(String name) {
    Service ingressService = getIstioIngressService();
    return new Pair<>(getIstioIngressClusterIp(ingressService), getIstioIngressNodePort(ingressService, name));
  }
  
  private Service getIstioIngressService() throws KubernetesClientException {
    return kubeClientService.handleClientOp(
      (client) -> client.services().inNamespace(ISTIOSYSTEM_NS).withName(ISTIOSYSTEM_INGRESSSERVICE)
        .get());
  }
  
  private String getIstioIngressClusterIp(Service ingressService) throws KubernetesClientException {
    return ingressService.getSpec().getClusterIP();
  }
  
  private Integer getIstioIngressNodePort(Service ingressService, String name) throws KubernetesClientException {
    Optional<ServicePort> servicePort =
      ingressService.getSpec().getPorts().stream().filter(port -> port.getName().equals(name)).findFirst();
    
    return servicePort.map(ServicePort::getPort).orElse(null);
  }
}
