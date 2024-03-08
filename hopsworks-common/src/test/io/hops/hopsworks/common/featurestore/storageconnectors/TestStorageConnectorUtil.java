/*Users
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors;

import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestStorageConnectorUtil {

  private Settings settings;

  private StorageConnectorUtil storageConnectorUtil;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    settings = Mockito.mock(Settings.class);
    storageConnectorUtil = new StorageConnectorUtil(settings);
  }

  @Test
  public void testToOptions() throws Exception {
    String testString = "[{\"name\":\"option1\",\"value\":\"value1\"},{\"name\":\"option2\"},{\"name\":\"option3\",\"value\":\"null\"},{\"name\":\"option4\",\"value\":\"\"}]";
    List<OptionDTO> result = storageConnectorUtil.toOptions(testString);

    Assert.assertEquals("option1", result.get(0).getName());
    Assert.assertEquals("option2", result.get(1).getName());
    Assert.assertEquals("option3", result.get(2).getName());
    Assert.assertEquals("option4", result.get(3).getName());
    Assert.assertEquals("value1", result.get(0).getValue());
    Assert.assertNull(result.get(1).getValue());
    Assert.assertEquals("null", result.get(2).getValue());
    Assert.assertEquals("", result.get(3).getValue());
  }

  @Test
  public void testFromOptions() throws Exception {
    List<OptionDTO> optionList = Arrays.asList(new OptionDTO("option1", "value1"), new OptionDTO("option2", null),
      new OptionDTO("option3", "null"), new OptionDTO("option4", ""));
    String result = storageConnectorUtil.fromOptions(optionList);

    Assert.assertEquals("[{\"name\":\"option1\",\"value\":\"value1\"},{\"name\":\"option2\"},{\"name\":\"option3\",\"value\":\"null\"},{\"name\":\"option4\",\"value\":\"\"}]", result);
  }

  @Test
  public void testCreateSecretName() throws Exception {
    String result = storageConnectorUtil.createSecretName(1, "connector name", FeaturestoreConnectorType.REDSHIFT);
    Assert.assertEquals("redshift_connector_name_1", result);
  }

  @Test
  public void testShouldUpdateString() throws Exception {
    boolean result;

    result = storageConnectorUtil.shouldUpdate("old", "new");
    Assert.assertTrue(result);

    result = storageConnectorUtil.shouldUpdate("old", "old");
    Assert.assertFalse(result);

    result = storageConnectorUtil.shouldUpdate(null, "new");
    Assert.assertTrue(result);

    result = storageConnectorUtil.shouldUpdate(null, (String) null);
    Assert.assertFalse(result);
  }

  @Test
  public void testShouldUpdateInteger() throws Exception {
    boolean result;

    result = storageConnectorUtil.shouldUpdate(1, 2);
    Assert.assertTrue(result);

    result = storageConnectorUtil.shouldUpdate(1, 1);
    Assert.assertFalse(result);

    result = storageConnectorUtil.shouldUpdate(null, 1);
    Assert.assertTrue(result);

    result = storageConnectorUtil.shouldUpdate(null, (Integer) null);
    Assert.assertFalse(result);
  }

  @Test
  public void testNumberOfStorageConnectorTypes() {
    // If a new feature store connector type is introduced, make sure the following methods are updated accordingly,
    // as well as their corresponding tests:
    // StorageConnectorUtil.getEnabledStorageConnectorTypes() -> testGetEnabledStorageConnectorTypes
    // StorageConnectorUtil.isStorageConnectorTypeEnabled() -> testIsStorageConnectorTypeEnabled
    Assert.assertEquals(10, FeaturestoreConnectorType.values().length);
  }

  @Test
  public void testGetEnabledStorageConnectorTypes() {
    Mockito.when(settings.isSnowflakeStorageConnectorsEnabled()).thenReturn(true);
    Mockito.when(settings.isRedshiftStorageConnectorsEnabled()).thenReturn(true);
    Mockito.when(settings.isAdlsStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isKafkaStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isGcsStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isBigqueryStorageConnectorsEnabled()).thenReturn(false);

    Set<FeaturestoreConnectorType> enabledScTypes = storageConnectorUtil.getEnabledStorageConnectorTypes();

    FeaturestoreConnectorType[] expectedScTypes = new FeaturestoreConnectorType[]{
            FeaturestoreConnectorType.JDBC, FeaturestoreConnectorType.HOPSFS, FeaturestoreConnectorType.S3,
            FeaturestoreConnectorType.SNOWFLAKE, FeaturestoreConnectorType.REDSHIFT
    };

    Assert.assertArrayEquals(
            enabledScTypes.stream().sorted().toArray(),
            Arrays.stream(expectedScTypes).sorted().toArray());
  }

  @Test
  public void testIsEnabledStorageConnectorTypes() {
    Mockito.when(settings.isSnowflakeStorageConnectorsEnabled()).thenReturn(true);
    Mockito.when(settings.isRedshiftStorageConnectorsEnabled()).thenReturn(true);
    Mockito.when(settings.isAdlsStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isKafkaStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isGcsStorageConnectorsEnabled()).thenReturn(false);
    Mockito.when(settings.isBigqueryStorageConnectorsEnabled()).thenReturn(false);

    // Always enabled
    Assert.assertTrue(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.JDBC));
    Assert.assertTrue(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.HOPSFS));
    Assert.assertTrue(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.S3));

    // Configurable through Hopsworks variables
    Assert.assertTrue(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.SNOWFLAKE));
    Assert.assertTrue(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.REDSHIFT));
    Assert.assertFalse(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.ADLS));
    Assert.assertFalse(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.KAFKA));
    Assert.assertFalse(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.GCS));
    Assert.assertFalse(storageConnectorUtil.isStorageConnectorTypeEnabled(FeaturestoreConnectorType.BIGQUERY));
  }
  
  @Test
  public void test_replaceToPlainText() {
    // Test the method replaceToPlainText with connectionString
    final String  PASSWORD_VALUE = "password123";
    String connectionString = "jdbc:mysql://localhost:3306/hopsworks?user=root&password="
      + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE;
    String password = "password123";
    String expectedConnectionString = "jdbc:mysql://localhost:3306/hopsworks?user=root&password=" + PASSWORD_VALUE;
    String actualConnectionString = storageConnectorUtil.replaceToPlainText(connectionString, password);
    Assert.assertEquals(expectedConnectionString, actualConnectionString);
    
    // Test the method replaceToPlainText with arguments
    List<OptionDTO> arguments = Collections.singletonList(new OptionDTO("password",
      FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE));
    List<OptionDTO> expectedArguments = Collections.singletonList(new OptionDTO("password", PASSWORD_VALUE));
    List<OptionDTO> actualArguments = storageConnectorUtil.replaceToPlainText(arguments, password);
    Assert.assertEquals(expectedArguments.get(0).getValue(), actualArguments.get(0).getValue());
  }
  
  @Test
  public void test_replaceToPasswordTemplate() {
    // Test the method replaceToPasswordTemplate with connectionString
    final String  PASSWORD_VALUE = "password123";
    String connectionString = "jdbc:mysql://localhost:3306/hopsworks?user=root&password=" + PASSWORD_VALUE;
    String expectedConnectionString = "jdbc:mysql://localhost:3306/hopsworks?user=root&password="
      + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE;
    String actualConnectionString = storageConnectorUtil.replaceToPasswordTemplate(connectionString, PASSWORD_VALUE);
    Assert.assertEquals(expectedConnectionString, actualConnectionString);
    
    // Test the method replaceToPasswordTemplate with arguments
    List<OptionDTO> arguments = Collections.singletonList(new OptionDTO("password", PASSWORD_VALUE));
    String actualArguments = storageConnectorUtil.replaceToPasswordTemplate(arguments);
    String expectedArguments = storageConnectorUtil.fromOptions(Collections.singletonList(
      new OptionDTO("password", FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE)));
    Assert.assertEquals(expectedArguments, actualArguments);
  }
  
  @Test
  public void test_fetchPasswordFromUrl() {
    // Test the method fetchPassword with connectionString
    final String PASSWORD_VALUE = "password987";
    String connectionString = "jdbc:mysql://localhost:3306/hopsworks?user=root&password=" + PASSWORD_VALUE;
    String actualPassword = storageConnectorUtil.fetchPasswordFromJdbcUrl(connectionString);
    Assert.assertEquals(PASSWORD_VALUE, actualPassword);
  }
}
