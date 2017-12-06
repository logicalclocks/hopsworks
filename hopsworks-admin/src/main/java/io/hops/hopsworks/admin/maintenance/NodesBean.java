/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "nodesBean")
@ViewScoped
public class NodesBean implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Logger LOG = Logger.getLogger(NodesBean.class.getName());
  
  @EJB
  private HostEJB hostsFacade;
  
  private List<Host> allNodes;
  private final Map<String, Object> dialogOptions;
  private String newNodeHostId;
  private String newNodeHostname_;
  private Host toBeDeletedNode;
  
  public NodesBean() {
    dialogOptions = new HashMap<>(3);
    dialogOptions.put("resizable", false);
    dialogOptions.put("draggable", false);
    dialogOptions.put("modal", true);
  }
  
  @PostConstruct
  public void init() {
    allNodes = hostsFacade.find();
  }
  
  public List<Host> getAllNodes() {
    return allNodes;
  }
  
  public void setAllNodes(List<Host> allNodes) {
    this.allNodes = allNodes;
  }
  
  public String getNewNodeHostId() {
    return newNodeHostId;
  }
  
  public void setNewNodeHostId(String newNodeHostId) {
    this.newNodeHostId = newNodeHostId;
  }
  
  public String getNewNodeHostname_() {
    return newNodeHostname_;
  }
  
  public void setNewNodeHostname_(String newNodeHostname_) {
    this.newNodeHostname_ = newNodeHostname_;
  }
  
  public Host getToBeDeletedNode() {
    return toBeDeletedNode;
  }
  
  public void setToBeDeletedNode(Host toBeDeletedNode) {
    this.toBeDeletedNode = toBeDeletedNode;
  }
  
  public void onRowEdit(RowEditEvent event) {
    Host host = (Host) event.getObject();
    
    Host storedHost = hostsFacade.findByHostId(host.getHostId());
    if (storedHost != null) {
      storedHost.setHostname(host.getHostname());
      storedHost.setPublicIp(host.getPublicIp());
      storedHost.setPrivateIp(host.getPrivateIp());
      storedHost.setAgentPassword(host.getAgentPassword());
      hostsFacade.storeHost(storedHost, true);
      MessagesController.addInfoMessage("Updated host");
      LOG.log(Level.FINE, "Updated Host with ID: " + host.getHostId() + " Hostname: " + host.getHostname()
          + " Public IP: " + host.getPublicIp() + " Private IP: " + host.getPrivateIp());
    }
  }
  
  public void dialogAddNewNode() {
    RequestContext.getCurrentInstance().openDialog("addNewNodeDialog", dialogOptions, null);
  }
  
  public void typedNewNodeDetails() {
    String[] obj = new String[2];
    obj[0] = newNodeHostId;
    obj[1] = newNodeHostname_;
    RequestContext.getCurrentInstance().closeDialog(obj);
  }
  
  public void onDialogAddNewNodeClosed(SelectEvent event) {
    String newNodeHostId = ((String[]) event.getObject())[0];
    String newNodeHostname_ = ((String[]) event.getObject())[1];
    if (newNodeHostId == null || newNodeHostId.isEmpty()
        || newNodeHostname_ == null || newNodeHostname_.isEmpty()) {
      MessagesController.addErrorMessage("Host not added", "All fields must be filled");
    } else {
      Host existingNode = hostsFacade.findByHostId(newNodeHostId);
      if (existingNode != null) {
        LOG.log(Level.WARNING, "Tried to add Host with ID " + newNodeHostId + " but a host already exist with the " +
            "same ID");
        MessagesController.addErrorMessage("Host with the same ID already exist!");
      } else {
        Host newNode = new Host();
        newNode.setHostId(newNodeHostId);
        newNode.setHostname(newNodeHostname_);
        allNodes.add(newNode);
        hostsFacade.storeHost(newNode, true);
        LOG.log(Level.INFO, "Added new cluster node with ID " + newNode.getHostId());
        MessagesController.addInfoMessage("New node added", "Start kagent on the new node to " +
            "register with Hopsworks");
      }
    }
  }
  
  public void deleteNode() {
    if (toBeDeletedNode != null) {
      boolean deleted = hostsFacade.removeHostById(toBeDeletedNode.getHostId());
      if (deleted) {
        allNodes.remove(toBeDeletedNode);
        LOG.log(Level.INFO, "Removed Host with ID " + toBeDeletedNode.getHostId() + " from the database");
        MessagesController.addInfoMessage("Node deleted");
      } else {
        LOG.log(Level.WARNING, "Could not delete Host " + toBeDeletedNode.getHostId() + " from the database");
        MessagesController.addErrorMessage("Could not delete node");
      }
    }
  }
}
