/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.project.jobconfig;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DefaultJobConfigurationPK;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
public class DefaultJobConfigurationFacade extends AbstractFacade<DefaultJobConfiguration> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DefaultJobConfigurationFacade() {
    super(DefaultJobConfiguration.class);
  }

  @Override
  public List<DefaultJobConfiguration> findAll() {
    TypedQuery<DefaultJobConfiguration> query = em.createNamedQuery("Project.findAll", DefaultJobConfiguration.class);
    return query.getResultList();
  }

  public DefaultJobConfiguration createOrUpdateDefaultJobConfig(Project project, JobConfiguration jobConfiguration,
                                                     JobType jobType, DefaultJobConfiguration currentConfig) {

    if(jobConfiguration instanceof SparkJobConfiguration) {
      ((SparkJobConfiguration) jobConfiguration).setMainClass(Settings.SPARK_PY_MAINCLASS);
      jobType = JobType.PYSPARK;
    }

    //create
    if(currentConfig == null) {
      currentConfig = new DefaultJobConfiguration();
      DefaultJobConfigurationPK pk = new DefaultJobConfigurationPK();

      pk.setProjectId(project.getId());
      pk.setType(jobType);

      currentConfig.setDefaultJobConfigurationPK(pk);
      currentConfig.setJobConfig(jobConfiguration);

      project.getDefaultJobConfigurationCollection().add(currentConfig);

    //update
    } else {
      currentConfig.setJobConfig(jobConfiguration);
    }

    em.merge(project);

    return currentConfig;
  }

  public void removeDefaultJobConfig(Project project, JobType type) {
    Collection<DefaultJobConfiguration> defaultJobConfigurationCollection =
      project.getDefaultJobConfigurationCollection();

    if(type.equals(JobType.PYSPARK) || type.equals(JobType.SPARK)) {
      defaultJobConfigurationCollection.removeIf(conf -> conf.getJobConfig().getJobType().equals(JobType.PYSPARK));
    } else {
      defaultJobConfigurationCollection.removeIf(conf -> conf.getJobConfig().getJobType().equals(type));
    }

    em.merge(project);
  }

  public CollectionInfo findByProject(Integer offset, Integer limit,
                                      Set<? extends AbstractFacade.FilterBy> filter,
                                      Set<? extends AbstractFacade.SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT c FROM DefaultJobConfiguration c ",
      filter, sort, "c.project = :project");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT c.defaultJobConfigurationPK.type) FROM DefaultJobConfiguration c ",
        filter, sort, "c.project = :project ");
    Query query = em.createQuery(queryStr, DefaultJobConfiguration.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, DefaultJobConfiguration.class).setParameter("project", project);
    return findAll(offset, limit, filter, query, queryCount);
  }

  private CollectionInfo findAll(Integer offset, Integer limit,
                                 Set<? extends AbstractFacade.FilterBy> filter,
                                 Query query, Query queryCount) {
    setFilter(filter, query);
    setFilter(filter, queryCount);
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
    switch (DefaultJobConfigurationFacade.Filters.valueOf(filterBy.getValue())) {
      case JOBTYPE:
        Set<JobType> jobTypes = new HashSet<>(JobFacade.getJobTypes(filterBy.getField(), filterBy.getParam()));
        q.setParameter(filterBy.getField(), jobTypes);
        break;
      default:
        break;
    }
  }

  public enum Filters {
    JOBTYPE("JOBTYPE", "c.defaultJobConfigurationPK.type IN :types ", "types","");

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

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getValue() {
      return value;
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

  public enum Sorts {
    JOBTYPE("JOBTYPE", "c.defaultJobConfigurationPK.type ", "ASC");
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

}
