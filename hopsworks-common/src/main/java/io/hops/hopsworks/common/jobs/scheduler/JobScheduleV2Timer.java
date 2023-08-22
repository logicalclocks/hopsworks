/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.jobs.scheduler;

import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.logging.Logger;

@Singleton
@Startup
public class JobScheduleV2Timer {
  private static final Logger LOGGER = Logger.getLogger(JobScheduleV2Timer.class.getName());
  
  @EJB
  private JobScheduleV2Controller jobScheduleController;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;

  @PostConstruct
  public void init() {
    ScheduleExpression schedule = new ScheduleExpression();
    schedule.hour("*");
    schedule.minute("*");
    timer = timerService.createCalendarTimer(schedule, new TimerConfig("Job scheduler timer", false));
  }

  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  @Timeout
  public void schedule() throws JobException, ProjectException, ServiceException, GenericException {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    LOGGER.fine("JobScheduleV2Timer schedule just triggered");
    jobScheduleController.executeWithCron();
  }
}
