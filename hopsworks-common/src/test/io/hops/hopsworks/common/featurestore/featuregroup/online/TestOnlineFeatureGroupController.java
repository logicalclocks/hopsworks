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

package io.hops.hopsworks.common.featurestore.featuregroup.online;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.util.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class TestOnlineFeatureGroupController {
  
  private Settings settings;
  
  private OnlineFeaturegroupController onlineFeaturegroupController;
  
  @Before
  public void setup() {
    settings = Mockito.mock(Settings.class);
    onlineFeaturegroupController = new OnlineFeaturegroupController(settings);
  }
  
  @Test
  public void testBuildAlterStatementNoDefaultValue() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "string", ""));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` CHAR(100) DEFAULT NULL;";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildAlterStatementDefaultValueString() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "string", "",
      false, "defaultValue"));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` CHAR(100) NOT NULL DEFAULT 'defaultValue';";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildAlterStatementDefaultValueOther() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "float", "",
      false, "10.0"));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` float NOT NULL DEFAULT 10.0;";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildAlterStatementMultiColumns() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "float", "",
      false, "10.0"));
    features.add(new FeatureGroupFeatureDTO("feature2", "float", ""));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` float NOT NULL DEFAULT 10.0, ADD COLUMN `feature2`" +
      " float DEFAULT NULL;";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildCreateStatementNoDefaultValue() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");
    
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("feature", "String", "", false, false));
    
    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` CHAR(100), PRIMARY KEY (`pk`))" +
      "ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildCreateStatementTableSpaceNoDefaultValue() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("abc");
    
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("feature", "String", "", false, false));
    
    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` CHAR(100), PRIMARY KEY (`pk`))" +
      "ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'/*!50100 TABLESPACE `abc` STORAGE DISK */";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildCreateStatementDefaultValueString() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");
    
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("feature", "String", "", false,
      "hello"));
    
    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` CHAR(100) NOT NULL DEFAULT " +
      "'hello', PRIMARY KEY (`pk`))ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'";
    Assert.assertEquals(expected, output);
  }
  
  @Test
  public void testBuildCreateStatementDefaultValueOther() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");
    
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("feature", "float", "", false,
      "10.0"));
    
    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` float NOT NULL DEFAULT " +
      "10.0, PRIMARY KEY (`pk`))ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildCreateStatementNoPrimaryKey() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");

    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", false, false));
    features.add(new FeatureGroupFeatureDTO("feature", "float", "", false, false));

    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` float)" +
        "ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'";
    Assert.assertEquals(expected, output);
  }
}
