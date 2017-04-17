package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JupyterConfigFactory {

  private static final Logger logger = Logger.getLogger(
          JupyterConfigFactory.class.getName());
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "conf/jupyter_notebook_config.py";

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsername;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;

  private final ConcurrentMap<String, JupyterConfig> hdfsuserConfCache
          = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, Process> runningServers
          = new ConcurrentHashMap<>();

  protected EntityManager getEntityManager() {
    return em;
  }

//  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;
  @PostConstruct
  public void init() {
//    JupyterConfig.COMMON_CONF = 
    loadConfig();
  }

  @PreDestroy
  public void preDestroy() {
    for (Process p : runningServers.values()) {
      this.killNotebookServer(p);
    }
    for (JupyterConfig conf : hdfsuserConfCache.values()) {
      conf.clean();
    }
    hdfsuserConfCache.clear();
  }

  private void loadConfig() {

  }

  public JupyterConfig init(String projectName, String owner) {

    JupyterConfig conf = new JupyterConfig(projectName, owner, settings);
    this.hdfsuserConfCache.put(owner, conf);
    return conf;
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
    removeNotebookServer(hdfsUsername);
    runningServers.put(hdfsUsername, process);

  }

  private boolean killNotebookServer(Process p) {
    if (p != null) {
      p.destroyForcibly();
      return true;
    }
    return false;
  }

  public List<JupyterProject> findNotebooksByProject(Integer projectId) {
    TypedQuery<JupyterProject> query = em.createNamedQuery(
            "JupyterProject.findByProjectId",
            JupyterProject.class);
    query.setParameter("projectId", projectId);
    List<JupyterProject> res = query.getResultList();
    List<JupyterProject> notebooks = new ArrayList<>();
    for (JupyterProject pt : res) {
//      notebooks.add(new TopicDTO(pt.getProjectTopicsPK().getTopicName(),
//              pt.getSchemaTopics().getSchemaTopicsPK().getName(),
//              pt.getSchemaTopics().getSchemaTopicsPK().getVersion()));
    }
    return notebooks;
  }

  public boolean removeNotebookServer(String hdfsUsername) {
    if (runningServers.containsKey(hdfsUsername)) {
      Process oldProcess = runningServers.get(hdfsUsername);
      killNotebookServer(oldProcess);
      runningServers.remove(hdfsUsername);
//      BufferedReader br = consoleOutput.get(hdfsUsername);
//      if (br != null) {
//        try {
//          br.close();
//        } catch (IOException ex) {
//          Logger.getLogger(JupyterConfig.class.getName()).
//                  log(Level.SEVERE, null, ex);
//        }
//        consoleOutput.remove(hdfsUsername);
//      }
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

  public JupyterProject findByUser(String hdfsUser) {
    HdfsUsers res = null;
    TypedQuery<HdfsUsers> query = em.createNamedQuery(
            "HdfsUsers.findByName", HdfsUsers.class);
    query.setParameter("name", hdfsUser);
    try {
      res = query.getSingleResult();
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
      return null;
    }
    JupyterProject res2 = null;
    TypedQuery<JupyterProject> query2 = em.createNamedQuery(
            "JupyterProject.findByHdfsUserId", JupyterProject.class);
    query.setParameter("hdfsUserId", res.getId());
    try {
      res2 = query2.getSingleResult();
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return res2;
  }

  public void initNotebook(Project project, HdfsUsers user) {

  }

  public void startServer(Project project, HdfsUsers user) throws AppException {

    // Set to point to project directory
    // JUPYTER_DATA_DIR
    // JUPYTER_CONFIG_DIR 
    // JUPYTER_RUNTIME_DIR
    // Store the files:
    // custom
    // jupyter_notebook_config.py
    // jupyter_nbconvert_config.py 
    // jupyter_qtconsole_config.py
    // jupyter_console_config.py
    // 
    // 
    // jupyter --no-browser --certfile=mycert.pem --keyfile mykey.key
    // The Jupyter Notebook is running at: http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
//    JupyterConfig jc = new JupyterConfig(project.getName(), user.getUsername(),
//            settings);
    JupyterConfig jc = hdfsuserConfCache.get(user.getUsername());
    if (jc == null) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not find a Jupyter Notebook server configuration .");

    }

    boolean failed = true;
    int maxTries = 50;

    while (failed && maxTries > 0) {
      Integer id = 1;
      int port = ThreadLocalRandom.current().nextInt(40000, 59999);

//      String[] command = {"JUPYTER_CONFIG_DIR=" + jc.getConfDirPath(), 
      String[] command
              = {"JUPYTER_CONFIG_DIR=" + jc.getConfDirPath(), "jupyter",
                "--no-browser", " > " + jc.getLogDirPath() + "/"
                  + user.getUsername() + "-" + port + ".log"};
      ProcessBuilder pb = new ProcessBuilder(command);
      Map<String, String> env = pb.environment();
      env.put("JUPYTER_CONFIG_DIR", jc.getConfDirPath());
      try {
        Process process = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream(), Charset.forName("UTF8")));
        String token = "";
        String line;
// [I 11:59:16.597 NotebookApp] The Jupyter Notebook is running at: 
// http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
        String pattern = "(.*)token=(.*)";
        Pattern r = Pattern.compile(pattern);
        boolean foundToken = false;
        while (((line = br.readLine()) != null) && !foundToken) {
          logger.info(line);
          Matcher m = r.matcher(line);
          if (m.find()) {
            token = m.group(2);
            foundToken = true;
          }
        }
        saveServer(port, user.getId(), token, process);
        failed = false;
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Problem starting a jupyter server: {0}", ex.
                toString());
      }
      maxTries--;
    }

  }

  public void stopServer(String hdfsUser) throws AppException {

    if (hdfsUser == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find a Jupyter Notebook server to delete.");
    }

//    JupyterConfig.removeNotebookServer(hdfsUser);
    JupyterProject jp = this.findByUser(hdfsUser);
    remove(jp);

    // delete JupyterProject entity bean
  }

  public void stopServers(Project project) {

    // delete JupyterProject entity bean
  }

  private void saveServer(int port, int userId, String token,
          Process process) throws AppException {

    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();

      JupyterProject jp
              = new JupyterProject(port, userId, Date.from(Instant.now()), ip,
                      token, JupyterConfig.getPidOfProcess(process));

      persist(jp);
      HdfsUsers user = hdfsUsersFacade.find(userId);
//      JupyterConfig.addNotebookServer(user.getUsername(), process);
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterConfigFactory.class.getName()).
              log(Level.SEVERE, null, ex);
    }

  }

  private void persist(JupyterProject jp) {
    if (jp != null) {
      em.persist(jp);
    }
  }

  private void update(JupyterProject jp) {
    if (jp != null) {
      em.merge(jp);
    }
  }

  private void remove(JupyterProject jp) {
    if (jp != null) {
      em.remove(jp);
    }
  }

  public void removeProject(Project project) {
    // Find any active jupyter servers

    Collection<JupyterProject> instances = project.getJupyterProjectCollection();
    if (instances != null) {
      for (JupyterProject jp : instances) {
        HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
        if (hdfsUser != null) {
          String user = hdfsUser.getUsername();
          //          if (!JupyterConfig.removeNotebookServer(user)) {
          //            // try and kill any process with the PID
          //            long pid = jp.getPid();
          //            String[] command = {"kill", "-9", Long.toString(pid)};
          //            ProcessBuilder pb = new ProcessBuilder(command);
          //            try {
          //              pb.start();
          //              pb.wait(5000l);
          //            } catch (IOException | InterruptedException ex) {
          //              Logger.getLogger(JupyterFacade.class.getName()).
          //                      log(Level.SEVERE, null, ex);
          //            }
          //
          //          }
        }
        remove(jp);
      }
    }
    // Kill any processes

  }

}
