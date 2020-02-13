/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.remote;

import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Set;

import static io.hops.hopsworks.common.remote.OpenIdConstant.AUTHORIZATION_ENDPOINT;
import static io.hops.hopsworks.common.remote.OpenIdConstant.CLAIMS_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.ISSUER;
import static io.hops.hopsworks.common.remote.OpenIdConstant.JWKS_URI;
import static io.hops.hopsworks.common.remote.OpenIdConstant.REGISTRATION_ENDPOINT;
import static io.hops.hopsworks.common.remote.OpenIdConstant.RESPONSE_TYPES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.REVOCATION_ENDPOINT;
import static io.hops.hopsworks.common.remote.OpenIdConstant.SCOPES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.SUBJECT_TYPES_SUPPORTED;
import static io.hops.hopsworks.common.remote.OpenIdConstant.TOKEN_ENDPOINT;
import static io.hops.hopsworks.common.remote.OpenIdConstant.USERINFO_ENDPOINT;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static javax.json.JsonValue.ValueType.STRING;

@XmlRootElement
public class OpenIdProviderConfig implements Serializable {
  
  private JsonObject document;
  private String issuerURI;
  private String authorizationEndpoint;
  private String tokenEndpoint;
  private String userInfoEndpoint;
  private String revocationEndpoint;
  private String jwkSetURI;
  private String registrationEndpointURI;
  private Set<String> responseTypesSupported;
  private Set<String> scopesSupported;
  private Set<String> claimsSupported;
  private Set<String> idTokenSigningAlgorithmsSupported;
  private Set<String> idTokenEncryptionAlgorithmsSupported;
  private Set<String> idTokenEncryptionMethodsSupported;
  private Set<String> subjectTypesSupported;
  
  public OpenIdProviderConfig(JsonObject document) {
    this.document = document;
    this.issuerURI = getStrOrNull(document, ISSUER);
    this.authorizationEndpoint = getStrOrNull(document, AUTHORIZATION_ENDPOINT);
    this.tokenEndpoint = getStrOrNull(document, TOKEN_ENDPOINT);
    this.userInfoEndpoint = getStrOrNull(document, USERINFO_ENDPOINT);
    this.revocationEndpoint = getStrOrNull(document, REVOCATION_ENDPOINT);
    this.registrationEndpointURI = getStrOrNull(document, REGISTRATION_ENDPOINT);
    this.jwkSetURI = getStrOrNull(document, JWKS_URI);
    this.scopesSupported = getValues(SCOPES_SUPPORTED);
    this.claimsSupported = getValues(CLAIMS_SUPPORTED);
    this.responseTypesSupported = getValues(RESPONSE_TYPES_SUPPORTED);
    this.idTokenSigningAlgorithmsSupported = getValues(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
    this.idTokenEncryptionAlgorithmsSupported = getValues(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
    this.idTokenEncryptionMethodsSupported = getValues(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
    this.subjectTypesSupported = getValues(SUBJECT_TYPES_SUPPORTED);
  }
  
  public OpenIdProviderConfig(OauthClient client) {
    this.issuerURI = client.getProviderURI();
    this.authorizationEndpoint = client.getAuthorisationEndpoint();
    this.tokenEndpoint = client.getTokenEndpoint();
    this.userInfoEndpoint = client.getUserInfoEndpoint();
  }
  
  public String getIssuerURI() {
    return issuerURI;
  }
  
  public void setIssuerURI(String issuerURI) {
    this.issuerURI = issuerURI;
  }
  
  
  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }
  
  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }
  
  public String getTokenEndpoint() {
    return tokenEndpoint;
  }
  
  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }
  
  public String getUserInfoEndpoint() {
    return userInfoEndpoint;
  }
  
  public void setUserInfoEndpoint(String userInfoEndpoint) {
    this.userInfoEndpoint = userInfoEndpoint;
  }
  
  public String getRevocationEndpoint() {
    return revocationEndpoint;
  }
  
  public void setRevocationEndpoint(String revocationEndpoint) {
    this.revocationEndpoint = revocationEndpoint;
  }
  
  public String getJwkSetURI() {
    return jwkSetURI;
  }
  
  public void setJwkSetURI(String jwkSetURI) {
    this.jwkSetURI = jwkSetURI;
  }
  
  public String getRegistrationEndpointURI() {
    return registrationEndpointURI;
  }
  
  public void setRegistrationEndpointURI(String registrationEndpointURI) {
    this.registrationEndpointURI = registrationEndpointURI;
  }
  
  public JsonObject getDocument() {
    return document;
  }
  
  public void setDocument(JsonObject document) {
    this.document = document;
  }
  
  public Set<String> getResponseTypesSupported() {
    return responseTypesSupported;
  }
  
  public void setResponseTypesSupported(Set<String> responseTypesSupported) {
    this.responseTypesSupported = responseTypesSupported;
  }
  
  public Set<String> getScopesSupported() {
    return scopesSupported;
  }
  
  public void setScopesSupported(Set<String> scopesSupported) {
    this.scopesSupported = scopesSupported;
  }
  
  public Set<String> getClaimsSupported() {
    return claimsSupported;
  }
  
  public void setClaimsSupported(Set<String> claimsSupported) {
    this.claimsSupported = claimsSupported;
  }
  
  public Set<String> getIdTokenSigningAlgorithmsSupported() {
    return idTokenSigningAlgorithmsSupported;
  }
  
  public void setIdTokenSigningAlgorithmsSupported(Set<String> idTokenSigningAlgorithmsSupported) {
    this.idTokenSigningAlgorithmsSupported = idTokenSigningAlgorithmsSupported;
  }
  
  public Set<String> getIdTokenEncryptionAlgorithmsSupported() {
    return idTokenEncryptionAlgorithmsSupported;
  }
  
  public void setIdTokenEncryptionAlgorithmsSupported(Set<String> idTokenEncryptionAlgorithmsSupported) {
    this.idTokenEncryptionAlgorithmsSupported = idTokenEncryptionAlgorithmsSupported;
  }
  
  public Set<String> getIdTokenEncryptionMethodsSupported() {
    return idTokenEncryptionMethodsSupported;
  }
  
  public void setIdTokenEncryptionMethodsSupported(Set<String> idTokenEncryptionMethodsSupported) {
    this.idTokenEncryptionMethodsSupported = idTokenEncryptionMethodsSupported;
  }
  
  public Set<String> getSubjectTypesSupported() {
    return subjectTypesSupported;
  }
  
  public void setSubjectTypesSupported(Set<String> subjectTypesSupported) {
    this.subjectTypesSupported = subjectTypesSupported;
  }
  
  private String getStrOrNull(JsonObject jsonObj, String key) {
    if (jsonObj.containsKey(key))
      return jsonObj.getString(key);
    return null;
  }
  
  private Set<String> getValues(String key) {
    JsonArray jsonArray = document.getJsonArray(key);
    if (isNull(jsonArray)) {
      return emptySet();
    } else {
      return jsonArray
        .stream()
        .filter(element -> element.getValueType() == STRING)
        .map(element -> (JsonString) element)
        .map(JsonString::getString)
        .collect(toSet());
    }
  }
  
  @Override
  public String toString() {
    return "OpenIdProviderConfig{" +
      "document=" + document +
      ", issuerURI='" + issuerURI + '\'' +
      ", authorizationEndpoint='" + authorizationEndpoint + '\'' +
      ", tokenEndpoint='" + tokenEndpoint + '\'' +
      ", userInfoEndpoint='" + userInfoEndpoint + '\'' +
      ", revocationEndpoint='" + revocationEndpoint + '\'' +
      ", jwkSetURI='" + jwkSetURI + '\'' +
      ", registrationEndpointURI='" + registrationEndpointURI + '\'' +
      ", responseTypesSupported=" + responseTypesSupported +
      ", scopesSupported=" + scopesSupported +
      ", claimsSupported=" + claimsSupported +
      ", idTokenSigningAlgorithmsSupported=" + idTokenSigningAlgorithmsSupported +
      ", idTokenEncryptionAlgorithmsSupported=" + idTokenEncryptionAlgorithmsSupported +
      ", idTokenEncryptionMethodsSupported=" + idTokenEncryptionMethodsSupported +
      ", subjectTypesSupported=" + subjectTypesSupported +
      '}';
  }
}
