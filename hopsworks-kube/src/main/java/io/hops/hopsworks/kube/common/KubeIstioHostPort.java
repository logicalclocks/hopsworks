/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.hops.common.Pair;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeIstioHostPort {
  
  public enum Host {
    LOAD_BALANCER, // load balancer IP
    EXTERNAL, // external IPs
    CLUSTER, // clusterIP within the Kubernetes cluster
    NODE // hostIP of one of the Kubernetes cluster nodes (random)
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
  
  class HostPortKey {
    private Host host;
    private Port port;
    
    public HostPortKey(Host host, Port port) {
      this.host = host;
      this.port = port;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof HostPortKey) {
        HostPortKey k = (HostPortKey) obj;
        return host.equals(k.host) && port.equals(k.port);
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return (host.toString() + port.toString()).hashCode();
    }
  }
  
  // host-port pairs
  private final HashMap<HostPortKey, Pair<String, Integer>> hostPortMap = new HashMap<>();
  
  public Pair<String, Integer> getHostPort(Host host, Port port) {
    return hostPortMap.getOrDefault(new HostPortKey(host, port), null);
  }
  
  public void cacheHostPort(Host hostType, String host, Port portType, Integer port) {
    hostPortMap.put(new HostPortKey(hostType, portType), new Pair<>(host, port));
  }
}
