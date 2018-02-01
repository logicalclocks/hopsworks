/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.agent;

import static io.hops.hopsworks.api.agent.AgentResource.logger;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.mail.MessagingException;

@Singleton
@Startup
public class HostHeartbeatTimeout {

  @Resource
  TimerService timerService;

  @EJB
  private HostsFacade hostFacade;
  @EJB
  private Settings settings;
  @EJB
  private EmailBean emailBean;
  
  private final Map<String, Health> hostsHealth = new HashMap<>();
  
  @PostConstruct
  public void startTimer() {
    setTimer(10);
  }

  @Timeout
  public void checkHosts(Timer timer) {
    //get list of existing project
    List<Hosts> hosts = hostFacade.find();
  
    for(Hosts host : hosts){
      if(host.getHealth().equals(Health.Bad)){
        Health previousHealth = hostsHealth.get(host.getHostname());
        if(previousHealth==null || !previousHealth.equals(host.getHealth())){
          String subject = "Alert: " + host.getHostname();
          String body = "Host " + host.getHostname() + " transitioned from state " + previousHealth + " to state " +
              host.getHealth();
          emailAlert(subject, body);
        }
      }
      hostsHealth.put(host.getHostname(), host.getHealth());
    }
    
    //wait for next iteration
    setTimer(Hosts.getHeartbeatInterval()*1000);
  }

  private void setTimer(long intervalDuration) {
    timerService.createTimer(intervalDuration,
            "time to check hosts");
  }
  
  private void emailAlert(String subject, String body){
    try {
      emailBean.sendEmails(settings.getAlertEmailAddrs(), subject, body);
    } catch (MessagingException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
}
