/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.util.Settings;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement
public class NewClientRegistrationDTO {
  
  @JsonProperty("application_type")
  private String applicationType;
  @JsonProperty("client_name")
  private String clientName;
  @JsonProperty("logo_uri")
  private String logoUri;
  @JsonProperty("subject_type")
  private String subjectType;
  @JsonProperty("tos_uri")
  private String tosUri;
  @JsonProperty("policy_uri")
  private String policyUri;
  @JsonProperty("token_endpoint_auth_method")
  private String tokenEndpointAuthMethod;
  @JsonProperty("scope")
  private Set<String> scope;
  @JsonProperty("grant_types")
  private Set<String> grantTypes;
  @JsonProperty("response_types")
  private Set<String> responseTypes;
  @JsonProperty("contacts")
  private Set<String> contacts;
  @JsonProperty("redirect_uris")
  private Set<String> redirectUris;
  
  public NewClientRegistrationDTO(Settings settings) {
    this.applicationType = "web";
    this.clientName = "Hopsworks";
    this.responseTypes = new HashSet<>();
    this.responseTypes.add("code");
    this.redirectUris = new HashSet<>();
    this.redirectUris.add(settings.getOauthRedirectUri(this.clientName));
    this.contacts = new HashSet<>();
    this.contacts.add("");
  }
  
  public String getApplicationType() {
    return applicationType;
  }
  
  public void setApplicationType(String applicationType) {
    this.applicationType = applicationType;
  }
  
  public String getClientName() {
    return clientName;
  }
  
  public void setClientName(String clientName) {
    this.clientName = clientName;
  }
  
  public String getLogoUri() {
    return logoUri;
  }
  
  public void setLogoUri(String logoUri) {
    this.logoUri = logoUri;
  }
  
  public String getSubjectType() {
    return subjectType;
  }
  
  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }
  
  public String getTosUri() {
    return tosUri;
  }
  
  public void setTosUri(String tosUri) {
    this.tosUri = tosUri;
  }
  
  public String getPolicyUri() {
    return policyUri;
  }
  
  public void setPolicyUri(String policyUri) {
    this.policyUri = policyUri;
  }
  
  public String getTokenEndpointAuthMethod() {
    return tokenEndpointAuthMethod;
  }
  
  public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
    this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
  }
  
  public Set<String> getScope() {
    return scope;
  }
  
  public void setScope(Set<String> scope) {
    this.scope = scope;
  }
  
  public Set<String> getGrantTypes() {
    return grantTypes;
  }
  
  public void setGrantTypes(Set<String> grantTypes) {
    this.grantTypes = grantTypes;
  }
  
  public Set<String> getResponseTypes() {
    return responseTypes;
  }
  
  public void setResponseTypes(Set<String> responseTypes) {
    this.responseTypes = responseTypes;
  }
  
  public Set<String> getContacts() {
    return contacts;
  }
  
  public void setContacts(Set<String> contacts) {
    this.contacts = contacts;
  }
  
  public Set<String> getRedirectUris() {
    return redirectUris;
  }
  
  public void setRedirectUris(Set<String> redirectUris) {
    this.redirectUris = redirectUris;
  }
  
  public String toJsonString() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString = mapper.writeValueAsString(this);
    return jsonInString;
  }
}
