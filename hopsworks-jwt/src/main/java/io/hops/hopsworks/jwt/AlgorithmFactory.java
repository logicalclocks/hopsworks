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

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.ECDSAKeyProvider;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import io.hops.hopsworks.jwt.dao.JwtSigningKey;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.NotSupportedException;

@Stateless
public class AlgorithmFactory {

  @EJB
  private JwtSigningKeyFacade jwtSigningKeyFacade;

  public Algorithm getAlgorithm(DecodedJWT jwt) throws SigningKeyNotFoundException {
    return getAlgorithm(jwt.getAlgorithm(), jwt.getKeyId());
  }

  public Algorithm getAlgorithm(JsonWebToken jwt) throws SigningKeyNotFoundException {
    return getAlgorithm(jwt.getAlgorithm(), jwt.getKeyId());
  }

  public Algorithm getAlgorithm(String algorithm, String keyId) throws SigningKeyNotFoundException {
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(algorithm);
    return getAlgorithm(alg, keyId);
  }

  public Algorithm getAlgorithm(SignatureAlgorithm algorithm, String keyId) throws SigningKeyNotFoundException {
    switch (algorithm) {
      case ES256:
        return getES256Algorithm(keyId);
      case ES384:
        return getES384Algorithm(keyId);
      case ES512:
        return getES512Algorithm(keyId);
      case HS256:
        return getHS256Algorithm(keyId);
      case HS384:
        return getHS384Algorithm(keyId);
      case HS512:
        return getHS512Algorithm(keyId);
      case RS256:
        return getRS256Algorithm(keyId);
      case RS384:
        return getRS384Algorithm(keyId);
      case RS512:
        return getRS512Algorithm(keyId);
      default:
        throw new NotSupportedException("Algorithm not supported.");
    }
  }

  private Algorithm getES256Algorithm(String keyId) {
    ECDSAKeyProvider keyProvider = new ECDSAKeyProviderImpl(keyId);
    return Algorithm.ECDSA256(keyProvider);
  }

  private Algorithm getES384Algorithm(String keyId) {
    ECDSAKeyProvider keyProvider = new ECDSAKeyProviderImpl(keyId);
    return Algorithm.ECDSA384(keyProvider);
  }

  private Algorithm getES512Algorithm(String keyId) {
    ECDSAKeyProvider keyProvider = new ECDSAKeyProviderImpl(keyId);
    return Algorithm.ECDSA512(keyProvider);
  }

  private String getSigningKey(String keyId) throws SigningKeyNotFoundException {
    Integer id;
    try {
      id = Integer.parseInt(keyId);
    } catch (NumberFormatException e) {
      throw new SigningKeyNotFoundException("Signing key not found. The key id should be integer.");
    }
    JwtSigningKey signingKey = jwtSigningKeyFacade.find(id);
    if (signingKey == null) {
      throw new SigningKeyNotFoundException("Signing key not found.");
    }
    return signingKey.getSecret();
  }

  private Algorithm getHS256Algorithm(String keyId) throws SigningKeyNotFoundException {
    return Algorithm.HMAC256(getSigningKey(keyId));
  }

  private Algorithm getHS384Algorithm(String keyId) throws SigningKeyNotFoundException {
    return Algorithm.HMAC384(getSigningKey(keyId));
  }

  private Algorithm getHS512Algorithm(String keyId) throws SigningKeyNotFoundException {
    return Algorithm.HMAC512(getSigningKey(keyId));
  }

  private Algorithm getRS256Algorithm(String keyId) {
    RSAKeyProvider keyProvider = new RSAKeyProviderImpl(keyId);
    return Algorithm.RSA256(keyProvider);
  }

  private Algorithm getRS384Algorithm(String keyId) {
    RSAKeyProvider keyProvider = new RSAKeyProviderImpl(keyId);
    return Algorithm.RSA384(keyProvider);
  }

  private Algorithm getRS512Algorithm(String keyId) {
    RSAKeyProvider keyProvider = new RSAKeyProviderImpl(keyId);
    return Algorithm.RSA512(keyProvider);
  }

  private class RSAKeyProviderImpl implements RSAKeyProvider {

    final String privateKeyId;

    public RSAKeyProviderImpl(String keyId) {
      this.privateKeyId = keyId;
    }

    @Override
    public RSAPublicKey getPublicKeyById(String keyId) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrivateKeyId() {
      return privateKeyId;
    }

  }

  private class ECDSAKeyProviderImpl implements ECDSAKeyProvider {

    final String privateKeyId;

    public ECDSAKeyProviderImpl(String keyId) {
      this.privateKeyId = keyId;
    }

    @Override
    public ECPublicKey getPublicKeyById(String keyId) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ECPrivateKey getPrivateKey() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrivateKeyId() {
      return privateKeyId;
    }

  }

}
