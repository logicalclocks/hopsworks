/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.transformationFunction;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * A facade for the hopsfs_transformation_function table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class TransformationFunctionFacade extends AbstractFacade<TransformationFunction> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public TransformationFunctionFacade() {
    super(TransformationFunction.class);
  }

  /**
   * Create and persiste a training dataset builder function
   * @param name
   * @param outputType
   * @param version
   * @param featurestore
   * @return
   */
  public TransformationFunction register(String name, String outputType, Integer version, Featurestore featurestore,
                                         Date created, Users user) {
    TransformationFunction transformationFunction = new TransformationFunction();
    transformationFunction.setFeaturestore(featurestore);
    transformationFunction.setName(name);
    transformationFunction.setOutputType(outputType);
    transformationFunction.setVersion(version);
    transformationFunction.setCreated(created);
    transformationFunction.setCreator(user);
    em.persist(transformationFunction);
    em.flush();
    return transformationFunction;
  }

  /**
   * Retrieves transformation functions by featurestore
   *
   * @param transformationFunctionId
   * @return
   */
  public Optional<TransformationFunction> findById( Integer transformationFunctionId) {
    TypedQuery<TransformationFunction> query = em.createNamedQuery(
        "TransformationFunction.findById", TransformationFunction.class)
        .setParameter("id", transformationFunctionId);

    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }

  }

  /**
   * Retrieves transformation functions by featurestore
   *
   * @param featurestore
   * @param offset
   * @return
   */
  public AbstractFacade.CollectionInfo findByFeaturestore(Integer offset, Integer limit,
                                                Set<? extends AbstractFacade.FilterBy> filters,
                                                Set<? extends AbstractFacade.SortBy> sort, Featurestore featurestore) {

    String queryStr = buildQuery("SELECT tfn FROM TransformationFunction tfn ", filters, sort,
        "tfn.featurestore = :featurestore ");
    String queryCountStr = buildQuery("SELECT COUNT(tfn.id) FROM TransformationFunction tfn ", filters, sort,
        "tfn.featurestore = :featurestore ");

    Query query = em.createQuery(queryStr, TransformationFunction.class)
        .setParameter("featurestore", featurestore);
    Query queryCount = em.createQuery(queryCountStr, TransformationFunction.class)
        .setParameter("featurestore", featurestore);

    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return findAll(offset, limit, filters, query, queryCount);
  }

  /**
   * Retrieves a list of transformation functions (different versions) given their name and feature store from the
   * database ordered by their version number in descending order
   *
   * @param name name of the transformation function
   * @param featurestore featurestore of the transformation function
   * @return a single Transformation function
   */
  public CollectionInfo  findByNameAndFeaturestoreOrderedDescVersion(
      String name, Featurestore featurestore, Integer offset) {
    TypedQuery<TransformationFunction> query =
        em.createNamedQuery("TransformationFunction.findByNameAndFeaturestoreOrderedDescVersion",
            TransformationFunction.class)
            .setParameter("featurestore", featurestore)
            .setParameter("name", name);
    TypedQuery<Long> queryCount = em.createNamedQuery(
        "TransformationFunction.countByFeaturestore", Long.class)
        .setParameter("featurestore", featurestore);

    query.setFirstResult(offset);
    queryCount.setFirstResult(offset);
    return new CollectionInfo(queryCount.getSingleResult(), query.getResultList());
  }

  /**
   * Retrieves a transformation function given its name, version and feature store from the database
   *
   * @param name name of the transformation function
   * @param version version of the transformation function
   * @param featurestore featurestore of the transformation function
   * @return a single transformation function entity
   */

  public Optional<TransformationFunction> findByNameVersionAndFeaturestore(String name, Integer version,
                                                                           Featurestore featurestore) {
    try {
      return Optional.of(
          em.createNamedQuery("TransformationFunction.findByNameVersionAndFeaturestore",
              TransformationFunction.class)
              .setParameter("featurestore", featurestore)
              .setParameter("name", name)
              .setParameter("version", version)
              .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  private AbstractFacade.CollectionInfo findAll(Integer offset, Integer limit,
                                                Set<? extends AbstractFacade.FilterBy> filter,
                                                Query query, Query queryCount) {

    setOffsetAndLim(offset, limit, query);
    return new AbstractFacade.CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }


  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void delete(TransformationFunction transformationFunction) {
    if (transformationFunction != null) {
      em.remove(em.merge(transformationFunction));
    }
  }

  private void setFilter(Set<? extends FilterBy> filters, Query q) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filters) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
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

  public enum Sorts {
    NAME("NAME", " tsfn.name ", "DESC"),
    VERSION("VERSION", " tsfn.version ", "DESC");

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

    public String getSql() {
      return sql;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public enum Filters {
    NAME("NAME", "tsfn.name = :name ", "name", " "),
    VERSION("VERSION", "tsfn.version = :version ", "version", " ");

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
