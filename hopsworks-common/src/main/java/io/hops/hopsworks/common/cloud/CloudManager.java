/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.util.CloudClient;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.json.JSONArray;
import org.json.JSONObject;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class CloudManager {

  private static final Logger LOG = Logger.getLogger(CloudManager.class.getName());

  @Resource
  private TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private CloudClient cloudClient;
  @EJB
  private HostsController hostsController;
  @EJB
  private HostsFacade hostsFacade;

  @PostConstruct
  public void init() {
    if (settings.isCloud()) {
      timerService.createIntervalTimer(0, 60000, new TimerConfig("cloud heartbeat", false));
    }
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void isAlive() {
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
