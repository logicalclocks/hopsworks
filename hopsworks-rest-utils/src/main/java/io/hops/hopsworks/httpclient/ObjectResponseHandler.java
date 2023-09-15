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

package io.hops.hopsworks.httpclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class ObjectResponseHandler<T> implements ResponseHandler<T> {

  private Class<T> cls;
  private ObjectMapper objectMapper;

  public ObjectResponseHandler(Class<T> cls, ObjectMapper objectMapper) {
    this.cls = cls;
    this.objectMapper = objectMapper;
  }

  @Override
  public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
    String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
    if (response.getStatusLine().getStatusCode() / 100 == 2) {
      return objectMapper.readValue(responseJson, cls);
    } else if (response.getStatusLine().getStatusCode() / 100 == 5) {
      throw new IOException(responseJson);
    } else {
      throw new NotRetryableClientProtocolException(responseJson);
    }
  }
}