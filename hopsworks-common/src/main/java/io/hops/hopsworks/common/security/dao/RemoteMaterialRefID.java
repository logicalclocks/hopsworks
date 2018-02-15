/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.security.dao;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RemoteMaterialRefID implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Column(name = "username",
          nullable = false,
          length = 128)
  private String username;
  
  @Column(name = "path",
          nullable = false)
  private String path;
  
  public RemoteMaterialRefID() {
  }
  
  public RemoteMaterialRefID(String username, String path) {
    this.username = username;
    this.path = path;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(username, path);
  }
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    if (other instanceof RemoteMaterialRefID) {
      return username.equals(((RemoteMaterialRefID) other).getUsername())
          && path.equals(((RemoteMaterialRefID) other).getPath());
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return "Username <" + username + ">, Path <" + path + ">";
  }
}
