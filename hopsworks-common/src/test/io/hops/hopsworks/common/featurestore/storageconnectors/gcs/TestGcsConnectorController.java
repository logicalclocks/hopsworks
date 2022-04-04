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

import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.gcs.EncryptionAlgorithm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestGcsConnectorController {
  private FeatureStoreGcsConnectorController gcsConnectorController;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    gcsConnectorController = new FeatureStoreGcsConnectorController();
  }
  
  @Test
  public void testValidateInput_keyPath() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath(null);
  
    Assert.assertThrows(FeaturestoreException.class, () -> gcsConnectorController.validateInput(gcsConnectorDTO));
  }
  
  @Test
  public void testValidateInput_bucket() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setBucket(null);
    
    Assert.assertThrows(FeaturestoreException.class, () -> gcsConnectorController.validateInput(gcsConnectorDTO));
  }
  
  @Test
  public void testValidateInput_encryptionFaulty() {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath("abc");
    gcsConnectorDTO.setBucket("bucket");

    // setting 1 of the three
    gcsConnectorDTO.setAlgorithm(EncryptionAlgorithm.AES256);
    Assert.assertThrows(FeaturestoreException.class, () -> gcsConnectorController.validateInput(gcsConnectorDTO));
  
    // setting 2 of the three
    gcsConnectorDTO.setEncryptionKey("key");
    Assert.assertThrows(FeaturestoreException.class, () -> gcsConnectorController.validateInput(gcsConnectorDTO));
  }
  
  @Test
  public void testValidateInput_encryptionCorrect() throws FeaturestoreException {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO();
    gcsConnectorDTO.setKeyPath("abc");
    gcsConnectorDTO.setBucket("bucket");
  
    // setting none of the three should not fail
    gcsConnectorController.validateInput(gcsConnectorDTO);
    
    // setting all three should not throw
    gcsConnectorDTO.setAlgorithm(EncryptionAlgorithm.AES256);
    gcsConnectorDTO.setEncryptionKey("key");
    gcsConnectorDTO.setEncryptionKeyHash("keyHash");
    gcsConnectorController.validateInput(gcsConnectorDTO);
  }
}
