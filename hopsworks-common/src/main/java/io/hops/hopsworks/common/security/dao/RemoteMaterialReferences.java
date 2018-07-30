/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.common.security.dao;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "hopsworks.remote_material_references")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RemoteMaterialReferences.findAll",
                query = "SELECT r FROM RemoteMaterialReferences r")})
public class RemoteMaterialReferences implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @EmbeddedId
  private RemoteMaterialRefID identifier;
  
  @Column(name = "`references`",
          nullable = false)
  @Basic(optional = false)
  private Integer references;
  
  @Column(name = "`lock`",
          nullable = false)
  @Basic(optional = false)
  private boolean lock;
  
  @Column(name = "lock_id",
          nullable = false,
          length = 30)
  private String lockId;
  
  public RemoteMaterialReferences() {
  }
  
  public RemoteMaterialReferences(RemoteMaterialRefID identifier) {
    this.identifier = identifier;
    this.references = -1;
    this.lock = false;
    this.lockId = "";
  }
  
  public RemoteMaterialRefID getIdentifier() {
    return identifier;
  }
  
  public void setIdentifier(RemoteMaterialRefID identifier) {
    this.identifier = identifier;
  }
  
  public Integer getReferences() {
    return references;
  }
  
  public void setReferences(Integer references) {
    this.references = references;
  }
  
  public boolean getLock() {
    return lock;
  }
  
  public void setLock(boolean lock) {
    this.lock = lock;
  }
  
  public String getLockId() {
    return lockId;
  }
  
  public void setLockId(String lockId) {
    this.lockId = lockId;
  }
  
  public void incrementReferences() {
    ++references;
  }
  
  public Integer decrementReferences() {
    return --references;
  }
}
