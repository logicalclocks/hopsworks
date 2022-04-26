/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.persistence.entity.remote.oauth.CodeChallengeMethod;
import io.hops.hopsworks.remote.user.GroupMapping;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@XmlRootElement
public class OAuthClientDTO extends RestDTO<OAuthClientDTO> {
  
  private Integer id;
  private String clientId;
  private String clientSecret;
  private String providerLogoUri;
  private String providerUri;
  private String redirectUri;
  private String authorizationEndpoint;
  private String providerName;
  private String providerDisplayName;
  private String tokenEndpoint;
  private String userInfoEndpoint;
  private String endSessionEndpoint;
  private String logoutRedirectParam;
  private String jwksURI;
  private CodeChallengeMethod codeChallengeMethod;
  private Boolean offlineAccess;
  private Boolean codeChallenge;
  private Boolean verifyEmail;
  private Boolean providerMetadataEndpointSupported;
  private Boolean activateUser;
  private GroupMapping groupMapping;
  private String groupMappings;
  private Boolean needConsent;
  private Boolean registrationDisabled;
  private String callbackURI;
  private Boolean rejectRemoteNoGroup;
  private String managedCloudRedirectUri;
  
  public OAuthClientDTO() {
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getClientId() {
    return clientId;
  }
  
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
  
  public String getClientSecret() {
    return clientSecret;
  }
  
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
  
  public String getProviderLogoUri() {
    return providerLogoUri;
  }
  
  public void setProviderLogoUri(String providerLogoUri) {
    this.providerLogoUri = providerLogoUri;
  }
  
  public String getProviderUri() {
    return providerUri;
  }
  
  public void setProviderUri(String providerUri) {
    this.providerUri = providerUri;
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
  
  public void setProviderDisplayName(String providerDisplayName) {
    this.providerDisplayName = providerDisplayName;
  }
  
  public String getTokenEndpoint() {
    return tokenEndpoint;
  }
  
  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }
  
  public String getUserInfoEndpoint() {
    return userInfoEndpoint;
  }
  
  public void setUserInfoEndpoint(String userInfoEndpoint) {
    this.userInfoEndpoint = userInfoEndpoint;
  }
  
  public String getEndSessionEndpoint() {
    return endSessionEndpoint;
  }
  
  public void setEndSessionEndpoint(String endSessionEndpoint) {
    this.endSessionEndpoint = endSessionEndpoint;
  }
  
  public String getLogoutRedirectParam() {
    return logoutRedirectParam;
  }
  
  public void setLogoutRedirectParam(String logoutRedirectParam) {
    this.logoutRedirectParam = logoutRedirectParam;
  }
  
  public String getJwksURI() {
    return jwksURI;
  }
  
  public void setJwksURI(String jwksURI) {
    this.jwksURI = jwksURI;
  }
  
  public CodeChallengeMethod getCodeChallengeMethod() {
    return codeChallengeMethod;
  }
  
  public void setCodeChallengeMethod(CodeChallengeMethod codeChallengeMethod) {
    this.codeChallengeMethod = codeChallengeMethod;
  }
  
  public Boolean getOfflineAccess() {
    return offlineAccess;
  }
  
  public void setOfflineAccess(Boolean offlineAccess) {
    this.offlineAccess = offlineAccess;
  }
  
  public Boolean getCodeChallenge() {
    return codeChallenge;
  }
  
  public void setCodeChallenge(Boolean codeChallenge) {
    this.codeChallenge = codeChallenge;
  }
  
  public Boolean getVerifyEmail() {
    return verifyEmail;
  }
  
  public void setVerifyEmail(Boolean verifyEmail) {
    this.verifyEmail = verifyEmail;
  }
  
  public Boolean getProviderMetadataEndpointSupported() {
    return providerMetadataEndpointSupported;
  }
  
  public void setProviderMetadataEndpointSupported(Boolean providerMetadataEndpointSupported) {
    this.providerMetadataEndpointSupported = providerMetadataEndpointSupported;
  }
  
  public Boolean getRegistrationDisabled() {
    return registrationDisabled;
  }
  
  public void setRegistrationDisabled(Boolean registrationDisabled) {
    this.registrationDisabled = registrationDisabled;
  }
  
  public GroupMapping getGroupMapping() {
    return groupMapping;
  }
  
  public void setGroupMapping(GroupMapping groupMapping) {
    this.groupMapping = groupMapping;
  }
  
  public String getGroupMappings() {
    return groupMappings;
  }
  
  public void setGroupMappings(String groupMappings) {
    this.groupMappings = groupMappings;
  }
  
  public Boolean getNeedConsent() {
    return needConsent;
  }
  
  public void setNeedConsent(Boolean needConsent) {
    this.needConsent = needConsent;
  }
  
  public Boolean getActivateUser() {
    return activateUser;
  }
  
  public void setActivateUser(Boolean activateUser) {
    this.activateUser = activateUser;
  }
  
  public String getCallbackURI() {
    return callbackURI;
  }
  
  public void setCallbackURI(String callbackURI) {
    this.callbackURI = callbackURI;
  }
  
  public Boolean getRejectRemoteNoGroup() {
    return rejectRemoteNoGroup;
  }
  
  public void setRejectRemoteNoGroup(Boolean rejectRemoteNoGroup) {
    this.rejectRemoteNoGroup = rejectRemoteNoGroup;
  }
  
  public String getManagedCloudRedirectUri() {
    return managedCloudRedirectUri;
  }
  
  public void setManagedCloudRedirectUri(String managedCloudRedirectUri) {
    this.managedCloudRedirectUri = managedCloudRedirectUri;
  }
  
  public void validate() throws RemoteAuthException {
    if (Strings.isNullOrEmpty(this.getClientId())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Client id not set.");
    }
    if (Strings.isNullOrEmpty(this.getClientSecret())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Client secret not set.");
    }
    if (Strings.isNullOrEmpty(this.getProviderName())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Provider name not set.");
    }
    if (Strings.isNullOrEmpty(this.getProviderDisplayName()) || this.getProviderDisplayName().trim().isEmpty()) {
      setProviderDisplayName(this.providerName);
    }
    if (Strings.isNullOrEmpty(this.getProviderUri())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Provider uri not set.");
    }
    validateURI(this.getProviderUri(), "Provider uri syntax exception.");
    if (!Strings.isNullOrEmpty(this.getProviderLogoUri())) {
      validateURI(this.getProviderLogoUri(), "Provider logo uri syntax exception.");
    }
  
    if (!fromBoolean(this.getProviderMetadataEndpointSupported())) {
      if (Strings.isNullOrEmpty(this.getAuthorizationEndpoint()) ||
        Strings.isNullOrEmpty(this.getTokenEndpoint()) ||
        Strings.isNullOrEmpty(this.getUserInfoEndpoint()) ||
        Strings.isNullOrEmpty(this.getJwksURI())) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
          "Failed to create client. Required field/s missing.");
      }
      validateURI(this.getAuthorizationEndpoint(), "Provider AuthorizationEndpoint uri syntax exception.");
      validateURI(this.getTokenEndpoint(), "Provider uri TokenEndpoint syntax exception.");
      validateURI(this.getUserInfoEndpoint(), "Provider UserInfoEndpoint uri syntax exception.");
      validateURI(this.getJwksURI(), "Provider JWKS uri syntax exception.");
    }
    if (!Strings.isNullOrEmpty(this.getEndSessionEndpoint())) {
      validateURI(this.getEndSessionEndpoint(), "Provider EndSessionEndpoint uri syntax exception.");
    }
  }
  
  private void validateURI(String uriStr, String errorMsg) throws RemoteAuthException {
    try {
      URI uri = new URI(uriStr);
      if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, errorMsg + " No " +
          "scheme provided.");
      }
      if (uri.getHost() == null || uri.getHost().isEmpty()) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, errorMsg + " No " +
          "host provided.");
      }
    } catch (URISyntaxException e) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, errorMsg);
    }
  }
  
  public boolean fromBoolean(Boolean val) {
    return val != null? val : false;
  }
  
  @Override
  public String toString() {
    return "OAuthClientDTO{" +
      "id=" + id +
      ", clientId='" + clientId + '\'' +
      ", providerLogoUri='" + providerLogoUri + '\'' +
      ", providerUri='" + providerUri + '\'' +
      ", redirectUri='" + redirectUri + '\'' +
      ", authorizationEndpoint='" + authorizationEndpoint + '\'' +
      ", providerName='" + providerName + '\'' +
      ", providerDisplayName='" + providerDisplayName + '\'' +
      ", tokenEndpoint='" + tokenEndpoint + '\'' +
      ", userInfoEndpoint='" + userInfoEndpoint + '\'' +
      ", endSessionEndpoint='" + endSessionEndpoint + '\'' +
      ", logoutRedirectParam='" + logoutRedirectParam + '\'' +
      ", jwksURI='" + jwksURI + '\'' +
      ", codeChallengeMethod=" + codeChallengeMethod +
      ", offlineAccess=" + offlineAccess +
      ", codeChallenge=" + codeChallenge +
      ", verifyEmail=" + verifyEmail +
      ", providerMetadataEndpointSupported=" + providerMetadataEndpointSupported +
      ", activateUser=" + activateUser +
      ", groupMapping=" + groupMapping +
      ", groupMappings='" + groupMappings + '\'' +
      ", needConsent=" + needConsent +
      ", registrationDisabled=" + registrationDisabled +
      ", callbackURI='" + callbackURI + '\'' +
      ", rejectRemoteNoGroup=" + rejectRemoteNoGroup +
      ", managedCloudRedirectUri='" + managedCloudRedirectUri + '\'' +
      '}';
  }
}
