/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
