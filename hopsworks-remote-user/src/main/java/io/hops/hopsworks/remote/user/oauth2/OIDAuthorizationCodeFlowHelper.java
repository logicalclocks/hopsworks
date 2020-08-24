/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.dao.remote.oauth.OauthLoginStateFacade;
import io.hops.hopsworks.common.remote.oauth.OpenIdConstant;
import io.hops.hopsworks.common.remote.oauth.OpenIdProviderConfig;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthLoginState;
import net.minidev.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OIDAuthorizationCodeFlowHelper {
  
  private final static Logger LOGGER = Logger.getLogger(OIDAuthorizationCodeFlowHelper.class.getName());

  @EJB
  private OAuthProviderCache oAuthProviderCache;
  @EJB
  private OauthLoginStateFacade oauthLoginStateFacade;
  @EJB
  private OauthClientFacade oauthClientFacade;
  @EJB
  private Settings settings;
  
  /**
   *
   * @param client
   * @param invalidateCache
   * @return
   */
  public OpenIdProviderConfig getOpenIdProviderConfig(OauthClient client, boolean invalidateCache) {
    return oAuthProviderCache.getProviderConfig(client, invalidateCache);
  }
  
  /**
   *
   * @param providerURI
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public OpenIdProviderConfig getOpenIdProviderConfig(String providerURI) throws IOException, URISyntaxException {
    return oAuthProviderCache.getProviderConfigFromURI(providerURI);
  }
  
  private Scope getSupportedScope(Set<String> scopesSupported) {
    Scope scope = new Scope(OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE);
    if (scopesSupported == null || scopesSupported.isEmpty()) {
      return scope;
    }
    if (scopesSupported.contains(OpenIdConstant.ROLES_SCOPE)) {
      scope.add(OpenIdConstant.ROLES_SCOPE);
    } else if (scopesSupported.contains(OpenIdConstant.GROUPS_SCOPE)) {
      scope.add(OpenIdConstant.GROUPS_SCOPE);
    }
    return scope;
  }
  
  /**
   *
   * @param providerName
   * @return
   * @throws URISyntaxException
   */
  public URI getAuthenticationRequestURL(String providerName) throws URISyntaxException {
    OauthClient client = oauthClientFacade.findByProviderName(providerName);
    if (client == null) {
      throw new NotFoundException("Client not found.");
    }
    OpenIdProviderConfig providerConfig = oAuthProviderCache.getProviderConfig(client, false);
    Nonce nonce = new Nonce();
    State state = saveOauthLoginState(client, nonce);
    Scope scope = getSupportedScope(providerConfig.getScopesSupported());
    URI redirectURI = new URI(settings.getOauthRedirectUri());
    URI providerURI = new URI(providerConfig.getAuthorizationEndpoint());
    ClientID clientId = new ClientID(client.getClientId());
    ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
    AuthenticationRequest authenticationRequest =
      new AuthenticationRequest(providerURI, responseType, scope, clientId, redirectURI, state, nonce);
    URI authReqURI = authenticationRequest.toURI();
    return authReqURI;
  }
  
  private State saveOauthLoginState(OauthClient client, Nonce nonce) {
    State state = new State();
    int count = 10;
    OauthLoginState oauthLoginState = oauthLoginStateFacade.findByState(state.getValue());
    while (oauthLoginState != null && count > 0) {
      state = new State();
      oauthLoginState = oauthLoginStateFacade.findByState(state.getValue());
      count--;
    }
    if (oauthLoginState != null) {
      throw new IllegalStateException("Failed to create state.");
    }
    oauthLoginState = new OauthLoginState(state.getValue(), client);
    oauthLoginState.setNonce(nonce.getValue());
    oauthLoginStateFacade.save(oauthLoginState);
    return state;
  }
  
  /**
   *
   * @param code
   * @param oauthLoginState
   * @param providerMetadata
   * @return
   * @throws IOException
   * @throws ParseException
   * @throws URISyntaxException
   */
  public AccessToken requestToken(String code, OauthLoginState oauthLoginState, OpenIdProviderConfig providerMetadata)
    throws IOException, ParseException, URISyntaxException, VerificationException {
    ClientID clientId = new ClientID(oauthLoginState.getClientId().getClientId());
    Secret secret = new Secret(oauthLoginState.getClientId().getClientSecret());
    ClientSecretBasic clientSecretBasic = new ClientSecretBasic(clientId, secret);
    AuthorizationCode authCode = new AuthorizationCode(code);
    URI redirectURI = new URI(settings.getOauthRedirectUri());
    Scope scope = getSupportedScope(providerMetadata.getScopesSupported());
    AuthorizationCodeGrant authorizationCodeGrant = new AuthorizationCodeGrant(authCode, redirectURI);
    URI tokenEndpoint = new URI(providerMetadata.getTokenEndpoint());
    TokenRequest tokenReq = new TokenRequest(tokenEndpoint, clientSecretBasic, authorizationCodeGrant, scope);
  
    HTTPResponse tokenHTTPResp = tokenReq.toHTTPRequest().send();
    TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);
  
    if (tokenResponse instanceof TokenErrorResponse) {
      ErrorObject error = ((TokenErrorResponse) tokenResponse).getErrorObject();
      LOGGER.log(Level.SEVERE, "Error in token response: {0}", error.getDescription());
      throw new IOException("Error in token response " + error.getDescription());
    }
  
    OIDCTokenResponse accessTokenResponse = (OIDCTokenResponse) tokenResponse;
    OIDCTokens token = accessTokenResponse.getOIDCTokens();
    validateIdTokenWithRetry(token.getIDTokenString(), oauthLoginState, providerMetadata);
    AccessToken accessToken = token.getAccessToken();
    oauthLoginState.setToken(accessToken.getValue());
    oauthLoginStateFacade.update(oauthLoginState);
    return accessToken;
  }
  
  /**
   *
   * @param code
   * @param oauthLoginState
   * @param providerMetadata
   * @return
   * @throws URISyntaxException
   * @throws VerificationException
   * @throws ParseException
   * @throws IOException
   */
  public AccessToken getAccessToken(String code, OauthLoginState oauthLoginState, OpenIdProviderConfig providerMetadata)
    throws URISyntaxException, VerificationException, ParseException, IOException {
    AccessToken accessToken;
    if (oauthLoginState.getToken() == null) {
      accessToken = requestToken(code, oauthLoginState, providerMetadata);
    } else {
      accessToken = new BearerAccessToken(oauthLoginState.getToken());
    }
    return accessToken;
  }
  
  /**
   *
   * @param idToken
   * @param oauthLoginState
   * @param providerConfig
   * @throws VerificationException
   */
  public void validateIdTokenWithRetry(String idToken, OauthLoginState oauthLoginState,
    OpenIdProviderConfig providerConfig) throws VerificationException {
    DecodedJWT jwt = com.auth0.jwt.JWT.decode(idToken);
    int maxAttempts = 2;
    boolean invalidateCache = false;
    for (int count = 0; count < maxAttempts; count++) {
      try {
        validateIdToken(jwt, oauthLoginState, providerConfig, invalidateCache);
      } catch (SignatureVerificationException sve) {
        if (jwt != null && jwt.getAlgorithm().startsWith("RS")) {
          //if RSA and key is cached invalidate cache and try again
          LOGGER.log(Level.INFO, "SignatureVerificationException retrying with new signing key. Attempt = ", count);
          invalidateCache = true;
        } else {
          throw new VerificationException("Exception verifying id token. " + sve.getMessage());
        }
      } catch (IOException | JOSEException | URISyntaxException | ParseException | java.text.ParseException e) {
        LOGGER.log(Level.SEVERE, "Exception verifying id token: {0}", e.getMessage());
        throw new VerificationException("Exception verifying id token. " + e.getMessage());
      }
    }
  }
  
  private DecodedJWT validateIdToken(DecodedJWT jwt, OauthLoginState oauthLoginState,
    OpenIdProviderConfig providerConfig, boolean invalidateCache) throws URISyntaxException, ParseException,
    JOSEException, java.text.ParseException, IOException {
    OauthClient client = oauthLoginState.getClientId();
    Algorithm alg = getAlgorithm(jwt, client, providerConfig, invalidateCache);
    JWTVerifier verifier = com.auth0.jwt.JWT.require(alg)
      .withIssuer(providerConfig.getIssuerURI())
      .withAudience(client.getClientId())//If the ID Token contains multiple audiences, the Client SHOULD verify that
      // an azp Claim is present.
      .withClaim(OpenIdConstant.NONCE, oauthLoginState.getNonce())
      .build();
    jwt = verifier.verify(jwt.getToken());
    return jwt;
  }
  
  private Algorithm getAlgorithm(DecodedJWT idToken, OauthClient client, OpenIdProviderConfig providerConfig,
    boolean invalidateCache) throws IOException, URISyntaxException, java.text.ParseException, JOSEException,
    ParseException {
    SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(idToken.getAlgorithm());
    switch (algorithm) {
      case HS256:
        return Algorithm.HMAC256(getSecret(client));
      case HS384:
        return Algorithm.HMAC384(getSecret(client));
      case HS512:
        return Algorithm.HMAC512(getSecret(client));
      case RS256:
        return Algorithm.RSA256(getRSAJWK(idToken, providerConfig, invalidateCache), null);
      case RS384:
        return Algorithm.RSA384(getRSAJWK(idToken, providerConfig, invalidateCache), null);
      case RS512:
        return Algorithm.RSA512(getRSAJWK(idToken, providerConfig, invalidateCache), null);
      default:
        throw new NotSupportedException("Algorithm not supported.");
    }
    
  }
  
  private RSAPublicKey getRSAJWK(DecodedJWT idToken, OpenIdProviderConfig providerConfig, boolean invalidateCache)
    throws ParseException, IOException, URISyntaxException, java.text.ParseException, JOSEException {
    JSONObject key = oAuthProviderCache.getJWK(idToken.getKeyId(), providerConfig, invalidateCache);
    return RSAKey.parse(key.toString()).toRSAPublicKey();
  }
  
  private String getSecret(OauthClient client) throws UnsupportedEncodingException {
    byte[] bytes = client.getClientSecret().getBytes();
    return new String(bytes, "UTF-8");
  }
  
  /**
   *
   * @param accessToken
   * @param providerMetadata
   * @return
   * @throws IOException
   * @throws ParseException
   * @throws URISyntaxException
   */
  public RemoteUserDTO getRemoteUser(AccessToken accessToken, OpenIdProviderConfig providerMetadata, OauthClient client)
    throws IOException, ParseException, URISyntaxException {
    HTTPResponse userInfoHTTPResp = null;
    URI userinfoEndpoint = new URI(providerMetadata.getUserInfoEndpoint());
    UserInfoRequest userInfoReq = new UserInfoRequest(userinfoEndpoint, (BearerAccessToken) accessToken);
    userInfoHTTPResp = userInfoReq.toHTTPRequest().send();
    
    UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoHTTPResp);
    if (userInfoResponse instanceof UserInfoErrorResponse) {
      ErrorObject error = ((UserInfoErrorResponse) userInfoResponse).getErrorObject();
      LOGGER.log(Level.SEVERE, "Error in UserInfo response: {0}", error.getDescription());
      throw new IOException("Error in token response " + error.getDescription());
    }
    
    UserInfoSuccessResponse successResponse = (UserInfoSuccessResponse) userInfoResponse;
    return getRemoteUserFromClaims(successResponse.getUserInfo(), client.getClientId());
  }
  
  private RemoteUserDTO getRemoteUserFromClaims(UserInfo userInfo, String clientId) {
    RemoteUserDTO remoteUserDTO = new RemoteUserDTO();
    remoteUserDTO.setUuid(clientId + "." + userInfo.getSubject().getValue());
    List<String> emails = new ArrayList<>();
    emails.add(userInfo.getEmailAddress());
    remoteUserDTO.setEmail(emails);
    remoteUserDTO.setEmailVerified(userInfo.getEmailVerified());
    remoteUserDTO.setGivenName(userInfo.getGivenName());
    remoteUserDTO.setSurname(userInfo.getFamilyName());
    List<String> groups = new ArrayList<>();
    if (userInfo.toJSONObject().containsKey(OpenIdConstant.GROUPS)) {
      groups.add(userInfo.toJSONObject().getAsString(OpenIdConstant.GROUPS));
    }
    if (userInfo.toJSONObject().containsKey(OpenIdConstant.ROLES)) {
      groups.add(userInfo.toJSONObject().getAsString(OpenIdConstant.ROLES));
    }
    remoteUserDTO.setGroups(groups);
    return remoteUserDTO;
  }
  
}
