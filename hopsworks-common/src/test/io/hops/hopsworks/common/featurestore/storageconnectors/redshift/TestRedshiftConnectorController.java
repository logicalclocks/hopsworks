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

package io.hops.hopsworks.common.featurestore.storageconnectors.redshift;

import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRedshiftConnectorController {
  private FeaturestoreRedshiftConnectorController featurestoreRedshiftConnectorController;
  private SecretsController secretsController;
  private StorageConnectorUtil storageConnectorUtil;
  private SecretsFacade secretsFacade;
  
  @Before
  public void setup() {
    secretsController = new SecretsController();
    storageConnectorUtil = new StorageConnectorUtil();
    featurestoreRedshiftConnectorController = new FeaturestoreRedshiftConnectorController(secretsController,
      secretsFacade,storageConnectorUtil);
  }
  
  public FeaturestoreRedshiftConnectorDTO setDefaultDTO() {
    FeaturestoreRedshiftConnectorDTO dto = new FeaturestoreRedshiftConnectorDTO();
    dto.setClusterIdentifier("cluster_id");
    dto.setDatabaseEndpoint("random.amazonaws.com");
    dto.setDatabaseUserName("user");
    dto.setDatabaseName("dev");
    dto.setDatabasePort(5439);
    return dto;
  }
  
  @Test
  public void testValidateInput_nullDTO() {
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(null));
  }
  
  @Test
  public void testValidateInput_clusterId() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    dto.setClusterIdentifier(null);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }
  
  @Test
  public void testValidateInput_endpoint() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    dto.setDatabaseEndpoint(null);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }
  
  @Test
  public void testValidateInput_databaseName() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    dto.setDatabaseName(null);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }
  
  @Test
  public void testValidateInput_port() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    dto.setDatabasePort(null);
    // test port range
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
    dto.setDatabasePort(100);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
    dto.setDatabasePort(70000);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }
  
  @Test
  public void testValidateInput_databaseUser() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    dto.setDatabaseUserName(null);
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }
  
  @Test
  public void testValidateInput_password() {
    FeaturestoreRedshiftConnectorDTO dto = setDefaultDTO();
    // both passowrd and iamRole should fail
    dto.setIamRole("iamRole");
    dto.setDatabasePassword("password");
    Assert.assertThrows(FeaturestoreException.class,
      () -> featurestoreRedshiftConnectorController.verifyCreateDTO(dto));
  }

}
