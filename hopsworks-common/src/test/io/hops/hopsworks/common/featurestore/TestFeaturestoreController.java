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

package io.hops.hopsworks.common.featurestore;

import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestFeaturestoreController {

  @Test
  public void testGetProjectFeaturestores() {
    FeaturestoreController featurestoreController = new FeaturestoreController();
    Featurestore featurestore = new Featurestore();
    featurestore.setId(1);

    Dataset fsDataset = new Dataset();
    fsDataset.setDsType(DatasetType.FEATURESTORE);
    fsDataset.setFeatureStore(featurestore);

    Project project = new Project();
    project.setDatasetCollection(Arrays.asList(fsDataset));
    project.setDatasetSharedWithCollection(new ArrayList<>());

    List<Featurestore> projectFeaturestores = featurestoreController.getProjectFeaturestores(project);
    Assert.assertEquals(featurestore.getId(), projectFeaturestores.get(0).getId());
  }

  @Test
  public void testGetProjectFeaturestores_notInitialized() {
    FeaturestoreController featurestoreController = new FeaturestoreController();

    Dataset fsDataset = new Dataset();
    fsDataset.setDsType(DatasetType.FEATURESTORE);
    fsDataset.setFeatureStore(null);

    Project project = new Project();
    project.setDatasetCollection(Arrays.asList(fsDataset));
    project.setDatasetSharedWithCollection(new ArrayList<>());

    List<Featurestore> projectFeaturestores = featurestoreController.getProjectFeaturestores(project);
    Assert.assertEquals(0, projectFeaturestores.size());
  }

}
