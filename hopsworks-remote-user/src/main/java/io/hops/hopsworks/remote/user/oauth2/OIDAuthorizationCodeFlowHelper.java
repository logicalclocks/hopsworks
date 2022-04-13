/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTParser;
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
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
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
import io.hops.hopsworks.common.dao.remote.oauth.OauthTokenFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.oauth.OpenIdConstant;
import io.hops.hopsworks.common.remote.oauth.OpenIdProviderConfig;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthLoginState;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthToken;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  @EJB
  private UserFacade userFacade;
  @EJB
  private OauthTokenFacade oauthTokenFacade;
  
  /**
   *
   * @param client
   * @param invalidateCache
   * @return
   * @throws RemoteAuthException
   */
  public OpenIdProviderConfig getOpenIdProviderConfig(OauthClient client, boolean invalidateCache)
    throws RemoteAuthException {
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
  
  private Scope getSupportedScope(Set<String> scopesSupported, boolean offlineAccess) {
    Scope scope = new Scope(OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE);
    if (scopesSupported == null || scopesSupported.isEmpty()) {
      return scope;
    }
    //Token request will include refresh token if offline_access is requested
    if (scopesSupported.contains(OpenIdConstant.OFFLINE_ACCESS_SCOPE) && offlineAccess) {
      scope.add(OpenIdConstant.OFFLINE_ACCESS_SCOPE);
    }
    if (scopesSupported.contains(OpenIdConstant.ROLES_SCOPE)) {
      scope.add(OpenIdConstant.ROLES_SCOPE);
    } else if (scopesSupported.contains(OpenIdConstant.GROUPS_SCOPE)) {
      scope.add(OpenIdConstant.GROUPS_SCOPE);
    }
    return scope;
  }
  
  private Scope getScope(Set<String> scopes) {
    Scope scope = new Scope(scopes.toArray(new String[0]));
    scope.add(OpenIdConstant.OFFLINE_ACCESS_SCOPE);
    scope.add(OpenIdConstant.OPENID_SCOPE);
    return scope;
  }
  
  /**
   *
   * @param sessionId
   * @param providerName
   * @return
   * @throws URISyntaxException
   */
  public URI getAuthenticationRequestURL(String sessionId, String providerName, String redirectUri, Set<String> scopes)
    throws URISyntaxException, RemoteAuthException {
    OauthClient client = oauthClientFacade.findByProviderName(providerName);
    if (client == null) {
      throw new NotFoundException("Client not found.");
    }
    OpenIdProviderConfig providerConfig = getOpenIdProviderConfig(client, false);
    Nonce nonce = new Nonce();
    CodeVerifier codeVerifier = null;
    CodeChallengeMethod codeChallengeMethod = null;
    if (client.isCodeChallenge()) {
      codeVerifier = new CodeVerifier();
      codeChallengeMethod = CodeChallengeMethod.parse(client.getCodeChallengeMethod().name());
    }
    
    Scope scope = scopes == null || scopes.isEmpty()? getSupportedScope(providerConfig.getScopesSupported(),
      client.isOfflineAccess()) : getScope(scopes);
    //set to database redirect uri if empty
    redirectUri = Strings.isNullOrEmpty(redirectUri) ? settings.getOauthRedirectUri(providerName) : redirectUri;
    State state = saveOauthLoginState(sessionId, client, nonce, codeVerifier, redirectUri, scope.toString().replace(
      " ", " ,"));
    URI authEndpoint = new URI(providerConfig.getAuthorizationEndpoint());
    ClientID clientId = new ClientID(client.getClientId());
    ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
    
    AuthenticationRequest authenticationRequest =
      new AuthenticationRequest.Builder(responseType, scope, clientId,
        new URI(getRedirectUri(redirectUri, providerName)))
        .state(state)
        .nonce(nonce)
        .codeChallenge(codeVerifier, codeChallengeMethod)
        .endpointURI(authEndpoint)
        .build();
    return authenticationRequest.toURI();
  }
  
  public URI getLogoutUrl(String providerName, String redirectURI, Users user) throws URISyntaxException,
    RemoteAuthException {
    OauthClient client = oauthClientFacade.findByProviderName(providerName);
    if (client == null) {
      throw new NotFoundException("Client not found.");
    }
    OpenIdProviderConfig providerConfig = getOpenIdProviderConfig(client, false);
    if (!Strings.isNullOrEmpty(providerConfig.getEndSessionEndpoint())) {
      URI postLogoutRedirectURI = new URI(redirectURI);
      URI logoutURI = new URI(providerConfig.getEndSessionEndpoint());
      LogoutReq.Builder logoutRequestBuilder = new LogoutReq.Builder(logoutURI, new ClientID(client.getClientId()),
        postLogoutRedirectURI).postLogoutRedirectParam(providerConfig.getLogoutRedirectParam());
      Optional<OauthToken> optionalOauthToken = oauthTokenFacade.findByUser(user);
      if (optionalOauthToken.isPresent()) {
        com.nimbusds.jwt.JWT idToken;
        try {
          idToken = JWTParser.parse(optionalOauthToken.get().getIdToken());
        } catch (java.text.ParseException e) {
          LOGGER.log(Level.SEVERE, "Id token ParseException {0}.", e.getMessage());
          throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.TOKEN_PARSE_EXCEPTION, Level.FINE, "Id token " +
            "ParseException.", e.getMessage());
        }
        logoutRequestBuilder.idTokenHint(idToken);
      }
      return logoutRequestBuilder.build().toURI();
    }
    return null;
  }
  
  private State saveOauthLoginState(String sessionId, OauthClient client, Nonce nonce, CodeVerifier codeVerifier,
    String redirectUri, String scopes) {
    State state = new State();
    /*
     * when using oauth for hopsworks.ai we need to first redirect to hopsworks.ai then to
     * the hopsworks cluster. For this purpose we pass the cluster url in the state
     */
    if(!settings.getManagedCloudRedirectUri().isEmpty()){
      state = new State(getRedirectUriPrefix(redirectUri, client.getProviderName()) + "_" + state.getValue());
    }
    int count = 10;
    OauthLoginState oauthLoginState = oauthLoginStateFacade.findByState(state.getValue());
    while (oauthLoginState != null && count > 0) {
      state = new State();
      /*
       * when using oauth for hopsworks.ai we need to first redirect to hopsworks.ai then to
       * the hopsworks cluster. For this purpose we pass the cluster url in the state
       */
      if (!settings.getManagedCloudRedirectUri().isEmpty()) {
        state = new State(getRedirectUriPrefix(redirectUri, client.getProviderName()) + "_" + state.getValue());
      }
      oauthLoginState = oauthLoginStateFacade.findByState(state.getValue());
      count--;
    }
    if (oauthLoginState != null) {
      throw new IllegalStateException("Failed to create state.");
    }
    
    oauthLoginState = new OauthLoginState(state.getValue(), client, sessionId,
      getRedirectUri(redirectUri, client.getProviderName()), scopes);
    oauthLoginState.setNonce(nonce.getValue());
    oauthLoginState.setCodeChallenge(codeVerifier != null? codeVerifier.getValue() : null);
    oauthLoginStateFacade.save(oauthLoginState);
    return state;
  }
  
  private String getRedirectUriPrefix(String redirectUri, String providerName) {
    // equals only if old ui
    // then we need to prefix with OauthRedirectUri skipping managed cloud
    return settings.getOauthRedirectUri(providerName).equals(redirectUri) ? settings.getOauthRedirectUri(providerName
      ,true) : redirectUri;
  }
  
  private String getRedirectUri(String redirectUri, String providerName) {
    //if managed cloud and provider is hopsworks.ai use redirect uri from database
    return Objects.equals(providerName, settings.getManagedCloudProviderName()) &&
      !Strings.isNullOrEmpty(settings.getManagedCloudRedirectUri()) ? settings.getManagedCloudRedirectUri() :
      redirectUri;
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
  public OIDCTokens requestTokenLogin(String code, OauthLoginState oauthLoginState,
    OpenIdProviderConfig providerMetadata) throws IOException, ParseException, URISyntaxException,
    VerificationException {
    OIDCTokens token = requestToken(code, oauthLoginState, providerMetadata);
    oauthLoginState.setIdToken(token.getIDTokenString());
    oauthLoginState.setAccessToken(token.getBearerAccessToken() != null ? token.getBearerAccessToken().getValue() :
      null);
    oauthLoginState.setRefreshToken(token.getRefreshToken() != null ? token.getRefreshToken().getValue() : null);
    oauthLoginStateFacade.update(oauthLoginState);
    return token;
  }
  
  public void createOAuthTokens(OIDCTokens token, Users user) {
    if (user != null) {
      oauthTokenFacade.updateOrCreate(user, token.getIDTokenString(), token.getBearerAccessToken().getValue(),
        token.getRefreshToken() != null ? token.getRefreshToken().getValue() : null);
    }
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
   * @throws VerificationException
   */
  public OIDCTokens requestToken(String code, OauthLoginState oauthLoginState, OpenIdProviderConfig providerMetadata)
    throws IOException, ParseException, URISyntaxException, VerificationException {
    ClientID clientId = new ClientID(oauthLoginState.getClientId().getClientId());
    Secret secret = new Secret(oauthLoginState.getClientId().getClientSecret());
    ClientSecretBasic clientSecretBasic = new ClientSecretBasic(clientId, secret);
    AuthorizationCode authCode = new AuthorizationCode(code);
    URI redirectURI = new URI(oauthLoginState.getRedirectURI());
    Scope scope = Scope.parse(oauthLoginState.getScopes());
    CodeVerifier pkceVerifier = null;
    if (oauthLoginState.getClientId().isCodeChallenge()) {
      pkceVerifier = new CodeVerifier(oauthLoginState.getCodeChallenge());
    }
    AuthorizationCodeGrant authorizationCodeGrant = new AuthorizationCodeGrant(authCode, redirectURI, pkceVerifier);
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
    return token;
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
  public OIDCTokens getAccessToken(String code, OauthLoginState oauthLoginState,
    OpenIdProviderConfig providerMetadata) throws URISyntaxException, VerificationException, ParseException,
    IOException {
    OIDCTokens token;
    if (oauthLoginState.getAccessToken() == null) {
      token = requestTokenLogin(code, oauthLoginState, providerMetadata);
    } else {
      RefreshToken refreshToken =
        oauthLoginState.getRefreshToken() != null ? new RefreshToken(oauthLoginState.getRefreshToken()) : null;
      token = new OIDCTokens(oauthLoginState.getIdToken(), new BearerAccessToken(oauthLoginState.getAccessToken()),
        refreshToken);
    }
    return token;
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
  public RemoteUserDTO getRemoteUser(BearerAccessToken accessToken, OpenIdProviderConfig providerMetadata,
    OauthClient client) throws IOException, ParseException, URISyntaxException, LoginException {
    HTTPResponse userInfoHTTPResp = null;
    URI userinfoEndpoint = new URI(providerMetadata.getUserInfoEndpoint());
    UserInfoRequest userInfoReq = new UserInfoRequest(userinfoEndpoint, accessToken);
    userInfoHTTPResp = userInfoReq.toHTTPRequest().send();
  
    UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoHTTPResp);
    if (userInfoResponse instanceof UserInfoErrorResponse) {
      ErrorObject error = ((UserInfoErrorResponse) userInfoResponse).getErrorObject();
      LOGGER.log(Level.SEVERE, "Error in UserInfo response: {0}", error.getDescription());
      throw new IOException("Error in UserInfo response: " + error.getDescription());
    }
  
    UserInfoSuccessResponse successResponse = (UserInfoSuccessResponse) userInfoResponse;
    return getRemoteUserFromClaims(accessToken, successResponse.getUserInfo(), client);
  }
  
  private List<String> getListOrStringClaim(UserInfo userInfo, String claimName) {
    List<String> groups = new ArrayList<>();
    if (userInfo.toJSONObject().containsKey(claimName)) {
      JSONArray groupList = userInfo.getClaim(claimName, JSONArray.class);
      if (groupList != null) {
        for (Object o : groupList) {
          groups.add((String) o);
        }
      } else {
        groups.add(userInfo.toJSONObject().getAsString(claimName));
      }
    }
    return groups;
  }
  
  private RemoteUserDTO getRemoteUserFromClaims(BearerAccessToken accessToken, UserInfo userInfo, OauthClient client)
    throws LoginException {
    RemoteUserDTO remoteUserDTO = new RemoteUserDTO();
    remoteUserDTO.setUuid(getUuid(client, userInfo.getSubject().getValue()));
    remoteUserDTO.setGivenName(userInfo.getGivenName());
    remoteUserDTO.setSurname(userInfo.getFamilyName());
    verifyAndSetEmail(remoteUserDTO, userInfo, client, accessToken);
    
    //TODO replace all of this by a system in which we can define the mapping during configuration
    List<String> groups = getListOrStringClaim(userInfo, OpenIdConstant.GROUPS);

    groups.addAll(getListOrStringClaim(userInfo, OpenIdConstant.ROLES));
    /*
     * this is the way we set the groups in hopsworks.ai
     */
    groups.addAll(getListOrStringClaim(userInfo, OpenIdConstant.COGNITO_USERS_GROUP));
    groups.addAll(getListOrStringClaim(userInfo, OpenIdConstant.COGNITO_ADMINS_GROUP));
    
    remoteUserDTO.setGroups(groups);
    validateRemoteUser(remoteUserDTO);
    return remoteUserDTO;
  }
  
  public String getUuid(OauthClient client, String subject) {
    return client.getClientId() + "." + subject;
  }
  
  private void verifyAndSetEmail(RemoteUserDTO remoteUserDTO, UserInfo userInfo, OauthClient client,
    BearerAccessToken accessToken) throws LoginException {
    Set<String> emails = new HashSet<>();
    if (userInfo.getEmailAddress() != null) {
      emails.add(userInfo.getEmailAddress());
    }
    if (client.isVerifyEmail()) {
      remoteUserDTO.setEmailVerified(userInfo.getEmailVerified() != null && userInfo.getEmailVerified());
    } else {
      if (emails.isEmpty() && accessToken != null && !Strings.isNullOrEmpty(accessToken.getValue())) {
        DecodedJWT decodedJWT = JWT.decode(accessToken.getValue());
        if (isEmail(decodedJWT, OpenIdConstant.AD_UNIQUE_NAME_CLAIM)) {
          emails.add(decodedJWT.getClaim(OpenIdConstant.AD_UNIQUE_NAME_CLAIM).asString());
        }
        if (isEmail(decodedJWT, OpenIdConstant.AD_PREFERRED_NAME_CLAIM)) {
          emails.add(decodedJWT.getClaim(OpenIdConstant.AD_PREFERRED_NAME_CLAIM).asString());
        }
        if (isEmail(decodedJWT, OpenIdConstant.AD_USER_PRINCIPAL_NAME_CLAIM)) {
          emails.add(decodedJWT.getClaim(OpenIdConstant.AD_USER_PRINCIPAL_NAME_CLAIM).asString());
        }
      } else if (emails.isEmpty()) {
        generateEmail(remoteUserDTO, emails);
      }
      remoteUserDTO.setEmailVerified(true);
    }
    remoteUserDTO.setEmail(new ArrayList<>(emails));
  }
  
  private boolean isEmail(DecodedJWT decodedJWT, String claimName) {
    return decodedJWT != null && !decodedJWT.getClaim(claimName).isNull() &&
        decodedJWT.getClaim(claimName).asString().contains("@") &&
        !decodedJWT.getClaim(claimName).asString().startsWith("@");
  }
  
  private void generateEmail(RemoteUserDTO remoteUserDTO, Set<String> emails) throws LoginException {
    String email;
    try {
      email = getUniqueEmail(remoteUserDTO.getGivenName(), remoteUserDTO.getSurname());
      if (email == null) {
        email = getUniqueEmail(remoteUserDTO.getSurname(), remoteUserDTO.getGivenName());
      }
    } catch (IllegalArgumentException illegalArgumentException) {
      throw new LoginException(illegalArgumentException.getMessage());
    }
    if (email == null) {
      throw new LoginException("No email found in userinfo. Failed to generate email.");
    }
    emails.add(email);
  }
  
  private String getUniqueEmail(String givenName, String surname) {
    if (Strings.isNullOrEmpty(givenName) || Strings.isNullOrEmpty(surname)) {
      throw new IllegalArgumentException("Given name or surname not found in userinfo.");
    }
    String email = givenName + OpenIdConstant.DEFAULT_EMAIL_DOMAIN;
    Users u = userFacade.findByEmail(email);
    int count = 1;
    while (u != null && count <= surname.length()) {
      email = givenName + surname.substring(0, count) + OpenIdConstant.DEFAULT_EMAIL_DOMAIN;
      u = userFacade.findByEmail(email);
      count++;
    }
    return u != null? null : email.toLowerCase();
  }
  
  private void validateRemoteUser(RemoteUserDTO remoteUserDTO) {
    if (Strings.isNullOrEmpty(remoteUserDTO.getUuid())) {
      LOGGER.log(Level.SEVERE, "Error in UserInfo response. UUID not set.");
    }
    if (Strings.isNullOrEmpty(remoteUserDTO.getUid())) {
      LOGGER.log(Level.WARNING, "Error in UserInfo response. UID not set.");
    }
    if (Strings.isNullOrEmpty(remoteUserDTO.getGivenName())) {
      LOGGER.log(Level.WARNING, "Error in UserInfo response. GivenName not set.");
    }
    if (Strings.isNullOrEmpty(remoteUserDTO.getSurname())) {
      LOGGER.log(Level.WARNING, "Error in UserInfo response. Surname not set.");
    }
    if (remoteUserDTO.getEmail().isEmpty()) {
      LOGGER.log(Level.SEVERE, "Error in UserInfo response. Email not set.");
    }
  }
}
