/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.embedding;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.EmbeddingFeature;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.vectordb.Field;
import io.hops.hopsworks.vectordb.Index;
import io.hops.hopsworks.vectordb.OpensearchVectorDatabase;
import io.hops.hopsworks.vectordb.VectorDatabase;
import io.hops.hopsworks.vectordb.VectorDatabaseException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class EmbeddingControllerTest {

  private VectorDatabaseClient vectorDatabaseClient;
  private VectorDatabase vectorDatabase;
  private Settings settings;
  private EmbeddingController target;
  private final int defaultMappingSize = 1000;
  private Project project;

  @Before
  public void setup() throws Exception {
    project = mock(Project.class);
    vectorDatabaseClient = mock(VectorDatabaseClient.class);
    settings = mock(Settings.class);
    when(settings.getOpensearchDefaultIndexMappingLimit()).thenReturn(defaultMappingSize);
    vectorDatabase = mock(VectorDatabase.class);
    when(vectorDatabaseClient.getClient()).thenReturn(vectorDatabase);
    target = spy(new EmbeddingController(vectorDatabaseClient, settings));
  }

  @Test
  public void testCreateIndex() {
    List<EmbeddingFeature> embeddingFeatures = new ArrayList<>();
    embeddingFeatures.add(new EmbeddingFeature(null, "vector", 512, "l2"));
    embeddingFeatures.add(new EmbeddingFeature(null, "vector2", 128, "l2"));
    List<FeatureGroupFeatureDTO> features = new ArrayList<>();
    Set<String> offlineTypes =
        FeaturestoreConstants.SUGGESTED_HIVE_FEATURE_TYPES.stream().map(type -> type.split(" ")[0])
            .collect(Collectors.toSet());
    offlineTypes.remove("DECIMAL"); // not supported by opensearch
    for (String offlineType : offlineTypes) {
      features.add(new FeatureGroupFeatureDTO("feature_" + offlineType, offlineType));
    }
    features.add(new FeatureGroupFeatureDTO("vector", "ARRAY<DOUBLE>"));
    features.add(new FeatureGroupFeatureDTO("vector2", "ARRAY<DOUBLE>"));


    String expectedMapping = "{\n" +
        "  \"settings\": {\n" +
        "    \"index\": {\n" +
        "      \"knn\": \"true\",\n" +
        "      \"knn.algo_param.ef_search\": 512\n" +
        "    }\n" +
        "  },\n" +
        "  \"mappings\": {\n" +
        "    \"properties\": {\n" +
        "        \"vector\": {\n" +
        "          \"type\": \"knn_vector\",\n" +
        "          \"dimension\": 512\n" +
        "        },\n" +
        "        \"vector2\": {\n" +
        "          \"type\": \"knn_vector\",\n" +
        "          \"dimension\": 128\n" +
        "        }";

    for (String offlineType : offlineTypes) {
      expectedMapping += ",\n        \"feature_" + offlineType + "\": {\n" +
          "          \"type\": \"" + OpensearchVectorDatabase.getDataType(offlineType) + "\"\n" +
          "        }";
    }

    expectedMapping += "\n    }\n" +
        "  }\n" +
        "}";

    assertEquals(expectedMapping, target.createIndex("", embeddingFeatures, features));
  }

  @Test
  public void testValidateWithinMappingLimit_Success() throws Exception {
    Index index = new Index("testIndex");
    int numFeatures = 5;

    when(vectorDatabase.getSchema(any())).thenReturn(new ArrayList<>(Collections.nCopies(defaultMappingSize - numFeatures, null)));

    // Call the method
    target.validateWithinMappingLimit(project, index, 0);

    // Verify that no exception is thrown
  }

  @Test(expected = FeaturestoreException.class)
  public void testValidateWithinMappingLimit_ExceedLimit_IndexExists() throws Exception {
    Index index = new Index("testIndex");
    int numFeatures = 5;

    doReturn(true).when(target).indexExist(eq(index.getName()));
    when(vectorDatabase.getSchema(any())).thenReturn(new ArrayList<>(Collections.nCopies(defaultMappingSize, new Field("f1", "int"))));
    doReturn(index.getName()).when(target).getProjectIndexName(any(), any());
    // Call the method
    target.validateWithinMappingLimit(project, index, numFeatures);

    // Verify that FeaturestoreException is thrown
  }

  @Test(expected = FeaturestoreException.class)
  public void testValidateWithinMappingLimit_ExceedLimit_IndexExists_SubField() throws Exception {
    Index index = new Index("testIndex");
    int numFeatures = 5;

    String jsonString = "{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}}";
    Gson gson = new Gson();
    // Define the type of the map using TypeToken
    Type type = new TypeToken<Map<String, Object>>(){}.getType();

    // Convert JSON string to Map
    Map<String, Object> opensearchType = gson.fromJson(jsonString, type);
    doReturn(true).when(target).indexExist(eq(index.getName()));
    when(vectorDatabase.getSchema(any())).thenReturn(new ArrayList<>(Collections.nCopies(defaultMappingSize/2, new Field("f1", opensearchType))));
    doReturn(index.getName()).when(target).getProjectIndexName(any(), any());
    // Call the method
    target.validateWithinMappingLimit(project, index, numFeatures);

    // Verify that FeaturestoreException is thrown
  }

  @Test(expected = FeaturestoreException.class)
  public void testValidateWithinMappingLimit_ExceedLimit_IndexNotExists() throws Exception {
    Index index = new Index("testIndex");
    int numFeatures = defaultMappingSize + 1;

    doReturn(false).when(target).indexExist(eq(index.getName()));
    doReturn(index.getName()).when(target).getProjectIndexName(any(), any());

    // Call the method
    target.validateWithinMappingLimit(project, index, numFeatures);

    // Verify that FeaturestoreException is thrown
  }

  @Test(expected = FeaturestoreException.class)
  public void testVerifyIndexName_IndexExists() throws FeaturestoreException {
    String name = "testIndex";
    String projectIndexName = "project_testIndex";

    // Mocking behavior to simulate index existence and non-default index
    doReturn(projectIndexName).when(target).getProjectIndexName(any(), any());
    doReturn(true).when(target).indexExist(any());
    doReturn(false).when(target).isDefaultVectorDbIndex(any(), any());

    // Call the method
    target.verifyIndexName(project, name);
  }

  @Test
  public void testVerifyIndexName_DefaultIndex() throws FeaturestoreException {
    String name = "testIndex";
    String projectIndexName = "project_testIndex";

    // Mocking behavior to simulate index existence and non-default index
    doReturn(projectIndexName).when(target).getProjectIndexName(any(), any());
    doReturn(true).when(target).indexExist(any());
    doReturn(true).when(target).isDefaultVectorDbIndex(any(), any());

    // Call the method
    target.verifyIndexName(project, name);
  }

  @Test
  public void testVerifyIndexName_IndexNotExists() throws FeaturestoreException {
    String name = "testIndex";
    String projectIndexName = "project_testIndex";

    // Mocking behavior to simulate index existence and non-default index
    doReturn(projectIndexName).when(target).getProjectIndexName(any(), any());
    doReturn(false).when(target).indexExist(any());
    doReturn(false).when(target).isDefaultVectorDbIndex(any(), any());

    // Call the method
    target.verifyIndexName(project, name);
  }

  @Test
  public void testVerifyIndexName_NullName() throws FeaturestoreException {
    // Mocking behavior to simulate index existence and non-default index
    doReturn("").when(target).getProjectIndexName(any(), any());
    doReturn(false).when(target).indexExist(any());
    doReturn(false).when(target).isDefaultVectorDbIndex(any(), any());

    // Call the method
    target.verifyIndexName(project, null);
    target.verifyIndexName(project, "");
  }

  @Test
  public void testGetProjectIndexName_NullOrEmptyName() throws FeaturestoreException {

    // Mocking behavior to simulate empty or null name
    doReturn("defaultIndex").when(target).getDefaultVectorDbIndex(any());

    // Call the method with empty name
    String emptyNameResult = target.getProjectIndexName(project, "");
    assertEquals("defaultIndex", emptyNameResult);

    // Call the method with null name
    String nullNameResult = target.getProjectIndexName(project, null);
    assertEquals("defaultIndex", nullNameResult);
  }

  @Test
  public void testGetProjectIndexName_NonEmptyName_NoPrefix() throws FeaturestoreException {
    String name = "testIndex";

    // Mocking behavior to simulate absence of prefix
    doReturn("prefix").when(target).getVectorDbIndexPrefix(any());

    // Call the method with empty name
    String result = target.getProjectIndexName(project, name);
    assertEquals("prefix_testIndex", result);
  }

  @Test
  public void testGetProjectIndexName_NonEmptyName_WithPrefix() throws FeaturestoreException {
    String name = "prefix_testIndex";

    // Mocking behavior to simulate presence of prefix
    doReturn("prefix").when(target).getVectorDbIndexPrefix(any());

    // Call the method
    String result = target.getProjectIndexName(project, name);

    // Verify the result
    assertEquals("prefix_testIndex", result);
  }
}