/*
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
 */

package io.hops.hopsworks.common.proxies;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.proxies.views.JWTRequest;
import io.hops.hopsworks.common.proxies.views.JWTResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JWTProxy {
  private final static Logger LOG = Logger.getLogger(JWTProxy.class.getName());
  private ResponseHandler<JWTResponse> JWT_RESPONSE_HANDLER ;
  private ObjectMapper objectMapper;
  
  private final static String JWT_RESOURCE_PATH = "/hopsworks-api/api/jwt";
  private final static String JWT_RENEW_PATH = JWT_RESOURCE_PATH + "/renew";
  
  @EJB
  private HttpClient client;
  
  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    
    JWT_RESPONSE_HANDLER = new JWTHTTPResponseHandler();
  }
  
  public JWTResponse renewToken(String token) throws IOException {
    return renewToken(token, null, null);
  }
  
  public JWTResponse renewToken(String token, Date notBefore, Date expiresOn) throws IOException {
    JWTRequest request = new JWTRequest(token, notBefore, expiresOn);
    String requestJSON = objectMapper.writeValueAsString(request);
    LOG.info("JWT Request is: " + requestJSON);
    HttpPost httpRequest = new HttpPost(JWT_RENEW_PATH);
    httpRequest.setEntity(new StringEntity(requestJSON));
    httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpRequest.setHeader(HttpHeaders.ACCEPT, "application/json");
    
    // TODO(Antonis) This should be a Service JWT but for the moment it is OK
    client.setAuthorizationHeader(httpRequest, token);
    
    return client.execute(httpRequest, JWT_RESPONSE_HANDLER);
  }
  
  public class JWTHTTPResponseHandler implements ResponseHandler<JWTResponse> {
  
    @Override
    public JWTResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      if (status != 200) {
        throw new ClientProtocolException(response.getStatusLine().getReasonPhrase());
      }
      String responseJSON = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      return objectMapper.readValue(responseJSON, JWTResponse.class);
    }
  }
}
