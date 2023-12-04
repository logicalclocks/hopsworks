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

package io.hops.hopsworks.vectordb;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContexts;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class VectorDatabaseFactory {

  public static VectorDatabase getOpensearchDatabase(String host, String user, String password, String certPath,
      String trustStorePassword) throws IOException {
    SSLContext sslCtx = null;
    Path trustStore = Paths.get(certPath);
    char[] trustStorePw = null;
    if (trustStorePassword != null) {
      trustStorePw =
          trustStorePassword.toCharArray();
    }
    try {
      sslCtx = SSLContexts.custom()
          .loadTrustMaterial(trustStore.toFile(), trustStorePw)
          .build();
    } catch (GeneralSecurityException | IOException e) {
      throw new IOException("Failed to load ssl context.");
    }
    CredentialsProvider credentialsProvider =
        new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(user, password));
    final SSLContext finalSslCtx = sslCtx;
    final CredentialsProvider finalCredentialsProvider = credentialsProvider;

    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(HttpHost.create(host))
            .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
              httpAsyncClientBuilder.setDefaultIOReactorConfig(
                  IOReactorConfig.custom().setIoThreadCount(5).build());
              return httpAsyncClientBuilder
                  .setSSLContext(finalSslCtx)
                  .setDefaultCredentialsProvider(finalCredentialsProvider)
                  .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }));
    return new OpensearchVectorDatabase(client);
  }

  public static VectorDatabase getOpensearchDatabase(RestHighLevelClient client) {
    return new OpensearchVectorDatabase(client);
  }
}

