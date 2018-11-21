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
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.jwt.dao.InvalidJwt;
import io.hops.hopsworks.jwt.dao.InvalidJwtFacade;
import io.hops.hopsworks.jwt.dao.JwtSigningKey;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.ROLES;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.ejb.AccessLocalException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
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
  public String createToken(JsonWebToken jwt) throws SigningKeyNotFoundException {
    String token = createToken(jwt.getKeyId(), jwt.getIssuer(), jwt.getAudience().toArray(new String[0]), jwt.
        getExpiresAt(), jwt.getNotBefore(), jwt.getSubject(), jwt.isRenewable(), jwt.getExpLeeway(), jwt.
        getRole().toArray(new String[0]), jwt.getAlgorithm());
    return token;
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
   * @param isRenewable
   * @param expLeeway
   * @param roles
   * @param algorithm
   * @return three Base64-URL strings separated by dots
   * @throws io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException
   */
  public String createToken(String keyId, String issuer, String[] audience, Date expiresAt, Date notBefore,
      String subject, boolean isRenewable, int expLeeway, String[] roles, SignatureAlgorithm algorithm) throws
      SigningKeyNotFoundException {
    String token = JWT.create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withIssuedAt(new Date())
        .withExpiresAt(expiresAt)
        .withNotBefore(notBefore)
        .withJWTId(generateJti())
        .withSubject(subject)
        .withClaim(RENEWABLE, isRenewable)
        .withClaim(EXPIRY_LEEWAY, getExpLeewayOrDefault(expLeeway))
        .withArrayClaim(ROLES, roles)
        .sign(algorithmFactory.getAlgorithm(algorithm, keyId));
    return token;
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
   * @param isRenewable
   * @param expLeeway
   * @param roles
   * @param algorithm
   * @return three Base64-URL strings separated by dots
   * @throws NoSuchAlgorithmException
   * @throws SigningKeyNotFoundException
   * @throws DuplicateSigningKeyException
   */
  public String createToken(String keyName, boolean createNewKey, String issuer, String[] audience, Date expiresAt,
      Date notBefore, String subject, boolean isRenewable, int expLeeway, String[] roles, SignatureAlgorithm algorithm)
      throws NoSuchAlgorithmException, SigningKeyNotFoundException, DuplicateSigningKeyException {
    JwtSigningKey signingKey;
    if (createNewKey) {
      signingKey = createNewSigningKey(keyName, algorithm);
    } else {
      signingKey = getOrCreateSigningKey(keyName, algorithm);
    }
    String token = createToken(signingKey.getId().toString(), issuer, audience, expiresAt, notBefore, subject,
        isRenewable, expLeeway, roles, algorithm);
    return token;
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
    return expLeewayClaim == null? DEFAULT_EXPIRY_LEEWAY : getExpLeewayOrDefault(expLeewayClaim.asInt());
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
    return renewableClaim != null ? renewableClaim.asBoolean() : DEFAULT_RENEWABLE;
  }
  
  /**
   * Get roles from jwt of empty array if no such claim exists.
   * @param jwt
   * @return 
   */
  public String[] getRolesClaim(DecodedJWT jwt) {
    Claim rolesClaim = jwt.getClaim(ROLES);
    return rolesClaim == null ? new String[0] : rolesClaim.asArray(String.class);
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
      SigningKeyNotFoundException, VerificationException {
    JsonWebToken jwt = new JsonWebToken(JWT.decode(token));
    issuer = issuer == null || issuer.isEmpty() ? jwt.getIssuer() : issuer;
    DecodedJWT djwt = verifyToken(token, issuer, jwt.getExpLeeway(), algorithmFactory.getAlgorithm(jwt));

    if (isTokenInvalidated(djwt)) {
      throw new VerificationException("Invalidated token.");
    }

    Set<String> rolesSet = new HashSet<>(jwt.getRole());
    if (roles != null && !roles.isEmpty()) {
      if (!intersect(roles, rolesSet)) {
        throw new AccessLocalException("Client not authorized for this invocation.");
      }
    }

    Set<String> audiencesSet = new HashSet<>(jwt.getAudience());
    if (audiences != null && !audiences.isEmpty()) {
      if (!intersect(audiences, audiencesSet)) {
        throw new AccessLocalException("Token not issued for this recipient.");
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
   *
   * @param token
   * @param newExp expiry date of the new token
   * @param notBefore
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException
   */
  public String autoRenewToken(String token, Date newExp, Date notBefore) throws SigningKeyNotFoundException,
      NotRenewableException, InvalidationException {
    DecodedJWT jwt = verifyTokenForRenewal(token);
    boolean isRenewable = getRenewableClaim(jwt);
    if (!isRenewable) {
      throw new NotRenewableException("Token not renewable.");
    }
    Date currentTime = new Date();
    if (currentTime.before(jwt.getExpiresAt())) {
      throw new NotRenewableException("Token not expired.");
    }

    JsonWebToken _jwt = new JsonWebToken(jwt);
    _jwt.setExpiresAt(newExp);
    _jwt.setNotBefore(notBefore);
    String renewedToken = createToken(_jwt);

    invalidateJWT(jwt.getId(), jwt.getExpiresAt(), _jwt.getExpLeeway());
    return renewedToken;
  }
  
  /**
   * Creates a new token with the same values as the given token but with newExp and notBefore.
   * @param token
   * @param newExp
   * @param notBefore
   * @return
   * @throws SigningKeyNotFoundException
   * @throws NotRenewableException
   * @throws InvalidationException
   */
  public String renewToken(String token, Date newExp, Date notBefore) throws SigningKeyNotFoundException,
      NotRenewableException, InvalidationException {
    DecodedJWT jwt = verifyTokenForRenewal(token);
    Date currentTime = new Date();
    if (currentTime.before(jwt.getExpiresAt())) {
      throw new NotRenewableException("Token not expired.");
    }

    JsonWebToken _jwt = new JsonWebToken(jwt);
    _jwt.setExpiresAt(newExp);
    _jwt.setNotBefore(notBefore);
    String renewedToken = createToken(_jwt);

    invalidateJWT(jwt.getId(), jwt.getExpiresAt(), _jwt.getExpLeeway());
    return renewedToken;
  }
  
  private DecodedJWT verifyTokenForRenewal(String token) throws SigningKeyNotFoundException, NotRenewableException {
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

  /**
   * Removes expired tokens from invalidated tokens table.
   *
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean markOldSigningKeys() {
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.findByName(Constants.ONE_TIME_JWT_SIGNING_KEY_NAME);
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -Constants.ONE_TIME_JWT_SIGNING_KEY_ROTATION_DAYS);
    if (jwtSigningKey != null && jwtSigningKey.getCreatedOn().before(cal.getTime())) {
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
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void removeMarkedKeys() {
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.findByName(Constants.OLD_ONE_TIME_JWT_SIGNING_KEY_NAME);
    if (jwtSigningKey != null) {
      jwtSigningKeyFacade.remove(jwtSigningKey);
    }
  }

}
