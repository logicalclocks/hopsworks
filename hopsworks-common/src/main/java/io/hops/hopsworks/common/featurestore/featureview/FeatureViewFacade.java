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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public List<FeatureView> findByFeaturestore(Featurestore featurestore, QueryParam queryParam) {
    Boolean latestVersion = false;
    if (queryParam != null && queryParam.getFilters().removeIf(filter -> filter.toString().equals("LATEST_VERSION"))) {
      latestVersion = true;
    }

    Map<String, Object> extraParam = new HashMap<>();
    extraParam.put("featurestore", featurestore);
    String queryStr = buildQuery("SELECT fv FROM FeatureView fv ",
        queryParam != null ? queryParam.getFilters(): null,
        queryParam != null ? queryParam.getSorts(): null,
        "fv.featurestore = :featurestore ");
    Query q = makeQuery(queryStr, queryParam, extraParam);
    List<FeatureView> results = q.getResultList();

    if (latestVersion) {
      results = retainLatestVersion(results);
    }
    return results;
  }

  public Long countByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("FeatureView.countByFeaturestore", Long.class)
      .setParameter("featurestore", featurestore)
      .getSingleResult();
  }

  List<FeatureView> retainLatestVersion(List<FeatureView> featureViews) {
    Map<String, FeatureView> latestVersion = new HashMap<>();
    for (FeatureView featureView : featureViews) {
      if (!latestVersion.containsKey(featureView.getName())) {
        latestVersion.put(featureView.getName(), featureView);
      } else if (latestVersion.containsKey(featureView.getName()) &&
          featureView.getVersion() > latestVersion.get(featureView.getName()).getVersion()) {
        latestVersion.put(featureView.getName(), featureView);
      }
    }
    return new ArrayList<>(latestVersion.values());
  }

  public List<FeatureView> findByNameAndFeaturestore(String name, Featurestore featurestore) {
    QueryParam queryParam = new QueryParam();
    return findByNameAndFeaturestore(name, featurestore, queryParam);
  }

  public List<FeatureView> findByNameAndFeaturestore(String name, Featurestore featurestore,
      QueryParam queryParam) {
    Set<FilterBy> filters = queryParam.getFilters();
    // Use different parameter name so do not conflict with the default one.
    filters.add(new FeatureViewFilterBy("name", name, "name1"));
    return findByFeaturestore(featurestore, queryParam);
  }

  public List<FeatureView> findByNameVersionAndFeaturestore(String name, Integer version,  Featurestore featurestore) {
    QueryParam queryParam = new QueryParam();
    return findByNameVersionAndFeaturestore(name, version, featurestore, queryParam);
  }

  public List<FeatureView> findByNameVersionAndFeaturestore(String name, Integer version,
      Featurestore featurestore, QueryParam queryParam) {
    Set<FilterBy> filters = queryParam.getFilters();
    // Use different parameter name so do not conflict with the default one.
    filters.add(new FeatureViewFilterBy("name", name, "name1"));
    filters.add(new FeatureViewFilterBy("version", version.toString(), "version1"));
    return findByFeaturestore(featurestore, queryParam);
  }

  public Integer findLatestVersion(String name, Featurestore featurestore) {
    TypedQuery<FeatureView> query = em.createNamedQuery("FeatureView.findByFeaturestoreAndNameOrderedByDescVersion",
        FeatureView.class);
    query.setParameter("name", name);
    query.setParameter("featurestore", featurestore);
    List<FeatureView> results = query.getResultList();
    if (results != null && !results.isEmpty()) {
      return results.get(0).getVersion();
    } else {
      return null;
    }
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
    VERSION("VERSION", String.format("%s.version = :%%s ", FeatureView.TABLE_NAME_ALIAS), "version", ""),
    LATEST_VERSION("LATEST_VERSION", null, null, null);

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
    NAME("ID", FeatureView.TABLE_NAME_ALIAS + ".name ", "ASC"),
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
