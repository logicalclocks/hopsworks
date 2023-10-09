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
package io.hops.hopsworks.persistence.entity.remote.oauth;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "oauth_client",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "OauthClient.findAll",
    query = "SELECT o FROM OauthClient o")
  ,
  @NamedQuery(name = "OauthClient.findById",
    query = "SELECT o FROM OauthClient o WHERE o.id = :id")
  ,
  @NamedQuery(name = "OauthClient.findByClientId",
    query = "SELECT o FROM OauthClient o WHERE o.clientId = :clientId")
  ,
  @NamedQuery(name = "OauthClient.findByClientSecret",
    query
      = "SELECT o FROM OauthClient o WHERE o.clientSecret = :clientSecret")
  ,
  @NamedQuery(name = "OauthClient.findByProviderLogoURI",
    query
      = "SELECT o FROM OauthClient o WHERE o.providerLogoURI = :providerLogoURI")
  ,
  @NamedQuery(name = "OauthClient.findByProviderURI",
    query
      = "SELECT o FROM OauthClient o WHERE o.providerURI = :providerURI")
  ,
  @NamedQuery(name = "OauthClient.findByProviderName",
    query
      = "SELECT o FROM OauthClient o WHERE o.providerName = :providerName")
  ,
  @NamedQuery(name = "OauthClient.findByProviderDisplayName",
    query
      = "SELECT o FROM OauthClient o WHERE o.providerDisplayName = :providerDisplayName")})
public class OauthClient implements Serializable {
  
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 256)
  @Column(name = "client_id")
  private String clientId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 2048)
  @Column(name = "client_secret")
  private String clientSecret;
  @Size(max = 2048)
  @Column(name = "provider_logo_uri")
  private String providerLogoURI;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 2048)
  @Column(name = "provider_uri")
  private String providerURI;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 256)
  @Column(name = "provider_name")
  private String providerName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 45)
  @Column(name = "provider_display_name")
  private String providerDisplayName;
  @Size(max = 1024)
  @Column(name = "authorisation_endpoint")
  private String authorisationEndpoint;
  @Size(max = 1024)
  @Column(name = "token_endpoint")
  private String tokenEndpoint;
  @Size(max = 1024)
  @Column(name = "userinfo_endpoint")
  private String userInfoEndpoint;
  @Size(max = 1024)
  @Column(name = "end_session_endpoint")
  private String endSessionEndpoint;
  @Size(max = 45)
  @Column(name = "logout_redirect_param")
  private String logoutRedirectParam;
  @Size(max = 1024)
  @Column(name = "jwks_uri")
  private String jwksURI;
  @Basic(optional = false)
  @NotNull
  @Column(name = "provider_metadata_endpoint_supported")
  private boolean providerMetadataEndpointSupported;
  @Basic(optional = false)
  @NotNull
  @Column(name = "offline_access")
  private boolean offlineAccess;
  @Column(name = "code_challenge")
  private boolean codeChallenge;
  @Column(name = "code_challenge_method")
  @Enumerated(EnumType.STRING)
  private CodeChallengeMethod codeChallengeMethod;
  @Column(name = "verify_email")
  private boolean verifyEmail;
  @OneToMany(cascade = CascadeType.ALL,
    mappedBy = "clientId")
  private Collection<OauthLoginState> oauthLoginStateCollection;
  @Basic(optional = false)
  @NotNull
  @Size(max = 256)
  @Column(name = "given_name_claim")
  private String givenNameClaim = "given_name";
  @Basic(optional = false)
  @NotNull
  @Size(max = 256)
  @Column(name = "family_name_claim")
  private String familyNameClaim = "family_name";
  @Basic(optional = false)
  @NotNull
  @Size(max = 256)
  @Column(name = "email_claim")
  private String emailClaim = "email";
  @Size(max = 256)
  @Column(name = "group_claim")
  private String groupClaim;
  
  public OauthClient() {
  }
  
  public OauthClient(Integer id) {
    this.id = id;
  }
  
  public OauthClient(String clientId, String clientSecret, String providerURI, String providerName,
    String providerLogoURI, String providerDisplayName, boolean providerMetadataEndpointSupported,
    String authorisationEndpoint, String tokenEndpoint, String userInfoEndpoint, String endSessionEndpoint,
    String logoutRedirectParam, String jwksURI, boolean offlineAccess, boolean codeChallenge,
    CodeChallengeMethod codeChallengeMethod, boolean verifyEmail) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.providerURI = providerURI;
    this.providerName = providerName;
    this.providerLogoURI = providerLogoURI;
    this.providerDisplayName = providerDisplayName;
    this.providerMetadataEndpointSupported = providerMetadataEndpointSupported;
    this.authorisationEndpoint = authorisationEndpoint;
    this.tokenEndpoint = tokenEndpoint;
    this.userInfoEndpoint = userInfoEndpoint;
    this.endSessionEndpoint = endSessionEndpoint;
    this.logoutRedirectParam = logoutRedirectParam;
    this.jwksURI = jwksURI;
    this.offlineAccess = offlineAccess;
    this.codeChallenge = codeChallenge;
    this.codeChallengeMethod = codeChallengeMethod;
    this.verifyEmail = verifyEmail;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getClientId() {
    return clientId;
  }
  
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
  
  public String getClientSecret() {
    return clientSecret;
  }
  
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
  
  public String getProviderLogoURI() {
    return providerLogoURI;
  }
  
  public void setProviderLogoURI(String providerLogoUri) {
    this.providerLogoURI = providerLogoUri;
  }
  
  public String getProviderURI() {
    return providerURI;
  }
  
  public void setProviderURI(String providerURI) {
    this.providerURI = providerURI;
  }
  
  public String getProviderName() {
    return providerName;
  }
  
  public void setProviderName(String authServerName) {
    this.providerName = authServerName;
  }
  
  public String getProviderDisplayName() {
    return providerDisplayName;
  }
  
  public void setProviderDisplayName(String authServerDisplayname) {
    this.providerDisplayName = authServerDisplayname;
  }
  
  public String getLogoutRedirectParam() {
    return logoutRedirectParam;
  }
  
  public void setLogoutRedirectParam(String logoutRedirectParam) {
    this.logoutRedirectParam = logoutRedirectParam;
  }
  
  public String getAuthorisationEndpoint() {
    return authorisationEndpoint;
  }
  
  public void setAuthorisationEndpoint(String authorisationEndpoint) {
    this.authorisationEndpoint = authorisationEndpoint;
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
  
  public String getEndSessionEndpoint() {
    return endSessionEndpoint;
  }
  
  public void setEndSessionEndpoint(String endSessionEndpoint) {
    this.endSessionEndpoint = endSessionEndpoint;
  }
  
  public String getJwksURI() {
    return jwksURI;
  }
  
  public void setJwksURI(String jwksURI) {
    this.jwksURI = jwksURI;
  }
  
  public void setUserInfoEndpoint(String userinfoEndpoint) {
    this.userInfoEndpoint = userinfoEndpoint;
  }
  
  public boolean getProviderMetadataEndpointSupported() {
    return providerMetadataEndpointSupported;
  }
  
  public void setProviderMetadataEndpointSupported(boolean providerMetadataEndpointSupported) {
    this.providerMetadataEndpointSupported = providerMetadataEndpointSupported;
  }
  
  public CodeChallengeMethod getCodeChallengeMethod() {
    return codeChallengeMethod;
  }
  
  public void setCodeChallengeMethod(CodeChallengeMethod codeChallengeMethod) {
    this.codeChallengeMethod = codeChallengeMethod;
  }
  
  public boolean isOfflineAccess() {
    return offlineAccess;
  }
  
  public void setOfflineAccess(boolean offlineAccess) {
    this.offlineAccess = offlineAccess;
  }
  
  public boolean isCodeChallenge() {
    return codeChallenge;
  }
  
  public void setCodeChallenge(boolean codeChallenge) {
    this.codeChallenge = codeChallenge;
  }
  
  public boolean isVerifyEmail() {
    return verifyEmail;
  }
  
  public void setVerifyEmail(boolean verifyEmail) {
    this.verifyEmail = verifyEmail;
  }
  
  public String getGivenNameClaim() {
    return givenNameClaim;
  }
  
  public void setGivenNameClaim(String givenNameClaim) {
    this.givenNameClaim = givenNameClaim;
  }
  
  public String getFamilyNameClaim() {
    return familyNameClaim;
  }
  
  public void setFamilyNameClaim(String familyNameClaim) {
    this.familyNameClaim = familyNameClaim;
  }
  
  public String getEmailClaim() {
    return emailClaim;
  }
  
  public void setEmailClaim(String emailClaim) {
    this.emailClaim = emailClaim;
  }
  
  public String getGroupClaim() {
    return groupClaim;
  }
  
  public void setGroupClaim(String groupClaim) {
    this.groupClaim = groupClaim;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<OauthLoginState> getOauthLoginStateCollection() {
    return oauthLoginStateCollection;
  }
  
  public void setOauthLoginStateCollection(Collection<OauthLoginState> oauthLoginStateCollection) {
    this.oauthLoginStateCollection = oauthLoginStateCollection;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof OauthClient)) {
      return false;
    }
    OauthClient other = (OauthClient) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "OauthClient{" +
      ", id=" + id +
      ", clientId='" + clientId + '\'' +
      ", providerLogoURI='" + providerLogoURI + '\'' +
      ", providerURI='" + providerURI + '\'' +
      ", providerName='" + providerName + '\'' +
      ", providerDisplayName='" + providerDisplayName + '\'' +
      ", authorisationEndpoint='" + authorisationEndpoint + '\'' +
      ", tokenEndpoint='" + tokenEndpoint + '\'' +
      ", userInfoEndpoint='" + userInfoEndpoint + '\'' +
      ", endSessionEndpoint='" + endSessionEndpoint + '\'' +
      ", logoutRedirectParam='" + logoutRedirectParam + '\'' +
      ", jwksURI='" + jwksURI + '\'' +
      ", providerMetadataEndpointSupported=" + providerMetadataEndpointSupported +
      ", offlineAccess=" + offlineAccess +
      ", codeChallenge=" + codeChallenge +
      ", codeChallengeMethod=" + codeChallengeMethod +
      ", verifyEmail=" + verifyEmail +
      ", oauthLoginStateCollection=" + oauthLoginStateCollection +
      ", givenNameClaim='" + givenNameClaim + '\'' +
      ", familyNameClaim='" + familyNameClaim + '\'' +
      ", emailClaim='" + emailClaim + '\'' +
      ", groupClaim='" + groupClaim + '\'' +
      '}';
  }

}
