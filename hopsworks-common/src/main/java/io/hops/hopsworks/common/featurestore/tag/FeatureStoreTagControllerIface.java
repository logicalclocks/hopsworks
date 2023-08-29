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

import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.Map;

public interface FeatureStoreTagControllerIface {
  
  Map<String, String> getAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
          throws DatasetException, MetadataException, FeaturestoreException;
  
  Map<String, String> getAll(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset)
    throws DatasetException, MetadataException, SchematizedTagException;

  Map<String, String> getAll(Project project, Users user, Featurestore featureStore, FeatureView featureView)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException;
  
  String get(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup, String name)
          throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException;
  
  String get(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset, String name)
    throws DatasetException, MetadataException, SchematizedTagException;

  String get(Project project, Users user, Featurestore featureStore, FeatureView featureView, String name)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException;
  
  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                         String name, String value)
          throws MetadataException, SchematizedTagException, FeaturestoreException, CommandException;
  
  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                         String name, String value)
    throws MetadataException, SchematizedTagException, CommandException, FeaturestoreException;

  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                         String name, String value)
      throws MetadataException, SchematizedTagException, FeaturestoreException, CommandException;
  
  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                         Map<String, String> tags)
          throws MetadataException, SchematizedTagException, FeaturestoreException, CommandException;
  
  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                         Map<String, String> tags)
    throws MetadataException, SchematizedTagException, CommandException, FeaturestoreException;

  AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                         Map<String, String> tags) throws FeaturestoreException, MetadataException,
                                                          SchematizedTagException;
  
  void deleteAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
          throws DatasetException, MetadataException, FeaturestoreException, CommandException;
  
  void deleteAll(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset)
    throws DatasetException, MetadataException, SchematizedTagException, CommandException, FeaturestoreException;

  void deleteAll(Project project, Users user, Featurestore featureStore, FeatureView featureView)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException, CommandException;
  
  void delete(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup, String name)
          throws DatasetException, MetadataException, FeaturestoreException, CommandException;
  
  void delete(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset, String name)
    throws DatasetException, MetadataException, SchematizedTagException, CommandException, FeaturestoreException;

  void delete(Project project, Users user, Featurestore featureStore, FeatureView featureView, String name)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException, CommandException;
}
