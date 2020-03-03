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
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.security.dao.RemoteMaterialRefID;
import io.hops.hopsworks.common.security.dao.RemoteMaterialReferences;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class RemoteMaterialReferencesFacade {
  private final static Logger LOG = Logger.getLogger(RemoteMaterialReferencesFacade.class.getName());
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public RemoteMaterialReferencesFacade() {
  }
  
  public RemoteMaterialReferences findById(RemoteMaterialRefID identifier) {
    RemoteMaterialReferences result = entityManager.find(RemoteMaterialReferences.class, identifier);
    return result;
  }
  
  public void persist(RemoteMaterialReferences materialReferences) {
    entityManager.persist(materialReferences);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void update(RemoteMaterialReferences materialReferences) {
    entityManager.merge(materialReferences);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void delete(RemoteMaterialRefID identifier) {
    RemoteMaterialReferences materialReferences = findById(identifier);
    if (materialReferences != null) {
      entityManager.remove(materialReferences);
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public List<RemoteMaterialReferences> findAll() {
    TypedQuery<RemoteMaterialReferences> query = entityManager.createNamedQuery("RemoteMaterialReferences.findAll",
        RemoteMaterialReferences.class);
    return query.getResultList();
  }
  
  public void createNewMaterialReference(RemoteMaterialRefID identifier) {
    RemoteMaterialReferences ref = new RemoteMaterialReferences(identifier);
    entityManager.persist(ref);
  }
  
  public RemoteMaterialReferences acquireLock(RemoteMaterialRefID identifier, String lockId)
      throws AcquireLockException {
    RemoteMaterialReferences ref = entityManager.find(RemoteMaterialReferences.class, identifier);
    if (ref != null) {
      if (ref.getLock() && !lockId.equals(ref.getLockId())) {
        throw new AcquireLockException("Could not get lock for <" + identifier + ">");
      } else {
        ref.setLock(true);
        ref.setLockId(lockId);
        return entityManager.merge(ref);
      }
    }
    
    return null;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void releaseLock(RemoteMaterialRefID identifier, String lockId) throws AcquireLockException {
    RemoteMaterialReferences ref = entityManager.find(RemoteMaterialReferences.class, identifier);
    if (ref != null) {
      if (ref.getLock() && lockId.equals(ref.getLockId())) {
        ref.setLock(false);
        ref.setLockId("");
        entityManager.merge(ref);
      } else {
        throw new AcquireLockException("Could not release lock. I does not belong to " + lockId);
      }
    } else {
      LOG.log(Level.WARNING, "Tried to release lock for non-existing reference: " + identifier);
    }
  }
}
