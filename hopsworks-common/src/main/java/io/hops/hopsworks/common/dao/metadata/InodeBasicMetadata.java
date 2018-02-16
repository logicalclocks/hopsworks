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

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;

@Entity
@Table(name = "hopsworks.meta_inode_basic_metadata")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "InodeBasicMetadata.findAll",
          query = "SELECT m FROM InodeBasicMetadata m"),
  @NamedQuery(name = "InodeBasicMetadata.findById",
          query
          = "SELECT m FROM InodeBasicMetadata m WHERE m.inode.inodePK.parentId "
          + "= :id AND m.inode.inodePK.name = :name")})
public class InodeBasicMetadata implements Serializable, EntityIntf {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name"),
    @JoinColumn(name = "partition_id",
            referencedColumnName = "partition_id")})
  @OneToOne(optional = false)
  private Inode inode;

  @Size(max = 3000)
  @Column(name = "description")
  private String description;

  @Column(name = "searchable")
  private boolean searchable;

  public InodeBasicMetadata() {

  }

  public InodeBasicMetadata(Inode inode, String description, boolean searchable) {
    this.inode = inode;
    this.description = description;
    this.searchable = searchable;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  @Override
  public Integer getId() {
    return this.id;
  }

  public Inode getInode() {
    return this.inode;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean getSearchable() {
    return this.searchable;
  }

  @Override
  public void copy(EntityIntf entity) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }
}
