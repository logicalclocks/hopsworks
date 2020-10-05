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

package io.hops.hopsworks.common.featurestore.featuregroup.ondemand;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A facade for the feature_store_feature table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class OnDemandFeatureFacade extends AbstractFacade<OnDemandFeature> {
  private static final Logger LOGGER = Logger.getLogger(OnDemandFeatureFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public OnDemandFeatureFacade() {
    super(OnDemandFeature.class);
  }

  /**
   * A transaction to persist a feature in the database
   *
   * @param onDemandFeature the feature to persist
   */
  public void persist(OnDemandFeature onDemandFeature) {
    try {
      em.persist(onDemandFeature);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new FeaturestoreFeature", cve);
      throw cve;
    }
  }

  /**
   * Updates the features of an on-demand Feature Group, first deletes all existing features for the on-demand Feature
   * Group and then insert the new ones.
   *
   * @param onDemandFeaturegroup the on-demand featuregroup to update
   * @param features the new features
   */
  public void updateOnDemandFeaturegroupFeatures(
      OnDemandFeaturegroup onDemandFeaturegroup, List<FeatureGroupFeatureDTO> features) {
    if(features == null) {
      return;
    }

    for (OnDemandFeature f: onDemandFeaturegroup.getFeatures()) {
      remove(f);
    }
    insertOnDemandFeaturegroupFeatures(onDemandFeaturegroup, features);
  }

  /**
   * Inserts a list of features into the database
   *
   * @param onDemandFeaturegroup the on-demand feature group that the features are linked to
   * @param features the list of features to insert
   */
  private void insertOnDemandFeaturegroupFeatures(
      OnDemandFeaturegroup onDemandFeaturegroup, List<FeatureGroupFeatureDTO> features) {
    List<OnDemandFeature> onDemandFeatures = convertFeaturesToOnDemandFeaturegroupFeatures(
        onDemandFeaturegroup, features);
    onDemandFeatures.forEach(this::persist);
  }

  /**
   * Utility method that converts a list of featureDTOs to FeaturestoreFeature entities
   *
   * @param onDemandFeaturegroup the on-demand featuregroup that the features are linked to
   * @param features the list of feature DTOs to convert
   * @return a list of FeaturestoreFeature entities
   */
  private List<OnDemandFeature> convertFeaturesToOnDemandFeaturegroupFeatures(
      OnDemandFeaturegroup onDemandFeaturegroup, List<FeatureGroupFeatureDTO> features) {
    return features.stream().map(f -> {
      OnDemandFeature onDemandFeature = new OnDemandFeature();
      onDemandFeature.setName(f.getName());
      onDemandFeature.setOnDemandFeaturegroup(onDemandFeaturegroup);
      onDemandFeature.setDescription(f.getDescription());
      onDemandFeature.setPrimary(f.getPrimary());
      onDemandFeature.setType(f.getType());
      return onDemandFeature;
    }).collect(Collectors.toList());
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

}
