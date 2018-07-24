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
package io.hops.hopsworks.admin.llap;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.admin.llap.LlapClusterFacade;
import io.hops.hopsworks.common.admin.llap.LlapClusterLifecycle;
import io.hops.hopsworks.common.admin.llap.LlapClusterStatus;
import org.primefaces.context.RequestContext;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "LlapBean")
@ViewScoped
public class LlapBean implements Serializable {

  @EJB
  LlapClusterFacade llapClusterFacade;
  @EJB
  LlapClusterLifecycle llapClusterLifecycle;

  private static final Logger logger = Logger.getLogger(LlapBean.class.getName());

  private static int MAXWAITINGITERATIONS = 240;

  private int nInstances = 2;
  private long execMemory = 4096;
  private long cacheMemory = 4096;
  private int nExecutors = 4;
  private int nIOThreads = 4;

  private LlapClusterStatus.Status clusterStatus;

  private List<String> llapHosts = null;
  private String selectedHost = "";

  @PostConstruct
  public void init() {
    LlapClusterStatus status = llapClusterFacade.getClusterStatus();
    this.clusterStatus = status.getClusterStatus();
    this.nInstances = status.getInstanceNumber();
    this.execMemory = status.getExecutorsMemory();
    this.cacheMemory = status.getCacheMemory();
    this.nExecutors = status.getExecutorsPerInstance();
    this.nIOThreads = status.getIOThreadsPerInstance();
    this.llapHosts = status.getHosts();

    if (llapHosts != null && llapHosts.size() > 0) {
      this.selectedHost = llapHosts.get(0);
    }
  }

  public int getnInstances() { return nInstances; }

  public void setnInstances(int nInstances) {
    this.nInstances = nInstances;
  }

  public long getExecMemory() { return execMemory; }

  public void setExecMemory(long execMemory) {
    this.execMemory = execMemory;
  }

  public long getCacheMemory() { return cacheMemory; }

  public void setCacheMemory(long cacheMemory) {
    this.cacheMemory = cacheMemory;
  }

  public int getnExecutors() { return nExecutors; }

  public void setnExecutors(int nExecutors) {
    this.nExecutors = nExecutors;
  }

  public int getnIOThreads() { return nIOThreads; }

  public void setnIOThreads(int nIOThreads) {
    this.nIOThreads = nIOThreads;
  }

  public List<String> getLlapHosts() { return llapHosts; }

  public String getSelectedHost() { return selectedHost; }

  public void setSelectedHost(String selectedHost) {
    this.selectedHost = selectedHost;
  }

  public boolean isClusterUp() {
    return clusterStatus == LlapClusterStatus.Status.UP;
  }

  public boolean isClusterStarting() {
    return clusterStatus == LlapClusterStatus.Status.LAUNCHING;
  }

  public boolean areContainersRunning() {
    return !llapClusterFacade.getLlapHosts().isEmpty();
  }

  public void waitForCluster(boolean shouldBeStarting) {
    if (!shouldBeStarting && (isClusterUp() || !isClusterUp() && !isClusterStarting())) {
      // The cluster is up or down and nobody asked for a new cluster. Unlock the ui
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "true");
      return;
    }

    // Wait for the cluster to come up.
    int counter = MAXWAITINGITERATIONS;
    try {
      // We can't immediately start polling for isClusterStarting, as this execution path
      // is faster than the startCluster function.
      Thread.sleep(5000);
      while (llapClusterFacade.isClusterStarting() && counter > 0) {
        Thread.sleep(1500);
        counter--;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error on waiting for LLAP cluster to come up", e);
    }

    if (counter > 0 && !llapClusterFacade.isClusterUp()) {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "error");
      MessagesController.addErrorMessage("Error starting the cluster. Check the glassfish logs for more info.");
    } else if (counter > 0) {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "false");
    } else {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "timeout");
      MessagesController.addErrorMessage("Timeout while starting the cluster. Try again.");
    }
  }

  public void startLLAP() {
    llapClusterLifecycle.startCluster(nInstances, execMemory,
        cacheMemory, nExecutors, nIOThreads);
    waitForCluster(true);
  }

  public void stopLLAP() {
    llapClusterLifecycle.stopCluster();
  }
}
