/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatRequest;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponse;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponseHttpMessage;
import io.hops.hopsworks.cloud.dao.heartbeat.Version;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandTypeDeserializer;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandsDeserializer;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.RemoveNodesCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.RemoveNodesCommandSerializer;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
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
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class CloudClient {
  private final static Logger LOGGER = Logger.getLogger(CloudClient.class.getName());
  private static final Gson GSON_SERIALIZER = new GsonBuilder()
          // Add your new command Serializer here
          .registerTypeAdapter(RemoveNodesCommand.class, new RemoveNodesCommandSerializer())

          // Add your new command type here
          .registerTypeAdapter(CloudCommand.class, new CloudCommandsDeserializer())
          .registerTypeAdapter(CloudCommandType.class, new CloudCommandTypeDeserializer())
          .create();

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

  public HeartbeatResponse sendHeartbeat(HeartbeatRequest heartbeatRequest) throws IOException {
    if (settings.getCloudEventsEndPoint().equals("")) {
      throw new IOException("Failed to send heartbeat endpoint not set");
    }
    heartbeatRequest.setVersion(Version.CURRENT);
    URI heartBeatUrl = URI.create(settings.getCloudEventsEndPoint() + "/heartbeat");

    HttpPost request = new HttpPost(heartBeatUrl);
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    request.setHeader("x-api-key", settings.getCloudEventsEndPointAPIKey());
    String heartbeatRequestJson = GSON_SERIALIZER.toJson(heartbeatRequest);
    LOGGER.log(Level.FINE, "HeartbeatRequest: " + heartbeatRequestJson);
    request.setEntity(new StringEntity(heartbeatRequestJson));

    HttpHost host = new HttpHost(heartBeatUrl.getHost(),
        heartBeatUrl.getPort(), heartBeatUrl.getScheme());
    return httpClient.execute(host, request, httpResponse -> {
      if (httpResponse.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Failed to send heartbeat, return status: " + httpResponse.getStatusLine().toString());
      }
      String json = EntityUtils.toString(httpResponse.getEntity(), Charset.defaultCharset());
      LOGGER.log(Level.FINE, "HeartbeatResponse: " + json);
      HeartbeatResponseHttpMessage message = GSON_SERIALIZER.fromJson(json, HeartbeatResponseHttpMessage.class);
      if (message.getPayload().getVersion() == null
              || !message.getPayload().getVersion().equals(Version.CURRENT)) {
        throw new IOException("Empty or unsupported CloudHeartbeat version. Current version is " + Version.CURRENT
                + " and received " + message.getPayload().getVersion());
      }
      return message.getPayload();
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
