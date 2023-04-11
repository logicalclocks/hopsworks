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

import io.hops.hopsworks.common.MockUtils;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;

public class TestBigqueryConnectorController {
  private FeaturestoreBigqueryConnectorController bigqueryConnectorController;
  private Project project = new Project();
  private Users user = new Users(123);
  private Settings settings;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws IOException {
    DistributedFileSystemOps dfso = Mockito.mock(DistributedFileSystemOps.class);
    Mockito.when(dfso.exists(Mockito.anyString())).thenReturn(true);

    settings = Mockito.mock(Settings.class);

    StorageConnectorUtil storageConnectorUtil = new StorageConnectorUtil(settings, MockUtils.mockDfs(dfso));
    bigqueryConnectorController = new FeaturestoreBigqueryConnectorController(storageConnectorUtil);
  }
  
  @Test
  public void testValidateInput_keyPath() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath(null);
    Assert.assertThrows(FeaturestoreException.class,
        () -> bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
  }

  @Test
  public void testValidateInput_keyPathNotFound() throws IOException{
    DistributedFileSystemOps dfso = Mockito.mock(DistributedFileSystemOps.class);
    Mockito.when(dfso.exists(Mockito.anyString())).thenReturn(false);
    StorageConnectorUtil storageConnectorUtil = new StorageConnectorUtil(settings, MockUtils.mockDfs(dfso));
    FeaturestoreBigqueryConnectorController featurestoreBigqueryConnectorController
        = new FeaturestoreBigqueryConnectorController(storageConnectorUtil);

    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("/path/does/not/exists");
    Assert.assertThrows(FeaturestoreException.class,
        () -> featurestoreBigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_parentProject() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setParentProject(null);
    
    Assert.assertThrows(FeaturestoreException.class,
        () -> bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_incompleteTableOptions() {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");

    // setting 1 of the three
    bigqConnectorDTO.setQueryProject("project");
    Assert.assertThrows(FeaturestoreException.class,
        () -> bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
  
    // setting 2 of the three
    bigqConnectorDTO.setQueryTable("table");
    Assert.assertThrows(FeaturestoreException.class,
        () -> bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
  }
  
  @Test
  public void testValidateInput_correctTableOptions() throws FeaturestoreException {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");

    // setting none of throw error
    Assert.assertThrows(FeaturestoreException.class,
        () -> bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO));
    
    // setting all three should not throw
    bigqConnectorDTO.setQueryProject("project");
    bigqConnectorDTO.setQueryTable("table");
    bigqConnectorDTO.setDataset("dataset");
    bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO);
  }
  
  @Test
  public void testValidateInput_correctMateriliazationDataset() throws FeaturestoreException {
    FeaturestoreBigqueryConnectorDTO bigqConnectorDTO = new FeaturestoreBigqueryConnectorDTO();
    bigqConnectorDTO.setKeyPath("abc");
    bigqConnectorDTO.setParentProject("project");
    // setting materialization dataset should not throw error
    bigqConnectorDTO.setMaterializationDataset("dataset");
    bigqueryConnectorController.validateInput(project, user, bigqConnectorDTO);
  }
}
