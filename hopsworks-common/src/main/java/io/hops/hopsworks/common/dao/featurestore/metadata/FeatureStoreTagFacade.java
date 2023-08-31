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
package io.hops.hopsworks.common.dao.featurestore.metadata;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.TagSchemas;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Stateless
public class FeatureStoreTagFacade extends FeatureStoreMetadataFacade<FeatureStoreTag> {
  private static final String SCHEMA = "schema";
  
  public FeatureStoreTagFacade() {
    super(FeatureStoreTag.class);
  }
  
  public Collector<FeatureStoreTag, ?, Map<String, String>> tagAsStringMapCollector() {
    return Collectors.toMap(t -> t.getSchema().getName(), FeatureStoreTag::getValue);
  }
  
  public Optional<FeatureStoreTag> findBySchema(Featuregroup fg, TagSchemas schema) {
    return findBySchema(FEATURE_GROUP, fg, schema);
  }
  
  public Optional<FeatureStoreTag> findBySchema(FeatureView fv, TagSchemas schema) {
    return findBySchema(FEATURE_VIEW, fv, schema);
  }
  
  public Optional<FeatureStoreTag> findBySchema(TrainingDataset td, TagSchemas schema) {
    return findBySchema(TRAINING_DATASET, td, schema);
  }
  
  private Optional<FeatureStoreTag> findBySchema(String artifactType, Object artifact, TagSchemas schema) {
    String queryStr = "SELECT m FROM " + getTableName() + " m";
    queryStr += " WHERE m." + artifactType + " = :" + FeatureStoreMetadataFacade.ID;
    queryStr += " AND m." + SCHEMA + " = :" + SCHEMA;
    TypedQuery<FeatureStoreTag> query = em.createQuery(queryStr, entityClass);
    query.setParameter(FeatureStoreMetadataFacade.ID, artifact);
    query.setParameter(SCHEMA, schema);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public int deleteBySchema(Featuregroup fg, TagSchemas schema) {
    return deleteBySchema(FEATURE_GROUP, fg, schema);
  }
  
  public int deleteBySchema(FeatureView fv, TagSchemas schema) {
    return deleteBySchema(FEATURE_VIEW, fv, schema);
  }
  
  public int deleteBySchema(TrainingDataset td, TagSchemas schema) {
    return deleteBySchema(TRAINING_DATASET, td, schema);
  }
  
  private int deleteBySchema(String artifactType, Object artifact, TagSchemas schema) {
    String queryStr = "DELETE FROM " + getTableName() + " m";
    queryStr += " WHERE m." + artifactType + " = :" + FeatureStoreMetadataFacade.ID;
    queryStr += " AND m." + SCHEMA +" = :" + SCHEMA;
    TypedQuery<FeatureStoreTag> query = em.createQuery(queryStr, entityClass);
    query.setParameter(FeatureStoreMetadataFacade.ID, artifact);
    query.setParameter(SCHEMA, schema);
    return query.executeUpdate();
  }
  
  @Override
  public String getTableName() {
    return "FeatureStoreTag";
  }
}
