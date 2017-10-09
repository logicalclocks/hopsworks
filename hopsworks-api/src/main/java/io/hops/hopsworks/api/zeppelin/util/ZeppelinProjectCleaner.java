package io.hops.hopsworks.api.zeppelin.util;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.TimerService;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import org.sonatype.aether.RepositoryException;

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
    zeppelinConfFactory.checkInterpreterJsonValidity(); // prevent deployment if interpreter json not valid.
    setTimer(10);
  }

  @Timeout
  public void synchronize(Timer timer) throws IOException, RepositoryException, TaskRunnerException {
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
