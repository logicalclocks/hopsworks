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

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.RENEWABLE;
import static io.hops.hopsworks.jwt.Constants.ROLES;

public class JsonWebToken {

  private SignatureAlgorithm algorithm;
  private String issuer;
  private List<String> audience;
  private Date expiresAt;
  private Date notBefore;
  private String keyId;
  private String subject; //email
  private boolean renewable;
  private int expLeeway;
  private List<String> role;

  public JsonWebToken() {
  }

  public JsonWebToken(String algorithm, String issuer, List<String> audience, Date expiresAt,
      Date notBefore, String keyId, String subject, boolean renewable, int expLeeway, List<String> role) {
    this.algorithm = SignatureAlgorithm.valueOf(algorithm);
    this.issuer = issuer;
    this.audience = audience;
    this.expiresAt = expiresAt;
    this.notBefore = notBefore;
    this.keyId = keyId;
    this.subject = subject;
    this.renewable = renewable;
    this.expLeeway = expLeeway;
    this.role = role;
  }

  public JsonWebToken(DecodedJWT jwt) {
    this.algorithm = SignatureAlgorithm.valueOf(jwt.getAlgorithm());
    this.issuer = jwt.getIssuer();
    this.audience = jwt.getAudience();
    this.expiresAt = jwt.getExpiresAt();
    this.notBefore = jwt.getNotBefore();
    this.keyId = jwt.getKeyId();
    this.subject = jwt.getSubject();
    Claim renewableClaim = jwt.getClaim(RENEWABLE);
    this.renewable = renewableClaim != null ? renewableClaim.asBoolean() : DEFAULT_RENEWABLE;
    Claim expLeewayClaim = jwt.getClaim(EXPIRY_LEEWAY);
    this.expLeeway = expLeewayClaim != null ? expLeewayClaim.asInt() : DEFAULT_EXPIRY_LEEWAY;
    Claim roleClaim = jwt.getClaim(ROLES);
    this.role = roleClaim != null ? roleClaim.asList(String.class) : new ArrayList<>();
  }

  public SignatureAlgorithm getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(SignatureAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public List<String> getAudience() {
    return audience;
  }

  public void setAudience(List<String> audience) {
    this.audience = audience;
  }

  public boolean isRenewable() {
    return renewable;
  }

  public void setRenewable(boolean renewable) {
    this.renewable = renewable;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Date getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(Date notBefore) {
    this.notBefore = notBefore;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public int getExpLeeway() {
    return expLeeway;
  }

  public void setExpLeeway(int expLeeway) {
    this.expLeeway = expLeeway;
  }

  public List<String> getRole() {
    return role;
  }

  public void setRole(List<String> role) {
    this.role = role;
  }

  @Override
  public String toString() {
    return "JsonWebToken{" + "algorithm=" + algorithm + ", issuer=" + issuer + ", audience=" + audience + ", expiresAt="
        + expiresAt + ", notBefore=" + notBefore + ", keyId=" + keyId + ", subject=" + subject + ", renewable="
        + renewable + ", expLeeway=" + expLeeway + ", role=" + role + '}';
  }

}
