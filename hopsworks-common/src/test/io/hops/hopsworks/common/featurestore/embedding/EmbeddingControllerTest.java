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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.EmbeddingFeature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.spy;

public class EmbeddingControllerTest {

  private EmbeddingController embeddingController;

  @Before
  public void setup() {
    embeddingController = spy(new EmbeddingController());
  }

  @Test
  public void testCreateIndex() {
    List<EmbeddingFeature> features = new ArrayList<>();
    features.add(new EmbeddingFeature(null, "vector", 512, "l2"));
    features.add(new EmbeddingFeature(null, "vector2", 128, "l2"));
    Assert.assertEquals(
        "{\n"
            + "  \"settings\": {\n"
            + "    \"index\": {\n"
            + "      \"knn\": \"true\",\n"
            + "      \"knn.algo_param.ef_search\": 512\n"
            + "    }\n"
            + "  },\n"
            + "  \"mappings\": {\n"
            + "    \"properties\": {\n"
            + "        \"vector\": {\n"
            + "          \"type\": \"knn_vector\",\n"
            + "          \"dimension\": 512\n"
            + "        },\n"
            + "        \"vector2\": {\n"
            + "          \"type\": \"knn_vector\",\n"
            + "          \"dimension\": 128\n"
            + "        }\n"
            + "    }\n"
            + "  }\n"
            + "}",
        embeddingController.createIndex("", features));
  }
}