package io.hops.hopsworks.api.agent;

import static io.hops.hopsworks.api.agent.AgentResource.logger;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostEJB;
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
  private HostEJB hostFacade;
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
