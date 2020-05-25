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
package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class JupyterNotebookCleaner {

  private static final  Logger LOGGER = Logger.getLogger(
      JupyterNotebookCleaner.class.getName());

  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private UserFacade usersFacade;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private Settings settings;
  @Resource
  private TimerService timerService;

  @PostConstruct
  public void init() {
    String rawInterval = settings.getJupyterShutdownTimerInterval();
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
    intervalValue = intervalTimeunit.toMillis(intervalValue);
    timerService.createIntervalTimer(intervalValue, intervalValue, new TimerConfig("Jupyter Notebook Cleaner", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(Timer timer) {
    try {
      LOGGER.log(Level.INFO, "Running JupyterNotebookCleaner.");
      // 1. Get all Running Jupyter Notebook Servers
      List<JupyterProject> servers = jupyterFacade.getAllNotebookServers();

      if (servers != null && !servers.isEmpty()) {
        Date currentDate = Calendar.getInstance().getTime();
        for (JupyterProject jp : servers) {
          // If the notebook is expired
          if (jp.getExpires().before(currentDate)) {
            try {
              HdfsUsers hdfsUser = hdfsUsersFacade.find(jp.getHdfsUserId());
              Users user = usersFacade.findByUsername(hdfsUser.getUsername());
              LOGGER.log(Level.FINE, "Shutting down expired notebook for hdfs user " + hdfsUser.getName());
              jupyterController.shutdown(jp.getProjectId(), hdfsUser.getName(), user,
                  jp.getSecret(), jp.getCid(), jp.getPort());
            } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Failed to cleanup notebook with port " + jp.getPort(), e);
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Got an exception while cleaning up jupyter notebooks");
    }
  }
}