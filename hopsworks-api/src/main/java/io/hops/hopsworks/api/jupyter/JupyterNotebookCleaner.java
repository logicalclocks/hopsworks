package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyService;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg.Session;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFactory;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Singleton
public class JupyterNotebookCleaner {

  private final static Logger LOGGER = Logger.getLogger(
          JupyterNotebookCleaner.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds

  @EJB
  private LivyService livyService;
  @EJB
  private Settings settings;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterConfigFactory jupyterConfigFactory;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;

  public JupyterNotebookCleaner() {
  }

  // Run once per hour
  @Schedule(persistent = false,
          minute = "0",
          hour = "*")
  public void execute(Timer timer) {

    // 1. Get all Running Jupyter Notebook Servers
    List<JupyterProject> servers = jupyterFacade.getAllNotebookServers();

    if (servers != null) {
      // 2. For each running Notebook Server, get the project_user and
      // then get the Livy sessions for that project_user
      for (JupyterProject jp : servers) {
        List<Session> sessions = livyService.getJupyterLivySessions(jp.
                getProjectId());
        // 3. If there is an active livy session, update the lastModified column
        if (!sessions.isEmpty()) {
          jp.setLastAccessed(new Date(System.currentTimeMillis()));
          jupyterFacade.update(jp);
        }
        // 3a. TODO - Check if there is an active Python kernel for the notebook

        // If notebook hasn't been used in the last 2 hours, kill it.
        if (jp.getLastAccessed().before(
                new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000)))) {
          int hdfsUserId = jp.getHdfsUserId();
          HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
          if (hdfsUser != null) {
//            try {
//              jupyterConfigFactory.stopServerJupyterUser(hdfsUser.getUsername());
//            } catch (AppException ex) {
//              Logger.getLogger(JupyterNotebookCleaner.class.getName()).
//                      log(Level.SEVERE, null, ex);
//            }
          }

        }

      }

      List<Long> pids = checkAllNotebookProcesses();
      for (Long p : pids) {
        boolean foundEntry = false;
        for (JupyterProject jp : servers) {
          if (jp.getPid() == p) {
            foundEntry = true;
            break;
          }
        }
        if (!foundEntry) {
          killNotebookProcess(p);
        }
      }

    } else {
      LOGGER.info("No Jupyter Notebook Servers running. Sleeping again.");
    }
  }

  private List<Long> checkAllNotebookProcesses() {
    List<Long> pids = new ArrayList<>();
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    String[] command = {"/usr/bin/sudo", prog, "list"};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        LOGGER.info(line);
      }

      process.waitFor(10l, TimeUnit.SECONDS);
    } catch (IOException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE,
              "Problem checking if Jupyter Notebook server is running: {0}", ex.
                      toString());
    }

    try {
      Scanner s = new Scanner(new File(Settings.JUPYTER_PIDS));
      while (s.hasNextLine()) {
        //read each line in the file and split the line content on the basis of space
        String line = s.nextLine();
        try {
          pids.add(Long.parseLong(line));
        } catch (NumberFormatException ex) {
          Logger.getLogger(JupyterNotebookCleaner.class.getName()).
                  log(Level.SEVERE, "Badly formatted PIDs in "
                          + Settings.JUPYTER_PIDS, ex);
        }
      }
    } catch (FileNotFoundException ex) {
      Logger.getLogger(JupyterNotebookCleaner.class.getName()).log(Level.INFO, "Could not find any Pids");
    }
    return pids;
  }

  private void killNotebookProcess(Long pid) {
    if (pid == null) {
      return;
    }
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    String[] command = {"/usr/bin/sudo", prog, "killhard", pid.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        LOGGER.info(line);
      }

      process.waitFor(10l, TimeUnit.SECONDS);
    } catch (IOException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE,
              "Problem checking if Jupyter Notebook server is running: {0}", ex.
                      toString());
    }
  }

}
