/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.hops.common.Pair;

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
  
  public enum Host {
    LOAD_BALANCER, // load balancer IP
    EXTERNAL, // external IPs
    CLUSTER, // clusterIP within the Kubernetes cluster
    NODE // hostIP of one of the Kubernetes cluster nodes
  }
  
  public enum Port {
    HTTP("http2"),
    HTTPS("https"),
    STATUS("status-port"),
    TLS("tls");
    
    private final String value;
    
    private Port(String value) {
      this.value = value;
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
  
  public Pair<String, Integer> getIstioIngressHostPort() {
    return getIstioIngressHostPort(Host.NODE, Port.HTTP);
  }
  
  public Pair<String, Integer> getIstioIngressHostPort(Host hostType, Port portType) {
    Service ingressService = getIstioIngressService();
    String host = getIstioIngressHost(ingressService, hostType);
    Integer port = getIstioIngressPort(ingressService, portType, withExternalPort(hostType));
    return new Pair<>(host, port);
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
      case EXTERNAL: return spec.getExternalIPs().stream().findFirst().orElse(null); // TODO: (Javier) randomize
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
    // TODO: (Javier) test load balancer and external ips.
  }
}
