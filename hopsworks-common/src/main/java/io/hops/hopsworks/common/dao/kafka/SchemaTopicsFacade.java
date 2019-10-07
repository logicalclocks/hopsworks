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
package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class SchemaTopicsFacade extends AbstractFacade<SchemaTopics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public SchemaTopicsFacade() {
    super(SchemaTopics.class);
  }
  
  public Optional<SchemaTopics> findSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(find(new SchemaTopicsPK(schemaName, schemaVersion)));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }
  
  @Deprecated
  public Optional<SchemaTopics> getSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(em.createNamedQuery("SchemaTopics.findByNameAndVersion", SchemaTopics.class)
        .setParameter("name", schemaName)
        .setParameter("version", schemaVersion)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
