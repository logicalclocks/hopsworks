/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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


package io.hops.hopsworks.common.featurestore.datavalidationv2.results;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the expectation_suite table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class ValidationResultFacade extends AbstractFacade<ValidationResult> {
  private static final Logger LOGGER = Logger.getLogger(ValidationResultFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public ValidationResultFacade() {
    super(ValidationResult.class);
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Transaction to create a new featuregroup in the database
   *
   * @param validationResult
   *   the ValidationResult to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(ValidationResult validationResult) {
    try {
      em.persist(validationResult);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new ValidationResult", cve);
    }
  }

  public Optional<ValidationResult> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("ValidationResult.findById",
        ValidationResult.class).setParameter("id", id).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<ValidationResult> findByExpectation(Integer offset, Integer limit,
    Set<? extends SortBy> sorts,
    Set<? extends FilterBy> filters,
    Expectation expectation) {
    String queryStr = buildQuery("SELECT vr from ValidationResult vr ", filters,
      sorts, "vr.expectation = :expectation");
    String queryCountStr = buildQuery("SELECT COUNT(vr.id) from ValidationResult vr ", filters,
      sorts, "vr.expectation = :expectation");
    Query query = em.createQuery(queryStr, ValidationResult.class)
      .setParameter("expectation", expectation);
    Query queryCount = em.createQuery(queryCountStr, ValidationResult.class)
      .setParameter("expectation", expectation);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    
    return new CollectionInfo<ValidationResult>((Long) queryCount.getSingleResult(), query.getResultList());
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      if ("INGESTION_RESULT_EQ".equals(aFilter.getValue())) {
        switch (aFilter.getParam().toUpperCase()) {
          case "INGESTED":
            q.setParameter(aFilter.getField(), IngestionResult.INGESTED);
            break;
          case "REJECTED":
            q.setParameter(aFilter.getField(), IngestionResult.REJECTED);
            break;
          default:
            break;
        }
      }
      if (aFilter.getValue().contains("VALIDATION_TIME")) {
        q.setParameter(aFilter.getField(), new Timestamp(Long.parseLong(aFilter.getParam())));
      }
    }
  }

  /* The validation time could be parsed from the json of the result 
  but for now the created field populated on result upload serves as 
  a proxy */
  public enum Sorts {
    VALIDATION_TIME("VALIDATION_TIME", "vr.validationTime ", "DESC");

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
    VALIDATION_TIME_GT("VALIDATION_TIME_GT", "vr.validationTime > :validationTime ", "validationTime", ""),
    VALIDATION_TIME_LT("VALIDATION_TIME_LT", "vr.validationTime < :validationTime ", "validationTime", ""),
    VALIDATION_TIME_GTE("VALIDATION_TIME_GTE", "vr.validationTime >= :validationTime ", "validationTime", ""),
    VALIDATION_TIME_LTE("VALIDATION_TIME_LTE", "vr.validationTime <= :validationTime ", "validationTime", ""),
    VALIDATION_TIME_EQ("VALIDATION_TIME_EQ", "vr.validationTime = :validationTime ", "validationTime", ""),
    INGESTION_RESULT_EQ("INGESTION_RESULT_EQ", "vr.ingestionResult = :ingestionResult ", "ingestionResult", "");

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