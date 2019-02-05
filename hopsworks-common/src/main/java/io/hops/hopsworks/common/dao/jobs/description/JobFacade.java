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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.InvalidQueryException;
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
import java.util.Date;
import java.util.HashSet;
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
  
  private static final String JPQL_EXECUTIONS = "LEFT JOIN FETCH j.executions e on e.id = " +
    "(select max(e.id) from Execution e where e.job = j group by e.job) ";
  
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
   *
   * @param creator The creator of the job.
   * @param project The project in which this job is defined.
   * @param config The job configuration file.
   * @return Jobs The created Jobs entity instance.
   */
  //This seems to ensure that the entity is actually created and can later 
  //be found using em.find().
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Jobs put(Users creator, Project project,
    JobConfiguration config, Jobs job) {
    //Argument checking
    if (creator == null || project == null || config == null) {
      throw new IllegalArgumentException("Owner, project and config must be non-null.");
    }
    if(job == null) {
      //First: create a job object
      job = new Jobs(config, project, creator, config.getAppName());
    } else {
      job.setJobConfig(config);
    }
    //Finally: persist it, getting the assigned id.
    job = em.merge(job);
    em.flush(); //To get the id.
    return job;
  }
  
  
  /**
   * Checks if a job with the given name exists in this project.
   *
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
  
  public CollectionInfo findByProject(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filters,
    Set<? extends AbstractFacade.SortBy> sorts, Project project) {
    //If filter or sort are on subresource, set inner join
    String join = "";
    if(sorts != null) {
      for (SortBy sort : sorts) {
        if(sort.getValue().equals(Sorts.FINALSTATUS.getValue())
          || sort.getValue().equals(Sorts.PROGRESS.getValue())
          || sort.getValue().equals(Sorts.STATE.getValue())
          || sort.getValue().equals(Sorts.SUBMISSIONTIME.getValue())
          || sort.getValue().equals(Sorts.DURATION.getValue())){
          join = JPQL_EXECUTIONS;
          break;
        }
      }
    }
    if(filters != null) {
      for (FilterBy filterBy : filters) {
        if(filterBy.getValue().equals(Filters.LATEST_EXECUTION.getValue())){
          join = JPQL_EXECUTIONS;
          break;
        }
      }
    }
    
    String queryStr = buildQuery("SELECT j FROM Jobs j " + join, filters, sorts, "j.project = :project ");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT j.id) FROM Jobs j " + join, filters, sorts, "j.project = :project ");
    Query query = em.createQuery(queryStr, Jobs.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, Jobs.class).setParameter("project", project);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case JOBTYPE:
      case JOBTYPE_NEQ:
        Set<JobType> jobTypes = new HashSet<>(getJobTypes(filterBy.getField(), filterBy.getParam()));
        q.setParameter(filterBy.getField(), jobTypes);
        break;
      case DATE_CREATED:
      case DATE_CREATED_GT:
      case DATE_CREATED_LT:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date);
        break;
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case CREATOR:
      case LATEST_EXECUTION:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        q.setParameter("searchUpper", filterBy.getParam().toUpperCase());
        break;
      default:
        break;
    }
  }
  
  private Set<JobType> getJobTypes(String field, String values) {
    String[] jobTypesArr = values.split(",");
    Set<JobType> jobTypes = new HashSet<>();
    for (String jobType : jobTypesArr) {
      try {
        jobTypes.add(JobType.valueOf(jobType.trim().toUpperCase()));
      } catch (IllegalArgumentException ie) {
        throw new InvalidQueryException("Filter value for " + field + " needs to set a valid " + field + ", but found: "
          + jobType);
      }
    }
    if (jobTypes.isEmpty()) {
      throw new InvalidQueryException(
        "Filter value for " + field + " needs to set valid job types, but found: " + values);
    }
    return jobTypes;
  }
  
  public enum Sorts {
    ID("ID", "j.id ", "ASC"),
    NAME("NAME", "j.name ", "ASC"),
    DATE_CREATED("DATE_CREATED", "j.creationTime ", "DESC"),
    JOBTYPE("JOBTYPE", "j.type ", "ASC"),
    CREATOR("CREATOR", "LOWER(CONCAT (j.creator.fname, j.creator.lname)) " , "ASC"),
    CREATOR_FIRST_NAME("CREATOR_FIRSTNAME", "j.creator.fname " , "ASC"),
    CREATOR_LAST_NAME("CREATOR_LASTNAME", "j.creator.lname " , "ASC"),
    //Execution related, to make it easier for clients to use pagination
    STATE("STATE", "e.state ", "ASC"),
    FINALSTATUS("FINALSTATUS", "e.finalStatus ", "ASC"),
    PROGRESS("PROGRESS", "e.progress ", "ASC"),
    SUBMISSIONTIME("SUBMISSIONTIME", "e.submissionTime ", "DESC"),
    DURATION("DURATION", " e.executionStop-e.executionStart ", "ASC");
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getSql() {
      return sql;
    }
  
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    JOBTYPE("JOBTYPE", "j.type IN :types ", "types",
      JobType.SPARK.toString().toUpperCase() + "," + JobType.PYSPARK.toString().toUpperCase()),
    JOBTYPE_NEQ("JOBTYPE_NEQ", "j.type NOT IN :types_neq ", "types_neq",
      JobType.FLINK.toString().toUpperCase() + "," + JobType.YARN.toString().toUpperCase()
        + "," + JobType.ERASURE_CODING.toString().toUpperCase()),
    DATE_CREATED("DATE_CREATED", "j.creationTime = :creationTime ","creationTime",""),
    DATE_CREATED_GT("DATE_CREATED_GT", "j.creationTime > :creationTimeFrom ","creationTimeFrom",""),
    DATE_CREATED_LT("DATE_CREATED_LT", "j.creationTime < :creationTimeTo ","creationTimeTo",""),
    NAME("NAME", "j.name LIKE CONCAT(:name, '%') ", "name", " "),
    CREATOR("CREATOR", "(j.creator.username LIKE CONCAT(:user, '%') "
      + "OR j.creator.fname LIKE CONCAT(:user, '%') "
      + "OR j.creator.lname LIKE CONCAT(:user, '%') ", "user", " "),
    LATEST_EXECUTION("LATEST_EXECUTION", "(j.creator.fname LIKE CONCAT(:search, '%') "
      + "OR j.creator.lname LIKE CONCAT(:search, '%') "
      + "OR j.name LIKE CONCAT(:search, '%') "
      + "OR j.type LIKE CONCAT(:searchUpper, '%') "
      + "OR e.state LIKE CONCAT(:searchUpper, '%') "
      + "OR e.finalStatus LIKE CONCAT(:searchUpper, '%')) ", "search", " ");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
  
  
}
