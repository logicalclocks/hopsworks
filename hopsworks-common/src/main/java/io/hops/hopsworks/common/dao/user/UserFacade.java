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
package io.hops.hopsworks.common.dao.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.UserGroup;
import io.hops.hopsworks.common.dao.user.security.UserGroupPK;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;

@Stateless
public class UserFacade extends AbstractFacade<Users> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  @EJB
  private BbcGroupFacade groupFacade;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public UserFacade() {
    super(Users.class);
  }

  @Override
  public List<Users> findAll() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAll", Users.class);
    return query.getResultList();
  }

  public List<Users> findAll(Integer offset, Integer limit, Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = "SELECT u FROM Users u " + buildSortString(sort);
    Query query = em.createQuery(queryStr, Users.class);
    setOffsetAndLim(offset, limit, query);
    return query.getResultList();
  }
  
  public List<Users> findAll(Set<? extends AbstractFacade.FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = "SELECT u FROM Users u " + buildFilterString(filter, "") + buildSortString(sort);
    Query query = em.createQuery(queryStr, Users.class);
    setFilter(filter, query);
    return query.getResultList();
  }

  public List<Users> findAll(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter, 
      Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = "SELECT u FROM Users u " + buildFilterString(filter, "") + buildSortString(sort);
    Query query = em.createQuery(queryStr, Users.class);
    setFilter(filter, query);
    setOffsetAndLim(offset, limit, query);
    return query.getResultList();
  }
  
  private List<BbcGroup> getGroups(String field, String values) {
    String[] groups = values.split(",");
    BbcGroup role;
    List<BbcGroup> roles = new ArrayList<>();
    for (String group : groups) {
      role = groupFacade.findByGroupName(group.trim());
      if (role != null) {
        roles.add(role);
      }
    }
    if (roles.isEmpty()) {
      throw new IllegalArgumentException("Filter value for " + field + " needs to set valid roles, but found: "
          + values);
    }
    return roles;
  }
  
  private Integer getIntValue(String field, String value) {
    Integer val;
    try {
      val = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Filter value for " + field + " needs to set an Integer, but found: " + value);
    }
    return val;
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    Iterator<? extends AbstractFacade.FilterBy> filterBy = filter.iterator();
    for (;filterBy.hasNext();) {
      setFilterQuery(filterBy.next(), q);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    if (filterBy.equals(FilterBy.ROLE) || filterBy.equals(FilterBy.ROLE_NEQ)) {
      List<BbcGroup> roles = getGroups(filterBy.getField(), filterBy.getParam());
      q.setParameter(filterBy.getField(), roles);
    } else {
      q.setParameter(filterBy.getField(), getIntValue(filterBy.getField(), filterBy.getParam()));
    }
  }

  public enum SortBy implements AbstractFacade.SortBy {
    FIRST_NAME("FIRST_NAME", "u.fname ", "ASC"),
    LAST_NAME("LAST_NAME", "u.lname ", "ASC"),
    EMAIL("EMAIL", "u.email ", "ASC"),
    DATE_CREATED("DATE_CREATED", "u.activated ", "ASC");

    @JsonCreator
    public static SortBy fromString(String param) {
      String[] sortByParams = param.split(":");
      SortBy sortBy = SortBy.valueOf(sortByParams[0].toUpperCase());
      String order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : sortBy.defaultParam;
      AbstractFacade.OrderBy orderBy = AbstractFacade.OrderBy.valueOf(order);
      sortBy.setParam(orderBy);
      return sortBy;
    }
    
    private final String value;
    private AbstractFacade.OrderBy param;
    private final String sql;
    private final String defaultParam;

    private SortBy(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public OrderBy getParam() {
      return param;
    }

    public void setParam(OrderBy param) {
      this.param = param;
    }
    
    @Override
    public String getSql() {
      return sql;
    }

    @Override
    public String toString() {
      return value;
    }

  }

  public enum FilterBy implements AbstractFacade.FilterBy {
    ROLE ("ROLE", "u.bbcGroupCollection IN :roles ", "roles", "HOPS_USER"),
    ROLE_NEQ ("ROLE_NEQ", "u.bbcGroupCollection NOT IN :roles ", "roles", "HOPS_ADMIN,AGENT,AUDITOR"),
    STATUS ("STATUS", "u.status = :status ", "status", "2"),
    STATUS_LT ("STATUS_LT", "u.status < :status ", "status", "2"),
    STATUS_GT ("STATUS_GT", "u.status > :status ", "status", "2"),
    IS_ONLINE ("IS_ONLINE", "u.isonline = :isonline ", "isonline", "1"),
    FALSE_LOGIN ("FALSE_LOGIN", "u.falseLogin = :falseLogin ", "falseLogin", "20"),
    FALSE_LOGIN_GT ("FALSE_LOGIN_GT", "u.falseLogin > :falseLogin ", "falseLogin", "20"),
    FALSE_LOGIN_LT ("FALSE_LOGIN_LT", "u.falseLogin < :falseLogin ", "falseLogin", "20");
    
    @JsonCreator
    public static FilterBy fromString(String param) {
      String[] filterByParams = param.split(":");
      FilterBy filterBy = FilterBy.valueOf(filterByParams[0].toUpperCase());
      String filter = filterByParams.length > 1 ? filterByParams[1].toUpperCase() : filterBy.defaultParam;
      filterBy.setParam(filter);
      return filterBy;
    }
    
    private final String value;
    private String param;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private FilterBy(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public String getParam() {
      return param;
    }

    public void setParam(String param) {
      this.param = param;
    }
    
    @Override
    public String getSql() {
      return sql;
    }

    @Override
    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return value;
    }

  }

  public List findAllUsers() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.users",
        Users.class);
    return query.getResultList();
  }

  public List<Users> findAllMobileRequests() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", UserAccountStatus.VERIFIED_ACCOUNT);
    query.setParameter("mode", UserAccountType.M_ACCOUNT_TYPE);
    List<Users> res = query.getResultList();

    query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", UserAccountStatus.NEW_MOBILE_ACCOUNT);
    query.setParameter("mode", UserAccountType.M_ACCOUNT_TYPE);

    res.addAll(query.getResultList());
    return res;
  }

  public Users findByUsername(String username) {
    try {
      return em.createNamedQuery("Users.findByUsername", Users.class).setParameter("username", username).
          getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<Users> filterUsersBasedOnProject(String name) {

    Query query = em.createNativeQuery(
        "SELECT * FROM hopsworks.users WHERE email NOT IN (SELECT team_member "
        + "FROM hopsworks.ProjectTeam WHERE name=?)",
        Users.class).setParameter(1, name);
    return query.getResultList();
  }

  public void persist(Users user) {
    em.persist(user);
  }

  public int lastUserID() {
    Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM hopsworks.users p");
    Object obj = query.getSingleResult();

    if (obj == null) {
      return Settings.STARTING_USER;
    }
    return (Integer) obj;
  }

  @Override
  public Users update(Users user) {
    return em.merge(user);
  }

  public void removeByEmail(String email) {
    Users user = findByEmail(email);
    if (user != null) {
      em.remove(user);
    }
  }

  @Override
  public void remove(Users user) {
    if (user != null && user.getEmail() != null && em.contains(user)) {
      em.remove(user);
    }
  }

  /**
   * Get the user with the given email.
   * <p/>
   * @param email
   * @return The user with given email, or null if no such user exists.
   */
  public Users findByEmail(String email) {
    try {
      return em.createNamedQuery("Users.findByEmail", Users.class).setParameter(
          "email", email)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void detach(Users user) {
    em.detach(user);
  }

  /**
   * Get all users with STATUS = status.
   *
   * @param status
   * @return
   */
  public List<Users> findAllByStatus(UserAccountStatus status) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatus",
        Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

  public List<Integer> findAllInGroup(int gid) {
    Query query = em.createNativeQuery(
        "SELECT u.uid FROM hopsworks.users u JOIN hopsworks.user_group g ON u.uid = g.uid Where g.gid = ?");
    query.setParameter(1, gid);
    return (List<Integer>) query.getResultList();
  }

  /**
   * Add a new group for a user.
   *
   * @param userMail
   * @param gidNumber
   * @return
   */
  public void addGroup(String userMail, int gidNumber) {
    BbcGroup bbcGroup = em.find(BbcGroup.class, gidNumber);
    Users user = findByEmail(userMail);
    user.getBbcGroupCollection().add(bbcGroup);
    em.merge(user);
  }

  /**
   * Remove user's group based on userMail/gid.
   *
   * @param userMail
   * @param gid
   */
  public void removeGroup(String userMail, int gid) {
    Users user = findByEmail(userMail);
    UserGroup p = em.find(UserGroup.class, new UserGroup(
        new UserGroupPK(user.getUid(), gid)).getUserGroupPK());
    em.remove(p);
  }

  /**
   * Update a user status
   *
   * @param userMail
   * @param newStatus
   */
  public void updateStatus(String userMail, UserAccountStatus newStatus) {
    Users user = findByEmail(userMail);
    user.setStatus(newStatus);
    em.merge(user);
  }

}
