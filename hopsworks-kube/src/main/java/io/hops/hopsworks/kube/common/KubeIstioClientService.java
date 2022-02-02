/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.hops.common.Pair;
import io.hops.hopsworks.kube.common.KubeIstioHostPort.Host;
import io.hops.hopsworks.kube.common.KubeIstioHostPort.Port;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.commons.lang.NotImplementedException;

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
  @EJB
  private KubeIstioHostPort kubeIstioHostPort;
  
  public String getIstioEndpoint(Pair<String, Integer> istioIngressHostPort) {
    return "http://" + istioIngressHostPort.getL() + ":" + istioIngressHostPort.getR();
  }
  
  public Pair<String, Integer> getIstioIngressHostPort() {
    return getIstioIngressHostPort(Host.NODE, Port.HTTP);
  }
  
  public Pair<String, Integer> getIstioIngressHostPort(Host hostType, Port portType) {
    Pair<String, Integer> hostPort = kubeIstioHostPort.getHostPort(hostType, portType);
    if (hostPort == null) {
      Service ingressService = getIstioIngressService();
      String host = getIstioIngressHost(ingressService, hostType);
      Integer port = getIstioIngressPort(ingressService, portType, withExternalPort(hostType));
      if (host != null && port != null) {
        kubeIstioHostPort.cacheHostPort(hostType, host, portType, port);
      }
      hostPort = new Pair<>(host, port);
    }
    return hostPort;
  }
  
  private Service getIstioIngressService() throws KubernetesClientException {
    return kubeClientService.handleClientOp(
      (client) -> client.services().inNamespace(ISTIOSYSTEM_NS).withName(ISTIOSYSTEM_INGRESSSERVICE)
        .get());
  }
  
  private String getIstioIngressHost(Service ingressService, Host hostType) throws KubernetesClientException {
    ServiceSpec spec = ingressService.getSpec();
    switch (hostType) {
      case NODE: return kubeClientService.getRandomReadyNodeIp();
      case CLUSTER: return spec.getClusterIP();
      case LOAD_BALANCER: return spec.getLoadBalancerIP();
      case EXTERNAL: return !spec.getExternalIPs().isEmpty()
        ? spec.getExternalIPs().get((int) (System.currentTimeMillis() % spec.getExternalIPs().size())) // random
        : null;
      default: throw new NotImplementedException();
    }
  }
  
  private Integer getIstioIngressPort(Service ingressService, Port portType, boolean external)
    throws KubernetesClientException {
    Optional<ServicePort> servicePort = ingressService.getSpec().getPorts().stream()
      .filter(port -> port.getName().equals(portType.toString())).findFirst();
    return servicePort.map(external ? ServicePort::getNodePort : ServicePort::getPort).orElse(null);
  }
  
  private boolean withExternalPort(Host hostType) {
    return hostType == Host.NODE || hostType == Host.LOAD_BALANCER;
  }
}
