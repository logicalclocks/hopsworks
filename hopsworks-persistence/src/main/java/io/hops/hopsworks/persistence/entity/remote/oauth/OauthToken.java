/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.user.Users;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "oauth_token",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "OauthToken.findAll",
    query = "SELECT o FROM OauthToken o")
  ,
  @NamedQuery(name = "OauthToken.findById",
    query = "SELECT o FROM OauthToken o WHERE o.id = :id")
  ,
  @NamedQuery(name = "OauthToken.findByUser",
    query = "SELECT o FROM OauthToken o WHERE o.user = :user")
  ,
  @NamedQuery(name = "OauthToken.findByLoginTimeBefore",
    query = "SELECT o FROM OauthToken o WHERE o.loginTime < :loginTime")
  ,
  @NamedQuery(name = "OauthToken.findByLoginTime",
    query = "SELECT o FROM OauthToken o WHERE o.loginTime = :loginTime")})
public class OauthToken implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "uid")
  private Users user;
  
  @Column(name = "id_token", nullable = false, length = 8000)
  private String idToken;
  
  @Column(name = "access_token", length = 8000)
  private String accessToken;
  
  @Column(name = "refresh_token", length = 8000)
  private String refreshToken;
  
  @Column(name = "login_time", nullable = false, updatable = false, insertable = false,
    columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
  @Temporal(TemporalType.TIMESTAMP)
  private Date loginTime;
  
  public OauthToken() {
  }
  
  public OauthToken(Users user, String idToken, String accessToken) {
    this.user = user;
    this.idToken = idToken;
    this.accessToken = accessToken;
  }
  
  public Date getLoginTime() {
    return loginTime;
  }
  
  public void setLoginTime(Date loginTime) {
    this.loginTime = loginTime;
  }
  
  public String getRefreshToken() {
    return refreshToken;
  }
  
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
  
  public String getAccessToken() {
    return accessToken;
  }
  
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
  
  public String getIdToken() {
    return idToken;
  }
  
  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }
  
  public Users getUser() {
    return user;
  }
  
  public void setUser(Users user) {
    this.user = user;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }
  
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof OauthToken)) {
      return false;
    }
    OauthToken other = (OauthToken) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "OauthToken{" +
      "id=" + id +
      ", user=" + user +
      ", loginTime=" + loginTime +
      '}';
  }
}