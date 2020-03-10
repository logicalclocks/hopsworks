/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.jobs.flink;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.util.Settings;
import io.hops.security.UserNotFoundException;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FlinkCompletedJobsCache {
  
  private static final Logger LOGGER = Logger.getLogger(FlinkCompletedJobsCache.class.getName());
  
  //<job,project>
  private LoadingCache<String, Project> cache;
  
  @EJB
  private FlinkController flinkController;
  @EJB
  private Settings settings;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private UserFacade userFacade;
  
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {

    try {
      String archivePath = flinkController.getArchiveDir();
      cache = CacheBuilder.newBuilder().maximumSize(1000000).build(new CacheLoader<String, Project>() {
        @Override
        public Project load(String job) throws UserNotFoundException {
          LOGGER.log(Level.FINE, "Fetching Flink job project for job:" + job);
          return flinkController.getProjectOfFlinkJob(archivePath, job);
        }
      });

      //Fetch
      Map<String, Project> jobsProjects = flinkController.getProjectsOfFlinkJobs(archivePath);
      for (String job : jobsProjects.keySet()) {
        LOGGER.log(Level.FINER, "Filling cache with job:" + job + ", project:" + jobsProjects.get(job));
        cache.put(job, jobsProjects.get(job));
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not access " + settings.getFlinkConfFile(), e);
    }
    
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean hasAccessToFlinkJob(String job, String user) {
    return hasAccessToFlinkJob(job, userFacade.findByEmail(user));
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean hasAccessToFlinkJob(String job, Users user) {
    try {
      return projectTeamFacade.isUserMemberOfProject(cache.getUnchecked(job), user);
    } catch (Exception e) {
      LOGGER.log(Level.FINE, e.getMessage(), e);
      return false;
    }
  }
}
