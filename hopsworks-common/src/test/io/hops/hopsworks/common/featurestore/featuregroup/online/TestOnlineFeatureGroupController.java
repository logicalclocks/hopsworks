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

import io.hops.hopsworks.common.featurestore.embedding.EmbeddingController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Embedding;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.vectordb.VectorDatabase;
import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestOnlineFeatureGroupController {

  private Settings settings;

  private OnlineFeaturegroupController onlineFeaturegroupController;
  private EmbeddingController embeddingController;
  private FeaturegroupController featuregroupController;
  private VectorDatabase vectorDatabase;
  private Project project;
  private Featurestore featureStore;
  private Featuregroup featureGroup;

  @Before
  public void setup() throws Exception {
    settings = mock(Settings.class);
    vectorDatabase = mock(VectorDatabase.class);
    featuregroupController = mock(FeaturegroupController.class);
    featureStore = mock(Featurestore.class);
    project = mock(Project.class);
    embeddingController = spy(new EmbeddingController());
    Mockito.when(featuregroupController.getFeatures(any(), any(), any())).thenReturn(Lists.newArrayList());
    onlineFeaturegroupController = spy(new OnlineFeaturegroupController(settings, embeddingController, featuregroupController));
    featureGroup = new Featuregroup();
    featureGroup.setEmbedding(null);
    featureGroup.setName("fg");
    featureGroup.setVersion(1);
  }

  @Test
  public void testBuildAlterStatementNoDefaultValue() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "string", ""));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` VARCHAR(100) DEFAULT NULL, ALGORITHM=INPLACE;";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildAlterStatementDefaultValueString() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "string", "",
        false, "defaultValue"));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` VARCHAR(100), ALGORITHM=INPLACE;";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildAlterStatementDefaultValueOther() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "float", "",
        false, "10.0"));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` float, ALGORITHM=INPLACE;";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildAlterStatementMultiColumns() {
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "float", "",
        false, "10.0"));
    features.add(new FeatureGroupFeatureDTO("feature2", "float", "",
        false, "19.0"));
    features.add(new FeatureGroupFeatureDTO("feature3", "float", ""));
    String output = onlineFeaturegroupController.buildAlterStatement("tbl", "db", features);
    String expected = "ALTER TABLE `db`.`tbl` ADD COLUMN `feature` float, ADD COLUMN `feature2` float, " +
        "ADD COLUMN `feature3` float DEFAULT NULL, ALGORITHM=INPLACE;";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testBuildCreateStatementNoDefaultValue() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");

    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("feature", "String", "", false, false));

    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` VARCHAR(100), PRIMARY KEY (`pk`))" +
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
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` VARCHAR(100), PRIMARY KEY (`pk`))" +
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
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `feature` VARCHAR(100) NOT NULL DEFAULT " +
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

  @Test
  public void testBuildCreateStatementDecimal() {
    Mockito.when(settings.getOnlineFeatureStoreTableSpace()).thenReturn("");

    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("pk", "int", "", true, false));
    features.add(new FeatureGroupFeatureDTO("decimal", "decimal(10, 7)", "", false, false));

    String output = onlineFeaturegroupController.buildCreateStatement("db", "tbl", features);
    String expected = "CREATE TABLE IF NOT EXISTS `db`.`tbl`(`pk` int, `decimal` decimal(10, 7), PRIMARY KEY (`pk`))" +
        "ENGINE=ndbcluster COMMENT='NDB_TABLE=READ_BACKUP=1'";
    Assert.assertEquals(expected, output);
  }

  @Test
  public void testSetupOnlineFeatureGroupWithEmbedding() throws Exception {
    // Arrange
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    Users user = new Users();
    // Set up the scenario where featureGroup.getEmbedding() is not null
    Embedding embedding = new Embedding();
    featureGroup.setEmbedding(embedding);

    // Mock the behavior for vectorDatabase initialization
    doNothing().when(embeddingController).createVectorDbIndex(any(), any(), any());
    doNothing().when(onlineFeaturegroupController).checkOnlineFsUserExist(eq(project));
    doNothing().when(onlineFeaturegroupController)
        .createMySQLTable(eq(featureStore), anyString(), anyList(), eq(project), eq(user));
    doNothing().when(onlineFeaturegroupController)
        .createFeatureGroupKafkaTopic(eq(project), eq(featureGroup), eq(features));
    doNothing().when(onlineFeaturegroupController).createOnlineFeatureStore(any(), any(), any());

    // Act
    onlineFeaturegroupController.setupOnlineFeatureGroup(featureStore, featureGroup, features, project, user);

    // Assert
    // Verify that vectorDatabase.createIndex is called with the correct parameters
    verify(embeddingController, times(1)).createVectorDbIndex(any(), any(), any());
    verify(onlineFeaturegroupController, times(1)).checkOnlineFsUserExist(eq(project));
    verify(onlineFeaturegroupController, times(1)).createFeatureGroupKafkaTopic(eq(project), eq(featureGroup),
        eq(features));
  }

  @Test
  public void testSetupOnlineFeatureGroupWithoutEmbedding() throws Exception {
    // Arrange

    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    Users user = new Users();

    // Mock the behavior for createMySQLTable
    doNothing().when(onlineFeaturegroupController)
        .createMySQLTable(eq(featureStore), anyString(), anyList(), eq(project), eq(user));
    doNothing().when(onlineFeaturegroupController).checkOnlineFsUserExist(eq(project));
    doNothing().when(onlineFeaturegroupController)
        .createFeatureGroupKafkaTopic(eq(project), eq(featureGroup), eq(features));
    doNothing().when(onlineFeaturegroupController).createOnlineFeatureStore(any(), any(), any());

    // Act
    onlineFeaturegroupController.setupOnlineFeatureGroup(featureStore, featureGroup, features, project, user);

    // Assert
    verify(onlineFeaturegroupController, times(1)).createMySQLTable(eq(featureStore), anyString(), anyList(),
        eq(project), eq(user));
    verify(onlineFeaturegroupController, times(1)).checkOnlineFsUserExist(eq(project));
    verify(onlineFeaturegroupController, times(1)).createFeatureGroupKafkaTopic(eq(project), eq(featureGroup),
        eq(features));
  }
}
