/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.google.common.base.Strings;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import io.hops.hadoop.shaded.javax.ws.rs.core.UriBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.remote.oauth.OpenIdConstant.CLIENT_ID;

public class LogoutReq {
  private final static Logger LOGGER = Logger.getLogger(LogoutReq.class.getName());
  private final URI uri;
  private final ClientID clientID;
  private JWT idTokenHint;
  private final URI postLogoutRedirectURI;
  private State state;
  private String postLogoutRedirectParam;
  
  public LogoutReq(URI uri, ClientID clientID, URI postLogoutRedirectURI) {
    this.uri = uri;
    this.clientID = clientID;
    this.postLogoutRedirectURI = postLogoutRedirectURI;
  }
  
  public LogoutReq(URI uri, ClientID clientID, JWT idTokenHint, URI postLogoutRedirectURI, State state,
    String postLogoutRedirectParam) {
    this.uri = uri;
    this.clientID = clientID;
    this.idTokenHint = idTokenHint;
    this.postLogoutRedirectURI = postLogoutRedirectURI;
    this.state = state;
    this.postLogoutRedirectParam = postLogoutRedirectParam;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public ClientID getClientID() {
    return clientID;
  }
  
  public JWT getIdTokenHint() {
    return idTokenHint;
  }
  
  public void setIdTokenHint(JWT idTokenHint) {
    this.idTokenHint = idTokenHint;
  }
  
  public URI getPostLogoutRedirectURI() {
    return postLogoutRedirectURI;
  }
  
  public State getState() {
    return state;
  }
  
  public void setState(State state) {
    this.state = state;
  }
  
  public String getPostLogoutRedirectParam() {
    return postLogoutRedirectParam;
  }
  
  public void setPostLogoutRedirectParam(String postLogoutRedirectParam) {
    this.postLogoutRedirectParam = postLogoutRedirectParam;
  }
  
  public URI toURI() {
    LogoutRequest logoutRequest = new LogoutRequest(this.uri, this.idTokenHint,
      Strings.isNullOrEmpty(this.postLogoutRedirectParam)? this.postLogoutRedirectURI : null, this.state);
    UriBuilder logoutURIWithParams = UriBuilder.fromUri(logoutRequest.toURI())
      .queryParam(CLIENT_ID, this.clientID.getValue());
    if (!Strings.isNullOrEmpty(this.postLogoutRedirectParam)) {
      try {
        logoutURIWithParams.queryParam(this.postLogoutRedirectParam,
          URLEncoder.encode(this.postLogoutRedirectURI.toString(), "utf-8"));
      } catch (UnsupportedEncodingException e) {
        LOGGER.log(Level.WARNING, "Failed to encode post logout redirect URI {0}", e.getMessage());
      }
    }
    return logoutURIWithParams.build();
  }
  
  public static class Builder {
    private final URI uri;
    private final ClientID clientID;
    private JWT idTokenHint;
    private final URI postLogoutRedirectURI;
    private State state;
    private String postLogoutRedirectParam;
  
    public Builder(URI uri, ClientID clientID, URI postLogoutRedirectURI) {
      this.uri = uri;
      this.clientID = clientID;
      this.postLogoutRedirectURI = postLogoutRedirectURI;
    }
  
    public LogoutReq.Builder idTokenHint(JWT idTokenHint) {
      this.idTokenHint = idTokenHint;
      return this;
    }
  
    public LogoutReq.Builder state(State state) {
      this.state = state;
      return this;
    }
  
    public LogoutReq.Builder postLogoutRedirectParam(String postLogoutRedirectParam) {
      this.postLogoutRedirectParam = postLogoutRedirectParam;
      return this;
    }
  
    public LogoutReq build() {
      return new LogoutReq(this.uri, this.clientID, this.idTokenHint, this.postLogoutRedirectURI, this.state,
        this.postLogoutRedirectParam);
    }
  }
}
