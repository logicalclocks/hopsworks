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

package io.hops.hopsworks.common.dao.hdfs.inode;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.exceptions.InvalidQueryException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

  private static final Logger LOGGER = Logger.getLogger(InodeFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public InodeFacade() {
    super(Inode.class);
  }

  /**
   * Find all the Inodes that have <i>parent</i> as parent.
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> findByParent(Inode parent) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParentId", Inode.class);
    query.setParameter("parentId", parent.getId());
    return query.getResultList();
  }
  
  public Long countByParentId(Inode parent) {
    TypedQuery<Long> query = em.createNamedQuery("Inode.countByParentId", Long.class);
    query.setParameter("parentId", parent.getId());
    return query.getSingleResult();
  }

  /**
   * Find all the Inodes that have <i>userId</i> as userId.
   * <p/>
   * @return
   */
  public List<Inode> findByHdfsUser(HdfsUsers hdfsUser) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByHdfsUser", Inode.class);
    query.setParameter("hdfsUser", hdfsUser);
    return query.getResultList();
  }

  /**
   * Find the parent of the given Inode. If the Inode has no parent, null is
   * returned.
   * <p/>
   * @param i
   * @return The parent, or null if no parent.
   */
  public Inode findParent(Inode i) {
    if(i == null){
      throw new IllegalArgumentException("Inode must be provided.");
    }
    long id = i.getInodePK().getParentId();
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * find inode by id
   * <p/>
   * @param id
   * @return
   */
  public Inode findById(Long id) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   *
   * @param name
   * @return
   */
  public Inode getRootNode(String name) {
    long partitionId = HopsUtils.calculatePartitionId(HopsUtils.ROOT_INODE_ID, name, HopsUtils.ROOT_DIR_DEPTH + 1);
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findRootByName", Inode.class);
    query.setParameter("name", name);
    query.setParameter("parentId", HopsUtils.ROOT_INODE_ID);
    query.setParameter("partitionId", partitionId);
    try {
      //Sure to give a single result because all children of same parent "null" 
      //so name is unique
      return query.getSingleResult();
    } catch (NoResultException e) {
      LOGGER.log(Level.WARNING, "Could not resolve root inode with name: {0} and partition_id" + partitionId, name);
      return null;
    }
  }

  /**
   * Find an Inode by its parent Inode and its name (i.e. its primary key).
   * <p/>
   * @param parent
   * @param name
   * @param partitionId
   * @return
   */
  public Inode findByInodePK(Inode parent, String name, long partitionId) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findByPrimaryKey", Inode.class);
    q.setParameter("inodePk", new InodePK(parent.getId(), name, partitionId));
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public Inode findByParentAndName(Inode parent, String name) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findByParentAndName", Inode.class);
    q.setParameter("parentId", parent.getId()).setParameter("name", name);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Find all the Inodes that have <i>userId</i> as userId and correspond to an
   * history file.
   * <p/>
   * @return
   */
  public List<Inode> findHistoryFileByHdfsUser(HdfsUsers hdfsUser) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findHistoryFileByHdfsUser", Inode.class);
    query.setParameter("hdfsUser", hdfsUser);
    return query.getResultList();
  }
  
  public CollectionInfo findByParentAndPartition(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort, Inode parent,
    Long partitionId, Project project) {
    String queryStr = buildQuery("SELECT i FROM Inode i ", filter, sort, "i.inodePK.partitionId = :partitionId" +
      " AND i.inodePK.parentId = :parentId ");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT i.inodePK.name) FROM Inode i ", filter, null,
      "i.inodePK.partitionId = :partitionId AND i.inodePK.parentId = :parentId ");
    Long parentId = parent != null ? parent.getId() : null;
    Query query = em.createQuery(queryStr, Inode.class)
      .setParameter("parentId", parentId)
      .setParameter("partitionId", partitionId);
    Query queryCount = em.createQuery(queryCountStr, Inode.class)
      .setParameter("parentId", parentId)
      .setParameter("partitionId", partitionId);
    return getResult(offset, limit, filter, project, query, queryCount);
  }
  
  public CollectionInfo findByParent(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Inode parent, Project project) {
    String queryStr = buildQuery("SELECT i FROM Inode i ", filter, sort, "i.inodePK.parentId = :parentId ");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT i.inodePK.name) FROM Inode i ", filter, null,
      "i.inodePK.parentId = :parentId ");
    Long parentId = parent != null ? parent.getId() : null;
    Query query = em.createQuery(queryStr, Inode.class).setParameter("parentId", parentId);
    Query queryCount = em.createQuery(queryCountStr, Inode.class).setParameter("parentId", parentId);
    return getResult(offset, limit, filter, project, query, queryCount);
  }
  
  private CollectionInfo getResult(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
    Project project, Query query, Query queryCount) {
    setFilter(filter, query, project);
    setFilter(filter, queryCount, project);
    setOffsetAndLim(offset, limit, query);
    CollectionInfo collectionInfo = new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
    return collectionInfo;
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q, Project project) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q, project);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q, Project project) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case UNDER_CONSTRUCTION:
        q.setParameter(filterBy.getField(), getBooleanValue(filterBy.getParam()));
        break;
      case HDFS_USER:
        q.setParameter(filterBy.getField(), gethdfsUser(filterBy.getParam()));
        break;
      case USER_EMAIL:
        q.setParameter(filterBy.getField(), getUsers(filterBy.getParam(), project));
        break;
      case ACCESS_TIME:
      case ACCESS_TIME_GT:
      case ACCESS_TIME_LT:
      case MODIFICATION_TIME:
      case MODIFICATION_TIME_GT:
      case MODIFICATION_TIME_LT:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date.getTime());
        break;
      case SIZE:
      case SIZE_LT:
      case SIZE_GT:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      default:
        break;
    }
  }
  
  public HdfsUsers gethdfsUser(String value) {
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(value);
    return hdfsUser;
  }
  
  public HdfsUsers getUsers(String email, Project project) {
    Users user = userFacade.findByEmail(email);
    if (project == null) {
      throw new InvalidQueryException("Filter by email needs a project.");
    }
    if (user != null) {
      String hdfsUserName = hdfsUsersController.getHdfsUserName(project, user);
      return hdfsUsersFacade.findByName(hdfsUserName);
    }
    return null;
  }
  
  public enum Sorts {
    ID("ID", "i.id ", "ASC"),
    NAME("NAME", "LOWER(i.inodePK.name) ", "ASC"),
    MODIFICATION_TIME("MODIFICATION_TIME", "i.modificationTime ", "ASC"),
    ACCESS_TIME("ACCESS_TIME", "i.accessTime ", "ASC"),
    SIZE("SIZE", "i.size ", "ASC"),
    TYPE("TYPE", "i.dir ", "ASC");
    
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
    
    public String getJoin(){
      return null;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    NAME("NAME", "i.inodePK.name LIKE CONCAT(:name, '%') ", "name", " "),//case
    USER_EMAIL("USER_EMAIL", "i.hdfsUser =:user ", "user", " "),
    HDFS_USER("HDFS_USER", "i.hdfsUser =:hdfsUser ", "hdfsUser", " "),
    UNDER_CONSTRUCTION("UNDER_CONSTRUCTION", "i.underConstruction  =:underConstruction ", "underConstruction", "true"),
    MODIFICATION_TIME("MODIFICATION_TIME", "i.modificationTime  =:modificationTime ", "modificationTime", ""),
    MODIFICATION_TIME_LT("MODIFICATION_TIME_LT", "i.modificationTime  <:modificationTime_lt ", "modificationTime_lt",
      ""),
    MODIFICATION_TIME_GT("MODIFICATION_TIME_GT", "i.modificationTime  >:modificationTime_gt ", "modificationTime_gt",
      ""),
    ACCESS_TIME("ACCESS_TIME", "i.accessTime  =:accessTime ", "accessTime", ""),
    ACCESS_TIME_LT("ACCESS_TIME_LT", "i.accessTime  <:accessTime_lt ", "accessTime_lt", ""),
    ACCESS_TIME_GT("ACCESS_TIME_GT", "i.accessTime  >:accessTime_gt ", "accessTime_gt", ""),
    SIZE("SIZE", "i.size  =:size_eq ", "size_eq", "0"),
    SIZE_LT("SIZE_LT", "i.size  <:size_lt ", "size_lt", "1"),
    SIZE_GT("SIZE_GT", "i.size  >:size_gt ", "size_gt", "0");
    
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
