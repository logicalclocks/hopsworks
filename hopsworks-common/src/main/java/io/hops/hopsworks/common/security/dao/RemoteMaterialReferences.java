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
