/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.google.common.base.Strings;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import io.hops.hopsworks.common.remote.oauth.OpenIdConstant;
import io.hops.hopsworks.common.remote.oauth.OpenIdProviderConfig;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.restutils.RESTCodes;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class OAuthProviderCache {
  
  private final static Logger LOGGER = Logger.getLogger(OAuthProviderCache.class.getName());
  private final static int CONNECTION_TIMEOUT = 5000;
  
  //Each node in HA setup can have its own cache
  private final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
  private final Cache<String, OpenIdProviderConfig> providerConfigCache =
    cacheManager.createCache("providerConfigCache",
      CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, OpenIdProviderConfig.class,
          ResourcePoolsBuilder.heap(10))
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1L)))
        .build());
  private final Cache<String, JSONObject> providerJWKCache = cacheManager.createCache("providerJWKCache",
    CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, JSONObject.class,
        ResourcePoolsBuilder.heap(20))
      .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1L)))
      .build());
  
  @PreDestroy
  private void destroy() {
    providerConfigCache.clear();
    providerJWKCache.clear();
  }
  
  public void removeFromCache(String clientId) {
    providerConfigCache.remove(clientId);
  }
  
  /**
   *
   * @param kid
   * @param providerConfig
   * @return
   * @throws ParseException
   * @throws IOException
   * @throws URISyntaxException
   */
  public JSONObject getJWK(String kid, OpenIdProviderConfig providerConfig, boolean invalidateCache)
    throws ParseException, IOException, URISyntaxException {
    return getJWKFromCacheOrURI(kid, providerConfig, invalidateCache);
  }
  
  private JSONObject getJWKFromCacheOrURI(String kid, OpenIdProviderConfig providerConfig, boolean invalidateCache)
    throws ParseException, IOException, URISyntaxException {
    JSONObject json = providerJWKCache.get(providerConfig.getJwkSetURI());
    if (json == null || !json.get("kid").equals(kid) || invalidateCache) {
      json = getJWKFromURI(kid, providerConfig);
      providerJWKCache.put(providerConfig.getJwkSetURI(), json);
    }
    return json;
  }
  
  /**
   *
   * @param client
   * @return
   */
  public OpenIdProviderConfig getProviderConfig(OauthClient client, boolean invalidateCache)
    throws RemoteAuthException {
    OpenIdProviderConfig providerConfig;
    if (client.getProviderMetadataEndpointSupported()) {
      try {
        providerConfig = getProviderConfigFromCacheOrURI(client, invalidateCache);
        if (!Strings.isNullOrEmpty(client.getEndSessionEndpoint())) {
          providerConfig.setEndSessionEndpoint(client.getEndSessionEndpoint());
          providerConfig.setLogoutRedirectParam(client.getLogoutRedirectParam());
        }
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Could not get provider configuration from URI {0}.", client.getProviderURI());
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.WRONG_CONFIG, Level.FINE, "Wrong configuration: " +
          "Could not get provider configuration.",
          "Could not get provider configuration from URI " + client.getProviderURI() + ". " + e.getMessage());
      }
    } else {
      providerConfig = new OpenIdProviderConfig(client);
      
    }
    return providerConfig;
  }
  
  private OpenIdProviderConfig getProviderConfigFromCacheOrURI(OauthClient client, boolean invalidateCache)
    throws IOException, URISyntaxException {
    OpenIdProviderConfig providerConfig = providerConfigCache.get(client.getClientId());
    if (providerConfig == null || invalidateCache) {
      providerConfig = getProviderConfigFromURI(client.getProviderURI());
      providerConfigCache.put(client.getClientId(), providerConfig);
    }
    return providerConfig;
  }
  /**
   *
   * @param providerURI
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public OpenIdProviderConfig getProviderConfigFromURI(String providerURI) throws IOException, URISyntaxException {
    URI issuerURI = new URI(providerURI);
    String wellKnownPath = removeTrailingSlash(issuerURI.getPath()) + OpenIdConstant.OPENID_CONFIGURATION_URL;
    URL providerConfigurationURL = issuerURI.resolve(wellKnownPath).toURL();
    HttpURLConnection conn = (HttpURLConnection) providerConfigurationURL.openConnection();
    conn.setConnectTimeout(CONNECTION_TIMEOUT);
    conn.setReadTimeout(CONNECTION_TIMEOUT);
    conn.connect();
    OpenIdProviderConfig openIdProviderConfig;
    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
      InputStream stream = conn.getInputStream();
      String providerInfo;
      try (Scanner s = new Scanner(stream)) {
        providerInfo = s.useDelimiter("\\A").hasNext() ? s.next() : "";
      }
      JsonObject document = readJson(providerInfo);
      openIdProviderConfig = new OpenIdProviderConfig(document);
    } else {
      LOGGER.log(Level.SEVERE, "Error getting provider config. Response Code = {0}", conn.getResponseCode());
      throw new IOException("Error getting provider config. Response Code = " + conn.getResponseCode());
    }
    return openIdProviderConfig;
  }
  
  private String removeTrailingSlash(String path) {
    if (path == null) {
      return "";
    }
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }
  
  private JsonObject readJson(String jsonStr) {
    JsonReader jsonReader = Json.createReader(new StringReader(jsonStr));
    JsonObject jsonObject = jsonReader.readObject();
    jsonReader.close();
    return jsonObject;
  }
  
  public JSONObject getJWKFromURI(String kid, OpenIdProviderConfig providerConfig) throws URISyntaxException,
    IOException, ParseException {
    URI providerURI = new URI(providerConfig.getJwkSetURI());
    URL providerConfigurationURL = providerURI.toURL();
    HttpURLConnection conn = (HttpURLConnection) providerConfigurationURL.openConnection();
    conn.setConnectTimeout(CONNECTION_TIMEOUT);
    conn.setReadTimeout(CONNECTION_TIMEOUT);
    conn.connect();
    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
      InputStream is = providerURI.toURL().openStream();
      StringBuilder sb = new StringBuilder();
      try (Scanner scanner = new Scanner(is)) {
        while (scanner.hasNext()) {
          sb.append(scanner.next());
        }
      }
      String jsonString = sb.toString();
      JSONObject json = JSONObjectUtils.parse(jsonString);
      JSONArray keys = (JSONArray) json.get("keys");
      for (Object key : keys) {
        JSONObject jsonObject = (JSONObject) key;
        if (jsonObject.get("kid").equals(kid)) {
          return jsonObject;
        }
      }
    } else {
      LOGGER.log(Level.SEVERE, "Error getting JSON Web Key Set. Response Code = {0}", conn.getResponseCode());
      throw new IOException("Error getting JSON Web Key Set. Response Code = " + conn.getResponseCode());
    }
    throw new IllegalStateException("Failed to get JSON Web Key for the given key id = " + kid);
  }
}
