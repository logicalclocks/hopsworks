/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
