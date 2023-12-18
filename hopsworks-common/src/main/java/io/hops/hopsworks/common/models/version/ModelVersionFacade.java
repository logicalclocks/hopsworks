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

package io.hops.hopsworks.common.models.version;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Set;

/**
 * Facade for management of persistent Model Version objects.
 */
@Stateless
public class ModelVersionFacade extends AbstractFacade<ModelVersion> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public ModelVersionFacade() {
    super(ModelVersion.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public CollectionInfo findByProject(Integer offset, Integer limit,
                                    Set<? extends AbstractFacade.FilterBy> filters,
                                    Set<? extends AbstractFacade.SortBy> sorts,
                                    Project project) {

    String queryStr = buildQuery(
      "SELECT * FROM hopsworks.`model_version` JOIN `hopsworks`.model ON `hopsworks`.model_version.model_id=model.id ",
      filters, sorts, "`hopsworks`.model.project_id = ?project_id ");

    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT concat(model_version.model_id, model_version.version)) " +
          "FROM hopsworks.`model_version` JOIN `hopsworks`.model ON `hopsworks`.model_version.model_id=model.id ",
        filters, sorts, "`hopsworks`.model.project_id = ?project_id ");

    Query query = em.createNativeQuery(queryStr, ModelVersion.class).setParameter("project_id", project.getId());
    Query queryCount = em.createNativeQuery(queryCountStr).setParameter("project_id", project.getId());
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long)queryCount.getSingleResult(), query.getResultList());
  }

  public ModelVersion findByProjectAndMlId(Integer modelId, Integer version) {
    TypedQuery<ModelVersion> query = em.createNamedQuery("ModelVersion.findByProjectAndMlId", ModelVersion.class);
    query.setParameter("modelId", modelId).setParameter("version", version);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
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
      case NAME_EQ:
      case NAME_LIKE:
      case VERSION:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    NAME("NAME", "`hopsworks`.model.name " , "ASC"),
    METRIC("METRIC", "JSON_VALUE(`metrics`, '$.attributes.METRIC') IS NULL, " +
            "CAST(JSON_VALUE(`metrics`, '$.attributes.METRIC') AS FLOAT) ",
            "ASC"); //sort twice needed to make sure nulls always at the end of sorted items
    private final String value;
    private final String sql;
    private final String defaultParam;

    private String jsonSortKey;

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
      if (this.value.equals(Sorts.METRIC.value)) {
        return sql.replace("METRIC", this.getJsonSortKey());
      } else {
        return sql;
      }
    }

    public String getJoin() {
      return null;
    }

    @Override
    public String toString() {
      return value;
    }

    public String getJsonSortKey() {
      return jsonSortKey;
    }

    public void setJsonSortKey(String jsonSortKey) {
      this.jsonSortKey = jsonSortKey;
    }
  }
  public enum Filters {
    NAME_EQ ("NAME_EQ",
      "`hopsworks`.model.name = ?name",
      "name", ""),
    NAME_LIKE ("NAME_LIKE",
      "`hopsworks`.model.name LIKE CONCAT('%', ?name, '%') ",
      "name", " "),
    VERSION ("VERSION",
      "`hopsworks`.model_version.version = ?version ",
      "version", "");
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

  public ModelVersion put(ModelVersion modelVersion) {
    //Finally: persist it, getting the assigned id.
    modelVersion = em.merge(modelVersion);
    em.flush(); //To get the id.
    return modelVersion;
  }
}
