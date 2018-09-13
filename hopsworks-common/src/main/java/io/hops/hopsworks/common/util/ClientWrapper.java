/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

public class ClientWrapper<T extends Object> {

  private Client client;
  private final Class<T> respContentClass;
  private String target;
  private String path;
  private Entity payload;
  private String mediaType = MediaType.APPLICATION_JSON;

  private ClientWrapper(Client client, Class<T> respContentClass) {
    this.client = client;
    this.respContentClass = respContentClass;
    this.payload = Entity.entity("", mediaType);
  }

  public ClientWrapper setTarget(String target) {
    this.target = target;
    return this;
  }

  public ClientWrapper setPath(String path) {
    this.path = path;
    return this;
  }

  public ClientWrapper setMediaType(String mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public ClientWrapper setPayload(Object jsonObject) {
    this.payload = Entity.entity(jsonObject, mediaType);
    return this;
  }

  public T doGet() {
    performSanityCheck();
    try {
      WebTarget webTarget = client.target(target).path(path);
      Response response = webTarget.request(mediaType).get();
      return getResponse(response);
    } catch (ProcessingException ex) {
      throw new IllegalStateException(ex.getMessage());
    } finally {
      if (client != null) {
        client.close();
        client = null;
      }
    }
  }

  public List<T> doGetGenericType() {
    performSanityCheck();
    try {
      WebTarget webTarget = client.target(target).path(path);
      Response response = webTarget.request(mediaType).get();
      ParameterizedType parameterizedGenericType = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return new Type[]{respContentClass};
        }

        @Override
        public Type getRawType() {
          return List.class;
        }

        @Override
        public Type getOwnerType() {
          return List.class;
        }
      };
      GenericType<List<T>> type = new GenericType<List<T>>(parameterizedGenericType) {
      };
      Family status = response.getStatusInfo().getFamily();
      try {
        if (status == Family.INFORMATIONAL || status == Family.SUCCESSFUL) {
          List<T> content = response.readEntity(type);
          return content;
        } else {
          JsonResponse jsonRes = response.readEntity(JsonResponse.class);
          throw new IllegalStateException(jsonRes.getErrorMsg());
        }
      } catch (ProcessingException e) {
        throw new IllegalStateException(e.getMessage());
      }
    } finally {
      if (client != null) {
        client.close();
        client = null;
      }
    }
  }

  public T doPost() {
    performSanityCheck();
    try {
      WebTarget webTarget = client.target(target).path(path);
      Response response = webTarget.request(mediaType).post(payload);
      return getResponse(response);
    } catch (ProcessingException ex) {
      throw new IllegalStateException(ex.getMessage());
    } finally {
      if (client != null) {
        client.close();
        client = null;
      }
    }
  }

  public T doPut() {
    performSanityCheck();
    try {
      WebTarget webTarget = client.target(target).path(path);
      Response response = webTarget.request(mediaType).put(payload);
      return getResponse(response);
    } catch (ProcessingException ex) {
      throw new IllegalStateException(ex.getMessage());
    } finally {
      if (client != null) {
        client.close();
        client = null;
      }
    }
  }

  public T doDelete() {
    performSanityCheck();
    try {
      WebTarget webTarget = client.target(target).path(path);
      Response response = webTarget.request(mediaType).delete();
      return getResponse(response);
    } catch (ProcessingException ex) {
      throw new IllegalStateException(ex.getMessage());
    } finally {
      if (client != null) {
        client.close();
        client = null;
      }
    }
  }

  private T getResponse(Response response) {
    Family statusFamily = response.getStatusInfo().getFamily();
    if (response.getMediaType().getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype())) {
      try {
        if (statusFamily == Family.INFORMATIONAL || statusFamily == Family.SUCCESSFUL) {
          T content = response.readEntity(respContentClass);
          return content;
        } else {
          JsonResponse jsonRes = response.readEntity(JsonResponse.class);
          throw new IllegalStateException(jsonRes.getErrorMsg());
        }
      } catch (ProcessingException e) {
        throw new IllegalStateException(e.getMessage());
      }
    } else {
      throw new IllegalStateException("Cannot Connect To Server.");
    }

  }

  private void performSanityCheck() {
    if (client == null) {
      throw new IllegalStateException("Client not created.");
    }

    if (target == null || target.isEmpty()) {
      throw new IllegalStateException("Target not set.");
    }

    if (path == null || path.isEmpty()) {
      throw new IllegalStateException("Path not set.");
    }
  }

  public String getFullPath() {
    return this.target + "/" + this.path;
  }

  public static <T> ClientWrapper<T> httpsInstance(KeyStore keystore, KeyStore truststore,
    String password, HostnameVerifier hostnameVerifier, Class<T> resultClass) {
    Client client = ClientBuilder.newBuilder().trustStore(truststore).keyStore(keystore, password.toCharArray()).
      hostnameVerifier(hostnameVerifier).build();
    return new ClientWrapper(client, resultClass);
  }

  public static <T> ClientWrapper<T> httpsInstance(Class<T> resultClass) {
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts(), new java.security.SecureRandom());
      Client client = ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier(acceptAnyHost()).build();
      return new ClientWrapper(client, resultClass);
    } catch (NoSuchAlgorithmException | KeyManagementException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static <T> ClientWrapper httpInstance(Class<T> resultClass) {
    Client client = ClientBuilder.newClient();
    return new ClientWrapper(client, resultClass);
  }

  private static TrustManager[] trustAllCerts() {
    return new TrustManager[]{
      new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
    };
  }

  private static HostnameVerifier acceptAnyHost() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String string, SSLSession ssls) {
        return true;
      }
    };
  }
}
