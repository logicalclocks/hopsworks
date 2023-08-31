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
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreKeyword;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
public class FeatureStoreKeywordFacade extends FeatureStoreMetadataFacade<FeatureStoreKeyword> {
  public static final String NAME = "name";
  
  public FeatureStoreKeywordFacade() {
    super(FeatureStoreKeyword.class);
  }
  
  @Override
  protected String getTableName() {
    return "FeatureStoreKeyword";
  }
  
  public int removeByName(Featuregroup fg, String keyword) {
    Set<String> keywords = new HashSet<>();
    keywords.add(keyword);
    return removeByName(FEATURE_GROUP, fg, keywords);
  }
  
  public int removeByName(FeatureView fv, String keyword) {
    Set<String> keywords = new HashSet<>();
    keywords.add(keyword);
    return removeByName(FEATURE_VIEW, fv, keywords);
  }
  
  public int removeByName(TrainingDataset td, String keyword) {
    Set<String> keywords = new HashSet<>();
    keywords.add(keyword);
    return removeByName(TRAINING_DATASET, td, keywords);
  }
  
  public int removeByName(Featuregroup fg, Set<String> keywords) {
    return removeByName(FEATURE_GROUP, fg, keywords);
  }
  
  public int removeByName(FeatureView fv, Set<String> keywords) {
    return removeByName(FEATURE_VIEW, fv, keywords);
  }
  
  public int removeByName(TrainingDataset td, Set<String> keywords) {
    return removeByName(TRAINING_DATASET, td, keywords);
  }
  
  private int removeByName(String artifactType, Object artifact, Set<String> keywords) {
    String queryStr = "DELETE FROM " + getTableName() + " m ";
    queryStr += " WHERE m." + artifactType + " = :" + artifactType;
    queryStr += " AND m.name IN :" + NAME;
    TypedQuery<FeatureStoreKeyword> query = em.createQuery(queryStr, entityClass);
    query.setParameter(artifactType, artifact);
    query.setParameter(NAME, keywords);
    return query.executeUpdate();
  }
  
  public List<String> getAll() {
    String queryStr = "SELECT DISTINCT m.name FROM " + getTableName() + " m";
    TypedQuery<String> query = em.createQuery(queryStr, String.class);
    return query.getResultList();
  }
}
