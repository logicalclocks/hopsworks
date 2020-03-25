/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.persistence.entity.host.ServiceStatus;
import io.hops.hopsworks.persistence.entity.kagent.HostServices;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.exceptions.InvalidQueryException;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class HostServicesFacade extends AbstractFacade<HostServices> {

  private static final Logger LOGGER = Logger.getLogger(HostServicesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public HostServicesFacade() {
    super(HostServices.class);
  }
  
  public Optional<HostServices> findByHostnameServiceNameGroup(String hostname, String group, String name) {
    try {
      return Optional.of(em.createNamedQuery("HostServices.findByHostnameServiceNameGroup",
        HostServices.class)
        .setParameter("hostname", hostname)
        .setParameter("group", group)
        .setParameter("name", name)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<HostServices> findGroupServices(String group) {
    return em.createNamedQuery("HostServices.findByGroup", HostServices.class)
      .setParameter("group", group)
      .getResultList();
  }

  public List<String> findGroups() {
    return em.createNamedQuery("HostServices.findGroups", String.class)
      .getResultList();
  }

  public List<HostServices> findByHostname(String hostname) {
    return em.createNamedQuery("HostServices.findByHostname", HostServices.class)
      .setParameter("hostname", hostname)
      .getResultList();
  }

  public List<HostServices> findServices(String name) {
    return em.createNamedQuery("HostServices.findByServiceName", HostServices.class)
      .setParameter("name", name)
      .getResultList();
  }

  public Long countServices(String group) {
    return em.createNamedQuery("HostServices.CountServices", Long.class)
      .setParameter("group", group)
      .getSingleResult();
  }
  
  public Optional<HostServices> findByServiceName(String serviceName, String hostname) {
    try {
      return Optional.of(em.createNamedQuery("HostServices.findByServiceNameAndHostname", HostServices.class)
        .setParameter("name", serviceName)
        .setParameter("hostname", hostname)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort) {
    String queryStr = buildQuery("SELECT DISTINCT h FROM HostServices h ", filter, sort, "");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT h.id) FROM HostServices h ", filter, sort, "");
    Query query = em.createQuery(queryStr, HostServices.class);
    Query queryCount = em.createQuery(queryCountStr, HostServices.class);
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public CollectionInfo findByHostname(String hostname, Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort) {
    String queryStr = buildQuery("SELECT DISTINCT h FROM HostServices h ", filter, sort,
      "h.host.hostname = :hostname");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT h.id) FROM HostServices h ", filter, sort,
      "h.host.hostname = :hostname");
    Query query = em.createQuery(queryStr, HostServices.class)
      .setParameter("hostname", hostname);
    Query queryCount = em.createQuery(queryCountStr, HostServices.class)
      .setParameter("hostname", hostname);
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
    switch (Filters.valueOf(filterBy.getValue())) {
      case ID:
        q.setParameter(filterBy.getField(), getLongValue(filterBy.getField(), filterBy.getParam()));
        break;
      case HOST_ID:
      case PID:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      case NAME:
      case GROUP_NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case STATUS:
        q.setParameter(filterBy.getField(), getStatusValue(filterBy.getField(), filterBy.getParam()));
        break;
      default:
        break;
    }
  }
  
  private ServiceStatus getStatusValue(String field, String value) {
    if (value == null || value.isEmpty()) {
      throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
        + ", but found: " + value);
    }
    ServiceStatus val;
    try {
      val = ServiceStatus.valueOf(value);
    } catch (IllegalArgumentException e) {
      try {
        val = ServiceStatus.valueOf(value);
      } catch (IllegalArgumentException ie) {
        throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
          + ", but found: " + value);
      }
    }
    return val;
  }
  
  public enum Sorts {
    ID("ID", "h.id", "ASC"),
    HOST_ID("HOST_ID", "h.host.id", "ASC"),
    PID("PID", "h.pid", "ASC"),
    NAME("NAME", "LOWER(h.name)", "ASC"),
    GROUP_NAME("GROUP_NAME", "LOWER(h.group)", "ASC"),
    STATUS("STATUS", "h.status", "ASC"),
    UPTIME("UPTIME", "h.uptime", "ASC"),
    START_TIME("START_TIME", "h.startTime", "ASC"),
    STOP_TIME("STOP_TIME", "h.stopTime", "ASC");
    
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
    
    public String getSql() {
      return sql;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getJoin() {
      return null;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    ID("ID", "h.id = :id", "id", "0"),
    HOST_ID("HOST_ID", "h.host_id = :hostId", "host_id", "0"),
    PID("PID", "h.pid = :pid", "pid", "0"),
    NAME("NAME", "h.name = :name", "name", ""),
    GROUP_NAME("GROUP_NAME", "h.group = :group_name", "group_name", ""),
    STATUS("STATUS", "h.status = :status", "status", "0");
    
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
