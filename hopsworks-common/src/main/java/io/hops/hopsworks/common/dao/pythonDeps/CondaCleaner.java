package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 * Strong eventually consistent protocol where the leader Glassfish instance
 * periodically tells all the kagents its set of active projects.
 * The kagent then adds/deletes any environments missing or in-excess.
 */
@Singleton
@Startup
public class CondaCleaner {

  private static final Logger logger = Logger.getLogger(CondaCleaner.class.
          getName());

  @EJB
  private WebCommunication web;
  @Resource
  private TimerService timerService;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  Settings settings;
  @EJB
  HostEJB hostsFacade;

  @PostConstruct
  public void startTimer() {
    setTimer(100);
  }

  @Timeout
  public void synchronize(Timer timer) {
    //get list of existing project
    List<Project> projects = projectFacade.findAll();

    Set<String> projectsNames = new HashSet<>();
    for (Project p : projects) {
      projectsNames.add(p.getName());
    }

    List<Host> hosts = hostsFacade.find();
    for (Host h : hosts) {
      // Send Rest call to every kagent with the list of projects
//      web.doCommand(hostAddress, agentPassword, cluster, service, role, command)
    }

    //wait for next iteration
    setTimer(settings.getCondaSyncInterval());
  }

  private void setTimer(long intervalDuration) {
    timerService.createTimer(intervalDuration, "time to synchronize");
  }
}
