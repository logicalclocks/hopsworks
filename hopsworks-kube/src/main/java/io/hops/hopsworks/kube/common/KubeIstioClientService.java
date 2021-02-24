/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

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
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeIstioClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeIstioClientService.class.getName());

  
  public static final String ISTIOSYSTEM_NS = "istio-system";
  public static final String ISTIOSYSTEM_INGRESSSERVICE = "istio-ingressgateway";
  
  @EJB
  private KubeClientService kubeClientService;
  
  public Integer getIstioIngressNodePort(String name) throws KubernetesClientException {
    Service ingressService =
      kubeClientService.handleClientOp(
        (client) -> client.services().inNamespace(ISTIOSYSTEM_NS).withName(ISTIOSYSTEM_INGRESSSERVICE)
          .get());
    Optional<ServicePort> nodePort =
      ingressService.getSpec().getPorts().stream().filter(port -> port.getName().equals(name)).findFirst();
    
    return nodePort.map(ServicePort::getNodePort).orElse(null);
  }
  
  public Integer getIstioIngressNodePort() throws KubernetesClientException {
    return getIstioIngressNodePort("http2");
  }
}
