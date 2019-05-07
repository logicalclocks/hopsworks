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
    List<Hosts> hosts = hostFacade.findAllHosts();
  
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
