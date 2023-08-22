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

package io.hops.hopsworks.common.dao.jobs.description;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Stateless
public class JobScheduleV2Facade extends AbstractFacade<JobScheduleV2> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JobScheduleV2Facade() {
    super(JobScheduleV2.class);
  }

  public Optional<JobScheduleV2> getByJobId(Integer jobId) {
    TypedQuery<JobScheduleV2> query = em.createNamedQuery("JobSchedule.getByJobId", JobScheduleV2.class);
    query.setParameter("jobId", jobId);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<JobScheduleV2> getById(Integer id) {
    TypedQuery<JobScheduleV2> query = em.createNamedQuery("JobSchedule.getById", JobScheduleV2.class);
    query.setParameter("id", id);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<JobScheduleV2> getAll() {
    TypedQuery<JobScheduleV2> query = em.createNamedQuery("JobSchedule.getAll", JobScheduleV2.class);
    return query.getResultList();
  }

  public void removeByJobId(Integer jobId) {
    TypedQuery<JobScheduleV2> query = em.createNamedQuery("JobSchedule.getByJobId", JobScheduleV2.class);
    query.setParameter("jobId", jobId);
    for (JobScheduleV2 schedule : query.getResultList()) {
      remove(schedule);
    }
  }

  public List<JobScheduleV2> getActiveWithCurrentExecutionTime(Instant currentDateTime) {
    TypedQuery<JobScheduleV2> query =
      em.createNamedQuery("JobSchedule.getActiveWithCurrentExecutionTime", JobScheduleV2.class);
    query.setParameter("currentDateTime", Date.from(currentDateTime));
    return query.getResultList();
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
}
