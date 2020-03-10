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
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "api_key_scope",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ApiKeyScope.findAll",
      query = "SELECT a FROM ApiKeyScope a")
  ,
    @NamedQuery(name = "ApiKeyScope.findById",
      query = "SELECT a FROM ApiKeyScope a WHERE a.id = :id")
  ,
    @NamedQuery(name = "ApiKeyScope.findByScope",
      query = "SELECT a FROM ApiKeyScope a WHERE a.scope = :scope")})
public class ApiKeyScope implements Serializable {

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
  @Enumerated(EnumType.STRING)
  @Column(name = "scope")
  private ApiScope scope;
  @JoinColumn(name = "api_key",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private ApiKey apiKey;

  public ApiKeyScope() {
  }

  public ApiKeyScope(ApiScope scope, ApiKey apiKey) {
    this.scope = scope;
    this.apiKey = apiKey;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ApiScope getScope() {
    return scope;
  }

  public void setScope(ApiScope scope) {
    this.scope = scope;
  }

  public ApiKey getApiKey() {
    return apiKey;
  }

  public void setApiKey(ApiKey apiKey) {
    this.apiKey = apiKey;
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
    if (!(object instanceof ApiKeyScope)) {
      return false;
    }
    ApiKeyScope other = (ApiKeyScope) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKeyScope[ id=" + id + " ]";
  }
  
}
