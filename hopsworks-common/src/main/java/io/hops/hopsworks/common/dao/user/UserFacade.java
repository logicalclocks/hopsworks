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

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.UserGroup;
import io.hops.hopsworks.common.dao.user.security.UserGroupPK;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import javax.ws.rs.core.Response;

@Stateless
public class UserFacade extends AbstractFacade<Users> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public UserFacade() {
    super(Users.class);
  }

  @Override
  public List<Users> findAll() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAll",
        Users.class);
    return query.getResultList();
  }

  public List<Users> findAllByName() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAllByName",
        Users.class);
    return query.getResultList();
  }

  public List<Users> findAllUsers() {
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

  public void removeByEmail(String email) throws AppException {
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
  public Users findByEmail(String email) throws AppException {
    try {
      return em.createNamedQuery("Users.findByEmail", Users.class).setParameter(
          "email", email)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "Problem accessing database.");
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
  public void addGroup(String userMail, int gidNumber) throws Exception {
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
  public void removeGroup(String userMail, int gid) throws Exception {
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
  public void updateStatus(String userMail, UserAccountStatus newStatus) throws Exception {
    Users user = findByEmail(userMail);
    user.setStatus(newStatus);
    em.merge(user);
  }

}
