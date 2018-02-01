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

package io.hops.hopsworks.common.dao.user;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.bbc_group")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "BbcGroup.findAll",
          query = "SELECT b FROM BbcGroup b"),
  @NamedQuery(name = "BbcGroup.findByGroupName",
          query = "SELECT b FROM BbcGroup b WHERE b.groupName = :groupName"),
  @NamedQuery(name = "BbcGroup.findByGroupDesc",
          query = "SELECT b FROM BbcGroup b WHERE b.groupDesc = :groupDesc"),
  @NamedQuery(name = "BbcGroup.findByGid",
          query = "SELECT b FROM BbcGroup b WHERE b.gid = :gid")})
public class BbcGroup implements Serializable {

  private static final long serialVersionUID = 1L;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 20)
  @Column(name = "group_name")
  private String groupName;
  @Size(max = 200)
  @Column(name = "group_desc")
  private String groupDesc;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "gid")
  private Integer gid;

  @ManyToMany(mappedBy = "bbcGroupCollection")
  private Collection<Users> usersCollection;

  public BbcGroup() {
  }

  public BbcGroup(Integer gid) {
    this.gid = gid;
  }

  public BbcGroup(Integer gid, String groupName) {
    this.gid = gid;
    this.groupName = groupName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupDesc() {
    return groupDesc;
  }

  public void setGroupDesc(String groupDesc) {
    this.groupDesc = groupDesc;
  }

  public Integer getGid() {
    return gid;
  }

  public void setGid(Integer gid) {
    this.gid = gid;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Users> getUsersCollection() {
    return usersCollection;
  }

  public void setUsersCollection(Collection<Users> usersCollection) {
    this.usersCollection = usersCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (gid != null ? gid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof BbcGroup)) {
      return false;
    }
    BbcGroup other = (BbcGroup) object;
    if ((this.gid == null && other.gid != null) || (this.gid != null
            && !this.gid.equals(other.gid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.model.BbcGroup[ gid=" + gid + " ]";
  }

}
