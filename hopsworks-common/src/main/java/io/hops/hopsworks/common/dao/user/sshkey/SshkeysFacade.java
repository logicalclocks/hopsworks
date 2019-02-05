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
