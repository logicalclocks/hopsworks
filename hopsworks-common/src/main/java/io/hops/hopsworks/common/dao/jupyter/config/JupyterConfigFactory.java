package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

  private String hadoopClasspath = null;

  private static final ConcurrentMap<String, JupyterConfig> hdfsuserConfCache
          = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Process> runningServers
          = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    loadConfig();
  }

  @PreDestroy
  public void preDestroy() {

  }

  private void loadConfig() {

  }

  /**
   * If an existing process is running for this username, kill it.
   * Starts a new process with that username.
   *
   * @param hdfsUsername
   * @param process
   *
   * @param hdfsUsername
   * @param process
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  private void addNotebookServer(String hdfsUsername,
          Process process) throws AppException {
    if (!process.isAlive()) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Jupyter server died unexpectadly");
    }
//    removeNotebookServer(hdfsUsername);
    runningServers.put(hdfsUsername, process);

  }

  private boolean killNotebookServer(Process p) {
    if (p != null) {
      // use destroy forcibly because ctrl-c causes the process to ask if you
      // want to kill it - it doesn't kill the process
      p.destroyForcibly();
      return true;
    }
    return false;
  }

  /**
   * Returns a unique jupyter configuration for the project user.
   *
   * @param hdfsUser
   * @return null if the notebook does not exist.
   */
  public JupyterConfig getFromCache(String hdfsUser) {
    JupyterConfig userConfig = hdfsuserConfCache.get(hdfsUser);
    return userConfig;
  }

  /**
   * Remove user configuration from cache.
   *
   * @param projectName
   * @param username
   */
  public void cleanup(String hdfsUser) throws AppException {
    JupyterConfig config = this.hdfsuserConfCache.remove(hdfsUser);
    if (config != null) {
      long pid = config.getPid();
      String projectPath = config.getProjectPath();
      int port = config.getPort();
      config.clean();
      stopServerJupyterUser(projectPath, pid, port);
    }
  }

  /**
   * Deletes jupyter configuration dir for user.
   *
   * @param project
   * @return
   */
  public boolean deleteProject(Project project) {
    Collection<ProjectTeam> ptc = project.getProjectTeamCollection();

    for (ProjectTeam pt : ptc) {

    }

    return false;
  }

  public void initNotebook(Project project, HdfsUsers user) {

  }

  public void stopServers(Project project) {

    // delete JupyterProject entity bean
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
//        remove(jp);
      }
    }
    // Kill any processes

  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startServerAsJupyterUser(Project project, String secret,
          String hdfsUser,
          int driverCores, String driverMemory, int numExecutors,
          int executorCores, String executorMemory, int gpus,
          String archives, String jars, String files, String pyFiles) throws
          AppException, IOException, InterruptedException {

    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    String hadoopClasspath = getHadoopClasspath();

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
    int maxTries = 10;
    Process process = null;
    Integer port = 0;
    JupyterConfig jc = null;

    // kill any running servers for this user, clear cached entries
    cleanup(hdfsUser);

    while (!foundToken && maxTries > 0) {
      // use pidfile to kill any running servers
      if (settings.getVagrantEnabled()) {
        port = 8888;
      } else {
        port = ThreadLocalRandom.current().nextInt(40000, 59999);
      }

      jc = new JupyterConfig(project.getName(), secret, hdfsUser, hdfsLeFacade.
              getSingleEndpoint(), settings, port, driverCores,
              driverMemory, numExecutors, executorCores, executorMemory, gpus,
              archives, jars, files, pyFiles);
      hdfsuserConfCache.put(hdfsUser, jc);

      String logfile = jc.getLogDirPath() + "/" + hdfsUser + "-" + port + ".log";
      String[] command
              = {"/usr/bin/sudo", prog, "start", jc.getProjectDirPath(),
                //              = {"/usr/bin/nohup", prog, "start", jc.getProjectDirPath(),
                jc.getSettings().getHadoopDir(), settings.getJavaHome(),
                settings.getAnacondaProjectDir(project.getName()), port.
                toString(),
                hdfsUser + "-" + port + ".log"};
      ProcessBuilder pb = new ProcessBuilder(command);
      String pidfile = jc.getRunDirPath() + "/jupyter.pid";
      try {
        // Send both stdout and stderr to the same stream
        pb.redirectErrorStream(true);
        pb.directory(new File(jc.getProjectDirPath()));

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
      // cleanup
      if (jc != null) {
//        jc.cleanAndRemoveConfDirs();
      }
      hdfsuserConfCache.remove(hdfsUser);
      return null;
    } else {
      jc.setPid(pid);
      jc.setToken(token);
    }

    return new JupyterDTO(jc.getPort(), jc.getToken(), jc.getPid(), jc.
            getDriverCores(), jc.getDriverMemory(), jc.getNumExecutors(), jc.
            getExecutorCores(), jc.getExecutorMemory(), jc.getGpus(), jc.
            getArchives(), jc.getJars(), jc.getFiles(), jc.getPyFiles());

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

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void stopServerJupyterUser(String projectPath, Long pid, Integer port)
          throws AppException {
    if (projectPath == null || pid == null || port == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(),
              "Invalid arguments when stopping the Jupyter Server.");
    }
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog, "stop", projectPath, 
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

}
