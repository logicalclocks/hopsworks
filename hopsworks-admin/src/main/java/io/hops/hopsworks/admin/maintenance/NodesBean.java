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
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "nodesBean")
@ViewScoped
public class NodesBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private final Logger logger = Logger.getLogger(NodesBean.class.getName());

  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;

  private List<Hosts> allNodes;
  private final Map<String, Object> dialogOptions;
  private String newNodeHostname;
  private String newNodeHostIp;
  private Hosts toBeDeletedNode;

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

  public List<Hosts> getAllNodes() {
    return allNodes;
  }

  public void setAllNodes(List<Hosts> allNodes) {
    this.allNodes = allNodes;
  }

  public String getNewNodeHostname() {
    return newNodeHostname;
  }

  public void setNewNodeHostname(String newNodeHostname) {
    this.newNodeHostname = newNodeHostname;
  }

  public String getNewNodeHostIp() {
    return newNodeHostIp;
  }

  public void setNewNodeHostIp(String newNodeHostIp) {
    this.newNodeHostIp = newNodeHostIp;
  }

  public Hosts getToBeDeletedNode() {
    return toBeDeletedNode;
  }

  public void setToBeDeletedNode(Hosts toBeDeletedNode) {
    this.toBeDeletedNode = toBeDeletedNode;
  }

  public void onRowEdit(RowEditEvent event) {
    Hosts host = (Hosts) event.getObject();

    Hosts storedHost = hostsFacade.findByHostname(host.getHostname());
    if (storedHost != null) {
      storedHost.setHostIp(host.getHostIp());
      storedHost.setPublicIp(host.getPublicIp());
      storedHost.setPrivateIp(host.getPrivateIp());
      storedHost.setAgentPassword(host.getAgentPassword());
      storedHost.setRegistered(host.isRegistered());
      hostsFacade.storeHost(storedHost, true);
      MessagesController.addInfoMessage("Updated host");
      logger.log(Level.FINE, "Updated Host with ID: " + host.getHostname() + " Hostname: " + host.getHostIp()
          + " Public IP: " + host.getPublicIp() + " Private IP: " + host.getPrivateIp());
    }
  }

  public void dialogAddNewNode() {
    RequestContext.getCurrentInstance().openDialog("addNewNodeDialog", dialogOptions, null);
  }

  public String anacondaLastSynchronized() {
    String file = settings.getHopsworksDomainDir() + "/docroot/anaconda.tgz";
    return lastModifiedFileDate(file);
  }

  private String lastModifiedFileDate(String fullPath) {
    File f = new File(fullPath);
    if (!f.isFile()) {
      return "Not available!!";
    }
    Path p = Paths.get(fullPath);
    FileTime fileTime;
    try {
      fileTime = Files.getLastModifiedTime(p);
      DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
      return "Last synchronized: " + dateFormat.format(fileTime.toMillis());
    } catch (IOException e) {
      return "Cannot get the last modified time - " + e.getLocalizedMessage();
    }
  }

  public String anacondaGpuLastSynchronized() {
    String file = settings.getHopsworksDomainDir() + "/bin/anaconda-gpu.tgz";
    return lastModifiedFileDate(file);
  }

  public void zipUpAnacondaLibs() {

    String prog = settings.getHopsworksDomainDir() + "/bin/anaconda-prepare.sh";
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
          process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor(10l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem zipping anaconda libraries for synchronization: {0}", ex.toString());
      exitValue = -2;
    }
    if (exitValue != 0) {
      logger.log(Level.INFO, "Zipped up Anaconda libraries for the new node.");
      MessagesController.addInfoMessage("Zipped up Anaconda libraries for the new node.", "Now install the new node"
          + "using Chef to download/sync up the installed libraries");
    }
  }

  public void typedNewNodeDetails() {
    String[] obj = new String[2];
    obj[0] = newNodeHostname;
    obj[1] = newNodeHostIp;
    RequestContext.getCurrentInstance().closeDialog(obj);
  }

  public void onDialogAddNewNodeClosed(SelectEvent event) {
    String newNodeHostname = ((String[]) event.getObject())[0];
    String newNodeHostIp = ((String[]) event.getObject())[1];
    if (newNodeHostname == null || newNodeHostname.isEmpty()
        || newNodeHostIp == null || newNodeHostIp.isEmpty()) {
      MessagesController.addErrorMessage("Host not added", "All fields must be filled");
    } else {
      Hosts existingNode = hostsFacade.findByHostname(newNodeHostname);
      if (existingNode != null) {
        logger.log(Level.WARNING, "Tried to add Host with ID " + newNodeHostname + " but a host already exist with the "
            + "same ID");
        MessagesController.addErrorMessage("Host with the same ID already exist!");
      } else {
        Hosts newNode = new Hosts();
        newNode.setHostname(newNodeHostname);
        newNode.setHostIp(newNodeHostIp);
        allNodes.add(newNode);
        hostsFacade.storeHost(newNode, true);
        logger.log(Level.INFO, "Added new cluster node with ID " + newNode.getHostname());
        MessagesController.addInfoMessage("New node added", "Now click the button 'Zip Anaconda Libraries' before "
            + "installing the new node.");
      }
    }
  }

  public void deleteNode() {
    if (toBeDeletedNode != null) {
      boolean deleted = hostsFacade.removeByHostname(toBeDeletedNode.getHostname());
      if (deleted) {
        allNodes.remove(toBeDeletedNode);
        logger.log(Level.INFO, "Removed Host with ID " + toBeDeletedNode.getHostname() + " from the database");
        MessagesController.addInfoMessage("Node deleted");
      } else {
        logger.log(Level.WARNING, "Could not delete Host " + toBeDeletedNode.getHostname() + " from the database");
        MessagesController.addErrorMessage("Could not delete node");
      }
    }
  }
}
