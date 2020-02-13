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
package io.hops.hopsworks.api.user.apiKey;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKeyScope;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ApiKeyDTO extends RestDTO<ApiKeyDTO> {
  
  private String key;
  private String name;
  private String prefix;
  private Date created;
  private Date modified;
  private List<ApiScope> scope;
  
  public ApiKeyDTO() {
  }
  
  public ApiKeyDTO(ApiKey apiKey) {
    this.name = apiKey.getName();
    this.prefix = apiKey.getPrefix();
    this.created = apiKey.getCreated();
    this.modified = apiKey.getModified();
    this.setFromKeyScope(apiKey.getApiKeyScopeCollection());
  }
  
  public ApiKeyDTO(String name, String prefix, Date created, Date modified, List<ApiScope> scope) {
    this.name = name;
    this.prefix = prefix;
    this.created = created;
    this.modified = modified;
    this.scope = scope;
  }
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String key) {
    this.key = key;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getPrefix() {
    return prefix;
  }
  
  public void setPrefix(String prefix) {
    this.prefix = prefix;
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
  
  public List<ApiScope> getScope() {
    return scope;
  }
  
  public void setScope(List<ApiScope> scope) {
    this.scope = scope;
  }
  
  public void setFromKeyScope(Collection<ApiKeyScope> scope) {
    this.scope = new ArrayList<>();
    for (ApiKeyScope keyScope : scope) {
      this.scope.add(keyScope.getScope());
    }
  }
}