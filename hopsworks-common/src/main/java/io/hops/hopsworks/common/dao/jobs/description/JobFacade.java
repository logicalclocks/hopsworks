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

package io.hops.hopsworks.common.dao.jobs.description;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade for management of persistent Jobs objects.
 */
@Stateless
public class JobFacade extends AbstractFacade<Jobs> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger LOGGER = Logger.getLogger(JobFacade.class.
      getName());

  public JobFacade() {
    super(Jobs.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  /**
   * Create a new Jobs instance.
   * <p/>
   * @param creator The creator of the job.
   * @param project The project in which this job is defined.
   * @param config The job configuration file.
   * @return
   */
  //This seems to ensure that the entity is actually created and can later 
  //be found using em.find().
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Jobs create(Users creator, Project project,
      JobConfiguration config) {
    //Argument checking
    if (creator == null || project == null || config == null) {
      throw new IllegalArgumentException("Owner, project and config must be non-null.");
    }
    //First: create a job object
    Jobs job = new Jobs(config, project, creator, config.
        getAppName());
    //Finally: persist it, getting the assigned id.
    em.persist(job);
    em.flush(); //To get the id.
    return job;
  }
  
  
  /**
   * Checks if a job with the given name exists in this project.
   * @param project project to search.
   * @param name name of job.
   * @return job if exactly one job with that name was found.
   */
  public Jobs findByProjectAndName(Project project, String name) {
    TypedQuery<Jobs> query = em.createNamedQuery("Jobs.findByProjectAndName", Jobs.class);
    query.setParameter("name", name).setParameter("project", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public Jobs findByProjectAndId(Project project, int id) {
    TypedQuery<Jobs> query = em.createNamedQuery("Jobs.findByProjectAndId", Jobs.class);
    query.setParameter("id", id).setParameter("project", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void removeJob(Jobs job) {
    try {
      Jobs managedJob = em.find(Jobs.class, job.getId());
      em.remove(em.merge(managedJob));
      em.flush();
    } catch (SecurityException | IllegalStateException ex) {
      LOGGER.log(Level.SEVERE, "Could not delete job:" + job.getId());
      throw ex;
    }

  }

  public boolean updateJobSchedule(int jobId, ScheduleDTO schedule) {
    boolean status = false;
    try {
      Jobs managedJob = em.find(Jobs.class, jobId);
      JobConfiguration config = managedJob.getJobConfig();
      config.setSchedule(schedule);
      TypedQuery<Jobs> q = em.createNamedQuery("Jobs.updateConfig", Jobs.class);
      q.setParameter("id", jobId);
      q.setParameter("jobconfig", config);
      int result = q.executeUpdate();
      LOGGER.log(Level.INFO, "Updated entity count = {0}", result);
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      LOGGER.log(Level.SEVERE, "Could not update job with id:" + jobId);
      throw ex;
    }
    return status;
  }

  public List<Jobs> getRunningJobs(Project project) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findJobsForExecutionInState", Jobs.class);
    q.setParameter("project", project);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

  public List<Jobs> getRunningJobs(Project project, String hdfsUser) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findUserJobsForExecutionInState", Jobs.class);
    q.setParameter("project", project);
    q.setParameter("hdfsUser", hdfsUser);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

  public List<Jobs> getRunningJobs(Project project, String hdfsUser, List<Integer> jobIds) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findUserJobsIdsForExecutionInState", Jobs.class);
    q.setParameter("jobids", jobIds);
    q.setParameter("project", project);
    q.setParameter("hdfsUser", hdfsUser);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }
  
  //====================================================================================================================
  
  public List<Jobs> findByProject(Project project) {
    TypedQuery<Jobs> q = em.createNamedQuery("Jobs.findByProject", Jobs.class);
    q.setParameter("project", project);
    return q.getResultList();
  }
  
  public List<Jobs> findByProject(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Project project) {
    return findByProjectAndCreator(offset, limit, filter, sort, project, null);
  }
  
  public List<Jobs> findByProjectAndCreator(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Project project, Users creator) {
    String queryStr = buildQuery("SELECT j FROM Jobs j ", filter, sort, "j.project = :project ");
    Query query = em.createQuery(queryStr, Jobs.class).setParameter("project", project);
    if (creator != null) {
      query.setParameter("creator", creator);
    }
    setFilter(filter, query);
    setOffsetAndLim(offset, limit, query);
    return query.getResultList();
  }
  
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    Iterator<? extends AbstractFacade.FilterBy> filterBy = filter.iterator();
    for (;filterBy.hasNext();) {
      setFilterQuery(filterBy.next(), q);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    if (filterBy.equals(FilterBy.TYPE) || filterBy.equals(FilterBy.TYPE_NEQ)) {
      Set<JobType> jobTypes = getJobTypes(filterBy.getField(), filterBy.getParam());
      q.setParameter(filterBy.getField(), jobTypes);
    }
  }
  
  private Set<JobType> getJobTypes(String field, String values) {
    String[] jobTypesArr = values.split(",");
    Set<JobType> jobTypes= new HashSet<>();
    for (String jobTypeStr : jobTypesArr) {
      jobTypes.add(JobType.valueOf(jobTypeStr.trim()));
    }
    return jobTypes;
  }
  
  public enum SortBy implements AbstractFacade.SortBy {
    ID("ID", "j.id ", "ASC"),
    NAME("NAME", "j.name ", "ASC"),
    DATE_CREATED("DATE_CREATED", "j.creationTime ", "DESC"),
    TYPE("TYPE", "j.type ", "ASC");
    
    @JsonCreator
    public static SortBy fromString(String param) {
      String[] sortByParams = param.split(":");
      SortBy sortBy = SortBy.valueOf(sortByParams[0].toUpperCase());
      String order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : sortBy.defaultParam;
      AbstractFacade.OrderBy orderBy = AbstractFacade.OrderBy.valueOf(order);
      sortBy.setParam(orderBy);
      return sortBy;
    }
    
    private final String value;
    private AbstractFacade.OrderBy param;
    private final String sql;
    private final String defaultParam;
    
    private SortBy(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    @Override
    public String getValue() {
      return value;
    }
    
    @Override
    public OrderBy getParam() {
      return param;
    }
    
    public void setParam(OrderBy param) {
      this.param = param;
    }
    
    @Override
    public String getSql() {
      return sql;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum FilterBy implements AbstractFacade.FilterBy {
    TYPE ("TYPE", "j.type IN :types ", "types",
      JobType.SPARK.toString().toUpperCase() + "," + JobType.PYSPARK.toString().toUpperCase()),
    TYPE_NEQ ("TYPE_NEQ", "j.type NOT IN :types ", "types",
      JobType.FLINK.toString().toUpperCase() + "," + JobType.YARN.toString().toUpperCase()
        + "," + JobType.ERASURE_CODING.toString().toUpperCase());
    
    @JsonCreator
    public static FilterBy fromString(String param) {
      String[] filterByParams = param.split(":");
      FilterBy filterBy = FilterBy.valueOf(filterByParams[0].toUpperCase());
      String filter = filterByParams.length > 1 ? filterByParams[1].toUpperCase() : filterBy.defaultParam;
      filterBy.setParam(filter);
      return filterBy;
    }
    
    private final String value;
    private String param;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private FilterBy(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }
    
    @Override
    public String getValue() {
      return value;
    }
    
    @Override
    public String getParam() {
      return param;
    }
    
    public void setParam(String param) {
      this.param = param;
    }
    
    @Override
    public String getSql() {
      return sql;
    }
    
    @Override
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
}
