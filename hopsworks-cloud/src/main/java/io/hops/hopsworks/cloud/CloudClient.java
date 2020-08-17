/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class CloudClient {
  private final static Logger LOGGER = Logger.getLogger(CloudClient.class.getName());

  @EJB
  private Settings settings;

  @EJB
  private HttpClient httpClient;

  public void notifyToSendEmail(String userEmail, String userName,
          AuthController.CredentialsResetToken resetToken) throws UserException {
    JSONObject json = new JSONObject();
    json.put("email", userEmail);
    json.put("token", resetToken.getToken());
    json.put("user", userName);
    json.put("validFor", resetToken.getValidity());

    JSONObject data = new JSONObject();
    data.put("data", json);

    URI recoverPasswordUrl = URI.create(settings.getCloudEventsEndPoint() + "/recoverpassword");

    HttpPost request = new HttpPost(recoverPasswordUrl);
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    request.setHeader("x-api-key", settings.getCloudEventsEndPointAPIKey());
    try {
      request.setEntity(new StringEntity(data.toString()));
    } catch (UnsupportedEncodingException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL, Level.SEVERE, null,
              ex.getMessage(), ex);
    }

    try {
      HttpHost host = new HttpHost(recoverPasswordUrl.getHost(),
              recoverPasswordUrl.getPort(), recoverPasswordUrl.getScheme());
      int statusCode = httpClient.execute(host, request, httpResponse -> {
        String responseStr = EntityUtils.toString(httpResponse.getEntity());
        LOGGER.log(Level.INFO, responseStr);
        return httpResponse.getStatusLine().getStatusCode();
      });

      if(statusCode != 200){
        throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL,
                Level.SEVERE);
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL, Level.SEVERE, null,
              ex.getMessage(), ex);
    }
  }

  public String sendHeartbeat() throws IOException {
    if(settings.getCloudEventsEndPoint().equals("")){
      throw new IOException("Failed to send heartbeat endpoint not set");
    }
    URI heartBeatUrl = URI.create(settings.getCloudEventsEndPoint() + "/heartbeat");

    HttpGet request = new HttpGet(heartBeatUrl);
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    request.setHeader("x-api-key", settings.getCloudEventsEndPointAPIKey());

    HttpHost host = new HttpHost(heartBeatUrl.getHost(),
            heartBeatUrl.getPort(), heartBeatUrl.getScheme());
    return httpClient.execute(host, request, httpResponse -> {
      if (httpResponse.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Failed to send heartbeat, return status: " + httpResponse.getStatusLine().toString());
      }
      String json = EntityUtils.toString(httpResponse.getEntity());
      LOGGER.log(Level.INFO, json);
      return json;
    });
  }

  public void sendStorageUsage(long bytes, long objects) throws IOException {
    JSONObject json = new JSONObject();
    json.put("bytes", bytes);
    json.put("objects", objects);

    JSONObject data = new JSONObject();
    data.put("data", json);

    URI storageUsageUrl = URI.create(settings.getCloudEventsEndPoint() + "/usage/storage");

    HttpPost request = new HttpPost(storageUsageUrl);
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    request.setHeader("x-api-key", settings.getCloudEventsEndPointAPIKey());
    request.setEntity(new StringEntity(data.toString()));

    HttpHost host = new HttpHost(storageUsageUrl.getHost(),
            storageUsageUrl.getPort(), storageUsageUrl.getScheme());
    int statusCode = httpClient.execute(host, request, httpResponse -> {
      String responseStr = EntityUtils.toString(httpResponse.getEntity());
      LOGGER.log(Level.INFO, responseStr);
      return httpResponse.getStatusLine().getStatusCode();
    });

    if(statusCode != 200){
      throw new IOException("Failed to send storage usage, return status: " + statusCode);
    }
  }
}
