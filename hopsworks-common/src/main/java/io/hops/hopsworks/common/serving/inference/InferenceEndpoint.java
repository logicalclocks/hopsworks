/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.serving.inference;

import java.util.List;
import io.hops.hopsworks.common.serving.inference.InferencePort.InferencePortName;

import javax.ws.rs.NotFoundException;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InferenceEndpoint {
  
  private InferenceEndpointType type;
  private List<String> hosts;
  private List<InferencePort> ports;
  
  public InferenceEndpoint() { }
  
  public InferenceEndpoint(InferenceEndpointType type, List<String> hosts, List<InferencePort> ports) {
    this.type = type;
    this.hosts = hosts;
    this.ports = ports;
  }
  
  public InferenceEndpointType getType() { return type; }
  public void setType(InferenceEndpointType type) { this.type = type; }
  
  public List<String> getHosts() { return hosts; }
  public void setHosts(List<String> hosts) { this.hosts = hosts; }
  
  public String getAnyHost() {
    return this.hosts.stream().findAny().orElse(null);
  }
  
  public List<InferencePort> getPorts() { return ports; }
  public void setPorts(List<InferencePort> ports) { this.ports = ports; }
  
  public InferencePort getPort(InferencePortName portName) {
    return this.ports.stream().filter(p -> p.getName() == portName).findAny().orElseThrow(NotFoundException::new);
  }
  
  public String getUrl(InferencePortName portName) {
    String protocol = portName == InferencePortName.HTTP || portName == InferencePortName.STATUS
      ? "http://"
      : "https://";
    String host = this.getHosts().stream().findAny().orElseThrow(NotFoundException::new);
    Integer port = this.getPort(portName).getNumber();
    
    return protocol + host + ":" + port;
  }
  
  public enum InferenceEndpointType {
    LOAD_BALANCER, // load balancer external
    KUBE_CLUSTER, // cluster IP within the Kubernetes cluster
    NODE // IP of a random cluster node
  }
}