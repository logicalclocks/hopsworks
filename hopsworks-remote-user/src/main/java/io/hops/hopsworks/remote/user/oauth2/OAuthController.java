/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.google.common.base.Strings;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.dao.remote.oauth.OauthLoginStateFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.remote.oauth.OpenIdConstant;
import io.hops.hopsworks.common.remote.oauth.OpenIdProviderConfig;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthLoginState;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.remote.user.GroupMapping;
import io.hops.hopsworks.remote.user.RemoteUserAuthController;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthController {
  
  private final static Logger LOGGER = Logger.getLogger(OAuthController.class.getName());
  private final static int LOGIN_STATE_TTL_SEC = 3600;
  
  @EJB
  private OauthLoginStateFacade oauthLoginStateFacade;
  @EJB
  private OIDAuthorizationCodeFlowHelper oidAuthorizationCodeFlowHelper;
  @EJB
  private RemoteUserAuthController remoteUserAuthController;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private Settings settings;
  @EJB
  private OauthClientFacade oauthClientFacade;
  @EJB
  private OAuthProviderCache oAuthProviderCache;
  
  /**
   *
   * @param sessionId
   * @param code
   * @param state
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO login(String sessionId, String code, String state, boolean consent, String chosenEmail)
    throws LoginException, RemoteAuthException, UserException {
    OauthLoginState oauthLoginState = getClient(state, sessionId);
    OauthClient client = oauthLoginState.getClientId();
  
    RemoteUserDTO remoteUserDTO;
    OIDCTokens oidcTokens;
    try {
      OpenIdProviderConfig providerConfig = oidAuthorizationCodeFlowHelper.getOpenIdProviderConfig(client, false);
      oidcTokens = oidAuthorizationCodeFlowHelper.getAccessToken(code, oauthLoginState, providerConfig);
      remoteUserDTO = oidAuthorizationCodeFlowHelper.getRemoteUser(oidcTokens.getBearerAccessToken(), providerConfig,
        client);
    } catch (URISyntaxException | VerificationException | IOException | ParseException e) {
      LOGGER.log(Level.SEVERE, "Error getting user info from {0}: {1}",
        new Object[]{client.getProviderName(), e.getMessage()});
      throw new LoginException("Error getting user info from " + client.getProviderName() + ": " + e.getMessage());
    }
    RemoteUserStateDTO remoteUserStateDTO =
      remoteUserAuthController.getRemoteUserStatus(remoteUserDTO, consent, chosenEmail, RemoteUserType.OAUTH2,
        UserAccountStatus.fromValue(settings.getOAuthAccountStatus()));
    if (remoteUserStateDTO.isSaved()) {
      oauthLoginStateFacade.remove(oauthLoginState);
      oidAuthorizationCodeFlowHelper.createOAuthTokens(oidcTokens, remoteUserStateDTO.getRemoteUser().getUid());
    }
    return remoteUserStateDTO;
  }
  
  public URI getLogoutURI(String providerName, String redirectURI, Users user) throws UserException,
    RemoteAuthException {
    try {
      return oidAuthorizationCodeFlowHelper.getLogoutUrl(providerName, redirectURI, user);
    } catch (URISyntaxException e) {
      String errorMsg = "Failed to logout from " + providerName + ". " + e.getMessage();
      throw new UserException(RESTCodes.UserErrorCode.LOGOUT_FAILURE, Level.SEVERE, errorMsg, errorMsg, e);
    }
  }
  
  private OauthLoginState getClient(String state, String sessionId) {
    OauthLoginState oauthLoginState = oauthLoginStateFacade.findByStateAndSession(state, sessionId)
      .orElseThrow(() -> new IllegalStateException("Authentication request not found."));
    Date currTime = new Date();
    long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(currTime.getTime() - oauthLoginState.getLoginTime().getTime());
    if (diffInSeconds > LOGIN_STATE_TTL_SEC) {
      oauthLoginStateFacade.remove(oauthLoginState);
      throw new IllegalStateException("Authentication request has expired.");
    }
    return oauthLoginState;
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int cleanupLoginStates() {
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, -LOGIN_STATE_TTL_SEC);
    List<OauthLoginState> expiredStates = oauthLoginStateFacade.findByLoginTimeBefore(cal.getTime());
    int count = 0;
    for (OauthLoginState expiredState : expiredStates) {
      oauthLoginStateFacade.remove(expiredState);
      count++;
    }
    return count;
  }
  
  public URI getErrorUrl (UriInfo uriInfo, Exception e) throws UnsupportedEncodingException, URISyntaxException {
    String scheme = uriInfo.getAbsolutePath().getScheme();
    String host = uriInfo.getAbsolutePath().getHost();
    int port = uriInfo.getAbsolutePath().getPort();
    return new URI(scheme + "://" + host + ":" + port + "/hopsworks/#!/error?e=" + getURLEncodedRootCause(e));
  }
  
  public String getURLEncodedRootCause(Exception e) throws UnsupportedEncodingException {
    Throwable t = e.getCause() != null ? e.getCause() : e;
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return URLEncoder.encode(t.getMessage(), "UTF-8");
  }
  
  public RemoteUserStateDTO getOAuthUser(Users user) throws HopsSecurityException {
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    if (remoteUser == null || !RemoteUserType.OAUTH2.equals(remoteUser.getType())) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.FINE);
    }
    return new RemoteUserStateDTO(true, remoteUser, null);
  }
  
  public void saveClient(OauthClient oauthClient) {
    removeWellKnownFromProviderUri(oauthClient);
    oauthClientFacade.save(oauthClient);
    oAuthProviderCache.removeFromCache(oauthClient.getClientId());
  }
  
  public void updateClient(OauthClient oauthClient) {
    removeWellKnownFromProviderUri(oauthClient);
    oauthClientFacade.update(oauthClient);
    oAuthProviderCache.removeFromCache(oauthClient.getClientId());
  }
  
  public void removeClient(OauthClient oauthClient) {
    oauthClientFacade.remove(oauthClient);
    oAuthProviderCache.removeFromCache(oauthClient.getClientId());
  }
  
  private void removeWellKnownFromProviderUri(OauthClient oauthClient) {
    if (oauthClient.getProviderURI().endsWith(OpenIdConstant.OPENID_CONFIGURATION_URL)) {
      oauthClient.setProviderURI(oauthClient.getProviderURI().replace(OpenIdConstant.OPENID_CONFIGURATION_URL, ""));
    }
  }
  
  public void updateSettings(Boolean needConsent, Boolean registrationDisabled, Boolean activateUser,
      GroupMapping groupMapping, String groupMappings, String domain, Boolean rejectNoGroup,
      String managedCloudRedirectUri) throws RemoteAuthException {
    if (groupMapping != null) {
      if (GroupMapping.GROUP_MAPPING.equals(groupMapping)) {
        if (Strings.isNullOrEmpty(groupMappings)) {
          throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
              "Group mapping not set.");
        }
        settings.updateOAuthGroupMapping(groupMappings);
      } else {
        settings.updateOAuthGroupMapping(groupMapping.getValue());
      }
    }
    
    if (needConsent != null) {
      settings.updateRemoteAuthNeedConsent(needConsent);
    }
    
    if (registrationDisabled != null) {
      settings.updateRegistrationDisabled(registrationDisabled);
    }
    
    if (activateUser != null && activateUser) {
      settings.updateOAuthAccountStatus(UserAccountStatus.ACTIVATED_ACCOUNT.getValue());
    }
    
    if (!Strings.isNullOrEmpty(domain)) {
      settings.updateOauthRedirectUri(domain);
    }
    
    if (!Strings.isNullOrEmpty(domain)) {
      settings.addPathAndupdateOauthLogoutRedirectUri(domain);
    }
    
    if (rejectNoGroup != null){
      settings.updateRejectRemoteNoGroup(rejectNoGroup);
    }
    
    if(!Strings.isNullOrEmpty((managedCloudRedirectUri))){
      settings.updateManagedCloudRedirectUri(managedCloudRedirectUri);
      settings.updateOauthLogoutRedirectUri(managedCloudRedirectUri);
    }
    
    settings.refreshCache();
  }

}
