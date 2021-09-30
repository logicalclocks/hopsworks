/*
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

package io.hops.hopsworks.common.serving.inference;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import java.nio.charset.StandardCharsets;

public class ServingInferenceUtilsTest {
  
  private ServingInferenceUtils servingInferenceUtils = new ServingInferenceUtils();
  
  @Test
  public void testBuildInferenceRequest() throws Exception {
    JSONObject json = new JSONObject();
    json.put("instances", new String[]{"∮ E⋅da = Q", "åäö", "��", "\u07FF"});

    HttpPost request = servingInferenceUtils.buildInferenceRequest(
      "localhost", 1234, "/v1/models/test:predict", json.toString());
  
    ContentType contentType = ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), StandardCharsets.UTF_8);
    String entityContentType = request.getEntity().getContentType().getValue();
    Assert.assertEquals(entityContentType, contentType.toString());
  }
}