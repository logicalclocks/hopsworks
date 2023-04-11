/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.gcs;

import io.hops.hopsworks.common.MockUtils;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.featurestore.storageconnectors.bigquery.FeaturestoreBigqueryConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.bigquery.FeaturestoreBigqueryConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.gcs.EncryptionAlgorithm;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;

public class TestGcsConnectorController {
  private FeatureStoreGcsConnectorController gcsConnectorController;
  private Project project = new Project();
  private Users user = new Users();
  private Settings settings;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() throws IOException {
    DistributedFileSystemOps dfso = Mockito.mock(DistributedFileSystemOps.class);
    Mockito.when(dfso.exists(Mockito.anyString())).thenReturn(true);

    settings = Mockito.mock(Settings.class);

    StorageConnectorUtil storageConnectorUtil = new StorageConnectorUtil(settings, MockUtils.mockDfs(dfso));
    gcsConnectorController = new FeatureStoreGcsConnectorController(storageConnectorUtil);
  }
  
  @Test
  public void testValidateInput_keyPath() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath(null);
  
    Assert.assertThrows(FeaturestoreException.class,
        () -> gcsConnectorController.validateInput(project, user, gcsConnectorDTO));
  }

  @Test
  public void testValidateInput_keyPathNotFound() throws IOException {
    DistributedFileSystemOps dfso = Mockito.mock(DistributedFileSystemOps.class);
    Mockito.when(dfso.exists(Mockito.anyString())).thenReturn(false);
    StorageConnectorUtil storageConnectorUtil = new StorageConnectorUtil(settings, MockUtils.mockDfs(dfso));
    FeatureStoreGcsConnectorController featureStoreGcsConnectorController
        = new FeatureStoreGcsConnectorController(storageConnectorUtil);

    FeatureStoreGcsConnectorDTO featureStoreGcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    featureStoreGcsConnectorDTO.setKeyPath("/path/does/not/exists");
    Assert.assertThrows(FeaturestoreException.class,
        () -> featureStoreGcsConnectorController.validateInput(project, user, featureStoreGcsConnectorDTO));
  }
  @Test
  public void testValidateInput_bucket() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath("abc");
    gcsConnectorDTO.setBucket(null);

    Assert.assertThrows(FeaturestoreException.class,
        () -> gcsConnectorController.validateInput(project, user, gcsConnectorDTO));
  }
  
  @Test
  public void testValidateInput_encryptionFaulty() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath("abc");
    gcsConnectorDTO.setBucket("bucket");

    // setting 1 of the three
    gcsConnectorDTO.setAlgorithm(EncryptionAlgorithm.AES256);
    Assert.assertThrows(FeaturestoreException.class,
        () -> gcsConnectorController.validateInput(project, user, gcsConnectorDTO));
  
    // setting 2 of the three
    gcsConnectorDTO.setEncryptionKey("key");
    Assert.assertThrows(FeaturestoreException.class,
        () -> gcsConnectorController.validateInput(project, user, gcsConnectorDTO));
  }
  
  @Test
  public void testValidateInput_encryptionCorrect() throws FeaturestoreException {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath("abc");
    gcsConnectorDTO.setBucket("bucket");
  
    // setting none of the three should not fail
    gcsConnectorController.validateInput(project, user, gcsConnectorDTO);
    
    // setting all three should not throw
    gcsConnectorDTO.setAlgorithm(EncryptionAlgorithm.AES256);
    gcsConnectorDTO.setEncryptionKey("key");
    gcsConnectorDTO.setEncryptionKeyHash("keyHash");
    gcsConnectorController.validateInput(project, user, gcsConnectorDTO);
  }
}
