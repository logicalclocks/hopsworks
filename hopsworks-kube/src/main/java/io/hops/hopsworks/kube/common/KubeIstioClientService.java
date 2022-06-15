/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint.InferenceEndpointType;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.serving.inference.InferencePort;
import io.hops.hopsworks.common.serving.inference.InferencePort.InferencePortName;
import org.apache.commons.lang.NotImplementedException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeIstioClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeIstioClientService.class.getName());
  
  public static final String ISTIOSYSTEM_NS = "istio-system";
  public static final String ISTIOSYSTEM_INGRESSSERVICE = "istio-ingressgateway";
  
  @EJB
  private KubeClientService kubeClientService;
  
  public InferenceEndpoint getIstioIngressEndpoint(InferenceEndpointType endpointType) {
    Service ingressService = getIstioIngressService();
    List<String> hosts = getIstioIngressHosts(ingressService, endpointType);
    List<InferencePort> ports = getIstioIngressPorts(ingressService, withExternalPort(endpointType));
    return new InferenceEndpoint(endpointType, hosts, ports);
  }
  
  private Service getIstioIngressService() throws KubernetesClientException {
    return kubeClientService.handleClientOp(
      (client) -> client.services().inNamespace(ISTIOSYSTEM_NS).withName(ISTIOSYSTEM_INGRESSSERVICE)
        .get());
  }
  
  private List<String> getIstioIngressHosts(Service ingressService, InferenceEndpointType endpointType)
      throws KubernetesClientException {
    switch (endpointType) {
      case NODE: return kubeClientService.getReadyNodeList(); // random kubernetes node IP
      case KUBE_CLUSTER:
        // ingress gateway IP within the k8s network
        return Collections.singletonList(ingressService.getSpec().getClusterIP());
      case LOAD_BALANCER: // load balancer external IP
        LoadBalancerStatus loadBalancer = ingressService.getStatus().getLoadBalancer();
        if (loadBalancer == null) return null;
        List<LoadBalancerIngress> ingresses = loadBalancer.getIngress();
        if (ingresses == null || ingresses.isEmpty()) return null;
        return ingresses.stream().map(
          i -> i.getHostname() != null ? i.getHostname() : i.getIp()).collect(Collectors.toList());
      default: throw new NotImplementedException();
    }
  }
  
  private List<InferencePort> getIstioIngressPorts(Service ingressService, boolean external)
      throws KubernetesClientException {
    return ingressService.getSpec().getPorts().stream()
      .map(p -> new InferencePort(InferencePortName.of(p.getName()), external ? p.getPort() : p.getNodePort()))
      .collect(Collectors.toList());
  }
  
  private boolean withExternalPort(InferenceEndpointType endpointType) {
    return endpointType == InferenceEndpointType.KUBE_CLUSTER || endpointType == InferenceEndpointType.LOAD_BALANCER;
  }
}
