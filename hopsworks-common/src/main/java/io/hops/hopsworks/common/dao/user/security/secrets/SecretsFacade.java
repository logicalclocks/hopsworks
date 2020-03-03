/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.dao.user.security.secrets;

import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.persistence.entity.user.security.secrets.SecretId;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SecretsFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public Secret findById(SecretId id) {
    return entityManager.find(Secret.class, id);
  }
  
  public void persist(Secret secret) {
    entityManager.persist(secret);
  }
  
  public void update(Secret secret) {
    entityManager.merge(secret);
  }
  
  public List<Secret> findAllForUser(Users user) {
    return entityManager.createNamedQuery("Secret.findByUser", Secret.class)
        .setParameter("uid", user.getUid())
        .getResultList();
  }
  
  public List<Secret> findAll() {
    return entityManager.createNamedQuery("Secret.findAll", Secret.class)
        .getResultList();
  }
  
  public void deleteSecret(SecretId id) {
    Secret secret = findById(id);
    if (secret != null) {
      entityManager.remove(secret);
    }
  }
  
  public void deleteSecretsForUser(Users user) {
    List<Secret> secrets = findAllForUser(user);
    for (Secret secret : secrets) {
      entityManager.remove(secret);
    }
  }
}
