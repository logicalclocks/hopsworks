package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
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
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;

/**
 * *
 * This class wraps a bash script with sudo rights that can be executed by the node['hopsworks']['user'].
 * /srv/hops/domains/domain1/bin/jupyter.sh
 * The bash script has several commands with parameters that can be exceuted.
 * This class provides a Java interface for executing the commands.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@DependsOn("Settings")
public class JupyterProcessMgr {

  private static final Logger logger = Logger.getLogger(JupyterProcessMgr.class.getName());

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
  private ProjectFacade projectFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private UserFacade userFacade;

  private String hadoopClasspath = null;

  @PostConstruct
  public void init() {
  }

  @PreDestroy
  public void preDestroy() {

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
    long pid = 0;

    try {
      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getLong(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      pid = 0;
    }
    return pid;
  }

  public String getHadoopClasspath() throws IOException,
      InterruptedException {
    if (this.hadoopClasspath == null) {
      ProcessBuilder ps = new ProcessBuilder(settings.getHadoopSymbolicLinkDir()
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
          try {
            killServerJupyterUser(user, "", jp.getPid(), 1);
          } catch (AppException ex) {
            Logger.getLogger(JupyterProcessMgr.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    // when the project is removed all of the SQL entries will be cleaned up automatically  
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startServerAsJupyterUser(Project project, String secretConfig, String hdfsUser,
      JupyterSettings js) throws AppException, IOException, InterruptedException {

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
    JupyterConfigFilesGenerator jc = null;

    // kill any running servers for this user, clear cached entries
    while (!foundToken && maxTries > 0) {
      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);

      jc = new JupyterConfigFilesGenerator(project, secretConfig, hdfsUser,
          hdfsLeFacade.getSingleEndpoint(), settings, port, token, js);

      String secretDir = settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret();

      if (settings.isPythonKernelEnabled()) {
        createPythonKernelForProjectUser(jc.getNotebookPath(), hdfsUser);
      }

      String logfile = jc.getLogDirPath() + "/" + hdfsUser + "-" + port + ".log";
      String[] command
          = {"/usr/bin/sudo", prog, "start", jc.getNotebookPath(),
            jc.getSettings().getHadoopSymbolicLinkDir() + "-" + settings.getHadoopVersion(), settings.getJavaHome(),
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
//  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
//  public void stopServerJupyterUser(String hdfsUser)
//      throws AppException {
//    if (hdfsUser == null) {
//      throw new AppException(Response.Status.BAD_REQUEST.
//          getStatusCode(),
//          "Null hdfsUsername when stopping the Jupyter Server.");
//    }
//    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
//    int exitValue;
//    Integer id = 1;
//    String[] command = {"/usr/bin/sudo", prog, "stop", hdfsUser};
//    logger.log(Level.INFO, Arrays.toString(command));
//    ProcessBuilder pb = new ProcessBuilder(command);
//    try {
//      Process process = pb.start();
//
//      BufferedReader br = new BufferedReader(new InputStreamReader(
//          process.getInputStream(), Charset.forName("UTF8")));
//      String line;
//      while ((line = br.readLine()) != null) {
//        logger.info(line);
//      }
//      process.waitFor(10l, TimeUnit.SECONDS);
//      exitValue = process.exitValue();
//    } catch (IOException | InterruptedException ex) {
//      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
//          toString());
//      exitValue = -2;
//    }
//
//    if (exitValue != 0) {
//      throw new AppException(Response.Status.REQUEST_TIMEOUT.getStatusCode(),
//          "Couldn't stop Jupyter Notebook Server.");
//    }
//
//  }
//  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
//  public int killOrphanedWithPid(Long pid) {
//    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
//    int exitValue;
//    Integer id = 1;
//    String[] command = {"/usr/bin/sudo", prog, "killhard", pid.toString()};
//    ProcessBuilder pb = new ProcessBuilder(command);
//    try {
//      Process process = pb.start();
//      BufferedReader br = new BufferedReader(new InputStreamReader(
//          process.getInputStream(), Charset.forName("UTF8")));
//      String line;
//      while ((line = br.readLine()) != null) {
//        logger.info(line);
//      }
//      process.waitFor(10l, TimeUnit.SECONDS);
//      exitValue = process.exitValue();
//    } catch (IOException | InterruptedException ex) {
//      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
//          toString());
//      exitValue = -2;
//    }
//    return exitValue;
//  }
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void killServerJupyterUser(String hdfsUsername, String jupyterHomePath, Long pid, Integer port)
      throws AppException {
    if (jupyterHomePath == null || pid == null || port == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(),
          "Invalid arguments when stopping the Jupyter Server.");
    }
    // 1. Remove jupyter settings from the DB for this notebook first. If this fails, keep going to kill the notebook
    try {
      jupyterFacade.removeNotebookServer(hdfsUsername);
    } catch (Exception e) {
      logger.warning("Problem when removing jupyter notebook entry from jupyter_project table: " + jupyterHomePath);
    }

    // 2. Then kill the jupyter notebook server. If this step isn't 
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    if (jupyterHomePath.isEmpty()) {
      jupyterHomePath = " ";
    }
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog, "kill", jupyterHomePath,
      pid.toString(), port.toString()};
    logger.log(Level.INFO, Arrays.toString(command));
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

  public void stopCleanly(String hdfsUser) throws AppException {
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp != null) {
      String jupyterHomePath = getJupyterHome(hdfsUser, jp);
      // stop the server, remove the user in this project's local dirs
      killServerJupyterUser(hdfsUser, jupyterHomePath, jp.getPid(), jp.getPort());
      // remove the reference to th e server in the DB.
      jupyterFacade.removeNotebookServer(hdfsUser);
    }
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void stopProject(Project project) //      throws AppException 
  {
    String jupyterHomePath = "";
    for (JupyterProject jp : project.getJupyterProjectCollection()) {
      HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
      String hdfsUsername = (hdfsUser == null) ? "" : hdfsUser.getName();
      try {
        killServerJupyterUser(hdfsUsername, jupyterHomePath, jp.getPid(), jp.getPort());
      } catch (AppException ex) {
        logger.warning("When removing a project, could not shutdown the jupyter notebook server.");
      }

    }
    projectCleanup(project);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void projectCleanup(Project project) {
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter-project-cleanup.sh";
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
    int exitValue = executeJupyterCommand("ping", pid.toString());
    return exitValue == 0;
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int createPythonKernelForProjectUser(String hdfsUser) {
    String secretPath = "";
    String projectName = hdfsUsersController.getProjectName(hdfsUser);
    Project project = projectFacade.findByName(projectName);
    boolean notFound = true;
    for (JupyterSettings js : project.getJupyterSettingsCollection()) {
      if (js.getPrivateDir().contains(hdfsUser)) {
        secretPath = js.getPrivateDir();
        notFound = false;
        break;
      }
    }
    if (notFound) {
      return -11;
    }
    String privateDir = this.settings.getJupyterDir()
        + Settings.DIR_ROOT + File.separator + project.getName()
        + File.separator + hdfsUser + File.separator + secretPath;

    return createPythonKernelForProjectUser(privateDir, hdfsUser);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int createPythonKernelForProjectUser(Project project, Users user) {
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(project.getId(), user.getEmail());
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);

    String privateDir = this.settings.getJupyterDir()
        + Settings.DIR_ROOT + File.separator + project.getName()
        + File.separator + hdfsUser + File.separator + js.getSecret();

    return executeJupyterCommand("kernel-add", privateDir, hdfsUser);
  }

  private int createPythonKernelForProjectUser(String privateDir, String hdfsUser) {
    return executeJupyterCommand("kernel-add", privateDir, hdfsUser);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int removePythonKernelForProjectUser(String hdfsUser) {
    return executeJupyterCommand("kernel-remove", hdfsUser);
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int removePythonKernelsForProject(String projectName) {
    return executeJupyterCommand("kernel-remove", projectName + "*");
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public List<JupyterProject> getAllNotebooks() {
    List<JupyterProject> allNotebooks = jupyterFacade.getAllNotebookServers();

    executeJupyterCommand("list");

    File file = new File(Settings.JUPYTER_PIDS);

    List<Long> pidsRunning = new ArrayList<>();
    try {
      Scanner scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        pidsRunning.add(Long.parseLong(line));
      }
    } catch (FileNotFoundException e) {
      logger.warning("Invalid pids in file: " + Settings.JUPYTER_PIDS);
    }

    List<Long> pidsOrphaned = new ArrayList<>();
    pidsOrphaned.addAll(pidsRunning);

    for (Long pid : pidsRunning) {
      boolean foundPid = false;
      for (JupyterProject jp : allNotebooks) {
        if (pid == jp.getPid()) {
          foundPid = true;
        }
        if (foundPid) {
          pidsOrphaned.remove(pid);
        }
      }
    }

    for (Long pid : pidsOrphaned) {
      JupyterProject jp = new JupyterProject();
      jp.setPid(pid);
      jp.setPort(0);
      jp.setLastAccessed(Date.from(Instant.now()));
      jp.setHdfsUserId(-1);
      allNotebooks.add(jp);
    }
    file.deleteOnExit();

    return allNotebooks;
  }

  private int executeJupyterCommand(String... args) {
    if (args == null || args.length == 0) {
      return -99;
    }
    int exitValue;
    Integer id = 1;
    String prog = this.settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    ArrayList<String> command = new ArrayList<>();
    command.add("/usr/bin/sudo");
    command.add(prog);
    command.addAll(java.util.Arrays.asList(args));
    logger.log(Level.INFO, Arrays.toString(command.toArray()));
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
    return exitValue;
  }

}
