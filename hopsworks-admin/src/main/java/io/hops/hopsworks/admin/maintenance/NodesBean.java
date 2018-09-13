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

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaStatus;
import io.hops.hopsworks.common.security.CertificatesMgmService;
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
import java.util.concurrent.Callable;
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
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private PythonDepsFacade pythonDepsFacade;

  @Resource(lookup = "concurrent/kagentExecutorService")
  private ManagedExecutorService executorService;

  private List<Hosts> allNodes;
  private final Map<String, Object> dialogOptions;
  private String newNodeHostname;
  private String newNodeHostIp;
  private Hosts toBeDeletedNode;

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
        String[] command = {prog, this.hostname};
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
          Process process = pb.start();
          pb.redirectErrorStream(true);
          BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
          String line;
          StringBuilder sb = new StringBuilder();

          while ((line = br.readLine()) != null) {
            sb.append(line).append("\r\n");
          }
          boolean status = process.waitFor(600, TimeUnit.SECONDS);
          if (status == false) {
            output = "COMMAND TIMED OUT: \r\n" + sb.toString();
            return output;
          }
          exitValue = process.exitValue();
          if (exitValue == 0) {
            // delete from conda_commands tables
            output = "SUCCESS. \r\n" + sb.toString();
          } else {
            output = "FAILED. \r\n" + sb.toString();
          }

        } catch (IOException | InterruptedException ex) {
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
      storedHost.setCondaEnabled(host.getCondaEnabled());
      hostsFacade.storeHost(storedHost);
      MessagesController.addInfoMessage("Updated host");
      logger.log(Level.FINE, "Updated Host with ID: " + host.getHostname() + " Hostname: " + host.getHostIp()
          + " Public IP: " + host.getPublicIp() + " Private IP: " + host.getPrivateIp()
          + " Conda Enabled: " + host.getCondaEnabled());
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

  public String condaStyle(String hostname) {
    Hosts h = hostsFacade.findByHostname(hostname);
    if (h != null) {
      List<CondaCommands> listCommands = pythonDepsFacade.findByHost(h);
      for (CondaCommands cc : listCommands) {
        if (cc.getStatus() == CondaStatus.FAILED) {
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
        newNode.setCondaEnabled(false);
        allNodes.add(newNode);
        hostsFacade.storeHost(newNode);
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

  public void rotateKeys() {
    certificatesMgmService.issueServiceKeyRotationCommand();
    MessagesController.addInfoMessage("Commands issued", "Issued command to rotate keys on hosts");
    logger.log(Level.INFO, "Issued key rotation command");
  }

  public String getOutput() {
    if (!isOutput()) {
      return "No Output to show for command executions.";
    }
    return this.output;
  }

  public boolean isOutput() {
    if (this.output == null || this.output.isEmpty()) {
      return false;
    }
    return true;
  }

}
