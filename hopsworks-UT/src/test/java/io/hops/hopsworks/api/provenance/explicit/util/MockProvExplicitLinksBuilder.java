/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.explicit.util;

import io.hops.hopsworks.api.featurestore.featureview.FeatureViewBuilder;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetDTOBuilder;
import io.hops.hopsworks.api.provenance.explicit.ProvExplicitLinksBuilder;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;


public class MockProvExplicitLinksBuilder {
  public static ProvExplicitLinksBuilder baseSetup()
    throws FeaturestoreException, ServiceException, DatasetException, MetadataException, FeatureStoreMetadataException,
           IOException, CloudException {
    FeaturestoreUtils featurestoreUtils;
    FeaturegroupController featuregroupController;
    FeatureViewBuilder featureViewBuilder;
    TrainingDatasetDTOBuilder trainingDatasetBuilder;
  
    featurestoreUtils = new FeaturestoreUtils();
    featuregroupController = Mockito.mock(FeaturegroupController.class);
    Mockito.when(featuregroupController.convertFeaturegrouptoDTO(
      Mockito.any(Featuregroup.class), Mockito.any(), Mockito.any()))
      .thenAnswer(
        (Answer<FeaturegroupDTO>) invocation -> {
          Featuregroup fg = invocation.getArgument(0);
          FeaturegroupDTO dto = new FeaturegroupDTO();
          dto.setName(fg.getName());
          return dto;
        });
    
    featureViewBuilder = Mockito.mock(FeatureViewBuilder.class);
    Mockito.when(featureViewBuilder.build(
      Mockito.any(FeatureView.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenAnswer(
        (Answer<FeatureViewDTO>) invocation -> {
          FeatureView fv = invocation.getArgument(0);
          FeatureViewDTO dto = new FeatureViewDTO();
          dto.setName(fv.getName());
          return dto;
        });
    
    trainingDatasetBuilder = Mockito.mock(TrainingDatasetDTOBuilder.class);
    Mockito.when(trainingDatasetBuilder.build(
      Mockito.any(), Mockito.any(), Mockito.any(TrainingDataset.class), Mockito.any(), Mockito.any()))
      .thenAnswer(
        (Answer<TrainingDatasetDTO>) invocation -> {
          TrainingDataset td = invocation.getArgument(2);
          TrainingDatasetDTO dto = new TrainingDatasetDTO();
          dto.setName(td.getName());
          return dto;
        });
    
    return new ProvExplicitLinksBuilder(featurestoreUtils, featuregroupController, featureViewBuilder,
      trainingDatasetBuilder);
  }
}
