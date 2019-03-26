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

package io.hops.hopsworks.common.dao.airflow;


import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AirflowMaterialFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public AirflowMaterialFacade() {
  }
  
  public AirflowMaterial findById(AirflowMaterialID identifier) {
    return entityManager.find(AirflowMaterial.class, identifier);
  }
  
  public void persist(AirflowMaterial airflowMaterial) {
    entityManager.persist(airflowMaterial);
  }
  
  public void delete(AirflowMaterialID identifier) {
    AirflowMaterial airflowMaterial = findById(identifier);
    if (airflowMaterial != null) {
      entityManager.remove(airflowMaterial);
    }
  }
  
  public List<AirflowMaterial> findAll() {
    TypedQuery<AirflowMaterial> query = entityManager.createNamedQuery("AirflowMaterial.findAll",
        AirflowMaterial.class);
    return query.getResultList();
  }
  
  public boolean exists(AirflowMaterialID identifier) {
    return findById(identifier) != null;
  }
}
