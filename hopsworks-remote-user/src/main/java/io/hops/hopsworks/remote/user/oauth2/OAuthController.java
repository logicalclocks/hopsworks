/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import io.hops.hopsworks.common.dao.remote.oauth.OauthLoginStateFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.remote.oauth.OpenIdProviderConfig;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthLoginState;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthController {
  
  private final static Logger LOGGER = Logger.getLogger(OAuthController.class.getName());
  private final static int LOGIN_STATE_TTL_MIN = 15;
  
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
  
  /**
   *
   * @param code
   * @param state
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO login(String code, String state, boolean consent, String chosenEmail)
    throws LoginException {
    OauthLoginState oauthLoginState = oauthLoginStateFacade.findByState(state);
    if (oauthLoginState == null) {
      throw new IllegalStateException("No login request found for the given state.");
    }
    OauthClient client = oauthLoginState.getClientId();
    if (client == null) {
      throw new IllegalStateException("No client found for the given id.");
    }
  
    RemoteUserDTO remoteUserDTO = null;
    try {
      OpenIdProviderConfig providerConfig = oidAuthorizationCodeFlowHelper.getOpenIdProviderConfig(client, false);
      AccessToken accessToken = oidAuthorizationCodeFlowHelper.getAccessToken(code, oauthLoginState, providerConfig);
      remoteUserDTO = oidAuthorizationCodeFlowHelper.getRemoteUser(accessToken, providerConfig, client);
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
    }
    return remoteUserStateDTO;
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int cleanupLoginStates() {
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MINUTE, -LOGIN_STATE_TTL_MIN);
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
    if (!RemoteUserType.OAUTH2.equals(remoteUser.getType())) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.FINE);
    }
    return new RemoteUserStateDTO(remoteUser == null, remoteUser, null);
  }
}
