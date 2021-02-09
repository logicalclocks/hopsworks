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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeatureGroupValidationFacade extends AbstractFacade<FeatureGroupValidationFacade> {

  private static final String JPQL_FEATURE_GROUP_COMMIT = "JOIN FETCH fgv.commit c ";

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureGroupValidationFacade() {
    super(FeatureGroupValidationFacade.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public void persist(FeatureGroupValidation featureGroupValidation) {
    em.persist(featureGroupValidation);
  }
  
  public FeatureGroupValidation findById(int id) {
    return em.find(FeatureGroupValidation.class, id);
  }
  
  public Optional<FeatureGroupValidation> findByFeaturegroupAndValidationTime(Featuregroup featureGroup,
    Date validationTime) {
    try {
      return Optional.of(em
        .createNamedQuery("FeatureGroupValidation.findByFeatureGroupAndValidationTime", FeatureGroupValidation.class)
        .setParameter("featureGroup", featureGroup)
        .setParameter("validationTime", validationTime)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public CollectionInfo findByFeatureGroup(Integer offset, Integer limit, Set<? extends FilterBy> filters,
                                           Set<? extends SortBy> sorts,
                                           Featuregroup featureGroup) {
    String join = "";
    if (featureGroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
      if (sorts != null) {
        for (SortBy sort : sorts) {
          if (sort.getValue().contains(Sorts.COMMIT_TIME.getValue())) {
            join = JPQL_FEATURE_GROUP_COMMIT;
            break;
          }
        }
      }
      if (filters != null) {
        for (FilterBy filterBy : filters) {
          if (filterBy.getValue().contains("COMMIT_TIME")) {
            join = JPQL_FEATURE_GROUP_COMMIT;
            break;
          }
        }
      }
    }
    String queryStr = buildQuery("SELECT fgv FROM FeatureGroupValidation fgv " + join, filters, sorts,
      "fgv.featureGroup = :featureGroup ");
    String queryCountStr = buildQuery("SELECT COUNT(fgv.id) FROM FeatureGroupValidation fgv " + join, filters, sorts,
      "fgv.featureGroup = :featureGroup ");
    Query query = em.createQuery(queryStr, FeatureGroupValidation.class)
      .setParameter("featureGroup", featureGroup);
    Query queryCount = em.createQuery(queryCountStr, FeatureGroupValidation.class)
      .setParameter("featureGroup", featureGroup);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  private void setFilter(Set<? extends FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case VALIDATION_TIME_EQ:
      case VALIDATION_TIME_GT:
      case VALIDATION_TIME_LT:
      case COMMIT_TIME_EQ:
      case COMMIT_TIME_LT:
      case COMMIT_TIME_GT:
        if (!Strings.isNullOrEmpty(filterBy.getParam()) && !filterBy.getParam().equalsIgnoreCase("null")) {
          Date date = new Timestamp(Long.parseLong(filterBy.getParam()));
          q.setParameter(filterBy.getField(), date);
        }
        break;
      case STATUS_EQ:
        q.setParameter(filterBy.getField(), FeatureGroupValidation.Status.valueOf(filterBy.getParam()));
      default:
        break;
    }
  }

  public enum Sorts {
    ID("ID", "fgv.id ", "ASC"),
    VALIDATION_TIME("VALIDATION_TIME", "fgv.validationTime", "ASC"),
    COMMIT_TIME("COMMIT_TIME", "c.committedOn","ASC");
    
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
  
  public enum Filters {
    VALIDATION_TIME_EQ("VALIDATION_TIME_EQ", "fgv.validationTime = :validationTime ", "validationTime", " "),
    VALIDATION_TIME_GT("VALIDATION_TIME_GT", "fgv.validationTime > :validationTime ", "validationTime", " "),
    VALIDATION_TIME_LT("VALIDATION_TIME_LT", "fgv.validationTime < :validationTime ", "validationTime", " "),
    COMMIT_TIME_EQ("COMMIT_TIME_EQ", "fgv.commit.committedOn = :commitTime ", "commitTime", " "),
    COMMIT_TIME_LT("COMMIT_TIME_LT", "fgv.commit.committedOn > :commitTime ", "commitTime", " "),
    COMMIT_TIME_GT("COMMIT_TIME_GT", "fgv.commit.committedOn < :commitTime ", "commitTime", " "),
    STATUS_EQ("STATUS_EQ", "fgv.status = :status", "status", "");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    Filters(String value, String sql, String field, String defaultParam) {
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
