/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.tutorials;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.tutorials.Tutorial;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;

@Stateless
public class TutorialFacade  extends AbstractFacade<Tutorial> {
  
  private static final Logger LOGGER = Logger.getLogger(TutorialFacade.class.getName());
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public TutorialFacade() {
    super(Tutorial.class);
  }
  
  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  /**
   * Retrieves all Tutorials from the database
   *
   * @return collection of Tutorial entities
   */
  public CollectionInfo<Tutorial> findAllTutorials() {
    TypedQuery<Tutorial> query = em.createNamedQuery("Tutorial.getAll", Tutorial.class);
    TypedQuery<Long> queryCount = em.createNamedQuery("Tutorial.countAll", Long.class);
    
    return new CollectionInfo<Tutorial>((Long) queryCount.getSingleResult(), query.getResultList());
  }
}
