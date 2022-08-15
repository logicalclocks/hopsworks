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

package io.hops.hopsworks.common.featurestore.keyword;

import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.List;

public interface KeywordControllerIface {

  List<String> getAll(Project project, Users user, Featuregroup featureGroup, TrainingDataset trainingDataset,
    FeatureView featureView) throws FeaturestoreException, MetadataException, GenericException;

  List<String> replaceKeywords(Project project, Users user, Featuregroup featureGroup, TrainingDataset trainingDataset,
    FeatureView featureView, List<String> keywords)
    throws FeaturestoreException, MetadataException, GenericException;

  List<String> deleteKeywords(Project project, Users user, Featuregroup featureGroup, TrainingDataset trainingDataset,
    FeatureView featureView, List<String> keywords)
    throws FeaturestoreException, MetadataException, GenericException;

  List<String> getUsedKeywords() throws FeaturestoreException, GenericException;
}