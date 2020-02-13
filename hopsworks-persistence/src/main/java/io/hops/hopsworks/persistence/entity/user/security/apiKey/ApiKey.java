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
package io.hops.hopsworks.persistence.entity.user.security.apiKey;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.hops.hopsworks.persistence.entity.user.Users;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "api_key",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ApiKey.findAll",
    query = "SELECT a FROM ApiKey a")
  ,
  @NamedQuery(name = "ApiKey.findByPrefix",
    query = "SELECT a FROM ApiKey a WHERE a.prefix = :prefix")
  ,
  @NamedQuery(name = "ApiKey.findByUser",
    query = "SELECT a FROM ApiKey a WHERE a.user = :user")
  ,
  @NamedQuery(name = "ApiKey.findByUserAndName",
    query = "SELECT a FROM ApiKey a WHERE a.user = :user AND a.name = :name")})
public class ApiKey implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 45)
  @Column(name = "prefix")
  private String prefix;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 512)
  @Column(name = "secret")
  private String secret;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 256)
  @Column(name = "salt")
  private String salt;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Column(name = "modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modified;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "name")
  private String name;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "apiKey")
  private Collection<ApiKeyScope> apiKeyScopeCollection;
  @JoinColumn(name = "user_id",
      referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users user;

  public ApiKey() {
  }

  public ApiKey(Users user, String prefix, String secret, String salt, Date created, Date modified, String name) {
    this.user = user;
    this.prefix = prefix;
    this.secret = secret;
    this.salt = salt;
    this.created = created;
    this.modified = modified;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getPrefix() {
    return prefix;
  }
  
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
  
  @XmlTransient
  @JsonIgnore
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
  
  @XmlTransient
  @JsonIgnore
  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<ApiKeyScope> getApiKeyScopeCollection() {
    return apiKeyScopeCollection;
  }

  public void setApiKeyScopeCollection(Collection<ApiKeyScope> apiKeyScopeCollection) {
    this.apiKeyScopeCollection = apiKeyScopeCollection;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
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
    if (!(object instanceof ApiKey)) {
      return false;
    }
    ApiKey other = (ApiKey) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "apiKey[ id=" + id + " ]";
  }
  
}
