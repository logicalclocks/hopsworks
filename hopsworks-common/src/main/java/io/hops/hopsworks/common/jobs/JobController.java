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
