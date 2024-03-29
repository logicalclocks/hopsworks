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

package io.hops.hopsworks.common.livy;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.persistence.entity.jobs.history.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.servicediscovery.HopsworksService;

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

  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private YarnApplicationstateFacade appStateBean;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  /**
   * Get all Jupyter livy sessions for project and user
   *
   * @param project
   * @param user
   * @return
   */
  public List<LivyMsg.Session> getLivySessionsForProjectUser(Project project, Users user) {
    List<LivyMsg.Session> sessions = new ArrayList<>();
    LivyMsg sessionList = getLivySessions();
    if (sessionList == null || sessionList.getSessions() == null || sessionList.getSessions().length == 0) {
      return sessions;
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    for (LivyMsg.Session s : sessionList.getSessions()) {
      if (hdfsUsername.equals(s.getProxyUser())) {
        YarnApplicationstate appStates = appStateBean.findByAppId(s.getAppId());
        if (appStates == null) {
          continue;
        }
        s.setOwner(user.getEmail());
        sessions.add(s);
      }
    }
    return sessions;
  }


  /**
   * Get all livy sessions
   *
   * @return
   */
  public LivyMsg getLivySessions() {
    LivyMsg livySession = null;
    Client client = ClientBuilder.newClient();
    try {
      WebTarget target = client.target(getLivyURL()).path("/sessions");
      livySession = target.request().get(LivyMsg.class);
    } catch (ServiceDiscoveryException ex) {
      LOGGER.log(Level.WARNING, null, ex);
      return null;
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
    Client client = ClientBuilder.newClient();
    Response res;
    try {
      WebTarget target = client.target(getLivyURL()).path("/sessions/" + sessionId);
      res = target.request().delete();
    } catch (ServiceDiscoveryException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    } catch (NotFoundException e) {
      return Response.Status.NOT_FOUND.getStatusCode();
    } finally {
      client.close();
    }
    return res.getStatus();
  }

  public void deleteAllLivySessions(Project project, Users user) {
    for (LivyMsg.Session session : getLivySessionsForProjectUser(project, user)) {
      deleteLivySession(session.getId());
    }
  }

  private String getLivyURL() throws ServiceDiscoveryException {
    Service livy = serviceDiscoveryController
        .getAnyAddressOfServiceWithDNS(HopsworksService.LIVY.getName());
    return "http://" + livy.getAddress() + ":" + livy.getPort();
  }
}
