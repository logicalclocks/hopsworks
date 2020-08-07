/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import org.json.JSONArray;
import org.json.JSONObject;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
      String json = cloudClient.sendHeartbeat();
      //parse response
      JSONObject message = new JSONObject(json);
      JSONArray array = message.getJSONArray("message");
      List<HostDTO> workers = new ArrayList<>();
      for (int i = 0; i < array.length(); i++) {
        JSONObject hostJSON = array.getJSONObject(i);
        HostDTO host = new HostDTO();
        host.setHostname(hostJSON.getString("host"));
        host.setHostIp(hostJSON.getString("ip"));
        host.setNumGpus(hostJSON.getInt("numGPUs"));
        workers.add(host);
      }
      //add worker nodes to host table if they are not present
      for (HostDTO worker : workers ) {
        if(!hostsFacade.findByHostIp(worker.getHostIp()).isPresent()) {
          hostsController.addOrUpdateClusterNode(worker.getHostname(), worker);
        }
      }
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "failded to send cloud heartbeat", ex);
    }
  }
}
