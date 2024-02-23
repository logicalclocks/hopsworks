/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.kube.client.utils;

import io.hops.hopsworks.persistence.entity.util.Variables;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Optional;

@Stateless
public class VariablesFacade {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public Optional<String> getVariableValue(String id) {
    TypedQuery<Variables> query = em.createNamedQuery("Variables.findById", Variables.class).setParameter("id", id);
    try {
      Variables var = query.getSingleResult();
      return Optional.of(var.getValue());
    } catch (NoResultException e) {
    }
    return Optional.empty();
  }
}
