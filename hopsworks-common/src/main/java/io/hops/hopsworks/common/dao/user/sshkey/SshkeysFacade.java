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

package io.hops.hopsworks.common.dao.user.sshkey;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class SshkeysFacade extends AbstractFacade<SshKeys> {

  private final int STARTING_USER = 1000;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public SshkeysFacade() {
    super(SshKeys.class);
  }

  public List<SshKeys> findAllById(int id) {
    TypedQuery<SshKeys> query = em.createNamedQuery("SshKeys.findByUid",
            SshKeys.class);
    query.setParameter("uid", id);
    return query.getResultList();
  }

  public void persist(SshKeys user) {
    em.persist(user);
  }

  public SshKeys update(SshKeys user) {
    return em.merge(user);
  }

  public void removeByIdName(int uid, String name) {
    SshKeysPK pk = new SshKeysPK(uid, name);
    if (pk != null) {
      TypedQuery<SshKeys> query = em.createNamedQuery("SshKeys.findByUidName",
              SshKeys.class);
      query.setParameter("name", name);
      query.setParameter("uid", uid);
      SshKeys item = query.getSingleResult();
      SshKeys sk = em.merge(item);
      em.remove(sk);
    }
  }

  @Override
  public void remove(SshKeys sshKey) {
    if (sshKey != null && sshKey.getSshKeysPK() != null && em.contains(sshKey)) {
      em.remove(sshKey);
    }
  }

  public void detach(SshKeys key) {
    em.detach(key);
  }

  /**
   * Get all ssh keys with user UID
   *
   * @param uid
   * @return
   */
  public List<SshKeys> findAllByUid(int uid) {
    TypedQuery<SshKeys> query = em.createNamedQuery("SshKeys.findByUid",
            SshKeys.class);
    query.setParameter("uid", uid);
    return query.getResultList();
  }

}
