/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.alert;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.alert.Alert.Provider;
import io.hops.hopsworks.common.dao.alert.Alert.Severity;

/**
 * See:
 * https://abhirockzz.wordpress.com/2015/02/19/valid-cdi-scoped-for-session-ejb-beans/
 */
@Stateless
public class AlertEJB {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public AlertEJB() {
  }

  public List<Alert> find(Date fromDate, Date toDate) {

    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findAll", Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate);
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Severity severity) {
    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Severity",
            Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("severity", severity.toString());
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Provider provider) {
    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Provider",
            Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("provider", provider.toString());
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Provider provider,
          Severity severity) {
    TypedQuery<Alert> query = em.createNamedQuery(
            "Alerts.findBy-Provider-Severity", Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("provider", provider.toString()).setParameter(
            "severity", severity.toString());
    return query.getResultList();
  }

  public void persistAlert(Alert alert) {
    em.persist(alert);
  }

  public void removeAlert(Alert alert) {
    em.remove(em.merge(alert));
  }

  public void removeAllAlerts() {
    em.createNamedQuery("Alerts.removeAll").executeUpdate();
  }
}
