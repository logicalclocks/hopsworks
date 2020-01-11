/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.common.agent.AgentLivenessMonitor;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.CondaCommands;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.FormatUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.RemoteCommandResult;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;

import io.hops.hopsworks.exceptions.ServiceException;
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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.enterprise.concurrent.ManagedExecutorService;

@ManagedBean(name = "nodesBean")
@ViewScoped
public class NodesBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private final Logger logger = Logger.getLogger(NodesBean.class.getName());

  @EJB
  private AgentLivenessMonitor agentLivenessMonitor;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private CondaCommandFacade condaCommandsFacade;
  @EJB
  private HostsController hostsController;


  @Resource(lookup = "concurrent/kagentExecutorService")
  private ManagedExecutorService executorService;

  private List<Hosts> allNodes;
  private final Map<String, Object> dialogOptions;
  private String newNodeHostname;
  private String newNodeHostIp;
  private List<Hosts> selectedHosts;

  private String output;
  private Future<String> future;

  class CondaTask implements Callable<String> {

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private final FacesContext context;
    private final String hostname;

    public CondaTask(FacesContext context, String hostname) {
      this.context = context;
      this.hostname = hostname;
    }

    public String getHostname() {
      return hostname;
    }

    @Override
    public String call() {
      FacesMessage message;
      String output = "";
      try {

        String prog = settings.getHopsworksDomainDir() + "/bin/anaconda-rsync.sh";
        int exitValue;
        Integer id = 1;
  
        ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
            .addCommand(prog)
            .addCommand(this.hostname)
            .redirectErrorStream(true)
            .setWaitTimeout(10L, TimeUnit.MINUTES)
            .build();
        
        try {
          ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
          exitValue = processResult.getExitCode();
          if (exitValue == 0) {
            output = "SUCCESS. \r\n" + processResult.getStdout();
          } else {
            if (processResult.processExited()) {
              output = "FAILED. \r\n" + processResult.getStdout();
            } else {
              output = "Process TIMED-OUT. \r\n" + processResult.getStdout();
            }
          }
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Problem zipping anaconda libraries for synchronization: {0}", ex.toString());
          exitValue = -2;
        }
        
        if (exitValue != 0) {
          MessagesController.addInfoMessage("Problem with synchronizing Anaconda libraries to host: " + hostname, null);
        } else {
          MessagesController.addInfoMessage("Succes: synchronized Anaconda libraries with host: " + hostname, null);
        }

      } catch (Exception e) {
        output = "Error.";
        message = new FacesMessage(FacesMessage.SEVERITY_FATAL,
            "Communication Error", e.toString());
      }
      return output;
    }
  }

  public NodesBean() {
    dialogOptions = new HashMap<>(3);
    dialogOptions.put("resizable", false);
    dialogOptions.put("draggable", false);
    dialogOptions.put("modal", true);
  }

  @PostConstruct
  public void init() {
    allNodes = hostsFacade.findAll();
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

  public List<Hosts> getSelectedHosts() {
    return selectedHosts;
  }

  public void setSelectedHosts(List<Hosts> selectedHosts) {
    this.selectedHosts = selectedHosts;
  }

  public void onRowEdit(RowEditEvent event) {
    Hosts host = (Hosts) event.getObject();

    Optional<Hosts> optional = hostsFacade.findByHostname(host.getHostname());
    if (optional.isPresent()) {
      Hosts storedHost = optional.get();
      storedHost.setHostIp(host.getHostIp());
      storedHost.setPublicIp(host.getPublicIp());
      storedHost.setPrivateIp(host.getPrivateIp());
      storedHost.setAgentPassword(host.getAgentPassword());
      storedHost.setRegistered(host.isRegistered());
      storedHost.setCondaEnabled(host.getCondaEnabled());
      hostsFacade.update(storedHost);
      MessagesController.addInfoMessage("Updated host");
      logger.log(Level.FINE, "Updated Host with ID: " + host.getHostname() + " Hostname: " + host.getHostIp()
          + " Public IP: " + host.getPublicIp() + " Private IP: " + host.getPrivateIp()
          + " Conda Enabled: " + host.getCondaEnabled());
    }
  }

  public void dialogAddNewNode() {
    RequestContext.getCurrentInstance().openDialog("addNewNodeDialog", dialogOptions, null);
  }

  public String condaStyle(String hostname) {
    Optional<Hosts> optional = hostsFacade.findByHostname(hostname);
    if (optional.isPresent()) {
      Hosts h = optional.get();
      List<CondaCommands> listCommands = condaCommandsFacade.findByHost(h);
      for (CondaCommands cc : listCommands) {
        if (cc.getStatus() == CondaCommandFacade.CondaStatus.FAILED) {
          return "condaOutOfSync";
        }
      }
    }
    return "condaSync";
  }

  public void rsyncAnacondaLibs(String hostname) {

    CondaTask condaTask = new CondaTask(FacesContext.getCurrentInstance(), hostname);
    this.future = executorService.submit(condaTask);

  }

  public void typedNewNodeDetails() {
    String[] obj = new String[2];
    obj[0] = newNodeHostname;
    obj[1] = newNodeHostIp;
    RequestContext.getCurrentInstance().closeDialog(obj);
  }

  public void onDialogAddNewNodeClosed(SelectEvent event) {
    String newHostname = ((String[]) event.getObject())[0];
    String newNodeHostIp = ((String[]) event.getObject())[1];
    if (newHostname == null || newHostname.isEmpty()
        || newNodeHostIp == null || newNodeHostIp.isEmpty()) {
      MessagesController.addErrorMessage("Host not added", "All fields must be filled");
    } else {
      if (hostsFacade.findByHostname(newHostname).isPresent()) {
        logger.log(Level.WARNING, "Tried to add Host with ID " + newHostname + " but a host already exists with the "
            + "same ID");
        MessagesController.addErrorMessage("Host with the same ID already exists!");
      } else {
        Hosts newNode = new Hosts();
        newNode.setHostname(newHostname);
        newNode.setHostIp(newNodeHostIp);
        newNode.setCondaEnabled(false);
        allNodes.add(newNode);
        hostsFacade.update(newNode);
        logger.log(Level.INFO, "Added new cluster node with ID " + newNode.getHostname());
        MessagesController.addInfoMessage("New node added", "Now click the button 'Zip Anaconda Libraries' before "
            + "installing the new node.");
      }
    }
  }

  public void deleteNode() {
    if (selectedHosts != null && !selectedHosts.isEmpty()) {
      for (Hosts host : selectedHosts) {
        boolean deleted = hostsController.removeByHostname(host.getHostname());
        if (deleted) {
          allNodes.remove(host);
          logger.log(Level.INFO, "Removed Host with ID " + host.getHostname() + " from the database");
          MessagesController.addInfoMessage("Node deleted");
        } else {
          logger.log(Level.WARNING, "Could not delete Host " + host.getHostname() + " from the database");
          MessagesController.addErrorMessage("Could not delete node");
        }
      }
    }
  }

  public void rotateKeys() {
    certificatesMgmService.issueServiceKeyRotationCommand();
    MessagesController.addInfoMessage("Commands issued", "Issued command to rotate keys on hosts");
    logger.log(Level.INFO, "Issued key rotation command");
  }

  public void restartKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.RESTART);
  }

  public void startKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.START);
  }

  public void stopKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.STOP);
  }

  private void performKagentAction(List<Hosts> hosts, KagentAction action) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = submitKagentAction(action, hosts);
    if (asyncResults != null) {
      boolean hadFailure = false;
      for (Map.Entry<Hosts, Future<RemoteCommandResult>> entry : asyncResults.entrySet()) {
        Future asyncResult = entry.getValue();
        try {
          if (asyncResult != null) {
            asyncResult.get();
          } else {
            throw new ExecutionException(new Throwable("Command failed"));
          }
        } catch (InterruptedException | ExecutionException ex) {
          FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to " + action.name() + " agent",
              "Failed to " + action.name() + " agent@" + entry.getKey());
          FacesContext.getCurrentInstance().addMessage(null, message);
          hadFailure = true;
        }
      }
      if (!hadFailure) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "All agents have " + action.effect + " successfully",
            "");
        FacesContext.getCurrentInstance().addMessage(null, message);
      }
    }
  }

  private Map<Hosts, Future<RemoteCommandResult>> submitKagentAction(KagentAction action, List<Hosts> hosts) {
    if (hosts == null || hosts.isEmpty()) {
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "No hosts selected",
          "Hosts list is null");
      FacesContext.getCurrentInstance().addMessage(null, message);
      return null;
    }
    switch (action) {
      case START:
        return startAgentsInternal(hosts);
      case STOP:
        return stopAgentsInternal(hosts);
      case RESTART:
        return restartAgentsInternal(hosts);
      default:
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unknown action",
            "Unknown action to perform on kagent");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return null;
    }
  }

  private Map<Hosts, Future<RemoteCommandResult>> startAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.startAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }

  private Map<Hosts, Future<RemoteCommandResult>> stopAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.stopAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }

  private Map<Hosts, Future<RemoteCommandResult>> restartAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.restartAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }

  enum KagentAction {
    START("started"),
    STOP("stopped"),
    RESTART("restarted");

    private final String effect;

    KagentAction(String effect) {
      this.effect = effect;
    }
  }

  public String getOutput() {
    if (!isOutput()) {
      return "No Output to show for command executions.";
    }
    return this.output;
  }

  public boolean isOutput() {
    return !(this.output == null || this.output.isEmpty());
  }

  public String formatMemoryCapacity(Hosts host) {
    if (host.getMemoryCapacity() == null) {
      return "N/A";
    }
    return FormatUtils.storage(host.getMemoryCapacity());
  }

}
