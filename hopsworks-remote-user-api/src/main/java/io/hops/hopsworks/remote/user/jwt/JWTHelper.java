/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import io.hops.hopsworks.api.auth.UserUtilities;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.remote.user.api.Audience;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.BEARER;
import static io.hops.hopsworks.jwt.Constants.EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.ROLES;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JWTHelper {

  @EJB
  private JWTController jwtController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private UserUtilities userUtilities;
  @EJB
  private Settings settings;

  /**
   * Get the user from the request header Authorization field.
   *
   * @param req
   * @return
   */
  public Users getUserPrincipal(HttpServletRequest req) {
    String jwt = getAuthToken(req);
    DecodedJWT djwt = jwtController.decodeToken(jwt);
    return djwt == null ? null : userFacade.findByUsername(djwt.getSubject());
  }
  
  /**
   * Get the user from SecurityContext
   * @param sc
   * @return 
   */
  public Users getUserPrincipal(SecurityContext sc) {
    return sc == null ? null : userFacade.findByUsername(sc.getUserPrincipal().getName());
  }

  /**
   *
   * @param req
   * @return
   */
  public Users getUserPrincipal(ContainerRequestContext req) {
    String jwt = getAuthToken(req);
    DecodedJWT djwt = jwtController.decodeToken(jwt);
    return djwt == null ? null : userFacade.findByUsername(djwt.getSubject());
  }

  /**
   * Extract jwt from the request header
   *
   * @param req
   * @return
   */
  public String getAuthToken(HttpServletRequest req) {
    String authorizationHeader = req.getHeader(AUTHORIZATION);
    String token = null;
    if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
      token = authorizationHeader.substring(Constants.BEARER.length()).trim();
    }
    return token;
  }

  /**
   * Extract jwt from the request header
   *
   * @param req
   * @return
   */
  public String getAuthToken(ContainerRequestContext req) {
    String authorizationHeader = req.getHeaderString(AUTHORIZATION);
    String token = null;
    if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
      token = authorizationHeader.substring(Constants.BEARER.length()).trim();
    }
    return token;
  }

  /**
   * Create a new jwt for the given user.
   *
   * @param user
   * @param issuer
   * @return
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   * @throws DuplicateSigningKeyException
   */
  public String createToken(Users user, String issuer, Map<String, Object> claims) throws NoSuchAlgorithmException,
      SigningKeyNotFoundException, DuplicateSigningKeyException {
    String[] audience = null;
    Date expiresAt = null;

    if (claims == null) {
      claims = new HashMap<>(3);
    }
    BbcGroup group = bbcGroupFacade.findByGroupName("AGENT");
    if (user.getBbcGroupCollection().contains(group)) {
      audience = new String[2];
      audience[0] = Audience.API;
      audience[1] = Audience.SERVICES;
      expiresAt = new Date(System.currentTimeMillis() + settings.getServiceJWTLifetimeMS());
      claims.put(EXPIRY_LEEWAY, settings.getServiceJWTExpLeewaySec());
    } else {
      audience = new String[1];
      audience[0] = Audience.API;
      expiresAt = new Date(System.currentTimeMillis() + settings.getJWTLifetimeMs());
      claims.put(EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
    }

    return createToken(user, audience, issuer, expiresAt, claims);
  }

  /**
   * One time token 60 sec life
   * @param user
   * @param issuer
   * @param claims
   * @return
   */
  public String createOneTimeToken(Users user, String issuer, Map<String, Object> claims) {
    String[] audience = {};
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + Constants.ONE_TIME_JWT_LIFETIME_MS);
    String[] roles = {};
    String token = null;
    try {
      token = createOneTimeToken(user, roles, issuer, audience, now, expiresAt,
          Constants.ONE_TIME_JWT_SIGNING_KEY_NAME, claims, false);
    } catch (NoSuchAlgorithmException | SigningKeyNotFoundException | DuplicateSigningKeyException ex) {
      Logger.getLogger(JWTHelper.class.getName()).log(Level.SEVERE, null, ex);
    }
    return token;
  }

  public String createOneTimeToken(Users user, String[] roles, String issuer, String[] audience, Date notBefore,
      Date expiresAt, String keyName, Map<String, Object> claims, boolean createNewKey)
    throws NoSuchAlgorithmException, SigningKeyNotFoundException, DuplicateSigningKeyException {
    SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(Constants.ONE_TIME_JWT_SIGNATURE_ALGORITHM);
    claims = jwtController.addDefaultClaimsIfMissing(claims, false, 0, roles);

    return jwtController.createToken(keyName, createNewKey, issuer, audience, expiresAt, notBefore,
        user.getUsername(), claims, algorithm);
  }

  /**
   * Create a new jwt for the given user that can be used for the specified audience.
   *
   * @param user
   * @param audience
   * @param issuer
   * @return
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   * @throws DuplicateSigningKeyException
   */
  public String createToken(Users user, String[] audience, String issuer, Date expiresAt, Map<String, Object> claims)
      throws NoSuchAlgorithmException, SigningKeyNotFoundException, DuplicateSigningKeyException {
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    String[] roles = userUtilities.getUserRoles(user).toArray(new String[0]);

    claims = jwtController.addDefaultClaimsIfMissing(claims, true, settings.getJWTExpLeewaySec(), roles);
    return jwtController.createToken(settings.getJWTSigningKeyName(), false, issuer, audience, expiresAt,
        new Date(), user.getUsername(), claims, alg);
  }
  
  /**
   * 
   * @param req
   * @param issuer
   * @return 
   */
  public boolean validToken(HttpServletRequest req, String issuer) {
    String jwt = getAuthToken(req);
    if (jwt == null) {
      return false;
    }
    try {
      jwtController.verifyToken(jwt, issuer);
    } catch (Exception ex) {
      return false;
    } 
    return true;
  }
  
  public Users validateToken(HttpServletRequest req) throws SigningKeyNotFoundException, VerificationException {
    String issuer = settings.getJWTIssuer();
    String jwt = getAuthToken(req);
    if (jwt == null) {
      throw new VerificationException("Token not provided.");
    }
    DecodedJWT djwt = jwtController.verifyToken(jwt, issuer);
    Users user = djwt == null ? null : userFacade.findByUsername(djwt.getSubject());
    return user;
  }

  /**
   * Invalidate a jwt found in the request header Authorization field.
   *
   * @param req
   * @throws InvalidationException
   */
  public void invalidateToken(HttpServletRequest req) throws InvalidationException {
    jwtController.invalidate(getAuthToken(req));
  }
  
  /**
   * Invalidate token
   * @param token 
   */
  public void invalidateToken(String token) {
    try {
      jwtController.invalidate(token);
    } catch (InvalidationException ex) {
      Logger.getLogger(JWTHelper.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Delete the signing key identified by keyName.
   * @param keyName 
   */
  public void deleteSigningKey(String keyName) {
    //Do not delete api signing key
    if (keyName == null || keyName.isEmpty()) {
      return;
    }
    if ( settings.getJWTSigningKeyName().equals(keyName) || Constants.ONE_TIME_JWT_SIGNING_KEY_NAME.equals(keyName)) {
      return; //TODO maybe throw exception here?
    }
    jwtController.deleteSigningKey(keyName);
  }
  
  /**
   * Verify then invalidate a jwt
   * @param token
   * @param issuer
   * @return
   * @throws SigningKeyNotFoundException
   * @throws VerificationException 
   */
  public DecodedJWT verifyOneTimeToken(String token, String issuer) throws SigningKeyNotFoundException, 
    VerificationException {
    DecodedJWT jwt = null;
    if (token == null || token.trim().isEmpty()) {
      throw new VerificationException("Token not provided.");
    }
    try {
      jwt = jwtController.verifyOneTimeToken(token, issuer);
    } catch (InvalidationException ex) {
      Logger.getLogger(JWTHelper.class.getName()).log(Level.SEVERE, "Failed to invalidate one time token.", ex);
    }
    if (jwt == null) {
      throw new VerificationException("Failed to verify one time token.");
    }
    return jwt;
  }
  
  /**
   *
   * @param token
   * @return
   * @throws NotRenewableException
   * @throws SigningKeyNotFoundException
   * @throws InvalidationException
   */
  public String autoRenewToken(String token)
    throws NotRenewableException, SigningKeyNotFoundException, InvalidationException {
    if (Strings.isNullOrEmpty(token)) {
      throw new NotRenewableException("Token not set");
    }
    DecodedJWT decodedJWT = jwtController.verifyTokenForRenewal(token);
    boolean isRenewable = jwtController.getRenewableClaim(decodedJWT);
    // Do not get the user if not renewable
    if (!isRenewable) {
      throw new NotRenewableException("Token not renewable.");
    }
    // this will be called on every call so do not get user if token not expired
    Date currentTime = new Date();
    if (currentTime.before(decodedJWT.getExpiresAt())) {
      throw new NotRenewableException("Token not expired.");
    }
    Users user = userFacade.findByUsername(decodedJWT.getSubject());
    if (user == null) {
      throw new NotRenewableException("User not found");
    }
    if (!UserAccountStatus.ACTIVATED_ACCOUNT.equals(user.getStatus())) {
      throw new NotRenewableException("User not active");
    }
    List<String> roles = userUtilities.getUserRoles(user);
    return jwtController.autoRenewToken(decodedJWT, roles.toArray(new String[0]));
  }
  
  /**
   * Create JWT Token for Proxy
   * @param user
   * @return
   * @throws DuplicateSigningKeyException
   * @throws SigningKeyNotFoundException
   * @throws NoSuchAlgorithmException
   */
  public String createTokenForProxy(Users user)
    throws DuplicateSigningKeyException, SigningKeyNotFoundException, NoSuchAlgorithmException {
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    Date expiresAt = new Date(System.currentTimeMillis() + settings.getJWTLifetimeMs());
    String[] userRoles = userUtilities.getUserRoles(user).toArray(new String[0]);
    String[] audience = new String[] {Audience.PROXY};
    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLES, userRoles);
    claims.put(RENEWABLE, true);
    claims.put(EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
    return jwtController.createTokenForProxy(user.getUsername(), settings.getJWTIssuer(), audience, claims, expiresAt,
      alg);
  }
}
