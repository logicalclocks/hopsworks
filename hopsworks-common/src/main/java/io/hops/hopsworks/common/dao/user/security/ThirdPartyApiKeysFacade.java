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

package io.hops.hopsworks.common.dao.user.security;

import io.hops.hopsworks.common.dao.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ThirdPartyApiKeysFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public ThirdPartyApiKey findById(ThirdPartyApiKeyId id) {
    return entityManager.find(ThirdPartyApiKey.class, id);
  }
  
  public void persist(ThirdPartyApiKey apiKey) {
    entityManager.persist(apiKey);
  }
  
  public List<ThirdPartyApiKey> findAllForUser(Users user) {
    return entityManager.createNamedQuery("ThirdPartyApiKey.findByUser", ThirdPartyApiKey.class)
        .setParameter("uid", user.getUid())
        .getResultList();
  }
  
  public void deleteKey(ThirdPartyApiKeyId id) {
    ThirdPartyApiKey key = findById(id);
    if (key != null) {
      entityManager.remove(key);
    }
  }
}
