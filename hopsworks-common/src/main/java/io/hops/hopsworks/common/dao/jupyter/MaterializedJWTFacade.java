/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.jupyter;


import io.hops.hopsworks.persistence.entity.jupyter.MaterializedJWT;
import io.hops.hopsworks.persistence.entity.jupyter.MaterializedJWTID;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MaterializedJWTFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public MaterializedJWTFacade() {
  }
  
  public MaterializedJWT findById(MaterializedJWTID identifier) {
    return entityManager.find(MaterializedJWT.class, identifier);
  }
  
  public void persist(MaterializedJWT jupyterMaterial) {
    entityManager.persist(jupyterMaterial);
  }
  
  public void delete(MaterializedJWTID identifier) {
    MaterializedJWT jupyterMaterial = findById(identifier);
    if (jupyterMaterial != null) {
      entityManager.remove(jupyterMaterial);
    }
  }
  
  public List<MaterializedJWT> findAll() {
    TypedQuery<MaterializedJWT> query = entityManager.createNamedQuery("MaterializedJWT.findAll",
        MaterializedJWT.class);
    return query.getResultList();
  }

  public List<MaterializedJWT> findAll4Jupyter() {
    return entityManager.createNamedQuery("MaterializedJWT.findByUsage", MaterializedJWT.class)
        .setParameter("usage", MaterializedJWTID.USAGE.JUPYTER)
        .getResultList();
  }
  
  public boolean exists(MaterializedJWTID identifier) {
    return findById(identifier) != null;
  }
}
