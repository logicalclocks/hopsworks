/*
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
 */

package io.hops.hopsworks.common.dao.airflow;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AirflowDagFacade {
  
  @PersistenceContext(unitName = "airflowPU")
  private EntityManager em;
  
  public List<AirflowDag> getAll() {
    TypedQuery<AirflowDag> query = em.createNamedQuery("AirflowDag.getAll", AirflowDag.class);
    return query.getResultList();
  }
  
  public List<AirflowDag> filterByOwner(String owner) throws IOException {
    if (owner == null || owner.isEmpty()) {
      throw new IOException("Airflow DAG owner cannot be null or empty");
    }
    return em.createNamedQuery("AirflowDag.filterByOwner", AirflowDag.class)
        .setParameter("owner", owner)
        .getResultList();
  }
}
