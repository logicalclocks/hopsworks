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
