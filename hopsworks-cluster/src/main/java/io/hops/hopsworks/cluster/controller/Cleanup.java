package io.hops.hopsworks.cluster.controller;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Startup
@Singleton
public class Cleanup {
  private final static Logger LOG = Logger.getLogger(Cleanup.class.getName());
  @EJB
  private ClusterController clusterController;
  
  @Resource
  TimerService timerService;
  
  @PostConstruct
  private void init() {
    timerService.createTimer(0, ClusterController.VALIDATION_KEY_EXPIRY_DATE_MS, "Timer for cluster agent cleanup.");
  }
  
  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }
  
  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void timeout(Timer timer) {
    try {
      clusterController.cleanupUnverifiedUsers();
    } catch(Exception e) {
      LOG.log(Level.WARNING, "Failed to cleanup cluster agents. {0}", e.getMessage());
    }
  }
}
