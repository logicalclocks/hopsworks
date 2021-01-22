/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.remote.oauth.CodeChallengeMethod;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OAuthClientDTO extends RestDTO<OAuthClientDTO> {
  
  private String clientId;
  private String providerLogoUri;
  private String providerURI;
  private String redirectUri;
  private String authorizationEndpoint;
  private String providerName;
  private String providerDisplayName;
  private boolean offlineAccess;
  private boolean codeChallenge;
  private CodeChallengeMethod codeChallengeMethod;
  private boolean verifyEmail;
  
  public String getClientId() {
    return clientId;
  }
  
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
  
  public String getProviderLogoUri() {
    return providerLogoUri;
  }
  
  public void setProviderLogoUri(String providerLogoUri) {
    this.providerLogoUri = providerLogoUri;
  }
  
  public String getProviderURI() {
    return providerURI;
  }
  
  public void setProviderURI(String providerURI) {
    this.providerURI = providerURI;
  }
  
  public String getRedirectUri() {
    return redirectUri;
  }
  
  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }
  
  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }
  
  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }
  
  public String getProviderName() {
    return providerName;
  }
  
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }
  
  public String getProviderDisplayName() {
    return providerDisplayName;
  }
  
  public void setProviderDisplayName(String authServerDisplayname) {
    this.providerDisplayName = authServerDisplayname;
  }
  
  public boolean isOfflineAccess() {
    return offlineAccess;
  }
  
  public void setOfflineAccess(boolean offlineAccess) {
    this.offlineAccess = offlineAccess;
  }
  
  public boolean isCodeChallenge() {
    return codeChallenge;
  }
  
  public void setCodeChallenge(boolean codeChallenge) {
    this.codeChallenge = codeChallenge;
  }
  
  public CodeChallengeMethod getCodeChallengeMethod() {
    return codeChallengeMethod;
  }
  
  public void setCodeChallengeMethod(CodeChallengeMethod codeChallengeMethod) {
    this.codeChallengeMethod = codeChallengeMethod;
  }
  
  public boolean isVerifyEmail() {
    return verifyEmail;
  }
  
  public void setVerifyEmail(boolean verifyEmail) {
    this.verifyEmail = verifyEmail;
  }
}
