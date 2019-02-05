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

package io.hops.hopsworks.common.dao;

import io.hops.hopsworks.common.exception.InvalidQueryException;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AbstractFacade<T> {

  private final Class<T> entityClass;

  public AbstractFacade(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  protected abstract EntityManager getEntityManager();

  public void save(T entity) {
    getEntityManager().persist(entity);
  }

  public T update(T entity) {
    return getEntityManager().merge(entity);
  }

  public void remove(T entity) {
    if (entity == null) {
      return;
    }
    getEntityManager().remove(getEntityManager().merge(entity));
    getEntityManager().flush();
  }

  public T find(Object id) {
    return getEntityManager().find(entityClass, id);
  }

  public List<T> findAll() {
    javax.persistence.criteria.CriteriaQuery cq = getEntityManager().
            getCriteriaBuilder().createQuery();
    cq.select(cq.from(entityClass));
    return getEntityManager().createQuery(cq).getResultList();
  }

  public List<T> findRange(int[] range) {
    javax.persistence.criteria.CriteriaQuery cq = getEntityManager().
            getCriteriaBuilder().createQuery();
    cq.select(cq.from(entityClass));
    javax.persistence.Query q = getEntityManager().createQuery(cq);
    q.setMaxResults(range[1] - range[0]);
    q.setFirstResult(range[0]);
    return q.getResultList();
  }

  public long count() {
    javax.persistence.criteria.CriteriaQuery cq = getEntityManager().
            getCriteriaBuilder().createQuery();
    javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
    cq.select(getEntityManager().getCriteriaBuilder().count(rt)).where();
    javax.persistence.Query q = getEntityManager().createQuery(cq);
    return (Long) q.getSingleResult();
  }
  
  public void setOffsetAndLim(Integer offset, Integer limit, Query q) {
    if (offset != null && offset > 0) {
      q.setFirstResult(offset);
    }
    if (limit != null && limit > 0) {
      q.setMaxResults(limit);
    }
  }
  
  public String OrderBy(SortBy sortBy) {
    return sortBy.getSql() + sortBy.getParam().getSql();
  }
  
  public String buildQuery(String query, Set<? extends AbstractFacade.FilterBy> filters,
      Set<? extends AbstractFacade.SortBy> sorts, String more) {
    return query + buildFilterString(filters, more) + buildSortString(sorts);
  }
  
  public String buildSortString(Set<? extends SortBy> sortBy) {
    if (sortBy == null || sortBy.isEmpty()) {
      return "";
    }
    Iterator<? extends SortBy> sort = sortBy.iterator();
    if (!sort.hasNext()) {
      return "";
    }
    String c = "ORDER BY " + OrderBy(sort.next());
    for (;sort.hasNext();) {
      c += ", " + OrderBy(sort.next());
    }
    return c;
  }
  
  public String buildFilterString(Set<? extends FilterBy> filter, String more) {
    if (filter == null || filter.isEmpty()) {
      return more == null || more.isEmpty()? "": "WHERE " + more;
    }
    Iterator<? extends FilterBy> filterBy = filter.iterator();
    String c = "WHERE " + filterBy.next().getSql();
    for (;filterBy.hasNext();) {
      c += "AND " + filterBy.next().getSql();
    }

    return c + (more == null || more.isEmpty()? "": "AND " + more);
  }
  
  public Date getDate(String field, String value) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    try {
      return formatter.parse(value);
    } catch (ParseException e) {
      throw new InvalidQueryException(
        "Filter value for " + field + " needs to set valid format. Expected:yyyy-mm-dd hh:mm:ss but found: " + value);
    }
  }
  
  public interface SortBy {
    String getValue();
    OrderBy getParam();
    String getSql();
  }
  
  public interface FilterBy {
    String getValue();
    String getParam();
    String getSql();
    String getField();
  }

  public enum OrderBy {
    ASC ("ASC", "ASC"),
    DESC ("DESC", "DESC");
    
    private final String value;
    private final String sql;

    private OrderBy(String value, String sql) {
      this.value = value;
      this.sql = sql;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    @Override
    public String toString() {
      return value;
    }

  }
  
  public class CollectionInfo {
    private Long count;
    private List<T> items;
  
    public CollectionInfo(Long count, List<T> items) {
      this.count = count;
      this.items = items;
    }
  
    public Long getCount() {
      return count;
    }
    
    public List<T> getItems() {
      return items;
    }
    
    public void setItems(List<T> items) {
      this.items = items;
    }
    
    public void setCount(Long count) {
      this.count = count;
    }
  }
}
