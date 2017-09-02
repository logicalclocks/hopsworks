package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JupyterConfigFactory {

  private static final Logger logger = Logger.getLogger(
          JupyterConfigFactory.class.getName());
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "conf/jupyter_notebook_config.py";

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;

  private String hadoopClasspath = null;

  @PostConstruct
  public void init() {
    loadConfig();
  }

  @PreDestroy
  public void preDestroy() {

  }

  private void loadConfig() {

  }

  public void initNotebook(Project project, HdfsUsers user) {

  }

  /**
   * This only works on Linux systems. From Java 9, you can just call
   * p.getPid();
   * http://stackoverflow.com/questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
   *
   * @param p
   * @return
   */
  public static synchronized long getPidOfProcess(Process p) {
    long pid = -1;

    try {
      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getLong(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      pid = -1;
    }
    return pid;
  }

  public String getHadoopClasspath() throws IOException,
          InterruptedException {
    if (this.hadoopClasspath == null) {
      ProcessBuilder ps = new ProcessBuilder(settings.getHadoopDir()
              + "/bin/hadoop", "classpath", "--glob");
      ps.redirectErrorStream(true);
      Process pr = ps.start();
      BufferedReader in = new BufferedReader(new InputStreamReader(pr.
              getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        sb.append(line);
      }
      pr.waitFor();
      in.close();
      this.hadoopClasspath = sb.toString();
    }
    return this.hadoopClasspath;
  }

  public void removeProject(Project project) {
    // Find any active jupyter servers

    Collection<JupyterProject> instances = project.getJupyterProjectCollection();
    if (instances != null) {
      for (JupyterProject jp : instances) {
        HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
        if (hdfsUser != null) {
          String user = hdfsUser.getUsername();
        }
      }
    }
    // Kill any processes

  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startServerAsJupyterUser(Project project,
          String secretConfig,
          String hdfsUser, JupyterSettings js) throws
          AppException, IOException, InterruptedException {

    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";

    JupyterProject jp = null;
    String token = null;
    Long pid = 0l;

    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);
    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find hdfs user. Not starting Jupyter.");
    }
    // The Jupyter Notebook is running at: http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
    boolean foundToken = false;
    int maxTries = 5;
    Process process = null;
    Integer port = 0;
    JupyterConfig jc = null;

    // kill any running servers for this user, clear cached entries
    while (!foundToken && maxTries > 0) {
      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);

      jc = new JupyterConfig(project, secretConfig, hdfsUser,
              hdfsLeFacade.
                      getSingleEndpoint(), settings, port, token, js);

      String secretDir = settings.getStagingDir() + Settings.PRIVATE_DIRS + js.
              getSecret();
      String logfile = jc.getLogDirPath() + "/" + hdfsUser + "-" + port + ".log";
      String[] command
              = {"/usr/bin/sudo", prog, "start", jc.getNotebookPath(),
                jc.getSettings().getHadoopDir(), settings.getJavaHome(),
                settings.getAnacondaProjectDir(project.getName()), port.
                toString(),
                hdfsUser + "-" + port + ".log", secretDir};
      logger.log(Level.INFO, Arrays.toString(command));
      ProcessBuilder pb = new ProcessBuilder(command);
      String pidfile = jc.getRunDirPath() + "/jupyter.pid";
      try {
        // Send both stdout and stderr to the same stream
        pb.redirectErrorStream(true);
        pb.directory(new File(jc.getNotebookPath()));

        process = pb.start();

        synchronized (pb) {
          try {
            // Wait until the launcher bash script has finished
            process.waitFor(20l, TimeUnit.SECONDS);
          } catch (InterruptedException ex) {
            logger.log(Level.SEVERE,
                    "Woken while waiting for the jupyter server to start: {0}",
                    ex.getMessage());
          }
        }

        // The logfile should now contain the token we need to read and save.
        final BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(logfile), Charset.forName("UTF8")));
        String line;
// [I 11:59:16.597 NotebookApp] The Jupyter Notebook is running at: 
// http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
        String pattern = "(.*)token=(.*)";
        Pattern r = Pattern.compile(pattern);

        int linesRead = 0;
        while (((line = br.readLine()) != null) && !foundToken && linesRead
                < 10000) {
          logger.info(line);
          linesRead++;
          Matcher m = r.matcher(line);
          if (m.find()) {
            token = m.group(2);
            foundToken = true;
          }
        }
        br.close();

        // Read the pid for Jupyter Notebook
        String pidContents = com.google.common.io.Files.readFirstLine(
                new File(
                        pidfile), Charset.defaultCharset());
        pid = Long.parseLong(pidContents);

      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Problem starting a jupyter server: {0}", ex.
                toString());
        if (process != null) {
          process.destroyForcibly();
        }
      }
      maxTries--;
    }

    if (!foundToken) {
//      hdfsuserConfCache.remove(hdfsUser);
      return null;
    } else {
      jc.setPid(pid);
      jc.setToken(token);
    }

    return new JupyterDTO(jc.getPort(), jc.getToken(), jc.getPid(), secretConfig);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String getJupyterHome(String hdfsUser, JupyterProject jp) throws
          AppException {
    if (jp == null) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Couldn't resolve JUPYTER_HOME using DB.");
    }
    String projectPath = settings.getJupyterDir() + File.separator
            + Settings.DIR_ROOT + File.separator + jp.getProjectId().getName()
            + File.separator + hdfsUser + File.separator + jp.getSecret();

    return projectPath;
  }

  /**
   * This method both stops any jupyter server for a proj_user
   *
   * @param hdfsUser
   * @throws AppException
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void stopServerJupyterUser(String hdfsUser)
          throws AppException {
    if (hdfsUser == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(),
              "Null hdfsUsername when stopping the Jupyter Server.");
    }
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog, "stop", hdfsUser};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor(10l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
              toString());
      exitValue = -2;
    }

    if (exitValue != 0) {
      throw new AppException(Response.Status.REQUEST_TIMEOUT.getStatusCode(),
              "Couldn't stop Jupyter Notebook Server.");
    }

  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void killServerJupyterUser(String projectPath, Long pid, Integer port)
          throws AppException {
    if (projectPath == null || pid == null || port == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(),
              "Invalid arguments when stopping the Jupyter Server.");
    }
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog, "kill", projectPath,
      pid.toString(), port.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }

      process.waitFor(10l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
              toString());
      exitValue = -2;
    }

    if (exitValue != 0) {
      throw new AppException(Response.Status.REQUEST_TIMEOUT.getStatusCode(),
              "Couldn't stop Jupyter Notebook Server.");
    }

  }

  private void stopCleanly(String hdfsUser) throws AppException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not find Jupyter entry for user: " + hdfsUser);
    }
    String projectPath = getJupyterHome(hdfsUser, jp);

    // stop the server, remove the user in this project's local dirs
    killServerJupyterUser(projectPath, jp.getPid(), jp.
            getPort());
    // remove the reference to th e server in the DB.
    jupyterFacade.removeNotebookServer(hdfsUser);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void stopProject(Project project)
          throws AppException {
    Collection<ProjectTeam> team = project.getProjectTeamCollection();
    for (ProjectTeam pt : team) {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, pt.
              getUser());
      try {
        stopServerJupyterUser(hdfsUsername);
      } catch (AppException ex) {
        // continue
      }
    }

    String prog = settings.getHopsworksDomainDir()
            + "/bin/jupyter-project-cleanup.sh";
    int exitValue;
    String[] command = {"/usr/bin/sudo", prog, project.getName()};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor(2l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem cleaning up project: "
              + project.getName() + ": {0}", ex.toString());
      exitValue = -2;
    }

    if (exitValue != 0) {
      logger.log(Level.WARNING, "Problem remove project's jupyter folder: "
              + project.getName());
    }

  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean pingServerJupyterUser(Long pid) {
    int exitValue;
    Integer id = 1;
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    String[] command = {"/usr/bin/sudo", prog, "ping", pid.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }

      process.waitFor(10l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE,
              "Problem checking if Jupyter Notebook server is running: {0}", ex.
                      toString());
      exitValue = -2;
    }

    return exitValue == 0;
  }

}
