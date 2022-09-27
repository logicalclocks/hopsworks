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

package io.hops.hopsworks.common.featurestore.storageconnectors.snowflake;

import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSnowflakeConnectorController {
  private FeaturestoreSnowflakeConnectorController snowflakeConnectorController;
  private SecretsController secretsController;
  private StorageConnectorUtil storageConnectorUtil;
  
  @Before
  public void setup() {
    secretsController = new SecretsController();
    storageConnectorUtil = new StorageConnectorUtil();
    snowflakeConnectorController = new FeaturestoreSnowflakeConnectorController(secretsController, storageConnectorUtil);
  }
  
  public FeaturestoreSnowflakeConnectorDTO setDefaultDTO() {
    FeaturestoreSnowflakeConnectorDTO dto = new FeaturestoreSnowflakeConnectorDTO();
    dto.setUser("test");
    dto.setUrl("user-id.snowflake.com");
    dto.setSchema("schema");
    dto.setDatabase("database");
    dto.setPassword("password");
    dto.setWarehouse("warehouse");
    return dto;
  }
  
  @Test
  public void testValidateInput_warehouse() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setWarehouse(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_database() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setDatabase(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_url() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setUrl(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_user() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setUrl(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_schema() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setSchema(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_noPasswordToken() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setPassword(null);
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_bothPasswordToken() {
    FeaturestoreSnowflakeConnectorDTO dto = setDefaultDTO();
    dto.setToken("token");
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(dto));
  }
  
  @Test
  public void testValidateInput_nullDto() {
    Assert.assertThrows(FeaturestoreException.class, () -> snowflakeConnectorController.verifyConnectorDTO(null));
  }
}
