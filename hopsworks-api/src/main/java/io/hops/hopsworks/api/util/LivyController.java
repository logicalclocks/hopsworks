package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@Stateless
public class LivyController {

  private static final Logger LOGGER = Logger.getLogger(LivyController.class.getName());

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
   * Get Livy sessions for project, depending on service type.
   *
   * @param project
   * @param service
   * @return
   */
  public List<LivyMsg.Session> getLivySessions(Project project, ProjectServiceEnum service) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    LivyMsg sessionList = getLivySessions();
    if (sessionList == null || sessionList.getSessions() == null || sessionList.getSessions().length == 0) {
      return sessions;
    }

    switch (service) {
      case JUPYTER:
        List<ProjectTeam> projectTeam;
        projectTeam = teambean.findMembersByProject(project);
        String hdfsUsername;
        YarnApplicationstate appStates;
        for (ProjectTeam member : projectTeam) {
          hdfsUsername = hdfsUserBean.getHdfsUserName(project, member.getUser());
          for (LivyMsg.Session s : sessionList.getSessions()) {
            if (hdfsUsername != null && hdfsUsername.equals(s.getProxyUser())) {
              appStates = appStateBean.findByAppId(s.getAppId());
              if (appStates == null || !appStates.getAppname().startsWith(JUPYTER_SESSION_NAME)) {
                continue;
              }
              s.setOwner(member.getUser().getEmail());
              sessions.add(s);
            }
          }
        }
        break;
      case ZEPPELIN:
        hdfsUsername = project.getProjectGenericUser();
        for (LivyMsg.Session s : sessionList.getSessions()) {
          if (hdfsUsername.equals(s.getProxyUser())) {
            appStates = appStateBean.findByAppId(s.getAppId());
            if (appStates == null || appStates.getAppname().startsWith(JUPYTER_SESSION_NAME)) {
              continue;
            }
            s.setOwner(hdfsUsername);
            sessions.add(s);
          }
        }
        break;
      default:
        break;
    }
    return sessions;
  }

  /**
   * Get all Jupyter livy sessions for project and user
   *
   * @param project
   * @param user
   * @param service
   * @return
   */
  public List<LivyMsg.Session> getLivySessionsForProjectUser(Project project, Users user, ProjectServiceEnum service) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    LivyMsg sessionList = getLivySessions();
    if (sessionList == null || sessionList.getSessions() == null || sessionList.getSessions().length == 0) {
      return sessions;
    }
    YarnApplicationstate appStates;
    String hdfsUsername = null;
    switch (service) {
      case JUPYTER:
        hdfsUsername = hdfsUserBean.getHdfsUserName(project, user);
        break;
      case ZEPPELIN:
        hdfsUsername = project.getProjectGenericUser();
        break;
      default:
        break;
    }
    for (LivyMsg.Session s : sessionList.getSessions()) {
      if (hdfsUsername != null && hdfsUsername.equals(s.getProxyUser())) {
        appStates = appStateBean.findByAppId(s.getAppId());
        if (service != ProjectServiceEnum.JUPYTER && (appStates == null || appStates.getAppname().startsWith(
            JUPYTER_SESSION_NAME))) {
          continue;
        }
        if (service == ProjectServiceEnum.JUPYTER && (appStates == null || !appStates.getAppname().startsWith(
            JUPYTER_SESSION_NAME))) {
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
      LOGGER.log(Level.WARNING, null, e);
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
   * Delete all Livy sessions.
   *
   * @param hdfsUser
   * @param service
   */
  public void deleteAllLivySessions(String hdfsUser, ProjectServiceEnum service) {
    String username = hdfsUsersController.getUserName(hdfsUser);
    String projectname = hdfsUsersController.getProjectName(hdfsUser);
    Users user = userFacade.findByUsername(username);
    Project project = projectFacade.findByName(projectname);
    List<LivyMsg.Session> sessions;
    sessions = getLivySessionsForProjectUser(project, user, service);
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
