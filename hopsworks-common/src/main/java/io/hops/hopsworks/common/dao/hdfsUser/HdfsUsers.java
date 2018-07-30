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

package io.hops.hopsworks.common.dao.hdfsUser;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
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
@Table(name = "hops.hdfs_users")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsUsers.findAll",
          query
          = "SELECT h FROM HdfsUsers h"),
  @NamedQuery(name = "HdfsUsers.findProjectUsers",
      query = "SELECT h FROM HdfsUsers h WHERE h.name LIKE CONCAT(:name, '\\_\\_%')"),
  @NamedQuery(name = "HdfsUsers.delete",
          query
          = "DELETE FROM HdfsUsers h WHERE h.id =:id"),
  @NamedQuery(name = "HdfsUsers.findByName",
          query
          = "SELECT h FROM HdfsUsers h WHERE h.name = :name")})
public class HdfsUsers implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 100)
  @Column(name = "name")
  private String name;
  @JoinTable(name = "hops.hdfs_users_groups",
          joinColumns
          = {
            @JoinColumn(name = "user_id",
                    referencedColumnName = "id")},
          inverseJoinColumns
          = {
            @JoinColumn(name = "group_id",
                    referencedColumnName = "id")})
  @ManyToMany
  private Collection<HdfsGroups> hdfsGroupsCollection;

  public HdfsUsers() {
  }

  public HdfsUsers(Integer id) {
    this.id = id;
  }

  public HdfsUsers(String name) {
    this.name = name;
  }

  public HdfsUsers(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    int index = this.name.indexOf("__");
    if (index == -1) {
      return this.name;
    }
    index += 2; //removes the "__"
    return this.name.substring(index);
  }

  public String getProject() {
    int index = this.name.indexOf("__");
    if (index == -1) {
      return "";
    }
    return this.name.substring(0, index);
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HdfsGroups> getHdfsGroupsCollection() {
    return hdfsGroupsCollection;
  }

  public void setHdfsGroupsCollection(
          Collection<HdfsGroups> hdfsGroupsCollection) {
    this.hdfsGroupsCollection = hdfsGroupsCollection;
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
    if (!(object instanceof HdfsUsers)) {
      return false;
    }
    HdfsUsers other = (HdfsUsers) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.hdfsUsers.HdfsUsers[ id=" + id + " ]";
  }

}
