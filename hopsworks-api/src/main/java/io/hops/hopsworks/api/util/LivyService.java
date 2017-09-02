package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@Stateless
public class LivyService {

  private static final Logger LOGGER = Logger.getLogger(LivyService.class.getName());

  private static final String JUPYTER_SESSION_NAME = "remotesparkmagics-jupyter";

  @EJB
  private Settings settings;
  @EJB
  private ProjectTeamFacade teambean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUserBean;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private YarnApplicationstateFacade appStateBean;

  /**
   * Get all Zeppelin livy sessions for project
   *
   * @param project
   * @return
   */
  public List<LivyMsg.Session> getZeppelinLivySessions(Project project) {
    return getLivySessions(project, false);
  }

  /**
   * Get all Jupyter livy sessions for project
   *
   * @param project
   * @return
   */
  public List<LivyMsg.Session> getJupyterLivySessions(Project project) {
    return getLivySessions(project, true);
  }

  private List<LivyMsg.Session> getLivySessions(Project project, boolean jupyter) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    LivyMsg sessionList = getLivySessions();
    if (sessionList == null || sessionList.getSessions() == null || sessionList.getSessions().length == 0) {
      return sessions;
    }
    List<ProjectTeam> projectTeam;
    projectTeam = teambean.findMembersByProject(project);
    String hdfsUsername;
    YarnApplicationstate appStates;
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = hdfsUserBean.getHdfsUserName(project, member.getUser());
      for (LivyMsg.Session s : sessionList.getSessions()) {
        if (hdfsUsername != null && hdfsUsername.equals(s.getProxyUser())) {
          appStates = appStateBean.findByAppId(s.getAppId());
          if (!jupyter && (appStates == null || appStates.getAppname().equals(JUPYTER_SESSION_NAME))) {
            continue;
          }
          if (jupyter && (appStates == null || !appStates.getAppname().equals(JUPYTER_SESSION_NAME))) {
            continue;
          }
          s.setOwner(member.getUser().getEmail());
          sessions.add(s);
        }
      }
    }

    return sessions;
  }

  /**
   * Get all Zeppelin livy sessions for project and user
   *
   * @param project
   * @param user
   * @return
   */
  public List<LivyMsg.Session> getZeppelinLivySessionsForProjectUser(Project project, Users user) {
    return getLivySessionsForProjectUser(project, user, false);
  }

  /**
   * Get all Jupyter livy sessions for project and user
   *
   * @param project
   * @param user
   * @return
   */
  public List<LivyMsg.Session> getJupyterLivySessionsForProjectUser(Project project, Users user) {
    return getLivySessionsForProjectUser(project, user, true);
  }

  private List<LivyMsg.Session> getLivySessionsForProjectUser(Project project, Users user, boolean jupyter) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    LivyMsg sessionList = getLivySessions();
    if (sessionList == null || sessionList.getSessions() == null || sessionList.getSessions().length == 0) {
      return sessions;
    }
    YarnApplicationstate appStates;
    String hdfsUsername = hdfsUserBean.getHdfsUserName(project, user);
    for (LivyMsg.Session s : sessionList.getSessions()) {
//      LOGGER.log(Level.INFO, "Found Livy session: {0}", s.getAppId());
      if (hdfsUsername != null && hdfsUsername.equals(s.getProxyUser())) {
        appStates = appStateBean.findByAppId(s.getAppId());
        if (!jupyter && (appStates == null || appStates.getAppname().equals(JUPYTER_SESSION_NAME))) {
          continue;
        }
        if (jupyter && (appStates == null || !appStates.getAppname().equals(JUPYTER_SESSION_NAME))) {
          continue;
        }
        s.setOwner(user.getEmail());
        sessions.add(s);
      }
    }
    return sessions;
  }

  /**
   * Get livy session by id
   *
   * @param sessionId
   * @return
   */
  public LivyMsg.Session getLivySession(int sessionId) {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions/" + sessionId);
    LivyMsg.Session session = null;
    try {
      session = target.request().get(LivyMsg.Session.class);
    } catch (NotFoundException e) {
      return null;
    } finally {
      client.close();
    }
    return session;
  }

  /**
   * Get all livy sessions
   *
   * @return
   */
  public LivyMsg getLivySessions() {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions");
    LivyMsg livySession = null;
    try {
      livySession = target.request().get(LivyMsg.class);
    } finally {
      client.close();
    }
    return livySession;
  }

  /**
   * Delete livy session with given id
   *
   * @param sessionId
   * @return
   */
  public int deleteLivySession(int sessionId) {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions/" + sessionId);
    Response res;
    try {
      res = target.request().delete();
    } catch (NotFoundException e) {
      return Response.Status.NOT_FOUND.getStatusCode();
    } finally {
      client.close();
    }
    return res.getStatus();
  }

  /**
   * Delete all Zeppelin livy sessions
   *
   * @param hdfsUser
   */
  public void deleteAllZeppelinLivySessions(String hdfsUser) {
    deleteAllLivySessions(hdfsUser, false);
  }

  /**
   * Delete all Jupyter livy sessions
   *
   * @param hdfsUser
   */
  public void deleteAllJupyterLivySessions(String hdfsUser) {
    deleteAllLivySessions(hdfsUser, true);
  }

  private void deleteAllLivySessions(String hdfsUser, boolean jupyter) {
    String username = hdfsUsersController.getUserName(hdfsUser);
    String projectname = hdfsUsersController.getProjectName(hdfsUser);
    Users user = userFacade.findByUsername(username);
    Project project = projectFacade.findByName(projectname);
    List<LivyMsg.Session> sessions;
    if (jupyter) {
      sessions = getJupyterLivySessionsForProjectUser(project, user);
    } else {
      sessions = getZeppelinLivySessionsForProjectUser(project, user);
    }
    for (LivyMsg.Session session : sessions) {
      deleteLivySession(session.getId());
    }
  }

  /**
   * Check if livy session with the given id exists.
   *
   * @param sessionId
   * @return
   */
  public boolean isLivySessionAlive(int sessionId) {
    LivyMsg.Session session = getLivySession(sessionId);
    return session != null;
  }
}
