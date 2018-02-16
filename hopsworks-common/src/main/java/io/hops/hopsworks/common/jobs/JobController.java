/*
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
 *
 */

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;

@Stateless
public class JobController {

  @EJB
  private JobFacade jdFacade;
  @EJB
  private JobScheduler scheduler;

  private static final Logger logger = Logger.getLogger(JobController.class.
          getName());

  public Jobs createJob(Users user, Project project,
          JobConfiguration config) {
    Jobs created = this.jdFacade.create(user, project, config);
    if (config.getSchedule() != null) {
      scheduler.scheduleJobPeriodic(created);
    }
    return created;
  }

  public boolean scheduleJob(int jobId) {
    boolean status = false;
    Jobs job = this.jdFacade.findById(jobId);
    if (job != null) {
      scheduler.scheduleJobPeriodic(job);
      status = true;
    }
    return status;
  }

}
