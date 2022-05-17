/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.tag;

import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@Stateless
@CommunityStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeatureStoreTagsController implements FeatureStoreTagControllerIface {
  @Override
  public Map<String, String> getAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public Map<String, String> getAll(Project project, Users user, Featurestore featureStore,
                                    TrainingDataset trainingDataset) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public Map<String, String> getAll(Project project, Users user, Featurestore featureStore, FeatureView featureView) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public String get(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public String get(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                    String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public String get(Project project, Users user, Featurestore featureStore, FeatureView featureView, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                String name, String value) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                                String name, String value) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                                String name, String value) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                Map<String, String> tags) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                                Map<String, String> tags) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                                Map<String, String> tags) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void deleteAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public void deleteAll(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void deleteAll(Project project, Users user, Featurestore featureStore, FeatureView featureView) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void delete(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public void delete(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                      String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void delete(Project project, Users user, Featurestore featureStore, FeatureView featureView, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
}
