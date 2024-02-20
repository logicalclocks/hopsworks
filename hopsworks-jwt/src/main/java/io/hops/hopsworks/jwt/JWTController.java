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
package io.hops.hopsworks.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.jwt.dao.InvalidJwtFacade;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import io.hops.hopsworks.jwt.exception.AccessException;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.jwt.InvalidJwt;
import io.hops.hopsworks.persistence.entity.jwt.JwtSigningKey;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.DEFAULT_EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.ROLES;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JWTController {

  private final static Logger LOGGER = Logger.getLogger(JWTController.class.getName());
  @EJB
  private InvalidJwtFacade invalidJwtFacade;
  @EJB
  private AlgorithmFactory algorithmFactory;
  @EJB
  private JwtSigningKeyFacade jwtSigningKeyFacade;

  /**
   * Create a jwt.
   *
   * @param jwt
   * @return three Base64-URL strings separated by dots
   * @throws io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException
   */
  public String createToken(JsonWebToken jwt, Map<String, Object> claims) throws SigningKeyNotFoundException {
    return createToken(jwt.getKeyId(), jwt.getIssuer(), jwt.getAudience().toArray(new String[0]), jwt.
        getExpiresAt(), jwt.getNotBefore(), jwt.getSubject(), claims, jwt.getAlgorithm());
  }

  /**
   * Creates a jwt.
   *
   * @param keyId
   * @param issuer
   * @param audience
   * @param expiresAt
   * @param notBefore
   * @param subject
   * @param claims
   * @param algorithm
   * @return three Base64-URL strings separated by dots
   * @throws io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException
   */
  public String createToken(String keyId, String issuer, String[] audience, Date expiresAt, Date notBefore,
      String subject, Map<String, Object> claims, SignatureAlgorithm algorithm) throws
      SigningKeyNotFoundException {
    JWTCreator.Builder jwtBuilder = JWT.create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withIssuedAt(new Date())
        .withExpiresAt(expiresAt)
        .withNotBefore(notBefore)
        .withJWTId(generateJti())
        .withSubject(subject);
    // Sanitize expiration leeway
    Integer expLeeway = (Integer) claims.getOrDefault(EXPIRY_LEEWAY, -1);
    claims.put(EXPIRY_LEEWAY, getExpLeewayOrDefault(expLeeway));
    
    jwtBuilder = addClaims(jwtBuilder, claims);
    return jwtBuilder.sign(algorithmFactory.getAlgorithm(algorithm, keyId));
  }

  private JWTCreator.Builder addClaims(JWTCreator.Builder jwtCreator, Map<String, Object> claims) {
    for (Map.Entry<String, Object> entry : claims.entrySet()) {
      Object value = entry.getValue();
      if (value.getClass().isArray()) {
        Class clazz = value.getClass().getComponentType();
        if (String.class.equals(clazz)) {
          jwtCreator = jwtCreator.withArrayClaim(entry.getKey(), (String[]) value);
        } else if (Integer.class.equals(clazz)) {
          jwtCreator = jwtCreator.withArrayClaim(entry.getKey(), (Integer[]) value);
        } else if (Long.class.equals(clazz)) {
          jwtCreator = jwtCreator.withArrayClaim(entry.getKey(), (Long[]) value);
        }
      } else {
        if (Boolean.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (Boolean) value);
        } else if (Integer.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (Integer) value);
        } else if (Long.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (Long) value);
        } else if (Double.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (Double) value);
        } else if (String.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (String) value);
        } else if (Date.class.isInstance(value)) {
          jwtCreator = jwtCreator.withClaim(entry.getKey(), (Date) value);
        }
      }
    }
    return jwtCreator;
  }
  
  /**
   * Creates a jwt signed with the key identified by the keyName. Tries to creates a new key with the given name
   * if second arg is set to true. If second arg is false it will try to get the key with the given name or creates
   * it if it does not exist.
   *
   * @param keyName
   * @param createNewKey
   * @param issuer
   * @param audience
   * @param expiresAt
   * @param notBefore
   * @param subject
   * @param claims
   * @param algorithm
   * @return three Base64-URL strings separated by dots
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   * @throws DuplicateSigningKeyException
   */
  public String createToken(String keyName, boolean createNewKey, String issuer, String[] audience, Date expiresAt,
      Date notBefore, String subject, Map<String, Object> claims, SignatureAlgorithm algorithm)
      throws NoSuchAlgorithmException, SigningKeyNotFoundException, DuplicateSigningKeyException {
    JwtSigningKey signingKey;
    if (createNewKey) {
      signingKey = createNewSigningKey(keyName, algorithm);
    } else {
      signingKey = getOrCreateSigningKey(keyName, algorithm);
    }
    
    return createToken(signingKey.getId().toString(), issuer, audience, expiresAt, notBefore, subject,
        claims, algorithm);
  }

  /**
   * Invalidate a token by adding it to the invalid tokens table.
   *
   * @param token
   * @throws io.hops.hopsworks.jwt.exception.InvalidationException
   */
  public void invalidate(String token) throws InvalidationException {
    if (token == null || token.isEmpty()) {
      return;
    }
    DecodedJWT jwt;
    try {
      jwt = verifyToken(token, null);
    } catch (Exception ex) {
      return; // no need to invalidate if not valid
    } 

    int expLeeway = getExpLeewayClaim(jwt);
    invalidateJWT(jwt.getId(), jwt.getExpiresAt(), expLeeway);
  }
  
  /**
   * Get expiration leeway from jwt or 60 if no such claim exists.
   * @param jwt
   * @return 
   */
  public int getExpLeewayClaim(DecodedJWT jwt) {
    Claim expLeewayClaim = jwt.getClaim(EXPIRY_LEEWAY);
    return expLeewayClaim == null || expLeewayClaim.isNull() ? DEFAULT_EXPIRY_LEEWAY :
      getExpLeewayOrDefault(expLeewayClaim.asInt());
  }
  
  /**
   * Get expLeeway or default if expLeeway < 1
   * @param expLeeway
   * @return
   */
  public int getExpLeewayOrDefault(int expLeeway) {
    return expLeeway < 1 ? DEFAULT_EXPIRY_LEEWAY : expLeeway;
  }
  
  /**
   * Get renewable claim from jwt or false if no such claim exists.
   * @param jwt
   * @return 
   */
  public boolean getRenewableClaim(DecodedJWT jwt) {
    Claim renewableClaim = jwt.getClaim(RENEWABLE);
    return renewableClaim != null && !renewableClaim.isNull() ? renewableClaim.asBoolean() : DEFAULT_RENEWABLE;
  }
  
  /**
   * Get roles from jwt of empty array if no such claim exists.
   * @param jwt
   * @return 
   */
  public String[] getRolesClaim(DecodedJWT jwt) {
    Claim rolesClaim = jwt.getClaim(ROLES);
    return rolesClaim == null || rolesClaim.isNull() ? new String[0] : rolesClaim.asArray(String.class);
  }
  
  /**
   * Decode a token
   *
   * @param token
   * @return
   */
  public DecodedJWT decodeToken(String token) {
    if (token == null || token.isEmpty()) {
      return null;
    }
    return JWT.decode(token);
  }

  /**
   * Verify a token
   *
   * @param token
   * @param issuer
   * @return
   * @throws SigningKeyNotFoundException
   * @throws VerificationException
   */
  public DecodedJWT verifyToken(String token, String issuer) throws SigningKeyNotFoundException, VerificationException {
    DecodedJWT jwt = JWT.decode(token);
    issuer = issuer == null || issuer.isEmpty() ? jwt.getIssuer() : issuer;
    int expLeeway = getExpLeewayClaim(jwt);
    jwt = verifyToken(token, issuer, expLeeway, algorithmFactory.getAlgorithm(jwt));

    if (isTokenInvalidated(jwt)) {
      throw new VerificationException("Invalidated token.");
    }
    return jwt;
  }
  
  /**
   * Will verify then invalidate a one time key
   * @param token
   * @param issuer
   * @return
   * @throws SigningKeyNotFoundException
   * @throws VerificationException
   * @throws InvalidationException 
   */
  public DecodedJWT verifyOneTimeToken(String token, String issuer) throws SigningKeyNotFoundException,
      VerificationException, InvalidationException {
    DecodedJWT jwt = verifyToken(token, issuer);
    invalidateJWT(jwt.getId(), jwt.getExpiresAt(), getExpLeewayClaim(jwt));
    return jwt;
  }

  /**
   * Verify a token
   *
   * @param token
   * @param issuer
   * @param audiences
   * @param roles
   * @return
   * @throws SigningKeyNotFoundException
   * @throws VerificationException
   */
  public DecodedJWT verifyToken(String token, String issuer, Set<String> audiences, Set<String> roles) throws
    SigningKeyNotFoundException, VerificationException, AccessException {
    JsonWebToken jwt = new JsonWebToken(JWT.decode(token));
    issuer = issuer == null || issuer.isEmpty() ? jwt.getIssuer() : issuer;
    DecodedJWT djwt = verifyToken(token, issuer, jwt.getExpLeeway(), algorithmFactory.getAlgorithm(jwt));

    if (isTokenInvalidated(djwt)) {
      throw new VerificationException("Invalidated token.");
    }

    Set<String> rolesSet = new HashSet<>(jwt.getRole());
    if (roles != null && !roles.isEmpty()) {
      if (!intersect(roles, rolesSet)) {
        throw new AccessException("Client not authorized for this invocation.");
      }
    }

    Set<String> audiencesSet = new HashSet<>(jwt.getAudience());
    if (audiences != null && !audiences.isEmpty()) {
      if (!intersect(audiences, audiencesSet)) {
        throw new AccessException("Token not issued for this recipient.");
      }
    }
    return djwt;
  }
  
  private DecodedJWT verifyToken(String token, String issuer, int expLeeway, Algorithm algorithm) throws
      VerificationException {
    DecodedJWT jwt = null;
    try {
      JWTVerifier verifier = JWT.require(algorithm)
          .withIssuer(issuer)
          .acceptExpiresAt(expLeeway)
          .build();
      jwt = verifier.verify(token);
    } catch (Exception e) {
      throw new VerificationException(e.getMessage());
    }
    return jwt;
  }

  private boolean intersect(Collection list1, Collection list2) {
    if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
      return false;
    }
    Set<String> set1 = new HashSet<>(list1);
    Set<String> set2 = new HashSet<>(list2);
    set1.retainAll(set2);
    return !set1.isEmpty();
  }

  /**
   * Checks if the token is in the invalid tokens table.
   *
   * @param jwt
   * @return
   */
  public boolean isTokenInvalidated(DecodedJWT jwt) {
    return isTokenInvalidated(jwt.getId());
  }

  private boolean isTokenInvalidated(String id) {
    InvalidJwt invalidJwt = invalidJwtFacade.find(id);
    return invalidJwt != null;
  }

  /**
   * Renews a jwt if it is renewable, not invalidated, and expired but within the renewal period.
   * Will neither check user status nor update user roles
   * @param token
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException
   */
  public String autoRenewToken(String token) throws SigningKeyNotFoundException, NotRenewableException,
    InvalidationException {
    DecodedJWT jwt = verifyTokenForRenewal(token);
    return autoRenewToken(jwt, null);
  }

  /**
   * Renews a jwt if it is renewable, not invalidated, and expired but within the renewal period.
   * Will update roles if not null.
   * @param jwt
   * @param newRoles
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException
   */
  public String autoRenewToken(DecodedJWT jwt, String[] newRoles) throws SigningKeyNotFoundException,
    NotRenewableException, InvalidationException {
    boolean isRenewable = getRenewableClaim(jwt);
    if (!isRenewable) {
      throw new NotRenewableException("Token not renewable.");
    }
    Date currentTime = new Date();
    if (currentTime.before(jwt.getExpiresAt())) {
      throw new NotRenewableException("Token not expired.");
    }

    // Keep the same lifetime of the current token
    long lifetimeMs = jwt.getExpiresAt().getTime() - jwt.getIssuedAt().getTime();

    JsonWebToken _jwt = new JsonWebToken(jwt);
    _jwt.setExpiresAt(new Date(System.currentTimeMillis() + lifetimeMs));
    _jwt.setNotBefore(new Date());
    
    Map<String, Object> claims = new HashMap<>(3);
    String[] roles = newRoles == null ? _jwt.getRole().toArray(new String[0]) : newRoles;
    addDefaultClaimsIfMissing(claims, _jwt.isRenewable(), getExpLeewayOrDefault(_jwt.getExpLeeway()), roles);
    String renewedToken = createToken(_jwt, claims);

    invalidateJWT(jwt.getId(), jwt.getExpiresAt(), _jwt.getExpLeeway());
    return renewedToken;
  }
  
  public String renewToken(String token, Date newExp, Date notBefore, boolean invalidate,
      Map<String, Object> claims)
      throws SigningKeyNotFoundException, NotRenewableException, InvalidationException {
    return renewToken(token, newExp, notBefore, invalidate, claims, false);
  }
  
  /**
   * Creates a new token with the same values as the given token but with newExp and notBefore.
   * @param token Token to renew
   * @param newExp New expiration date
   * @param notBefore New not-valid-before date
   * @param invalidate Flag whether to invalidate the old token or not
   * @param claims Set of claims added to the new token
   * @param force Flag whether to check if it is time to renew or not
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException
   */
  public String renewToken(String token, Date newExp, Date notBefore, boolean invalidate,
      Map<String, Object> claims, boolean force)
      throws SigningKeyNotFoundException, NotRenewableException, InvalidationException {
    DecodedJWT jwt = verifyTokenForRenewal(token);
    if (!force) {
      Date currentTime = new Date();
      if (currentTime.before(jwt.getExpiresAt())) {
        throw new NotRenewableException("Token not expired.");
      }
    }
    JsonWebToken _jwt = new JsonWebToken(jwt);
    _jwt.setExpiresAt(newExp);
    _jwt.setNotBefore(notBefore);
    claims = addDefaultClaimsIfMissing(claims, _jwt.isRenewable(), getExpLeewayOrDefault(_jwt.getExpLeeway()),
        _jwt.getRole().toArray(new String[1]));
    String renewedToken = createToken(_jwt, claims);

    if (invalidate) {
      invalidateJWT(jwt.getId(), jwt.getExpiresAt(), _jwt.getExpLeeway());
    }
    return renewedToken;
  }
  
  public Map<String, Object> addDefaultClaimsIfMissing(Map<String, Object> userClaims, boolean isRenewable, int leeway,
      String[] roles) {
    if (userClaims == null) {
      userClaims = new HashMap<>(3);
      userClaims.put(RENEWABLE, isRenewable);
      userClaims.put(EXPIRY_LEEWAY, leeway);
      userClaims.put(ROLES, roles);
    } else {
      userClaims.putIfAbsent(RENEWABLE, isRenewable);
      userClaims.putIfAbsent(EXPIRY_LEEWAY, leeway);
      userClaims.putIfAbsent(ROLES, roles);
    }
    return userClaims;
  }
  
  public DecodedJWT verifyTokenForRenewal(String token) throws SigningKeyNotFoundException, NotRenewableException {
    DecodedJWT jwt;
    try {
      jwt = verifyToken(token, null);
    } catch (VerificationException ex) {
      throw new NotRenewableException(ex.getMessage());
    }
    return jwt;
  }

  private void invalidateJWT(String id, Date exp, int leeway) throws InvalidationException {
    try {
      InvalidJwt invalidJwt = new InvalidJwt(id, exp, leeway);
      invalidJwtFacade.persist(invalidJwt);
    } catch (Exception e) {
      throw new InvalidationException("Could not persist token.", e.getCause());
    }
  }

  /**
   * Checks if the expiry date plus the expiry leeway of the jwt is in the past.
   *
   * @param jwt
   * @return
   */
  public boolean passedRenewal(DecodedJWT jwt) {
    int expLeeway = getExpLeewayClaim(jwt);
    return passedRenewal(jwt.getExpiresAt(), expLeeway);
  }

  /**
   * Checks if the expiry date plus the expiry leeway(given in seconds) is in the past.
   *
   * @param exp
   * @param expLeeway
   * @return
   */
  public boolean passedRenewal(Date exp, int expLeeway) {
    Date expireOn = new Date(exp.getTime() + expLeeway * 1000);
    return expireOn.before(new Date());
  }

  /**
   * Generates a jwt id which is a random UUID.
   *
   * @return
   */
  public String generateJti() {
    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString();
    InvalidJwt invalidJwt = invalidJwtFacade.find(randomUUIDString);
    //Wikipedia: the probability to find a duplicate within 103 trillion version 4 UUIDs is one in a billion.
    while (invalidJwt != null) {
      uuid = UUID.randomUUID();
      randomUUIDString = uuid.toString();
      invalidJwt = invalidJwtFacade.find(randomUUIDString);
    }
    return randomUUIDString;
  }

  /**
   * Gets the key identified by the key name, if it exists or creates a new key if it does not exist.
   *
   * @param keyName
   * @param alg
   * @return
   * @throws NoSuchAlgorithmException
   */
  public JwtSigningKey getOrCreateSigningKey(String keyName, SignatureAlgorithm alg) throws NoSuchAlgorithmException {
    return jwtSigningKeyFacade.getOrCreateSigningKey(keyName, alg);
  }

  /**
   * Creates a new signing key with the given key name and algorithm.
   *
   * @param keyName
   * @param alg
   * @return
   * @throws NoSuchAlgorithmException
   * @throws io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException
   */
  public JwtSigningKey createNewSigningKey(String keyName, SignatureAlgorithm alg) throws NoSuchAlgorithmException,
      DuplicateSigningKeyException {
    return jwtSigningKeyFacade.createNewSigningKey(keyName, alg);
  }

  /**
   * Delete a signing key to invalidate all keys signed by the key.
   *
   * @param keyName a unique name given to signing key when created.
   */
  public void deleteSigningKey(String keyName) {
    jwtSigningKeyFacade.remove(keyName);
  }

  public JwtSigningKey findSigningKeyById(Integer id) {
    return jwtSigningKeyFacade.find(id);
  }
  
  /**
   * Removes expired tokens from invalidated tokens table.
   *
   * @return
   */
//  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int cleanupInvalidTokens() {
    List<InvalidJwt> expiredTokens = invalidJwtFacade.findExpired();
    int count = 0;
    for (InvalidJwt expiredToken : expiredTokens) {
      if (passedRenewal(expiredToken.getExpirationTime(), expiredToken.getRenewableForSec())) {
        invalidJwtFacade.remove(expiredToken);
        count++;
      }
    }
    return count;
  }
  
  public boolean markOldSigningKeys() {
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.findByName(Constants.ONE_TIME_JWT_SIGNING_KEY_NAME);
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -Constants.ONE_TIME_JWT_SIGNING_KEY_ROTATION_DAYS);
    if (jwtSigningKey != null && jwtSigningKey.getCreatedOn().before(cal.getTime())) {
      removeMarkedKeys();//remove if there is an old marked but not deleted.
      jwtSigningKeyFacade.renameSigningKey(jwtSigningKey, Constants.OLD_ONE_TIME_JWT_SIGNING_KEY_NAME);
      try {
        jwtSigningKeyFacade.getOrCreateSigningKey(Constants.ONE_TIME_JWT_SIGNING_KEY_NAME, SignatureAlgorithm.HS256);
      } catch (NoSuchAlgorithmException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
      return true;
    }
    return false;
  }
  
  public void removeMarkedKeys() {
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.findByName(Constants.OLD_ONE_TIME_JWT_SIGNING_KEY_NAME);
    if (jwtSigningKey != null) {
      jwtSigningKeyFacade.remove(jwtSigningKey);
    }
  }
  
  /**
   * Get the ELK signing key, create a new one if doesn't exists then returns
   * it.
   * @param alg
   * @return
   * @throws NoSuchAlgorithmException
   */
  public String getSigningKeyForELK(SignatureAlgorithm alg) throws NoSuchAlgorithmException {
    return getOrCreateSigningKey(Constants.ELK_SIGNING_KEY_NAME, alg).getSecret();
  }
  
  /**
   * Creare a token with the ELK signing key.
   * @return
   * @throws DuplicateSigningKeyException
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   */
  public String createTokenForELK(String subjectName, String issuer,
      Map<String, Object> claims, Date expiresAt, SignatureAlgorithm alg)
      throws DuplicateSigningKeyException, NoSuchAlgorithmException,
      SigningKeyNotFoundException {
    
    return createToken(Constants.ELK_SIGNING_KEY_NAME,
        false, issuer, null, expiresAt, null, subjectName.toLowerCase(),
        claims, alg);
  }

  /**
   *
   * @param alg
   * @return
   * @throws NoSuchAlgorithmException
   */
  public String getSigningKeyForProxy(SignatureAlgorithm alg) throws NoSuchAlgorithmException {
    return getOrCreateSigningKey(Constants.PROXY_SIGNING_KEY_NAME, alg).getSecret();
  }

  /**
   *
   * @param subjectName
   * @param issuer
   * @param claims
   * @param expiresAt
   * @param alg
   * @return
   * @throws DuplicateSigningKeyException
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   */
  public String createTokenForProxy(String subjectName, String issuer, String[] audience, Map<String, Object> claims,
    Date expiresAt, SignatureAlgorithm alg) throws DuplicateSigningKeyException, NoSuchAlgorithmException,
    SigningKeyNotFoundException {
    return createToken(Constants.PROXY_SIGNING_KEY_NAME, false, issuer, audience, expiresAt, new Date(),
      subjectName.toLowerCase(), claims, alg);
  }
}
