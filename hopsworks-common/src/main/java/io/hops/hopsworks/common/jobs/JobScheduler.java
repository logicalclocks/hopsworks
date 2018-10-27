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

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO.TimeUnit;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allow jobs to be scheduled and take care of their execution.
 */
@Stateless
public class JobScheduler {

  private static final Logger logger = Logger.getLogger(JobScheduler.class.
          getName());

  @EJB
  private JobFacade jobFacade;
  @EJB
  private ExecutionController executionController;
  @Resource
  private TimerService timerService;

  /**
   * Execute the job given as extra info to the timer object.
   * <p/>
   * @param timer
   */
  @Timeout
  public void timeout(Timer timer) throws GenericException, JobException {
    Serializable jobId = timer.getInfo();
    //Valid id?
    if (!(jobId instanceof Integer)) {
      logger.log(Level.WARNING,
              "Trying to run a scheduled execution, but info is not of integer class.");
      return;
    }
    //Valid job?
    Jobs job = jobFacade.findById((Integer) jobId);
    if (job == null) {
      logger.log(Level.WARNING, "Trying to run a job with non-existing id, canceling timer.");
      timer.cancel();
      return;
    }
    //Yes! Now execute!
    executionController.start(job, job.getCreator());
  }

  /**
   * Schedule a job for periodic execution. The first execution will occur after
   * the specified interval.
   * <p/>
   * @param job
   * @param numberOfUnits
   * @param timeUnit
   */
  public void scheduleJobPeriodic(Jobs job, int numberOfUnits,
          TimeUnit timeUnit) {
    long interval = numberOfUnits * timeUnit.getDuration();
    timerService.createTimer(new Date().getTime() + interval, interval, job.
            getId());
  }

  /**
   * Schedule a job for a single execution, at the given date.
   * <p/>
   * @param job
   * @param when
   */
  public void scheduleJobOnce(Jobs job, Date when) {
    TimerConfig config = new TimerConfig();
    config.setInfo(job.getId());
    timerService.createSingleActionTimer(when, config);
  }

  /**
   * Schedule a job for regular execution.
   * <p/>
   * @param job
   * @param when The ScheduleExpression dictating when the job should be run.
   */
  public void scheduleJobOnCalendar(Jobs job, ScheduleExpression when) {
    TimerConfig config = new TimerConfig();
    config.setInfo(job.getId());
    timerService.createCalendarTimer(when, config);
  }

  /**
   * Schedule the given job according to the JobSchedule contained in its
   * configuration.
   * <p/>
   * @param job
   * @throws NullPointerException If the job or its contained schedule are null.
   */
  public void scheduleJobPeriodic(Jobs job) {
    //First: parameter checking
    if (job == null) {
      throw new NullPointerException("Cannot schedule null job.");
    } else if (job.getJobConfig().getSchedule() == null) {
      throw new NullPointerException(
              "Trying to schedule a job with null schedule: " + job);
    }
    //Then: set up interval timer
    ScheduleDTO schedule = job.getJobConfig().getSchedule();
    timerService.createTimer(new Date(schedule.getStart()), schedule.getNumber()
            * schedule.getUnit().getDuration(), job.getId());
  }
  /**
   * Unschedule the given job.
   * <p/>
   * @param job
   */
  public boolean unscheduleJob(Jobs job) {
    Collection<Timer> timers = timerService.getTimers();
    for(Timer timer: timers) {
      int jobId = (int)timer.getInfo();
      if(jobId == job.getId()) {
        timer.cancel();
        return true;
      }
    }
    return false;
  }

}
