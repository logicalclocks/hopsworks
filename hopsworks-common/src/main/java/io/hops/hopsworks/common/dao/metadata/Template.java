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

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;

@Entity
@Table(name = "hopsworks.meta_templates")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Template.findAll",
          query = "SELECT t FROM Template t"),
  @NamedQuery(name = "Template.findById",
          query = "SELECT t FROM Template t WHERE t.id = :templateid"),
  @NamedQuery(name = "Template.findByName",
          query = "SELECT t FROM Template t WHERE lower(t.name) = :name")})
public class Template implements Serializable, EntityIntf, Comparable<Template> {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "templateid")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 250)
  @Column(name = "name")
  private String name;

  @OneToMany(mappedBy = "template",
          targetEntity = MTable.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL)
  private List<MTable> tables;

  @JoinTable(name = "hopsworks.meta_template_to_inode",
          inverseJoinColumns = {
            @JoinColumn(name = "inode_pid",
                    referencedColumnName = "parent_id"),
            @JoinColumn(name = "inode_name",
                    referencedColumnName = "name"),
            @JoinColumn(name = "partition_id",
                    referencedColumnName = "partition_id")
          },
          joinColumns = {
            @JoinColumn(name = "template_id",
                    referencedColumnName = "templateid")
          })
  @ManyToMany(fetch = FetchType.EAGER)
  private Collection<Inode> inodes;

  public Template() {
  }

  public Template(Integer templateid) {
    this.id = templateid;
    this.tables = new LinkedList<>();
    this.inodes = new LinkedList<>();
  }

  public Template(Integer templateid, String name) {
    this.id = templateid;
    this.name = name;
    this.tables = new LinkedList<>();
    this.inodes = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf template) {
    Template t = (Template) template;

    this.id = t.getId();
    this.name = t.getName();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public List<MTable> getMTables() {
    return this.tables;
  }

  public void setMTables(List<MTable> tables) {
    this.tables = tables;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Inode> getInodes() {
    return this.inodes;
  }

  public void setInodes(List<Inode> inodes) {
    this.inodes = inodes;
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
    if (!(object instanceof Template)) {
      return false;
    }
    Template other = (Template) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.Templates[ templateid=" + id + " name=" + name
            + " ]";
  }

  @Override
  public int compareTo(Template t) {
    if (this.getId() > t.getId()) {
      return 1;
    } else if (this.getId() < t.getId()) {
      return -1;
    }
    return 0;
  }

}
