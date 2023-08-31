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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreMetadata;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class FeatureStoreMetadataFacade<M extends FeatureStoreMetadata> extends AbstractFacade<M>  {
  public static final String ID = "id";
  public static final String FEATURE_GROUP = "featureGroup";
  public static final String FEATURE_VIEW = "featureView";
  public static final String TRAINING_DATASET = "trainingDataset";
  
  @PersistenceContext(unitName = "kthfsPU")
  protected EntityManager em;
  
  protected FeatureStoreMetadataFacade(Class<M> entityClass) {
    super(entityClass);
  }
  
  protected abstract String getTableName();
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public void insertIfNotPresent(M metadata) {
    if(!em.contains(metadata) && metadata.getId() == null) {
      em.persist(metadata);
      em.flush();
    }
  }
  
  public void insertOrUpdate(M metadata) {
    if(metadata.getId() == null) {
      em.persist(metadata);
    } else {
      em.merge(metadata);
    }
    em.flush();
  }
  
  public Map<String, M> findAll(Featuregroup fg) {
    return findAll(FEATURE_GROUP, fg).stream().collect(Collectors.toMap(FeatureStoreMetadata::getName, t -> t));
  }
  
  public Map<String, M> findAll(FeatureView fv) {
    return findAll(FEATURE_VIEW, fv).stream().collect(Collectors.toMap(FeatureStoreMetadata::getName, t -> t));
  }
  
  public Map<String, M> findAll(TrainingDataset td) {
    return findAll(TRAINING_DATASET, td).stream().collect(Collectors.toMap(FeatureStoreMetadata::getName, t -> t));
  }
  
  public List<String> findAllAsNames(Featuregroup fg) {
    return findAll(FEATURE_GROUP, fg).stream().map(FeatureStoreMetadata::getName).collect(Collectors.toList());
  }
  
  public List<String> findAllAsNames(FeatureView fv) {
    return findAll(FEATURE_VIEW, fv).stream().map(FeatureStoreMetadata::getName).collect(Collectors.toList());
  }
  
  public List<String> findAllAsNames(TrainingDataset td) {
    return findAll(TRAINING_DATASET, td).stream().map(FeatureStoreMetadata::getName).collect(Collectors.toList());
  }
  
  private List<M> findAll(String artifactType, Object artifact) {
    String queryStr = "SELECT m FROM " + getTableName() + " m";
    queryStr += " WHERE m." + artifactType + " = :" + artifactType;
    TypedQuery<M> query = em.createQuery(queryStr, entityClass);
    query.setParameter(artifactType, artifact);
    return query.getResultList();
  }
  
  public int deleteAll(Featuregroup fg) {
    return deleteAll(FEATURE_GROUP, fg);
  }
  
  public int deleteAll(FeatureView fv) {
    return deleteAll(FEATURE_VIEW, fv);
  }
  
  public int deleteAll(TrainingDataset td) {
    return deleteAll(TRAINING_DATASET, td);
  }
  
  private int deleteAll(String artifactType, Object artifact) {
    String queryStr = "DELETE m FROM " + getTableName() + " m";
    queryStr += " WHERE m." + artifactType + " = :" + artifactType;
    TypedQuery<M> query = em.createQuery(queryStr, entityClass);
    query.setParameter(artifactType, artifact);
    return query.executeUpdate();
  }
  
  public Collector<M, ?, Map<String, M>> mapCollector() {
    return Collectors.toMap(FeatureStoreMetadata::getName, t -> t);
  }
}
