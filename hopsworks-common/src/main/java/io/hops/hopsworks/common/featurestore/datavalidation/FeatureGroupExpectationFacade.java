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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupExpectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureStoreExpectation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeatureGroupExpectationFacade extends AbstractFacade<FeatureGroupExpectation> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeatureGroupExpectationFacade() {
    super(FeatureGroupExpectation.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FeatureGroupExpectation merge(FeatureGroupExpectation featureGroupExpectation) {
    featureGroupExpectation = em.merge(featureGroupExpectation);
    em.flush();
    return featureGroupExpectation;
  }

  public FeatureGroupExpectation findById(int id) {
    return em.find(FeatureGroupExpectation.class, id);
  }
  
  public Optional<FeatureGroupExpectation> findByFeaturegroupAndExpectation(Featuregroup featuregroup,
    FeatureStoreExpectation expectation) {
    try {
      TypedQuery<FeatureGroupExpectation> query = em
        .createNamedQuery("FeatureGroupExpectation.findByFeatureGroupAndExpectation", FeatureGroupExpectation.class)
        .setParameter("featuregroup", featuregroup)
        .setParameter("featureStoreExpectation", expectation);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo findByFeaturegroup(Integer offset, Integer limit, Set<? extends FilterBy> filter,
                                Set<? extends SortBy> sort, Featuregroup featuregroup) {
    String queryStr = buildQuery("SELECT fge FROM FeatureGroupExpectation fge ", filter, sort,
        "fge.featuregroup = :featuregroup ");
    String queryCountStr = buildQuery("SELECT COUNT(fge.id) FROM FeatureGroupExpectation fge ", filter, sort,
        "fge.featuregroup = :featuregroup ");
    Query query = em.createQuery(queryStr, FeatureGroupExpectation.class)
        .setParameter("featuregroup", featuregroup);
    Query queryCount = em.createQuery(queryCountStr, FeatureGroupExpectation.class)
        .setParameter("featuregroup", featuregroup);
    return findAll(offset, limit, filter, query, queryCount);
  }

  private CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
                                 Query query, Query queryCount) {

    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public enum Sorts {
    NAME("NAME", "fge.featureStoreExpectation.name ", "ASC");

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
    NAME("NAME", "fge.featureStoreExpectation.name = :name ", "name", "");

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
