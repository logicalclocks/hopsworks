package io.hops.hopsworks.common.dao.jupyter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.AppException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;

@Stateless
public class JupyterFacade {

  private final static Logger logger = Logger.getLogger(JupyterFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  private ProjectFacade projectsFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;

  protected EntityManager getEntityManager() {
    return em;
  }

  public JupyterFacade() throws Exception {
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

  public void startServer(Project project, HdfsUsers user) {

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
    JupyterConfig jc = new JupyterConfig(project.getName(), user.getUsername(),
            settings);

    boolean failed = true;

    while (failed) {
      int port = ThreadLocalRandom.current().nextInt(40000, 59999);
//      String[] command = {"JUPYTER_CONFIG_DIR=" + jc.getConfDirPath(), 
      String[] command = {"jupyter", "--no-browser", " > " + jc.getLogDirPath()
          + "/" + user.getUsername() + "-" + port + ".log"};
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
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Problem starting a jupyter server: {0}", ex.
                toString());
      }
    }

  }

  public void stopServer(String hdfsUser) throws AppException {

    if (hdfsUser == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find a Jupyter Notebook server to delete.");
    }

    JupyterConfig.removeNotebookServer(hdfsUser);

    JupyterProject jp = this.findByUser(hdfsUser);
    remove(jp);

    // delete JupyterProject entity bean
  }

  public void stopServers(Project project) {

    // delete JupyterProject entity bean
  }

  private void saveServer(int port, int userId, String token,
          Process process) {

    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();

      JupyterProject jp
              = new JupyterProject(port, userId, Date.from(Instant.now()), ip,
                      token, JupyterConfig.getPidOfProcess(process));

      persist(jp);
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterFacade.class.getName()).
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
          if (!JupyterConfig.removeNotebookServer(user)) {
            // try and kill any process with the PID
            long pid = jp.getPid();
            String[] command = {"kill", "-9", Long.toString(pid)};
            ProcessBuilder pb = new ProcessBuilder(command);
            try {
              pb.start();
              pb.wait(5000l);
            } catch (IOException | InterruptedException ex) {
              Logger.getLogger(JupyterFacade.class.getName()).
                      log(Level.SEVERE, null, ex);
            }

          }
        }
        remove(jp);
      }
    }
    // Kill any processes

  }

}
