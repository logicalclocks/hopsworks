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
package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg.Session;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.sql.Date;
import java.util.List;
import java.util.logging.Level;

@Singleton
public class JupyterNotebookCleaner {

  private final static Logger LOGGER = Logger.getLogger(
      JupyterNotebookCleaner.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds

  @EJB
  private LivyController livyService;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUserscontroller;
  @EJB
  private UserFacade usersFacade;

  public JupyterNotebookCleaner() {
  }

  // Run once per hour
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {

    // 1. Get all Running Jupyter Notebook Servers
    List<JupyterProject> servers = jupyterFacade.getAllNotebookServers();

    if (servers != null) {
      // 2. For each running Notebook Server, get the project_user and
      // then get the Livy sessions for that project_user
      for (JupyterProject jp : servers) {
        List<Session> sessions = livyService.getLivySessions(jp.getProjectId(), ProjectServiceEnum.JUPYTER);

        HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
        // 3. If there is an active livy session, update the lastModified column
        if (!sessions.isEmpty()) {
          for (Session s : sessions) {
            String h = s.getProxyUser();
            if (h != null) {
              if (h.compareTo(hdfsUser.getUsername()) == 0) {
                jp.setLastAccessed(new Date(System.currentTimeMillis()));
                jupyterFacade.update(jp);
              }
            }
          }
        }
        // 3a. TODO - Check if there is an active Python kernel for the notebook

        Users user = usersFacade.findByUsername(hdfsUser.getUsername());
        JupyterSettings js = jupyterSettingsFacade.findByProjectUser(jp.getProjectId().getId(), user.getEmail());

        // If notebook hasn't been used in the last X hours, kill it.
        if (jp.getLastAccessed().before(
            new Date(System.currentTimeMillis() - (js.getShutdownLevel() * 60 * 60 * 1000)))) {
          String jupyterHomePath;
          try {
            jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser.getName(), jp);
            jupyterProcessFacade.killServerJupyterUser(hdfsUser.getName(), jupyterHomePath, jp.getPid(), jp.getPort());
          } catch (AppException ex) {
            Logger.getLogger(JupyterNotebookCleaner.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }

    } else {
      LOGGER.info("No Jupyter Notebook Servers running. Sleeping again.");
    }
  }

}
