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

package io.hops.hopsworks.common.models;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.models.Model;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Facade for management of persistent Model objects.
 */
@Stateless
public class ModelFacade extends AbstractFacade<Model> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger LOGGER = Logger.getLogger(ModelFacade.class.getName());

  public ModelFacade() {
    super(Model.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Model put(Project project, String name) {
    //Argument checking
    if (project == null || name == null) {
      throw new IllegalArgumentException("Project and name must be non-null.");
    }
    //First: create a model object
    Model model = new Model();
    model.setName(name);
    model.setProject(project);
    model = em.merge(model);
    em.flush(); //To get the id.
    return model;
  }


  /**
   * Checks if a model with the given name exists in this project.
   *
   * @param project project to search.
   * @param name name of model.
   * @return model if exactly one model with that name was found.
   */
  public Model findByProjectAndName(Project project, String name) {
    TypedQuery<Model> query = em.createNamedQuery("Model.findByProjectAndName", Model.class);
    query.setParameter("name", name).setParameter("project", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Model findByProjectIdAndName(Integer projectId, String name) {
    TypedQuery<Model> query = em.createNamedQuery("Model.findByProjectIdAndName", Model.class);
    query.setParameter("name", name).setParameter("projectId", projectId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public CollectionInfo findByProject(Integer offset, Integer limit,
                                      Set<? extends AbstractFacade.FilterBy> filters,
                                      Set<? extends AbstractFacade.SortBy> sorts, Project project) {

    String queryStr = buildQuery("SELECT m FROM Model m ", filters, sorts, "m.project = :project ");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT m.name, m.project) FROM Model m ", filters, sorts, "m.project = :project ");
    Query query = em.createQuery(queryStr, Model.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, Model.class).setParameter("project", project);
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
      case DATE_CREATED:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date);
        break;
      case NAME:
      case DESCRIPTION:
      case CREATOR:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    ID("ID", "e.id ", "ASC"),
    NAME("NAME", "e.name ", "ASC"),
    DATE_CREATED("DATE_CREATED", "e.created ", "DESC"),
    CREATOR("CREATOR", "LOWER(CONCAT (e.creator.fname, e.creator.lname)) " , "ASC");
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
    DATE_CREATED("DATE_CREATED", "e.created = :created ","created",""),
    NAME("NAME", "e.name LIKE CONCAT('%', :name, '%') ", "name", " "),
    DESCRIPTION("NAME", "e.description LIKE CONCAT('%', :description, '%') ", "description", " "),
    CREATOR("CREATOR", "(e.creator.username LIKE CONCAT('%', :creator, '%') "
      + "OR e.creator.fname LIKE CONCAT('%', :creator, '%') "
      + "OR e.creator.lname LIKE CONCAT('%', :creator, '%')) ", "creator", " ");

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

