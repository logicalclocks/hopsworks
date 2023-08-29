/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 *  Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.featuregroup.stream;

import io.hops.hopsworks.common.dao.AbstractFacade;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the stream_feature_group table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StreamFeatureGroupFacade extends AbstractFacade<StreamFeatureGroup> {
  
  private static final Logger LOGGER = Logger.getLogger(
    StreamFeatureGroupFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public StreamFeatureGroupFacade() {
    super(StreamFeatureGroup.class);
  }
  
  /**
   * A transaction to persist a stream featuregroup for the featurestore in the database
   *
   * @param streamFeatureGroup the stream featuregroup to persist
   */
  public void persist(StreamFeatureGroup streamFeatureGroup) {
    try {
      em.persist(streamFeatureGroup);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new stream feature group", cve);
      throw cve;
    }
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
   * Updates an existing Stream Feature Group
   *
   * @param streamFeatureGroup the entity to update
   * @return the updated entity
   */
  public StreamFeatureGroup updateMetadata(StreamFeatureGroup streamFeatureGroup) {
    em.merge(streamFeatureGroup);
    return streamFeatureGroup;
  }
}
