package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

@Stateless
public class JupyterFacade {

  private static final Logger logger = Logger.getLogger(JupyterFacade.class.
          getName());

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
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;

  protected EntityManager getEntityManager() {
    return em;
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
//    if (runningServers.containsKey(hdfsUsername)) {
//      Process oldProcess = runningServers.get(hdfsUsername);
//      killNotebookServer(oldProcess);
//      runningServers.remove(hdfsUsername);
//    }
    return false;
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
    } catch (EntityNotFoundException | NoResultException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.INFO, null,
              e);
      return null;
    }
    JupyterProject res2 = null;
    TypedQuery<JupyterProject> query2 = em.createNamedQuery(
            "JupyterProject.findByHdfsUserId", JupyterProject.class);
    query2.setParameter("hdfsUserId", res.getId());
    try {
      res2 = query2.getSingleResult();
    } catch (EntityNotFoundException | NoResultException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.INFO, null,
              e);
    }
    return res2;
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

  public JupyterProject saveServer(Project project, int port, int hdfsUserId, String token,
          long pid)
          throws AppException {
    JupyterProject jp = null;
    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();

      jp = new JupyterProject(project, port, hdfsUserId, ip, token, pid);

      persist(jp);
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterFacade.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return jp;
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
