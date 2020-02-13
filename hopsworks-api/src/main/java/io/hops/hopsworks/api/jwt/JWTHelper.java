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
import io.hops.hopsworks.api.user.ServiceJWTDTO;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.elastic.ElasticJWTController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
  private static final Logger LOGGER = Logger.getLogger(JWTHelper.class.getName());
  
  public static final List<String> SERVICE_RENEW_JWT_AUDIENCE = new ArrayList<>(1);
  static {
    SERVICE_RENEW_JWT_AUDIENCE.add(Audience.SERVICES);
  }
  
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
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ElasticJWTController elasticJWTController;

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
      SigningKeyNotFoundException,
      DuplicateSigningKeyException {
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
    String[] roles = userController.getUserRoles(user).toArray(new String[0]);
    
    claims = jwtController.addDefaultClaimsIfMissing(claims, true, settings.getJWTExpLeewaySec(), roles);
    return jwtController.createToken(settings.getJWTSigningKeyName(), false, issuer, audience, expiresAt,
        new Date(), user.getUsername(), claims, alg);
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
    Date nbf = jWTRequestDTO.getNbf() != null ? jWTRequestDTO.getNbf() : now;
    Date expiresOn = jWTRequestDTO.getExpiresAt() != null ? jWTRequestDTO.getExpiresAt() : new Date(now.getTime()
        + settings.getJWTLifetimeMs());
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    String[] roles = {"HOPS_USER"}; //What role should we give to users not in system
    int expLeeway = jwtController.getExpLeewayOrDefault(jWTRequestDTO.getExpLeeway());
    Map<String, Object> claims = new HashMap<>(3);
    claims.put(RENEWABLE, jWTRequestDTO.isRenewable());
    claims.put(EXPIRY_LEEWAY, expLeeway);
    claims.put(ROLES, roles);
    String token = jwtController.createToken(jWTRequestDTO.getKeyName(), true, issuer, jWTRequestDTO.getAudiences(),
        expiresOn, nbf, jWTRequestDTO.getSubject(), claims, alg);
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
  public JWTResponseDTO renewToken(JsonWebTokenDTO jsonWebTokenDTO, boolean invalidate,
      Map<String, Object> claims)
      throws SigningKeyNotFoundException, NotRenewableException, InvalidationException {
    if (jsonWebTokenDTO == null || jsonWebTokenDTO.getToken() == null || jsonWebTokenDTO.getToken().isEmpty()) {
      throw new IllegalArgumentException("No token provided.");
    }
    Date now = new Date();
    Date nbf = jsonWebTokenDTO.getNbf() != null ? jsonWebTokenDTO.getNbf() : now;
    Date newExp = jsonWebTokenDTO.getExpiresAt() != null ? jsonWebTokenDTO.getExpiresAt() : new Date(now.getTime()
        + settings.getJWTLifetimeMs());
    String token = jwtController.renewToken(jsonWebTokenDTO.getToken(), newExp, nbf, invalidate, claims);
    DecodedJWT jwt = jwtController.decodeToken(token);
    int expLeeway = jwtController.getExpLeewayClaim(jwt);
    return new JWTResponseDTO(token, newExp, nbf, expLeeway);
  }
  
  /**
   * Helper method to generate one-time tokens for service JWT renewal and renew the
   * master service JWT
   * @param token2renew Service JWT to renew
   * @param oneTimeRenewalToken Valid one-time token associated with the master token to be renewed.
   *                            One time tokens are generated once a service is logged-in and every time
   *                            it renews its master token
   * @param user Logged in user
   * @param remoteHostname Hostname of the machine the service runs
   * @return Renewed master service JWT and five one-time tokens used to renew it
   * @throws JWTException
   * @throws NoSuchAlgorithmException
   */
  public ServiceJWTDTO renewServiceToken(JsonWebTokenDTO token2renew, String oneTimeRenewalToken,
      Users user, String remoteHostname) throws JWTException, NoSuchAlgorithmException {
    if (Strings.isNullOrEmpty(oneTimeRenewalToken)) {
      throw new VerificationException("Service renewal token cannot be null or empty");
    }
    if (user == null) {
      DecodedJWT decodedJWT = jwtController.decodeToken(oneTimeRenewalToken);
      throw new VerificationException("Could not find user associated with JWT with ID: " + decodedJWT.getId());
    }
    
    LocalDateTime now = DateUtils.getNow();
    Date expiresAt = token2renew.getExpiresAt() != null ? token2renew.getExpiresAt()
        : DateUtils.localDateTime2Date(now.plus(settings.getServiceJWTLifetimeMS(), ChronoUnit.MILLIS));
    Date notBefore = token2renew.getNbf() != null ? token2renew.getNbf()
        : DateUtils.localDateTime2Date(now);
    
    List<String> userRoles = userController.getUserRoles(user);
    Pair<String, String[]> renewedTokens = jwtController.renewServiceToken(oneTimeRenewalToken, token2renew.getToken(),
        expiresAt, notBefore, settings.getServiceJWTLifetimeMS(), user.getUsername(),
        userRoles, SERVICE_RENEW_JWT_AUDIENCE, remoteHostname, settings.getJWTIssuer(),
        settings.getJWTSigningKeyName(), false);
  
    int expLeeway = jwtController.getExpLeewayClaim(jwtController.decodeToken(renewedTokens.getLeft()));
    JWTResponseDTO renewedServiceToken = new JWTResponseDTO(renewedTokens.getLeft(), expiresAt, notBefore, expLeeway);
    
    return new ServiceJWTDTO(renewedServiceToken, renewedTokens.getRight());
  }
  
  /**
   * Invalidate a service master token and delete the signing key of the temporary
   * one-time tokens.
   * @param serviceToken2invalidate
   */
  public void invalidateServiceToken(String serviceToken2invalidate) {
    jwtController.invalidateServiceToken(serviceToken2invalidate, settings.getJWTSigningKeyName());
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
  public void deleteSigningKeyByName(String keyName) {
    //Do not delete api signing key
    if (keyName == null || keyName.isEmpty()) {
      return;
    }
    if ( settings.getJWTSigningKeyName().equals(keyName) || Constants.ONE_TIME_JWT_SIGNING_KEY_NAME.equals(keyName)
        || Constants.ELK_SIGNING_KEY_NAME.equals(keyName)) {
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
   * Create a new signing key for ELK
   */
  public String getSigningKeyForELK() throws ElasticException {
    return elasticJWTController.getSigningKeyForELK();
  }
  
  /**
   * Create jwt token for a project in elastic.
   * @param sc
   * @param projectId
   * @return
   * @throws ElasticException
   */
  public ElasticJWTResponseDTO createTokenForELK(SecurityContext sc,
      Integer projectId) throws ElasticException {
    Users user = getUserPrincipal(sc);
    Project project = projectFacade.find(projectId);
    if(settings.isElasticJWTEnabled()){
      String token = elasticJWTController.createTokenForELK(user, project);
      String kibanaUrl = settings.getKibanaAppUri(token);
      return new ElasticJWTResponseDTO(token, kibanaUrl, project.getName());
    }else{
      String kibanaUrl = settings.getKibanaAppUri();
      return new ElasticJWTResponseDTO("", kibanaUrl, project.getName());
    }
  }
  
  public ElasticJWTResponseDTO createTokenForELKAsDataOwner(Integer projectId)
      throws ElasticException {
    Project project = projectFacade.find(projectId);
    if(settings.isElasticJWTEnabled()){
      String token = elasticJWTController.createTokenForELKAsDataOwner(project);
      String kibanaUrl = settings.getKibanaAppUri(token);
      return new ElasticJWTResponseDTO(token, kibanaUrl, project.getName());
    }else{
      String kibanaUrl = settings.getKibanaAppUri();
      return new ElasticJWTResponseDTO("", kibanaUrl, project.getName());
    }
  }
  
  public ElasticJWTResponseDTO createTokenForELKAsAdmin() throws ElasticException {
    if(settings.isElasticJWTEnabled()){
      String token = elasticJWTController.createTokenForELKAsAdmin();
      String kibanaUrl = settings.getKibanaAppUri(token);
      return new ElasticJWTResponseDTO(token, kibanaUrl,"");
    }else{
      String kibanaUrl = settings.getKibanaAppUri();
      return new ElasticJWTResponseDTO("", kibanaUrl,"");
    }
  }
}
