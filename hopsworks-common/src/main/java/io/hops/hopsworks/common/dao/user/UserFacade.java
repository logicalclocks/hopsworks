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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.UserGroup;
import io.hops.hopsworks.common.dao.user.security.UserGroupPK;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.exception.InvalidQueryException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends AbstractFacade.FilterBy> filter,
      Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = buildQuery("SELECT u FROM Users u ", filter, sort, "");
    String queryCountStr = buildQuery("SELECT COUNT(u.uid) FROM Users u ", filter, sort, "");
    Query query = em.createQuery(queryStr, Users.class);
    Query queryCount = em.createQuery(queryCountStr, Users.class);
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
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
      throw new InvalidQueryException("Filter value for " + field + " needs to set valid roles, but found: " + values);
    }
    return roles;
  }

  private Integer getIntValue(String field, String value) {
    Integer val;
    try {
      val = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer, but found: " + value);
    }
    return val;
  }
  
  private UserAccountStatus getStatusValue(String field, String value) {
    if (value == null || value.isEmpty()) {
      throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
          + ", but found: " + value);
    }
    UserAccountStatus val;
    try {
      int v = Integer.parseInt(value);
      val = UserAccountStatus.fromValue(v);
    } catch (IllegalArgumentException e) {
      try {
        val = UserAccountStatus.valueOf(value);
      } catch (IllegalArgumentException ie) {
        throw new InvalidQueryException("Filter value for " + field + " needs to set an Integer or a valid " + field
            + ", but found: " + value);
      }
    }
    return val;
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
      case ROLE:
      case ROLE_NEQ:
        List<BbcGroup> roles = getGroups(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), roles);
        break;
      case STATUS:
      case STATUS_GT:
      case STATUS_LT:
        q.setParameter(filterBy.getField(), getStatusValue(filterBy.getField(), filterBy.getParam()));
        break;
      case IS_ONLINE:
      case FALSE_LOGIN:
      case FALSE_LOGIN_GT:
      case FALSE_LOGIN_LT:
        q.setParameter(filterBy.getField(), getIntValue(filterBy.getField(), filterBy.getParam()));
        break;
      case USER_NAME:
      case USER_FIRST_NAME:
      case USER_LAST_NAME:
      case USER_EMAIL:
      case USER_LIKE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    FIRST_NAME("FIRST_NAME", "LOWER(u.fname) ", "ASC"),
    LAST_NAME("LAST_NAME", "LOWER(u.lname) ", "ASC"),
    EMAIL("EMAIL", "LOWER(u.email) ", "ASC"),
    DATE_CREATED("DATE_CREATED", "u.activated ", "ASC");

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
    ROLE("ROLE", "u.bbcGroupCollection IN :roles ", "roles", "HOPS_ADMIN,HOPS_USER"),
    ROLE_NEQ("ROLE_NEQ", "u.bbcGroupCollection NOT IN :roles_neq ", "roles_neq", "AGENT,AUDITOR"),
    STATUS("STATUS", "u.status = :status ", "status", "2"),
    STATUS_LT("STATUS_LT", "u.status < :status_lt ", "status_lt", "2"),
    STATUS_GT("STATUS_GT", "u.status > :status_gt ", "status_gt", "2"),
    IS_ONLINE("IS_ONLINE", "u.isonline = :isonline ", "isonline", "1"),
    FALSE_LOGIN("FALSE_LOGIN", "u.falseLogin = :falseLogin ", "falseLogin", "20"),
    FALSE_LOGIN_GT("FALSE_LOGIN_GT", "u.falseLogin > :falseLogin_gt ", "falseLogin_gt", "20"),
    FALSE_LOGIN_LT("FALSE_LOGIN_LT", "u.falseLogin < :falseLogin_lt ", "falseLogin_lt", "20"),
    USER_NAME("USER_NAME", "UPPER(u.username) LIKE CONCAT(:username, '%') ", "username", " "),
    USER_FIRST_NAME("USER_FIRST_NAME", "UPPER(u.fname) LIKE CONCAT(:fname, '%') ", "fname", " "),
    USER_LAST_NAME("USER_LAST_NAME", "UPPER(u.lname) LIKE CONCAT(:lname, '%') ", "lname", " "),
    USER_EMAIL("USER_EMAIL", "UPPER(u.email) LIKE CONCAT(:email, '%') ", "email", " "),
    USER_LIKE("USER_LIKE", "(UPPER(u.username) LIKE CONCAT(:user, '%') OR UPPER(u.fname) LIKE CONCAT(:user, '%') OR "
        + "UPPER(u.lname) LIKE CONCAT(:user, '%') OR UPPER(u.email) LIKE CONCAT(:user, '%')) ", "user", " ");

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

  public List findAllUsers() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.users", Users.class);
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

  public void persist(Users user) {
    em.persist(user);
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
      return em.createNamedQuery("Users.findByEmail", Users.class).setParameter("email", email).getSingleResult();
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
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatus", Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

  public List<Integer> findAllInGroup(int gid) {
    BbcGroup role = groupFacade.find(gid);
    List<Integer> uIds = new ArrayList<>();
    if (role == null) {
      return uIds;
    }
    List<BbcGroup> roles = new ArrayList<>();
    roles.add(role);
    TypedQuery<Integer> query = em.createNamedQuery("Users.findAllInGroup", Integer.class);
    query.setParameter("roles", roles);
    uIds.addAll(query.getResultList());
    return uIds;
  }

  /**
   * Add a new group for a user.
   *
   * @param userMail
   * @param gidNumber
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
