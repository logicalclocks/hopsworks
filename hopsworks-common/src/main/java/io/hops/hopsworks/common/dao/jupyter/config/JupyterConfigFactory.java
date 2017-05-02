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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
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
import javax.ws.rs.core.Response;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JupyterConfigFactory {

  private static final Logger logger = Logger.getLogger(
          JupyterConfigFactory.class.getName());
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "conf/jupyter_notebook_config.py";

//  @PersistenceContext(unitName = "kthfsPU")
//  private EntityManager em;
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

//  protected EntityManager getEntityManager() {
//    return em;
//  }
//  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;
  @PostConstruct
  public void init() {
//    JupyterConfig.COMMON_CONF = 
    loadConfig();
  }

  @PreDestroy
  public void preDestroy() {
//    for (Process p : runningServers.values()) {
//      if (p != null) {
//        this.killNotebookServer(p);
//      }
//    }
//    for (JupyterConfig conf : hdfsuserConfCache.values()) {
//      conf.clean();
//    }
//    hdfsuserConfCache.clear();
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
  public void removeFromCache(String hdfsUser) {
//    Project project = projectBean.findByName(projectName);
//    Users user = userFacade.findByEmail(username);
//    HdfsUsers user = hdfsUsername.get(username);
//    if (project == null || user == null) {
//      return;
//    }
//    if (project == null || user == null) {
//      return;
//    }
//    String hdfsUser = hdfsUsername.getHdfsUserName(project, user);
    JupyterConfig config = this.hdfsuserConfCache.remove(hdfsUser);
    if (config != null) {
      config.clean();
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

//    JupyterConfig conf = hdfsuserConfCache.remove(project.getName());
//    if (conf != null) {
//      return conf.cleanAndRemoveConfDirs();
//    }
//    String projectDirPath = settings.getZeppelinDir() + File.separator
//            + Settings.DIR_ROOT + File.separator + project.getName();
//    File projectDir = new File(projectDirPath);
//    String hdfsUser = hdfsUsername.getHdfsUserName(project, project.getOwner());
//    if (projectDir.exists()) {
//      conf = new JupyterConfig(project.getName(), hdfsUser, settings, null);
//      return conf.cleanAndRemoveConfDirs();
//    }
    return false;
  }

  public void initNotebook(Project project, HdfsUsers user) {

  }

  /**
   * 
   * This method will stop any running server for this hdfsUser, and start
   * a new one.
   *
   * @param project
   * @param hdfsUser
   * @param driverCores
   * @param driverMemory
   * @param numExecutors
   * @param executorCores
   * @param executorMemory
   * @param gpus
   * @param archives
   * @param jars
   * @param files
   * @param pyFiles
   * @return token for the Notebook server
   * @throws AppException
   * @throws java.lang.InterruptedException
   * @throws java.io.IOException
   */
  public JupyterDTO startServer(Project project, String hdfsUser,
          int driverCores, String driverMemory, int numExecutors,
          int executorCores, String executorMemory, int gpus,
          String archives, String jars, String files, String pyFiles) throws
          AppException, InterruptedException, IOException {

    JupyterProject jp = null;
    String token = null;
    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);
    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find hdfs user. Not starting Jupyter.");
    }
    // The Jupyter Notebook is running at: http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
    boolean failed = true;
    int maxTries = 50;
    Process process = null;
    int port = 0;
    JupyterConfig jc = null;
    
    // kill any running servers for this user, clear cached entries
    hdfsuserConfCache.remove(hdfsUser);
    if (runningServers.containsKey(hdfsUser)) {
      Process p = runningServers.get(hdfsUser);
      if (p != null) {
        p.destroyForcibly();
      }
      runningServers.remove(hdfsUser);
    }

    while (failed && maxTries > 0) {

      if (settings.getVagrantEnabled()) {
        port = 8888;
      } else {
        port = ThreadLocalRandom.current().nextInt(40000, 59999);
      }
//      boolean alreadyAllocated = false;
//      for (JupyterConfig tmp : hdfsuserConfCache.values()) {
//        if (tmp.getPort() == port) {
//          alreadyAllocated = true;
//          break;
//        }
//      }
//      if (alreadyAllocated) {
//        maxTries--;
//        continue;
//      }
//      port = Settings.JUPYTER_PORT;
      jc = new JupyterConfig(project.getName(), hdfsUser, hdfsLeFacade.
              getActiveNN().getHostname(), settings, port, driverCores,
              driverMemory, numExecutors, executorCores, executorMemory, gpus,
              archives, jars, files, pyFiles);
      hdfsuserConfCache.put(hdfsUser, jc);

      String[] command = {"jupyter", "notebook"};
      ProcessBuilder pb = new ProcessBuilder(command);
      Map<String, String> env = pb.environment();
      env.put("JUPYTER_DATA_DIR", jc.getNotebookDirPath());
      env.put("JUPYTER_PATH", jc.getNotebookDirPath());
      env.put("JUPYTER_CONFIG_DIR", jc.getConfDirPath());
      env.put("JUPYTER_RUNTIME_DIR", jc.getRunDirPath());
      env.put("LD_LIBRARY_PATH", "$LD_LIBRARY_PATH:" + settings.getHadoopDir()
              + "/lib/native");
      env.put("CLASSPATH", "$CLASSPATH:" + getHadoopClasspath());
      env.put("HADOOP_HOME", settings.getHadoopDir());
      env.put("JAVA_HOME", settings.getJavaHome());
      env.put("SPARKMAGIC_CONF_DIR", jc.getConfDirPath());
      String logfile = jc.getLogDirPath() + "/" + hdfsUser + "-" + port + ".log";
      try {
        // Send both stdout and stderr to the same stream
        pb.redirectErrorStream(true);
        pb.directory(new File(jc.getNotebookDirPath()));

        process = pb.start();

        final BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream(), Charset.forName("UTF8")));
        String line;
// [I 11:59:16.597 NotebookApp] The Jupyter Notebook is running at: 
// http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
        String pattern = "(.*)token=(.*)";
        Pattern r = Pattern.compile(pattern);
        boolean foundToken = false;

        try (PrintWriter out = new PrintWriter(logfile)) {
          while (((line = br.readLine()) != null) && !foundToken) {
            logger.info(line);
            out.println(line);
            Matcher m = r.matcher(line);
            if (m.find()) {
              token = m.group(2);
              foundToken = true;
            }
          }
        }

        // We need to start a thread to write the log to a file, otherwise
        // the process hangs. It's ok to create a thread in java ee, as long as 
        // it doesn't access Java EE beans/facades/resources.
        // http://stackoverflow.com/questions/34788234/how-to-create-threads-in-java-ee-environment
        // The thread will exit when it can no longer read from stout/stderr
        new Thread() {
          public void run() {
            String line = "";
            try (PrintWriter out = new PrintWriter(logfile)) {
              while (((line = br.readLine()) != null)) {
                out.println(line);
              }
            } catch (FileNotFoundException ex) {
              Logger.getLogger(JupyterConfigFactory.class.getName()).
                      log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
              Logger.getLogger(JupyterConfigFactory.class.getName()).
                      log(Level.SEVERE, null, ex);
            }
          }
        }.start();

        failed = false;
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Problem starting a jupyter server: {0}", ex.
                toString());
        if (process != null) {
          process.destroyForcibly();
        }
      }
      maxTries--;
    }

    if (failed || token == null) {
      // cleanup
      if (jc != null) {
//        jc.cleanAndRemoveConfDirs();
      }
      hdfsuserConfCache.remove(hdfsUser);
      return null;
    } else {
      jc.setPid(getPidOfProcess(process));
      jc.setToken(token);
    }

    return new JupyterDTO(jc.getPort(), jc.getToken(), jc.getPid(), jc.
            getDriverCores(), jc.getDriverMemory(), jc.getNumExecutors(), jc.
            getExecutorCores(), jc.getExecutorMemory(), jc.getGpus(), jc.
            getArchives(), jc.getJars(), jc.getFiles(), jc.getPyFiles());
  }

  public boolean stopServer(String hdfsUser) throws AppException {

    if (hdfsUser == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find a Jupyter Notebook server to delete.");
    }

    JupyterConfig config = getFromCache(hdfsUser);
    Process p = runningServers.get(hdfsUser);
    if (config == null || p == null) {
//      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
//              "Could not find a Jupyter Notebook server for this user to delete.");
      return false;
    }
    killNotebookServer(p);
    removeFromCache(hdfsUser);
    runningServers.remove(hdfsUser);

//    jupyterFacade.remove(jp);
    // delete JupyterProject entity bean
    return true;
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

}
