/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.cloud.dao.HeartbeartResponse;
import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.proxies.CAProxy;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudManager {
  private static final Logger LOG = Logger.getLogger(CloudManager.class.getName());

  @Resource
  private TimerService timerService;
  @EJB
  private CloudClient cloudClient;
  @EJB
  private HostsController hostsController;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private CAProxy caProxy;

  private List<CloudNode> removedNodes = new ArrayList<>();
  private final Set<CloudNode> decommissionedNodes = new HashSet<>();
  
  @PostConstruct
  public void init() {
    LOG.log(Level.INFO, "Hopsworks@Cloud - Initializing CloudManager");
    timerService.createIntervalTimer(0, 1000, new TimerConfig("Cloud heartbeat", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void heartbeat() {
    try {
      //send heartbeat to hopsworks-cloud
      HeartbeartResponse response = cloudClient.sendHeartbeat(removedNodes);

      removedNodes = new ArrayList<>();

      //add worker nodes to host table if they are not present
      for (CloudNode worker : response.getWorkers()) {
        // Do not put back nodes that were removed by the previous heartbeat
        // but not yet shutdown
        if (!decommissionedNodes.contains(worker) &&
                !hostsFacade.findByHostIp(worker.getIp()).isPresent()) {
          HostDTO hostDTO = new HostDTO();
          hostDTO.setHostname(worker.getHost());;
          hostDTO.setHostIp(worker.getIp());
          hostDTO.setNumGpus(worker.getNumGPUs());
          hostsController.addOrUpdateClusterNode(worker.getHost(), hostDTO);
        }
      }

      // If it's finally removed by the list of cluster nodes, it's safe to forget them
      // from decommissionedNodes
      decommissionedNodes.removeIf(host -> !response.getWorkers().contains(host));

      if (!response.getRemoveRequest().isEmpty()) {
        for (CloudNode worker : response.getWorkers()) {
          Integer numberOfNodesToRemove = response.getRemoveRequest().get(worker.getInstanceType());
          if (numberOfNodesToRemove > 0) {
            LOG.log(Level.INFO, "Removing Node: " + worker.getHost() + " type: " + worker.getInstanceType());
            caProxy.revokeHostX509(worker.getHost());
            hostsController.removeByHostname(worker.getHost());
            removedNodes.add(worker);
            decommissionedNodes.add(worker);
            response.getRemoveRequest().put(worker.getInstanceType(), --numberOfNodesToRemove);
          }
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Error in Cloud Heartbeat", ex);
    }
  }
}
