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

package io.hops.hopsworks.common.dao.host;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Stateless
public class HostsFacade extends AbstractFacade<Hosts> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HostsFacade() {
    super(Hosts.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  @Override
  public void save(Hosts entity) {
    super.save(entity);
    getEntityManager().flush();
  }
  
  public CollectionInfo findHosts(Integer offset,
    Integer limit,
    Set<? extends FilterBy> filters,
    Set<? extends AbstractFacade.SortBy> sorts) {
    String queryStr = buildQuery("SELECT h FROM Hosts h ", filters,
      sorts, "");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT(h.id)) FROM Hosts h ",
      filters, sorts, "");
    Query query = em.createQuery(queryStr, Hosts.class);
    Query queryCount = em.createQuery(queryCountStr, Hosts.class);
    setFilter(filters, query);
    setFilter(filters, queryCount);
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
      case HOSTNAME:
      case HOST_IP:
      case PUBLIC_IP:
      case PRIVATE_IP:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case REGISTERED:
      case CONDA_ENABLED:
        q.setParameter(filterBy.getField(), Boolean.valueOf(filterBy.getParam()));
        break;
      default:
        break;
    }
  }
  
  
  public Optional<Hosts> findByHostIp(String hostIp) {
    try {
      return Optional.of(em.createNamedQuery("Hosts.findByHostIp", Hosts.class)
        .setParameter("hostIp", hostIp)
        .getSingleResult());
    } catch (NoResultException ex) {
      return Optional.empty();
    }
  }
  
  public Optional<Hosts> findByHostname(String hostname) {
    try {
      return Optional.of(em.createNamedQuery("Hosts.findByHostname", Hosts.class)
        .setParameter("hostname", hostname)
        .getSingleResult());
    } catch (NoResultException ex) {
      return Optional.empty();
    }
  }

  public Optional<String> findCPUHost() {
    List<Hosts> list = em.createNamedQuery("Hosts.findByCondaEnabledCpu", Hosts.class)
      .getResultList();
    if (list.isEmpty()) {
      return Optional.empty();
    }
    else {
      return Optional.of(list.get(new Random().nextInt(list.size())))
        .map(Hosts::getHostname);
    }
  }

  public Optional<String> findGPUHost() {
    List<Hosts> list = em.createNamedQuery("Hosts.findByCondaEnabledGpu", Hosts.class)
      .getResultList();
    if (list.isEmpty()) {
      return Optional.empty();
    }
    else {
      return Optional.of(list.get(new Random().nextInt(list.size())))
        .map(Hosts::getHostname);
    }
  }

  public List<Hosts> getCondaHosts(LibraryFacade.MachineType machineType) {
    TypedQuery<Hosts> query;
    switch (machineType) {
      case CPU:
        query = em.createNamedQuery("Hosts.findByCondaEnabledCpu", Hosts.class);
        break;
      case GPU:
        query = em.createNamedQuery("Hosts.findByCondaEnabledGpu", Hosts.class);
        break;
      default:
        query = em.createNamedQuery("Hosts.findByCondaEnabled", Hosts.class);
    }

    return query.getResultList();
  }

  public Long countHosts() {
    TypedQuery<Long> query = em.createNamedQuery("Host.Count", Long.class);
    return query.getSingleResult();
  }

  public Long totalCores() {
    TypedQuery<Long> query = em.createNamedQuery("Host.TotalCores", Long.class);
    return query.getSingleResult();
  }

  public Long totalGPUs() {
    TypedQuery<Long> query = em.createNamedQuery("Host.TotalGPUs", Long.class);
    return query.getSingleResult();
  }

  public Long totalMemoryCapacity() {
    TypedQuery<Long> query = em.createNamedQuery("Host.TotalMemoryCapacity", Long.class);
    return query.getSingleResult();
  }
  
  public enum Sorts {
    ID("ID", " h.id ", "ASC"),
    HOSTNAME("HOSTNAME", " LOWER(h.hostname) ", "ASC"),
    HOST_IP("HOST_IP", " LOWER(h.hostIp) ", "ASC"),
    PUBLIC_IP("PUBLIC_IP", " LOWER(h.publicIp) ", "ASC"),
    PRIVATE_IP("PRIVATE_IP", " LOWER(h.privateIp) ", "ASC"),
    CORES("CORES", " h.cores ", "ASC"),
    NUM_GPUS("NUM_GPUS", " h.numGpus ", "ASC"),
    MEMORY_CAPACITY("MEMORY_CAPACITY", " h.memoryCapacity ", "ASC");
  
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
  
    public String getJoin() {
      return null;
    }
  
    @Override
    public String toString() {
      return value;
    }
  }
  
  public enum Filters {
    HOSTNAME("HOSTNAME", " h.hostname = :hostname", "hostname" , ""),
    HOST_IP("HOST_IP", " h.hostIp = :hostIp", "hostIp", ""),
    PUBLIC_IP("PUBLIC_IP", " h.publicIp = :publicIp", "publicIp", ""),
    PRIVATE_IP("PRIVATE_IP", " h.privateIp = :privateIp", "privateIp", ""),
    REGISTERED("REGISTERED", " h.registered = :registered", "registered", "false"),
    CONDA_ENABLED("CONDA_ENABLED", " h.condaEnabled = :condaEnabled", "condaEnabled", "false");
  
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
  
    public String getDefaultParam() {
      return defaultParam;
    }
  
    public String getValue() {
      return value;
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
