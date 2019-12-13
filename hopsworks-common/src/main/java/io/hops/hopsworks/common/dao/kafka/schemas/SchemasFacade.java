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
package io.hops.hopsworks.common.dao.kafka.schemas;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class SchemasFacade extends AbstractFacade<Schemas> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public SchemasFacade() {
    super(Schemas.class);
  }
  
  public Optional<Schemas> findSchemaById(Project project, Integer id) {
    try {
      return Optional.of(em.createNamedQuery("Schemas.findById", Schemas.class)
        .setParameter("project", project)
        .setParameter("id", id)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<Schemas> findBySchema(Project project, String schema) {
    try {
      return Optional.of(em.createNamedQuery("Schemas.findBySchema", Schemas.class)
        .setParameter("project", project)
        .setParameter("schema", schema)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  @Override
  public void save(Schemas entity) {
    super.save(entity);
    em.flush();
  }
}
