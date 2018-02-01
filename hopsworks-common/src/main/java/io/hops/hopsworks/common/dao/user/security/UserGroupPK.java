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

package io.hops.hopsworks.common.dao.user.security;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class UserGroupPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "uid")
  private int uid;
  @Basic(optional = false)
  @NotNull
  @Column(name = "gid")
  private int gid;

  public UserGroupPK() {
  }

  public UserGroupPK(int uid, int gid) {
    this.uid = uid;
    this.gid = gid;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public int getGid() {
    return gid;
  }

  public void setGid(int gid) {
    this.gid = gid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) uid;
    hash += (int) gid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof UserGroupPK)) {
      return false;
    }
    UserGroupPK other = (UserGroupPK) object;
    if (this.uid != other.uid) {
      return false;
    }
    if (this.gid != other.gid) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.UserGroupPK[ uid=" + uid + ", gid="
            + gid + " ]";
  }

}
