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

package io.hops.hopsworks.common.featurestore.storageconnectors.bigquery;

import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestBigqueryConnectorController {
  private FeaturestoreBigqueryConnectorController bigqueryConnectorController;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    bigqueryConnectorController = new FeaturestoreBigqueryConnectorController();
  }
  
  @Test
  public void testValidateInput_keyPath() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath(null);
    Assert.assertThrows(FeaturestoreException.class, () -> bigqueryConnectorController.validateInput(bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_parentProject() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setParentProject(null);
    
    Assert.assertThrows(FeaturestoreException.class, () -> bigqueryConnectorController.validateInput(bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_incompleteTableOptions() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");

    // setting 1 of the three
    bigqConnectorDTO.setQueryProject("project");
    Assert.assertThrows(FeaturestoreException.class, () -> bigqueryConnectorController.validateInput(bigqConnectorDTO));
  
    // setting 2 of the three
    bigqConnectorDTO.setQueryTable("table");
    Assert.assertThrows(FeaturestoreException.class, () -> bigqueryConnectorController.validateInput(bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_correctTableOptions() throws FeaturestoreException {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");

    // setting none of throw error
    Assert.assertThrows(FeaturestoreException.class, () -> bigqueryConnectorController.validateInput(bigqConnectorDTO));
    
    // setting all three should not throw
    bigqConnectorDTO.setQueryProject("project");
    bigqConnectorDTO.setQueryTable("table");
    bigqConnectorDTO.setDataset("dataset");
    bigqueryConnectorController.validateInput(bigqConnectorDTO);
  }
  
  @Test
  public void testValidateInput_correctMateriliazationDataset() throws FeaturestoreException {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");
    // setting materialization dataset should not throw error
    bigqConnectorDTO.setMaterializationDataset("dataset");
    bigqueryConnectorController.validateInput(bigqConnectorDTO);
  }
}
