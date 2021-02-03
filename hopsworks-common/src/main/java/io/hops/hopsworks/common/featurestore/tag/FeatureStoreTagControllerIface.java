/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public interface FeatureStoreTagControllerIface {
  
  default Map<String, String> getAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
    throws DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default Map<String, String> getAll(Project project, Users user, Featurestore featureStore,
                                     TrainingDataset trainingDataset)
    throws DatasetException, MetadataException, FeaturestoreException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default Map<String, String> get(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                  String name)
    throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default Map<String, String> get(Project project, Users user, Featurestore featureStore,
                                  TrainingDataset trainingDataset, String name)
    throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                 String name, String value)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default AttachTagResult upsert(Project project, Users user, Featurestore featureStore,
                                 TrainingDataset trainingDataset, String name, String value)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                 Map<String, String> tags)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default AttachTagResult upsert(Project project, Users user, Featurestore featureStore,
                                 TrainingDataset trainingDataset, Map<String, String> tags)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default void deleteAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
    throws DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default void deleteAll(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset)
    throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default void delete(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                      String name)
    throws DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  default void delete(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                      String name)
    throws FeaturestoreException, DatasetException, MetadataException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
}
