package se.kth.hopsworks.zeppelin.util;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.TimerService;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import org.apache.commons.io.FileUtils;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;

@Singleton
@Startup
public class ZeppelinProjectCleaner {

  @Resource
  TimerService timerService;

  @EJB
  Settings settings;

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;

  @PostConstruct
  public void startTimer() {
    setTimer(10);
  }

  @Timeout
  public void synchronize(Timer timer) {
    //get list of existing project
    List<Project> projects = projectFacade.findAll();
    //check if their is a zeepeling project folder localy for these projects
    File projectsDir = new File(settings.getZeppelinProjectsDir());
    if (!projectsDir.exists()) {
      return;
    }
    Set<String> projectsDirs = new HashSet<>();
    for (String projectName : projectsDir.list()) {
      projectsDirs.add(projectName);
    }

    for (Project project : projects) {
      projectsDirs.remove(project.getName());
    }

    //remove all the projects present localy but not in the db
    for (String projectName : projectsDirs) {
      Project project = new Project(-1, projectName);
      zeppelinConfFactory.deleteZeppelinConfDir(project);
    }

    //wait for next iteration
    setTimer(settings.getZeppelinSyncInterval());
  }

  private void setTimer(long intervalDuration) {
    timerService.createTimer(intervalDuration,
            "time to synchronize");
  }
}
