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
