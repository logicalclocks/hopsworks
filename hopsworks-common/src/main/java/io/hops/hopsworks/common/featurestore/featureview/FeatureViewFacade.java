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

package io.hops.hopsworks.common.featurestore.featureview;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeatureViewFacade extends AbstractFacade<FeatureView> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeatureViewFacade() {
    super(FeatureView.class);
  }

  @Override
  public List<FeatureView> findAll() {
    TypedQuery<FeatureView> q = em.createNamedQuery("FeatureView.findAll", FeatureView.class);
    return q.getResultList();
  }

  public CollectionInfo findByFeatureStore(Featurestore featurestore, QueryParam queryParam) {
    return new CollectionInfo(countByFeatureStore(featurestore, queryParam),
    getByFeatureStore(featurestore, queryParam));
  }

  /**
   * Retrieves feature views for a particular featurestore
   *
   * @param featurestore the featurestore to query
   * @param queryParam
   * @return feature view entities
   */
  public List<FeatureView> getByFeatureStore(Featurestore featurestore, QueryParam queryParam) {
    Map<String, Object> extraParam = new HashMap<>();
    extraParam.put("featurestore", featurestore);
    String queryStr = buildQuery("SELECT fv FROM FeatureView fv ",
        queryParam != null ? queryParam.getFilters(): null,
        queryParam != null ? queryParam.getSorts(): null,
        "fv.featurestore = :featurestore ");
    Query q = makeQuery(queryStr, queryParam, extraParam);
    return q.getResultList();
  }

  /**
   * Retrieves count of feature view for a particular featurestore
   *
   * @param featurestore the featurestore to query
   * @param queryParam
   * @return number of feature view entities
   */
  public Long countByFeatureStore(Featurestore featurestore, QueryParam queryParam) {
    String queryStr = buildQuery("SELECT count(fv.id) FROM FeatureView fv ",
      queryParam != null ? queryParam.getFilters(): null,
      null,
      "fv.featurestore = :featurestore ");

    TypedQuery<Long> query = em.createQuery(queryStr, Long.class)
        .setParameter("featurestore", featurestore);
    if (queryParam != null) {
      setFilter(queryParam.getFilters(), query);
    }
    return query.getSingleResult();
  }

  public Optional<FeatureView> findByIdAndFeatureStore(Integer id, Featurestore featureStore) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureView.findByIdAndFeaturestore", FeatureView.class)
          .setParameter("id", id)
          .setParameter("featurestore", featureStore)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeatureView> findByNameVersionAndFeaturestore(
        String name, Integer version, Featurestore featureStore) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureView.findByNameVersionAndFeaturestore", FeatureView.class)
          .setParameter("name", name)
          .setParameter("version", version)
          .setParameter("featurestore", featureStore)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Integer findLatestVersion(String name, Featurestore featurestore) {
    TypedQuery<Integer> query = em.createNamedQuery("FeatureView.findMaxVersionByFeaturestoreAndName", Integer.class);
    query.setParameter("name", name);
    query.setParameter("featurestore", featurestore);
    return query.getSingleResult();
  }

  public List<FeatureView> findByFeatureGroup(Integer featureGroupId) {
    return em.createNamedQuery("FeatureView.findByFeatureGroup", FeatureView.class)
      .setParameter("featureGroupId", featureGroupId).getResultList();
  }

  private Query makeQuery(String queryStr, QueryParam queryParam, Map<String, Object> extraParam) {
    Query query = em.createQuery(queryStr, FeatureView.class);
    if (queryParam != null) {
      setFilter(queryParam.getFilters(), query);
      setOffsetAndLim(queryParam.getOffset(), queryParam.getLimit(), query);
    }
    if (extraParam != null) {
      for (Map.Entry<String, Object> item : extraParam.entrySet()) {
        query.setParameter(item.getKey(), item.getValue());
      }
    }
    return query;
  }

  private void setFilter(Set<FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (FeatureViewFacade.Filters.valueOf(filterBy.toString())) {
      case NAME:
      case NAME_LIKE:
        q.setParameter(filterBy.getParam(), filterBy.getValue());
        break;
      case VERSION:
        q.setParameter(filterBy.getParam(), Integer.valueOf(filterBy.getValue()));
        break;
      default:
        break;
    }
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public enum Filters {
    NAME("NAME", String.format("%s.name = :%%s ", FeatureView.TABLE_NAME_ALIAS), "name", ""),
    NAME_LIKE("NAME_LIKE", String.format("%s.name LIKE CONCAT('%%%%', :%%s, '%%%%') ", FeatureView.TABLE_NAME_ALIAS),
        "name", ""),
    VERSION("VERSION", String.format("%s.version = :%%s ", FeatureView.TABLE_NAME_ALIAS), "version", ""),
    LATEST_VERSION("LATEST_VERSION", String.format("%1$s.version = ( " +
        "SELECT MAX(%2$s.version) " +
        "FROM FeatureView %2$s " +
        "WHERE %1$s.name = %2$s.name AND %1$s.featurestore = %2$s.featurestore " +
        ") ", FeatureView.TABLE_NAME_ALIAS, "fv2"), null, null);

    private final String name;
    private final String sql;
    private final String field;
    private final String defaultValue;

    Filters(String name, String sql, String field, String defaultValue) {
      this.name = name;
      this.sql = sql;
      this.field = field;
      this.defaultValue = defaultValue;
    }

    public String getName() {
      return name;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public String getSql() {
      return sql;
    }

    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public enum Sorts {
    ID("ID", FeatureView.TABLE_NAME_ALIAS + ".id ", "ASC"),
    NAME("NAME", FeatureView.TABLE_NAME_ALIAS + ".name ", "ASC"),
    VERSION("VERSION", FeatureView.TABLE_NAME_ALIAS + ".version ", "ASC"),
    CREATION("CREATION", FeatureView.TABLE_NAME_ALIAS + ".created ", "ASC");
    private final String value;
    private final String sql;
    private final String defaultParam;

    Sorts(String value, String sql, String defaultParam) {
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
