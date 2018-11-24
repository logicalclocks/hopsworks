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

package io.hops.hopsworks.common.dao.jobhistory;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.JobInputFile;
import io.hops.hopsworks.common.dao.jobs.JobOutputFile;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade for management of persistent Execution objects.
 */
@Stateless
public class ExecutionFacade extends AbstractFacade<Execution> {

  private static final Logger logger = Logger.getLogger(ExecutionFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public ExecutionFacade() {
    super(Execution.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public Execution findByAppId(String appId) {
    try {
      return em.createNamedQuery("Execution.findByAppId",
              Execution.class).setParameter("appId", appId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public Execution findById(int id) {
    TypedQuery<Execution> q = em.createNamedQuery("Execution.findById", Execution.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Enforces
   * @param id
   * @return
   */
  public Execution findByIdAndJob(int id, Jobs job) {
    TypedQuery<Execution> q = em.createNamedQuery("Execution.findByIdAndJob", Execution.class);
    q.setParameter("id", id).setParameter("job", job);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  
  /**
   * Find all the Execution entries for the given project and type.
   * <p/>
   * @param project
   * @param type
   * @return List of JobHistory objects.
   * @throws IllegalArgumentException If the given JobType is not supported.
   */
  public List<Execution> findByProjectAndType(Project project, JobType type) {
    TypedQuery<Execution> q = em.createNamedQuery(
      "Execution.findByProjectAndType", Execution.class);
    q.setParameter("type", type);
    q.setParameter("project", project);
    return q.getResultList();
  }
  
  public List<Execution> findByJob(Jobs job) {
    TypedQuery<Execution> q = em.createNamedQuery("Execution.findByJob",
      Execution.class);
    q.setParameter("job", job);
    return q.getResultList();
  }

  /**
   * Get all executions that are not in a final state.
   *
   * @return list of executions
   */
  public List<Execution> findNotFinished() {
    return em.createNamedQuery("Execution.findByStates",
      Execution.class).setParameter("states", JobState.getRunningStates()).getResultList();
  }
  
  public List<Execution> findByJob(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort,
    Jobs job) {
    String queryStr = buildQuery("SELECT e FROM Execution e ", filter, sort, "e.job = :job ");
    Query query = em.createQuery(queryStr, Execution.class).setParameter("job", job);
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
    switch (Filters.valueOf(filterBy.getValue())) {
      case STATE:
      case STATE_NEQ:
        Set<JobState> jobTypes = getJobStates(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), jobTypes);
        break;
      case FINALSTATUS:
      case FINALSTATUS_NEQ:
        Set<JobFinalStatus> jobFinalStatuses = getJobFinalStatus(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), jobFinalStatuses);
        break;
      default:
        break;
    }
  }
  
  private Set<JobState> getJobStates(String field, String values) {
    Set<JobState> states = new HashSet<>();
    for (String state : values.split(",")) {
      states.add(JobState.valueOf(state.trim()));
    }
    return states;
  }
  
  private Set<JobFinalStatus> getJobFinalStatus(String field, String values) {
    Set<JobFinalStatus> statuses = new HashSet<>();
    for (String status : values.split(",")) {
      statuses.add(JobFinalStatus.valueOf(status.trim()));
    }
    return statuses;
  }
  
  
  public enum Sorts {
    ID("ID", "e.id ", "ASC"),
    SUBMISSION_TIME("SUBMISSION_TIME", "e.submissionTime ", "DESC"),
    DURATION("DURATION", "(e.execution_stop-e.execution_start) ", "ASC");
  
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
    STATE ("STATE", "e.state IN :states ", "states", ""),
    STATE_NEQ ("STATE_NEQ", "e.state NOT IN :states ", "states", ""),
    FINALSTATUS ("FINALSTATUS", "e.finalStatus IN :finalstatuses ", "finalstatuses", ""),
    FINALSTATUS_NEQ ("FINALSTATUS_NEQ", "e.finalStatus NOT IN :finalstatuses ", "finalstatuses", ""),
    SUBMISSION_TIME("SUBMISSION_TIME", "e.submissionTime = :submissionTime ", "submissionTime", "");
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

  //====================================================================================================================
  // Create update remove
  //====================================================================================================================
  
  public Execution create(Jobs job, Users user, String stdoutPath,
          String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    return create(job, user, JobState.INITIALIZING, stdoutPath, stderrPath,
            input, finalStatus, progress, hdfsUser);
  }

  public Execution create(Jobs job, Users user, JobState state,
          String stdoutPath,
          String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    //Check if state is ok
    if (state == null) {
      state = JobState.INITIALIZING;
    }
    if (finalStatus == null) {
      finalStatus = JobFinalStatus.UNDEFINED;
    }
    //Create new object
    Execution exec = new Execution(state, job, user, stdoutPath, stderrPath,
            input, finalStatus, progress, hdfsUser);
    //And persist it
    em.persist(exec);
    em.flush();
    return exec;
  }

  public Execution updateState(Execution exec, JobState newState) {
    exec = getExecution(exec);
    exec.setState(newState);
    merge(exec);
    return exec;
  }

  public Execution updateFinalStatus(Execution exec, JobFinalStatus finalStatus) {
    exec = getExecution(exec);
    exec.setFinalStatus(finalStatus);
    merge(exec);
    return exec;
  }

  public Execution updateProgress(Execution exec, float progress) {
    exec = getExecution(exec);
    exec.setProgress(progress);
    merge(exec);
    return exec;
  }

  public Execution updateExecutionStart(Execution exec, long executionStart) {
    exec = getExecution(exec);
    exec.setExecutionStart(executionStart);
    merge(exec);
    return exec;
  }

  public Execution updateExecutionStop(Execution exec, long executionStop) {
    exec = getExecution(exec);
    exec.setExecutionStop(executionStop);
    merge(exec);
    return exec;
  }
  
  public Execution updateOutput(Execution exec,
          Collection<JobOutputFile> outputFiles) {
    exec = getExecution(exec);
    exec.setJobOutputFileCollection(outputFiles);
    merge(exec);
    return exec;
  }

  public Execution updateStdOutPath(Execution exec, String stdOutPath) {
    exec = getExecution(exec);
    exec.setStdoutPath(stdOutPath);
    merge(exec);
    return exec;
  }

  public Execution updateStdErrPath(Execution exec, String stdErrPath) {
    exec = getExecution(exec);
    exec.setStderrPath(stdErrPath);
    merge(exec);
    return exec;
  }

  public Execution updateAppId(Execution exec, String appId) {
    exec = getExecution(exec);
    exec.setAppId(appId);
    merge(exec);
    return exec;
  }

  public Execution updateFilesToRemove(Execution exec, List<String> filesToRemove) {
    exec = getExecution(exec);
    exec.setFilesToRemove(filesToRemove);
    merge(exec);
    return exec;
  }
  
  private Execution getExecution(Execution exec){
    //Find the updated execution object
    Execution obj = em.find(Execution.class, exec.getId());
    int count = 0;
    while (obj == null && count < 10) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
      logger.info("Trying to get the Execution Object");
      obj = em.find(Execution.class, exec.getId());
      count++;
    }
    if (obj == null) {
      throw new IllegalStateException(
              "Unable to find Execution object with id " + exec.getId());
    }
    return obj;
  }
  
  private void merge(Execution exec){
    em.merge(exec);
  }

}
