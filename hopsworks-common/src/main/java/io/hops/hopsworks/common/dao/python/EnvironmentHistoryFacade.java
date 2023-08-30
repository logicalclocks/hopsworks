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
package io.hops.hopsworks.common.dao.python;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.history.EnvironmentDelta;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.NoResultException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import javax.persistence.Query;

@Stateless
public class EnvironmentHistoryFacade extends AbstractFacade<EnvironmentHistoryFacade> {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public EnvironmentHistoryFacade() { super(EnvironmentHistoryFacade.class);}

  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<EnvironmentDelta> getPreviousBuild(Project project) {
    try {
      TypedQuery<EnvironmentDelta> query = em.createNamedQuery(
          "EnvironmentDelta.findAllForProject",
          EnvironmentDelta.class
      );
      query.setParameter("project", project);
      query.setMaxResults(1);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<EnvironmentDelta> getAll(Project project, Set<? extends FilterBy> filters,
                                                 Set<? extends AbstractFacade.SortBy> sorts, Integer limit,
                                                 Integer offset) {
    String historyQueryStr = buildQuery("SELECT e FROM EnvironmentDelta e ", filters, sorts,
        "e.project = :project");
    String countQueryStr = buildQuery("SELECT count(e.id) FROM EnvironmentDelta e ", filters, sorts,
        "e.project = :project");
    Query countQuery = em.createQuery(countQueryStr, EnvironmentDelta.class);
    countQuery.setParameter("project", project);
    Query historyQuery = em.createQuery(historyQueryStr, EnvironmentDelta.class);
    historyQuery.setParameter("project", project);
    setFilter(filters, countQuery);
    setFilter(filters, historyQuery);
    setOffsetAndLim(offset, limit, historyQuery);
    return new CollectionInfo((Long) countQuery.getSingleResult(), historyQuery.getResultList());
  }

  public Optional<EnvironmentDelta> getById(Project project, Integer id) {
    try {
      TypedQuery<EnvironmentDelta> query = em.createNamedQuery(
          "EnvironmentDelta.findById",
          EnvironmentDelta.class
      );
      query.setParameter("project", project);
      query.setParameter("id", id);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public EnvironmentDelta create(EnvironmentDelta diff) {
    em.persist(diff);
    em.flush();
    return diff;
  }

  public void deleteAll(Project project) {
    TypedQuery<EnvironmentDelta> query = em.createNamedQuery(
        "EnvironmentDelta.deleteAllForProject",
        EnvironmentDelta.class
    );
    query.setParameter("project", project);
    query.executeUpdate();
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
    switch (EnvironmentHistoryFacade.Filters.valueOf(filterBy.getValue())) {
      case LIBRARY:
        q.setParameter(filterBy.getField(), filterBy.getParam().toLowerCase());
        break;
      case DATE_FROM:
      case DATE_TO:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date);
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    ID("ID", "e.id ", "DESC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() { return value; }

    public String getSql() { return sql; }

    public String getDefaultParam() { return defaultParam; }

    public String getJoin() { return null; }

    @Override
    public String toString() { return value; }

  }

  public enum Filters {
    LIBRARY("LIBRARY", "(LOWER(e.installed) LIKE CONCAT('%', :library, '%') " +
        "OR LOWER(e.uninstalled) LIKE CONCAT('%', :library, '%') " +
        "OR LOWER(e.upgraded) LIKE CONCAT('%', :library, '%') " +
        "OR LOWER(e.downgraded) LIKE CONCAT('%', :library, '%'))", "library", ""),
    DATE_TO("DATE_TO", "e.created <= :dateTo ", "dateTo", " "),
    DATE_FROM("DATE_FROM", "e.created >= :dateFrom ", "dateFrom", " ");

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

    public String getDefaultParam() { return defaultParam; }

    public String getValue() { return value; }

    public String getSql() { return sql; }

    public String getField() { return field; }

    @Override
    public String toString() { return value; }

  }
}
