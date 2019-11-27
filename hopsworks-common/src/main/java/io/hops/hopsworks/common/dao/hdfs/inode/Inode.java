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

package io.hops.hopsworks.common.dao.hdfs.inode;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.metadata.Template;

@Entity
@Table(name = "hdfs_inodes", catalog = "hops")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Inode.findAll",
          query
          = "SELECT i FROM Inode i"),
  @NamedQuery(name = "Inode.findById",
          query
          = "SELECT i FROM Inode i WHERE i.id = :id"),
  @NamedQuery(name = "Inode.findByParentId",
          query
          = "SELECT i FROM Inode i WHERE i.inodePK.parentId = :parentId"),
  @NamedQuery(name = "Inode.countByParentId",
          query
          = "SELECT COUNT(DISTINCT i.inodePK.name) FROM Inode i WHERE i.inodePK.parentId = :parentId"),
  @NamedQuery(name = "Inode.findByHdfsUser",
          query
          = "SELECT i FROM Inode i WHERE i.hdfsUser = :hdfsUser"),
  @NamedQuery(name = "Inode.findByName",
          query
          = "SELECT i FROM Inode i WHERE i.inodePK.name = :name"),
  @NamedQuery(name = "Inode.findByModificationTime",
          query
          = "SELECT i FROM Inode i WHERE i.modificationTime = :modificationTime"),
  @NamedQuery(name = "Inode.findByPrimaryKey",
          query
          = "SELECT i FROM Inode i WHERE i.inodePK = :inodePk"),
  @NamedQuery(name = "Inode.findByAccessTime",
          query
          = "SELECT i FROM Inode i WHERE i.accessTime = :accessTime"),
  @NamedQuery(name = "Inode.findBySymlink",
          query
          = "SELECT i FROM Inode i WHERE i.symlink = :symlink"),
  @NamedQuery(name = "Inode.findByQuotaEnabled",
          query
          = "SELECT i FROM Inode i WHERE i.quotaEnabled = :quotaEnabled"),
  @NamedQuery(name = "Inode.findByUnderConstruction",
          query
          = "SELECT i FROM Inode i WHERE i.underConstruction = :underConstruction"),
  @NamedQuery(name = "Inode.findRootByName",
          query
          = "SELECT i FROM Inode i WHERE i.inodePK.parentId = :parentId "
          + "AND i.inodePK.name = :name AND i.inodePK.partitionId = :partitionId"),
  @NamedQuery(name = "Inode.findByParentAndName",
          query = "SELECT i FROM Inode i WHERE i.inodePK.parentId = :parentId AND i.inodePK.name = :name"),
  @NamedQuery(name = "Inode.findHistoryFileByHdfsUser",
          query = "SELECT i FROM Inode i WHERE i.hdfsUser = :hdfsUser AND " + "i.inodePK.name LIKE '%snappy%'")})
public class Inode implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected InodePK inodePK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Long id;
  @Column(name = "modification_time")
  private BigInteger modificationTime;
  @Column(name = "access_time")
  private BigInteger accessTime;
  @JoinColumn(name = "user_id",
          referencedColumnName = "id")
  @OneToOne
  private HdfsUsers hdfsUser;
  @JoinColumn(name = "group_id",
          referencedColumnName = "id")
  @OneToOne
  private HdfsGroups hdfsGroup;
  @Column(name = "permission")
  private short permission;
  @Size(max = 255)
  @Column(name = "symlink")
  private String symlink;
  @Basic(optional = false)
  @NotNull
  @Column(name = "quota_enabled")
  private boolean quotaEnabled;
  @Basic(optional = false)
  @NotNull
  @Column(name = "under_construction")
  private boolean underConstruction;
  @Column(name = "meta_enabled")
  @NotNull
  private boolean metaEnabled;
  @Column(name = "is_dir")
  @NotNull
  private boolean dir;
  @Basic(optional = false)
  @NotNull
  @Column(name = "children_num")
  private int childrenNum;

  @Basic(optional = false)
  @NotNull
  @Column(name = "size")
  private long size;
  @ManyToMany(mappedBy = "inodes",
          cascade = CascadeType.PERSIST,
          fetch = FetchType.LAZY)
  private Collection<Template> templates;

  public Inode() {
  }

  public Inode(InodePK inodePK) {
    this.inodePK = inodePK;
  }

  public Inode(InodePK inodePK, Long id, boolean quotaEnabled,
          boolean underConstruction, boolean metaEnabled,
          boolean dir) {
    this.inodePK = inodePK;
    this.id = id;
    this.quotaEnabled = quotaEnabled;
    this.underConstruction = underConstruction;
    this.metaEnabled = metaEnabled;
    this.dir = dir;
  }

  //copy constructor
  public Inode(Inode inode) {
    this(new InodePK(inode.getInodePK().getParentId(), inode.getInodePK().
            getName(), inode.getInodePK().getPartitionId()), inode.getId(),
            inode.isQuotaEnabled(), inode.
            isUnderConstruction(), inode.
            isMetaEnabled(), inode.isDir());
  }

  public Inode(long parentId, String name, long partitionId) {
    this.inodePK = new InodePK(parentId, name, partitionId);
  }

  public InodePK getInodePK() {
    return inodePK;
  }

  public void setInodePK(InodePK inodePK) {
    this.inodePK = inodePK;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BigInteger getModificationTime() {
    return modificationTime;
  }

  public void setModificationTime(BigInteger modificationTime) {
    this.modificationTime = modificationTime;
  }

  public BigInteger getAccessTime() {
    return accessTime;
  }

  public void setAccessTime(BigInteger accessTime) {
    this.accessTime = accessTime;
  }

  public short getPermission() {
    return permission;
  }

  public void setPermission(short permission) {
    this.permission = permission;
  }

  public String getSymlink() {
    return symlink;
  }

  public void setSymlink(String symlink) {
    this.symlink = symlink;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }
  
  public int getChildrenNum() {
    return childrenNum;
  }
  
  public void setChildrenNum(int childrenNum) {
    this.childrenNum = childrenNum;
  }
  
  public HdfsUsers getHdfsUser() {
    return hdfsUser;
  }
  
  public void setHdfsUser(HdfsUsers hdfsUser) {
    this.hdfsUser = hdfsUser;
  }
  
  public HdfsGroups getHdfsGroup() {
    return hdfsGroup;
  }
  
  public void setHdfsGroup(HdfsGroups hdfsGroup) {
    this.hdfsGroup = hdfsGroup;
  }
  
  public void setDir(boolean dir) {
    this.dir = dir;
  }
  
  public boolean isDir() {
    return dir;
  }
  
  public boolean isMetaEnabled() {
    return metaEnabled;
  }
  
  public void setMetaEnabled(boolean metaEnabled) {
    this.metaEnabled = metaEnabled;
  }
  
  public boolean isQuotaEnabled() {
    return quotaEnabled;
  }
  
  public void setQuotaEnabled(boolean quotaEnabled) {
    this.quotaEnabled = quotaEnabled;
  }
  
  public boolean isUnderConstruction() {
    return underConstruction;
  }
  
  public void setUnderConstruction(boolean underConstruction) {
    this.underConstruction = underConstruction;
  }
  
  @XmlTransient
  public Collection<Template> getTemplates() {
    return this.templates;
  }

  public void setTemplates(Collection<Template> templates) {
    this.templates = templates;
  }

  public void addTemplate(Template template) {
    if (template != null) {
      this.templates.add(template);
    }
  }

  /**
   * for the time being we treat the many to many relationship between inodes
   * and templates as a many to one, where an inode may be associated only to
   * one template, while the same template may be associated to many inodes
   * <p/>
   * @return the template id
   */
  public int getTemplate() {

    int templateId = -1;

    if (this.templates != null && !this.templates.isEmpty()) {
      Iterator it = this.templates.iterator();
      Template template = (Template) it.next();
      templateId = template.getId();
    }

    return templateId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (inodePK != null ? inodePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Inode)) {
      return false;
    }
    Inode other = (Inode) object;
    if ((this.inodePK == null && other.inodePK != null) || (this.inodePK != null
            && !this.inodePK.equals(other.inodePK))) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.hdfs.inode.Inode[ inodePK= " + inodePK + " ]";
  }
}
