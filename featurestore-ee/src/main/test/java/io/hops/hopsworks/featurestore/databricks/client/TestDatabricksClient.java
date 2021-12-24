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

package io.hops.hopsworks.featurestore.databricks.client;

import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Matchers.anyObject;

public class TestDatabricksClient {

    private DatabricksClient databricksClient;

    private HttpClient httpClient;

    @Before
    public void setup() throws IOException {
        httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(anyObject(),anyObject(),anyObject()))
                .thenReturn("");

        databricksClient = new DatabricksClient(httpClient);
    }

    @Test
    public void testGetNotebook() throws Exception {
        //arrange
        String path = "2";
        String format = "HTML";
        String uri = "/api/2.0/workspace/export?path="+path+"&direct_download=true&format="+format;

        //act
        databricksClient.getNotebook("1", "1", path, format);

        //assert
        Mockito.verify(httpClient).execute(anyObject(), Matchers.argThat(new HttpRequestMatcher(uri)),anyObject());
    }

    class HttpRequestMatcher extends ArgumentMatcher<HttpRequest> {

        private String uri;

        public HttpRequestMatcher(String uri){
            this.uri = uri;
        }

        @Override
        public boolean matches(Object httpRequestObject) {
            HttpRequest httpRequest = (HttpRequest) httpRequestObject;
            return httpRequest.getRequestLine().getUri().equals(uri);
        }
    }
}