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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.user_group")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "UserGroup.findAll",
          query = "SELECT p FROM UserGroup p"),
  @NamedQuery(name = "UserGroup.findByUid",
          query = "SELECT p FROM UserGroup p WHERE p.userGroupPK.uid = :uid"),
  @NamedQuery(name = "UserGroup.findByGid",
          query = "SELECT p FROM UserGroup p WHERE p.userGroupPK.gid = :gid")})
public class UserGroup implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected UserGroupPK userGroupPK;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Users user;

  public UserGroup() {
  }

  public UserGroup(UserGroupPK userGroupPK) {
    this.userGroupPK = userGroupPK;
  }

  public UserGroup(int uid, int gid) {
    this.userGroupPK = new UserGroupPK(uid, gid);
  }

  public UserGroupPK getUserGroupPK() {
    return userGroupPK;
  }

  public void setUserGroupPK(UserGroupPK userGroupPK) {
    this.userGroupPK = userGroupPK;
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
    hash += (userGroupPK != null ? userGroupPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof UserGroup)) {
      return false;
    }
    UserGroup other = (UserGroup) object;
    if ((this.userGroupPK == null && other.userGroupPK != null)
            || (this.userGroupPK != null && !this.userGroupPK.equals(
                    other.userGroupPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.UserGroup[ userGroupPK="
            + userGroupPK + " ]";
  }

}
