/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import static io.hops.hopsworks.jwt.Constants.BEARER;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import javax.ws.rs.core.SecurityContext;

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
  private UsersController userController;
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
    Users user = djwt == null ? null : userFacade.findByUsername(djwt.getSubject());
    return user;
  }
  
  /**
   * Get the user from SecurityContext
   * @param sc
   * @return 
   */
  public Users getUserPrincipal(SecurityContext sc) {
    Users user = sc == null ? null : userFacade.findByUsername(sc.getUserPrincipal().getName());
    return user;
  }

  /**
   *
   * @param req
   * @return
   */
  public Users getUserPrincipal(ContainerRequestContext req) {
    String jwt = getAuthToken(req);
    DecodedJWT djwt = jwtController.decodeToken(jwt);
    Users user = djwt == null ? null : userFacade.findByUsername(djwt.getSubject());
    return user;
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
  public String createToken(Users user, String issuer) throws NoSuchAlgorithmException, SigningKeyNotFoundException,
      DuplicateSigningKeyException {
    String[] audience = {Audience.API};
    BbcGroup group = bbcGroupFacade.findByGroupName("AGENT");
    if (user.getBbcGroupCollection().contains(group)) {
      audience = new String[2];
      audience[0] = Audience.API;
      audience[1] = Audience.SERVICES;
    }
    return createToken(user, audience, issuer);
  }
  
  /**
   * One time token 60 sec life
   * @param user
   * @param issuer
   * @return
   */
  public String createOneTimeToken(Users user, String issuer) {
    String[] audience = {};
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + Constants.ONE_TIME_JWT_LIFETIME_MS);
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(Constants.ONE_TIME_JWT_SIGNATURE_ALGORITHM);
    String[] roles = {};
    String token = null;
    try {
      token = jwtController.createToken(Constants.ONE_TIME_JWT_SIGNING_KEY_NAME, false, issuer, audience, expiresAt, 
          now, user.getUsername(), false, 0, roles, alg);
    } catch (NoSuchAlgorithmException | SigningKeyNotFoundException | DuplicateSigningKeyException ex) {
      Logger.getLogger(JWTHelper.class.getName()).log(Level.SEVERE, null, ex);
    }
    return token;
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
  public String createToken(Users user, String[] audience, String issuer) throws NoSuchAlgorithmException,
      SigningKeyNotFoundException, DuplicateSigningKeyException {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + settings.getJWTLifetimeMs());
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    String[] roles = userController.getUserRoles(user).toArray(new String[0]);
    String token = jwtController.createToken(settings.getJWTSigningKeyName(), false, issuer, audience, expiresAt,
        new Date(), user.getUsername(), true, settings.getJWTExpLeewaySec(), roles, alg);
    return token;
  }

  /**
   * Create jwt with a new signing key. Fails if the keyName already exists.
   *
   * @param jWTRequestDTO
   * @param issuer
   * @return
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   * @throws DuplicateSigningKeyException
   */
  public JWTResponseDTO createToken(JWTRequestDTO jWTRequestDTO, String issuer) throws NoSuchAlgorithmException,
      SigningKeyNotFoundException, DuplicateSigningKeyException {
    if (jWTRequestDTO == null || jWTRequestDTO.getKeyName() == null || jWTRequestDTO.getKeyName().isEmpty()
        || jWTRequestDTO.getAudiences() == null || jWTRequestDTO.getAudiences().length == 0
        || jWTRequestDTO.getSubject() == null || jWTRequestDTO.getSubject().isEmpty()) {
      return null;
    }
    Date now = new Date();
    Date nbf = jWTRequestDTO.getNotBefore() != null ? jWTRequestDTO.getNotBefore() : now;
    Date expiresOn = jWTRequestDTO.getExpiresAt() != null ? jWTRequestDTO.getExpiresAt() : new Date(now.getTime()
        + settings.getJWTLifetimeMs());
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    String[] roles = {"HOPS_USER"}; //What role should we give to users not in system 
    
    String token = jwtController.createToken(jWTRequestDTO.getKeyName(), true, issuer, jWTRequestDTO.getAudiences(),
        expiresOn, nbf, jWTRequestDTO.getSubject(), jWTRequestDTO.isRenewable(), jWTRequestDTO.getExpLeeway(), roles,
        alg);
    int expLeeway = jwtController.getExpLeewayOrDefault(jWTRequestDTO.getExpLeeway());
    return new JWTResponseDTO(token, expiresOn, nbf, expLeeway);
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

  /**
   * 
   * @param jsonWebTokenDTO
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException  
   */
  public JWTResponseDTO renewToken(JsonWebTokenDTO jsonWebTokenDTO) throws SigningKeyNotFoundException,
      NotRenewableException, InvalidationException {
    if (jsonWebTokenDTO == null || jsonWebTokenDTO.getToken() == null || jsonWebTokenDTO.getToken().isEmpty()) {
      throw new IllegalArgumentException("No token provided.");
    }
    Date now = new Date();
    Date nbf = jsonWebTokenDTO.getNbf() != null ? jsonWebTokenDTO.getNbf() : now;
    Date newExp = jsonWebTokenDTO.getExpiresAt() != null ? jsonWebTokenDTO.getExpiresAt() : new Date(now.getTime()
        + settings.getJWTLifetimeMs());
    String token = jwtController.renewToken(jsonWebTokenDTO.getToken(), newExp, nbf);
    DecodedJWT jwt = jwtController.decodeToken(token);
    int expLeeway = jwtController.getExpLeewayClaim(jwt);
    return new JWTResponseDTO(token, newExp, nbf, expLeeway);
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
   * Add jti of the token in the invalid jwt table.
   * @param jsonWebTokenDTO 
   * @throws InvalidationException 
   */
  public void invalidateToken(JsonWebTokenDTO jsonWebTokenDTO) throws InvalidationException {
    if (jsonWebTokenDTO == null || jsonWebTokenDTO.getToken() == null || jsonWebTokenDTO.getToken().isEmpty()) {
      throw new IllegalArgumentException("No token provided.");
    }
    jwtController.invalidate(jsonWebTokenDTO.getToken());
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
}
