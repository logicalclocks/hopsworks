/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg.Session;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.sql.Date;
import java.util.List;

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
  private JupyterProcessMgr jupyterProcessFacade;

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
        // 3. If there is an active livy session, update the lastModified column
        if (!sessions.isEmpty()) {
          jp.setLastAccessed(new Date(System.currentTimeMillis()));
          jupyterFacade.update(jp);
        }
        // 3a. TODO - Check if there is an active Python kernel for the notebook

        // If notebook hasn't been used in the last 2 hours, kill it.
        if (jp.getLastAccessed().before(
            new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000)))) {
        }

      }

      List<JupyterProject> notebooks = jupyterProcessFacade.getAllNotebooks();
      for (JupyterProject jp : notebooks) {
        if (!jupyterProcessFacade.pingServerJupyterUser(jp.getPid())) {
//          jupyterProcessFacade.killOrphanedWithPid(jp.getPid());
          int hdfsId = jp.getHdfsUserId();
//          String hdfsUser = hdfsUsersFacade.
//          jupyterProcessFacade.stopCleanly();
        }
      }

    } else {
      LOGGER.info("No Jupyter Notebook Servers running. Sleeping again.");
    }
  }

}
