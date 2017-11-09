/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.hops.hopsworks.admin.llap;

import io.hops.hopsworks.common.dao.util.VariablesFacade;
import org.primefaces.context.RequestContext;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "LlapBean")
@ViewScoped
public class LlapBean implements Serializable {

  @EJB
  LlapClusterFacade llapClusterFacade;
  @EJB
  VariablesFacade variablesFacade;
  @EJB
  LlapClusterLifecycle llapClusterLifecycle;

  private static final Logger logger = Logger.getLogger(LlapBean.class.getName());

  private static String NINSTANCES = "llap_ninstances";
  private static String EXECMEMORY = "llap_exec_memory";
  private static String CACHEMEMORY = "llap_cache_memory";
  private static String NEXECUTORS = "llap_executors_threads";
  private static String NIOTHREADS = "llap_io_threads";

  private static int MAXWAITINGITERATIONS = 240;

  private int nInstances = 2;
  private long execMemory = 4096;
  private long cacheMemory = 4096;
  private int nExecutors = 4;
  private int nIOThreads = 4;

  private Boolean isClusterUp = null;
  private Boolean isClusterStarting = null;

  private List<String> llapHosts = new ArrayList<>();
  private String selectedHost = "";

  public int getnInstances() {
    String nInstancesStr = variablesFacade.getVariableValue(NINSTANCES);
    if (nInstancesStr != null) {
      nInstances = Integer.valueOf(nInstancesStr);
    }
    return nInstances;
  }

  public void setnInstances(int nInstances) {
    variablesFacade.storeVariable(NINSTANCES, String.valueOf(nInstances));
    this.nInstances = nInstances;
  }

  public long getExecMemory() {
    String execMemoryStr = variablesFacade.getVariableValue(EXECMEMORY);
    if (execMemoryStr != null) {
      execMemory = Long.valueOf(execMemoryStr);
    }
    return execMemory;
  }

  public void setExecMemory(long execMemory) {
    variablesFacade.storeVariable(EXECMEMORY, String.valueOf(execMemory));
    this.execMemory = execMemory;
  }

  public long getCacheMemory() {
    String cacheMemoryStr = variablesFacade.getVariableValue(CACHEMEMORY);
    if (cacheMemoryStr != null) {
      cacheMemory = Long.valueOf(cacheMemoryStr);
    }
    return cacheMemory;
  }

  public void setCacheMemory(long cacheMemory) {
    variablesFacade.storeVariable(CACHEMEMORY, String.valueOf(cacheMemory));
    this.cacheMemory = cacheMemory;
  }

  public int getnExecutors() {
    String nExecutorsStr = variablesFacade.getVariableValue(NEXECUTORS);
    if (nExecutorsStr != null) {
      nExecutors = Integer.valueOf(nExecutorsStr);
    }
    return nExecutors;
  }

  public void setnExecutors(int nExecutors) {
    variablesFacade.storeVariable(NEXECUTORS, String.valueOf(nExecutors));
    this.nExecutors = nExecutors;
  }

  public int getnIOThreads() {
    String nIOThreadsStr = variablesFacade.getVariableValue(NIOTHREADS);
    if (nIOThreadsStr != null) {
      nIOThreads = Integer.valueOf(nIOThreadsStr);
    }
    return nIOThreads;
  }

  public void setnIOThreads(int nIOThreads) {
    variablesFacade.storeVariable(NIOTHREADS, String.valueOf(nIOThreads));
    this.nIOThreads = nIOThreads;
  }

  public List<String> getLlapHosts() {
    llapHosts = new ArrayList<>();
    if (isClusterUp()) {
      llapHosts.addAll(llapClusterFacade.getLlapHosts());
      if (llapHosts.size() != 0) {
        selectedHost = llapHosts.get(0);
      }
    }

    return llapHosts;
  }

  public void setLlapHosts(List<String> llapHosts) { llapHosts = llapHosts; }

  public String getSelectedHost() { return selectedHost; }

  public void setSelectedHost(String selectedHost) {
    this.selectedHost = selectedHost;
  }

  public boolean isClusterUp() {
    if (isClusterUp == null) {
      isClusterUp = llapClusterFacade.isClusterUp();
    }

    return isClusterUp;
  }

  public boolean isClusterStarting() {
    if (isClusterStarting == null) {
      isClusterStarting = llapClusterFacade.isClusterStarting();
    }

    return isClusterStarting;
  }

  public boolean areContainersRunning() {
    return !llapClusterFacade.getLlapHosts().isEmpty();
  }

  public void waitForCluster() {
    if (isClusterUp() || (!isClusterUp() && !isClusterStarting())) {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "true");
      return;
    }

    // Wait for the cluster to come up.
    int counter = MAXWAITINGITERATIONS;
    try {
      while (llapClusterFacade.isClusterStarting() && counter > 0 ) {
        Thread.sleep(1500);
        counter--;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error on waiting for LLAP cluster to come up", e);
    }

    if (counter > 0) {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "false");
    } else {
      RequestContext.getCurrentInstance().addCallbackParam("alreadyUp", "timeout");
    }
  }

  public void startLLAP() {
    llapClusterLifecycle.startCluster(nInstances, execMemory,
        cacheMemory, nExecutors, nIOThreads);
    waitForCluster();
  }

  public void stopLLAP() {
    llapClusterLifecycle.stopCluster();
  }
}
