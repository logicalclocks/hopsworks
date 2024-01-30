/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.arrowflight;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.bigquery.FeatureStoreBigqueryConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake.FeaturestoreSnowflakeConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;

public class TestArrowFlightController {
  
  private ArrowFlightController arrowFlightController = Mockito.spy(ArrowFlightController.class);
  
  List<FeatureGroupFeatureDTO> features = new ArrayList<>();
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    arrowFlightController.storageConnectorUtil = Mockito.mock(StorageConnectorUtil.class);
    arrowFlightController.featuregroupController = Mockito.mock(FeaturegroupController.class);

    features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "Integer", "", true, false, "10", null));
    features.add(new FeatureGroupFeatureDTO("feature2", "String", "", false, false, null, null));
  }

  @Test
  public void testGetArrowFlightQueryCachedFg() throws Exception {
    // Arrange
    doReturn("SELECT 'id' from 'project.fg_1'").when(arrowFlightController.featuregroupController)
        .getOfflineFeaturegroupQuery(any(), any(), any(), any(), anyInt());

    doReturn(Collections.singletonList(new FeatureGroupFeatureDTO("id", "int")))
        .when(arrowFlightController.featuregroupController).getFeatures(any(), any(), any());

    doReturn("fg_1").when(arrowFlightController.featuregroupController).getTblName(any());

    Project project = new Project();
    project.setName("project");

    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);

    Featuregroup fg = new Featuregroup();
    fg.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
    fg.setName("fg");
    fg.setVersion(1);
    fg.setFeaturestore(featurestore);

    // Act
    String result = arrowFlightController.getArrowFlightQuery(fg, null, null, "project.fg_1", 1);

    // Assert
    Assert.assertEquals("{" +
        "\"query_string\":\"SELECT 'id' from 'project.fg_1'\"," +
        "\"features\":{\"project.fg_1\":[\"id\"]}," +
        "\"filters\":null," +
        "\"connectors\":{\"project.fg_1\":{" +
          "\"type\":\"hudi\"," +
          "\"options\":null," +
          "\"query\":null," +
          "\"alias\":null," +
          "\"filters\":null" +
        "}}" +
      "}", result);
  }

  @Test
  public void testGetArrowFlightQueryStreamFg() throws Exception {
    // Arrange
    doReturn("SELECT 'id' from 'project.fg_1'").when(arrowFlightController.featuregroupController)
        .getOfflineFeaturegroupQuery(any(), any(), any(), any(), anyInt());

    doReturn(Collections.singletonList(new FeatureGroupFeatureDTO("id", "int")))
        .when(arrowFlightController.featuregroupController).getFeatures(any(), any(), any());
    
    doReturn("fg_1").when(arrowFlightController.featuregroupController).getTblName(any());

    Project project = new Project();
    project.setName("project");

    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);

    Featuregroup fg = new Featuregroup();
    fg.setFeaturegroupType(FeaturegroupType.STREAM_FEATURE_GROUP);
    fg.setName("fg");
    fg.setVersion(1);
    fg.setFeaturestore(featurestore);

    // Act
    String result = arrowFlightController.getArrowFlightQuery(fg, null, null, "project.fg_1", 1);

    // Assert
    Assert.assertEquals("{" +
        "\"query_string\":\"SELECT 'id' from 'project.fg_1'\"," +
        "\"features\":{\"project.fg_1\":[\"id\"]}," +
        "\"filters\":null," +
        "\"connectors\":{\"project.fg_1\":{" +
          "\"type\":\"hudi\"," +
          "\"options\":null," +
          "\"query\":null," +
          "\"alias\":null," +
          "\"filters\":null" +
        "}}" +
      "}", result);
  }

  @Test
  public void testGetArrowFlightQueryOndemandFgSnowflake() throws Exception {
    // Arrange
    doReturn("SELECT 'id' from 'fg_1'").when(arrowFlightController.featuregroupController)
        .getOfflineFeaturegroupQuery(any(), any(), any(), any(), anyInt());

    doReturn(Collections.singletonList(new FeatureGroupFeatureDTO("id", "int")))
        .when(arrowFlightController.featuregroupController).getFeatures(any(), any(), any());

    String tbl = "fg_1";
    doReturn(tbl).when(arrowFlightController.featuregroupController).getTblName(any());

    doReturn("test_password").when(arrowFlightController.storageConnectorUtil).getSecret(any(), any());

    Project project = new Project();
    project.setName("project");

    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);

    FeaturestoreSnowflakeConnector snowflakeConnector = new FeaturestoreSnowflakeConnector();
    snowflakeConnector.setDatabaseUser("test_user");
    snowflakeConnector.setUrl("https://test_account.snowflakecomputing.com");
    snowflakeConnector.setDatabaseName("test_dbname");
    snowflakeConnector.setDatabaseSchema("test_dbschema");
    snowflakeConnector.setPwdSecret(new Secret());
    snowflakeConnector.setWarehouse("test_warehouse");
    snowflakeConnector.setApplication("test_application");

    FeaturestoreConnector connector = new FeaturestoreConnector();
    connector.setConnectorType(FeaturestoreConnectorType.SNOWFLAKE);
    connector.setSnowflakeConnector(snowflakeConnector);

    OnDemandFeaturegroup onDemandFg = new OnDemandFeaturegroup();
    onDemandFg.setQuery("SELECT * FROM HOUSEHOLD_DEMOGRAPHICS;");
    onDemandFg.setFeaturestoreConnector(connector);

    Featuregroup fg = new Featuregroup();
    fg.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    fg.setOnDemandFeaturegroup(onDemandFg);
    fg.setName("fg");
    fg.setVersion(1);
    fg.setFeaturestore(featurestore);

    // Act
    String result = arrowFlightController.getArrowFlightQuery(fg, null, null, tbl, 1);

    // Assert
    Assert.assertEquals("{" +
        "\"query_string\":\"SELECT 'id' from 'fg_1'\"," +
        "\"features\":{\"fg_1\":[\"id\"]}," +
        "\"filters\":null," +
        "\"connectors\":{\"fg_1\":{" +
          "\"type\":\"SNOWFLAKE\"," +
          "\"options\":{\"database\":\"test_dbname/test_dbschema\",\"password\":\"test_password\",\"application\":\"test_application\",\"warehouse\":\"test_warehouse\",\"user\":\"test_user\",\"account\":\"test_account\"}," +
          "\"query\":\"SELECT * FROM HOUSEHOLD_DEMOGRAPHICS\"," +
          "\"alias\":\"fg_1\"," +
          "\"filters\":null" +
        "}}" +
      "}", result);
  }

  @Test
  public void testGetArrowFlightQueryOndemandFgBigQuery() throws Exception {
    // Arrange
    doReturn("SELECT 'id' from 'fg_1'").when(arrowFlightController.featuregroupController)
        .getOfflineFeaturegroupQuery(any(), any(), any(), any(), anyInt());

    doReturn(Collections.singletonList(new FeatureGroupFeatureDTO("id", "int")))
        .when(arrowFlightController.featuregroupController).getFeatures(any(), any(), any());
    
    String tbl = "fg_1";
    doReturn(tbl).when(arrowFlightController.featuregroupController).getTblName(any());

    Project project = new Project();
    project.setName("project");

    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);

    FeatureStoreBigqueryConnector bigqueryConnector = new FeatureStoreBigqueryConnector();
    bigqueryConnector.setKeyPath("test_keypath");
    bigqueryConnector.setQueryProject("test_project");
    bigqueryConnector.setDataset("test_dataset");
    bigqueryConnector.setParentProject("test_parent_project");

    FeaturestoreConnector connector = new FeaturestoreConnector();
    connector.setConnectorType(FeaturestoreConnectorType.BIGQUERY);
    connector.setBigqueryConnector(bigqueryConnector);

    OnDemandFeaturegroup onDemandFg = new OnDemandFeaturegroup();
    onDemandFg.setQuery("SELECT * FROM HOUSEHOLD_DEMOGRAPHICS;");
    onDemandFg.setFeaturestoreConnector(connector);

    Featuregroup fg = new Featuregroup();
    fg.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    fg.setOnDemandFeaturegroup(onDemandFg);
    fg.setName("fg");
    fg.setVersion(1);
    fg.setFeaturestore(featurestore);

    // Act
    String result = arrowFlightController.getArrowFlightQuery(fg, null, null, tbl, 1);

    // Assert
    Assert.assertEquals("{" +
        "\"query_string\":\"SELECT 'id' from 'fg_1'\"," +
        "\"features\":{\"fg_1\":[\"id\"]}," +
        "\"filters\":null," +
        "\"connectors\":{\"fg_1\":{" +
          "\"type\":\"BIGQUERY\"," +
          "\"options\":{\"parent_project\":\"test_parent_project\",\"project_id\":\"test_project\",\"dataset_id\":\"test_dataset\",\"key_path\":\"test_keypath\"}," +
          "\"query\":\"SELECT * FROM HOUSEHOLD_DEMOGRAPHICS\"," +
          "\"alias\":\"fg_1\"," +
          "\"filters\":null" +
        "}}" +
      "}", result);
  }

  @Test
  public void testGetArrowFlightQueryOndemandFgBadConnector() throws Exception {
    // Arrange
    doReturn("SELECT 'id' from 'fg_1'").when(arrowFlightController.featuregroupController)
        .getOfflineFeaturegroupQuery(any(), any(), any(), any(), anyInt());

    doReturn(Collections.singletonList(new FeatureGroupFeatureDTO("id", "int")))
        .when(arrowFlightController.featuregroupController).getFeatures(any(), any(), any());
      
    String tbl = "fg_1";

    Project project = new Project();
    project.setName("project");

    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);

    FeaturestoreConnector connector = new FeaturestoreConnector();
    connector.setConnectorType(FeaturestoreConnectorType.S3);

    OnDemandFeaturegroup onDemandFg = new OnDemandFeaturegroup();
    onDemandFg.setQuery("SELECT * FROM HOUSEHOLD_DEMOGRAPHICS;");
    onDemandFg.setFeaturestoreConnector(connector);

    Featuregroup fg = new Featuregroup();
    fg.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    fg.setOnDemandFeaturegroup(onDemandFg);
    fg.setName("fg");
    fg.setVersion(1);
    fg.setFeaturestore(featurestore);

    // Act
    thrown.expect(FeaturestoreException.class);
    arrowFlightController.getArrowFlightQuery(fg, null, null, tbl, 1);

    // Assert
  }
}
