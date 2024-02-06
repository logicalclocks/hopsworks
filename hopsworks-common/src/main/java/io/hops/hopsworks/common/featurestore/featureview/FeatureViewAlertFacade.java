/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.user.activity.Activity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.Set;

@Stateless
public class FeatureViewAlertFacade extends AbstractFacade<FeatureViewAlert> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureViewAlertFacade() {
    super(FeatureViewAlert.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public FeatureViewAlert findByFeatureViewAndStatus(
    FeatureView featureView, FeatureStoreAlertStatus featureStoreAlertStatus) {
    TypedQuery<FeatureViewAlert> query =
      em.createNamedQuery("FeatureViewAlert.findByFeatureViewAndStatus", FeatureViewAlert.class);
    query.setParameter("featureView", featureView)
      .setParameter("status", featureStoreAlertStatus);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  
  
  public CollectionInfo findFeatureViewAlerts(Integer offset, Integer limit,
    Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort, FeatureView featureView) {
    String queryStr = buildQuery("SELECT a FROM FeatureViewAlert a ", filter, sort,
      "a.featureView = :featureView ");
    String queryCountStr =
      buildQuery("SELECT COUNT(a.id) FROM FeatureViewAlert a ", filter, sort,
        "a.featureView = :featureView ");
    Query query =
      em.createQuery(queryStr, Activity.class).setParameter("featureView", featureView);
    Query queryCount =
      em.createQuery(queryCountStr, Activity.class).setParameter("featureView", featureView);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  public FeatureViewAlert findByFeatureViewAndId(FeatureView featureview, Integer id) {
    TypedQuery<FeatureViewAlert> query =
      em.createNamedQuery("FeatureViewAlert.findByFeatureViewAndId", FeatureViewAlert.class);
    query.setParameter("featureView", featureview)
      .setParameter("id", id);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  private CollectionInfo findAll(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter, Query query, Query queryCount) {
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
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
    switch (FeatureViewAlertFacade.Filters.valueOf(filterBy.getValue())) {
      case TYPE:
        q.setParameter(filterBy.getField(), getEnumValues(filterBy, AlertType.class));
        break;
      case STATUS:
        q.setParameter(filterBy.getField(), getEnumValues(filterBy, FeatureStoreAlertStatus.class));
        break;
      case SEVERITY:
        q.setParameter(filterBy.getField(), getEnumValues(filterBy, AlertSeverity.class));
        break;
      case CREATED:
      case CREATED_GT:
      case CREATED_LT:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date);
        break;
      default:
        break;
    }
  }
  
  
  
  public enum Filters {
    TYPE("TYPE", "a.alertType IN :alertType ", "alertType", AlertType.PROJECT_ALERT.toString()),
    STATUS("STATUS", "a.status IN :status ", "status", FeatureStoreAlertStatus.SUCCESS.toString()),
    SEVERITY("SEVERITY", "a.severity IN :severity ", "severity", AlertSeverity.INFO.toString()),
    CREATED("CREATED", "a.created = :created ", "created", ""),
    CREATED_GT("DATE_CREATED_GT", "a.created > :createdFrom ", "createdFrom", ""),
    CREATED_LT("DATE_CREATED_LT", "a.created < :createdTo ", "createdTo", "");
    
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