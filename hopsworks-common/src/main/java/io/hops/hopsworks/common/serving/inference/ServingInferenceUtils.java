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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServingInferenceUtils {
  
  /**
   * Builds an inference request to be sent to a model deployment. All implementations of ServingInferenceController
   * must use this method to build the inference request.
   *
   * @param host host of the deployment where the model server is running
   * @param port port of the deployment where the model server is listening to
   * @param path inference path
   * @param json request payload
   * @return the inference request
   * @throws URISyntaxException
   */
  public HttpPost buildInferenceRequest(String host, int port, String path, String json) throws URISyntaxException {
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(host)
      .setPort(port)
      .setPath(path)
      .build();
    
    ContentType contentType = ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), StandardCharsets.UTF_8);
    StringEntity payload = new StringEntity(json, contentType);
    
    HttpPost request = new HttpPost(uri);
    request.addHeader("content-type", contentType.toString());
    request.setEntity(payload);
    
    return request;
  }
}
