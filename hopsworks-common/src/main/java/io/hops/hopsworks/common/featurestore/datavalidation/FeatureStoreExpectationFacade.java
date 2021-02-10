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

package io.hops.hopsworks.common.featurestore.datavalidation;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureStoreExpectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.ValidationRule;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeatureStoreExpectationFacade extends AbstractFacade<FeatureStoreExpectation> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureStoreExpectationFacade() {
    super(FeatureStoreExpectation.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public FeatureStoreExpectation merge(FeatureStoreExpectation featureStoreExpectation) {
    featureStoreExpectation = em.merge(featureStoreExpectation);
    em.flush();
    return featureStoreExpectation;
  }
  
  public FeatureStoreExpectation findById(int id) {
    return em.find(FeatureStoreExpectation.class, id);
  }
  
  public List<FeatureStoreExpectation> findByFeaturestoreAndRule(Featurestore featureStore,
    ValidationRule validationRule) {
    return em.createNamedQuery("FeatureStoreExpectation.findByFeaturestoreAndRule", FeatureStoreExpectation.class)
      .setParameter("featureStore", featureStore)
      .setParameter("validationRules", validationRule)
      .getResultList();
  }
  
  public Optional<FeatureStoreExpectation> findByFeaturestoreAndName(Featurestore featureStore, String name) {
    try {
      TypedQuery<FeatureStoreExpectation> query =
        em.createNamedQuery("FeatureStoreExpectation.findByFeaturestoreAndName", FeatureStoreExpectation.class)
          .setParameter("featureStore", featureStore).setParameter("name", name);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  
  public CollectionInfo findByFeaturestore(Integer offset, Integer limit,
    Set<? extends FilterBy> filters,
    Set<? extends SortBy> sort, Featurestore featurestore) {
    String queryStr = buildQuery("SELECT fse FROM FeatureStoreExpectation fse ", filters, sort,
      "fse.featureStore = :featureStore ");
    String queryCountStr = buildQuery("SELECT COUNT(fse.id) FROM FeatureStoreExpectation fse ", filters, sort,
      "fse.featureStore = :featureStore ");
    Query query = em.createQuery(queryStr, FeatureStoreExpectation.class)
      .setParameter("featureStore", featurestore);
    Query queryCount = em.createQuery(queryCountStr, FeatureStoreExpectation.class)
      .setParameter("featureStore", featurestore);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return findAll(offset, limit, filters, query, queryCount);
  }

  private void setFilter(Set<? extends FilterBy> filters, Query q) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filters) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }
  
  private CollectionInfo findAll(Integer offset, Integer limit,
    Set<? extends FilterBy> filter,
    Query query, Query queryCount) {
    
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public enum Sorts {
    NAME("NAME", "fse.name ", "ASC");

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
    NAME("NAME", "fse.name = :name ", "name", "");

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
