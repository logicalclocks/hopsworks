/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeaturestoreConnectorFacade extends AbstractFacade<FeaturestoreConnector> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreConnectorFacade() {
    super(FeaturestoreConnector.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<FeaturestoreConnector> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findById", FeaturestoreConnector.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreConnector> findByIdType(Integer id, FeaturestoreConnectorType type) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByIdType", FeaturestoreConnector.class)
          .setParameter("id", id)
          .setParameter("type", type)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<FeaturestoreConnector> findByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("FeaturestoreConnector.findByFeaturestore", FeaturestoreConnector.class)
        .setParameter("featurestore", featurestore)
        .getResultList();
  }

  public Optional<FeaturestoreConnector> findByFeaturestoreId(Featurestore featurestore, Integer id) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByFeaturestoreId", FeaturestoreConnector.class)
          .setParameter("featurestore", featurestore)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreConnector> findByFeaturestoreName(Featurestore featurestore, String name) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByFeaturestoreName",
          FeaturestoreConnector.class)
          .setParameter("featurestore", featurestore)
          .setParameter("name", name)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<FeaturestoreConnector> findByType(Featurestore featurestore, Set<FeaturestoreConnectorType> types,
        QueryParam queryParam) {
    String queryStr = buildQuery("SELECT fsConn FROM FeaturestoreConnector fsConn ",
      queryParam != null ? queryParam.getFilters(): null,
      queryParam != null ? queryParam.getSorts(): null,
      "fsConn.featurestore = :featurestore AND fsConn.connectorType IN :types");

    Query query = em.createQuery(queryStr, FeaturestoreConnector.class)
      .setParameter("featurestore", featurestore)
      .setParameter("types", types);
    if (queryParam != null) {
      setFilter(queryParam.getFilters(), query);
      setOffsetAndLim(queryParam.getOffset(), queryParam.getLimit(), query);
    }
    return query.getResultList();
  }

  public void deleteByFeaturestoreName(Featurestore featurestore, String name) {
    findByFeaturestoreName(featurestore, name).ifPresent(this::remove);
  }

  public Long countByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("FeaturestoreConnector.countByFeaturestore", Long.class)
        .setParameter("featurestore", featurestore)
        .getSingleResult();
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filters, Query q) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    for (FilterBy filter : filters) {
      setFilterQuery(filter, q);
    }
  }

  private void setFilterQuery(FilterBy filter, Query q) {
    switch (Filters.valueOf(filter.getValue())) {
      case TYPE:
        q.setParameter(filter.getField(), FeaturestoreConnectorType.valueOf(filter.getParam().toUpperCase()));
        break;
      default:
        break;
    }
  }

  public enum Filters {
    TYPE("TYPE", "fsConn.connectorType = :type", "type", "");

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

  public enum Sorts {
    ID("ID", "fsConn.id", "ASC"),
    NAME("NAME", "fsConn.name", "ASC");
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
