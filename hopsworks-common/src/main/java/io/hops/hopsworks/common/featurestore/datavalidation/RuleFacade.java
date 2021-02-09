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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Name;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Predicate;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.ValidationRule;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Optional;
import java.util.Set;

@Stateless
public class RuleFacade extends AbstractFacade<ValidationRule> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public RuleFacade() {
    super(ValidationRule.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public ValidationRule findById(int id) {
    return em.find(ValidationRule.class, id);
  }
  
  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort) {
    String queryStr = buildQuery(
      "SELECT vr FROM ValidationRule vr ", filter, sort, null);
    String queryCountStr = buildQuery(
      "SELECT COUNT(vr.id) FROM ValidationRule vr ", filter, sort, null);
    Query query = em.createQuery(queryStr, ValidationRule.class);
    Query queryCount = em.createQuery(queryCountStr, ValidationRule.class);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  private CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Query query, Query queryCount) {
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public CollectionInfo findByName(Integer offset, Integer limit,
    Set<? extends FilterBy> filters,
    Set<? extends SortBy> sorts, Name name) {
    
    String queryStr = buildQuery("SELECT vr FROM ValidationRule vr ", filters, sorts, "vr.name = :name ");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT vr.id) FROM ValidationRule vr ", filters, sorts, "vr.name = :name ");
    Query query = em.createQuery(queryStr, Jobs.class).setParameter("name", name);
    Query queryCount = em.createQuery(queryCountStr, Jobs.class).setParameter("name", name);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public Optional<ValidationRule> findByName(Name name) {
    try {
      return Optional.of(em.createNamedQuery("ValidationRule.findByName", ValidationRule.class)
        .setParameter("name", name)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public ValidationRule findByNameAndPredicate(Name name, Predicate predicate) {
    try {
      return em.createNamedQuery("ValidationRule.findByNameAndPredicate", ValidationRule.class)
        .setParameter("name", name)
        .setParameter("predicate", predicate)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public enum Sorts {
    NAME("NAME", "vr.name ", "ASC");
    
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
