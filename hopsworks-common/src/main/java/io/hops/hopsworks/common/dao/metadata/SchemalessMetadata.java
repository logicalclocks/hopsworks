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

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;

@Entity
@Table(name = "hopsworks.meta_data_schemaless")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SchemalessMetadata.findByInode",
          query
          = "SELECT d FROM SchemalessMetadata d WHERE d.inode = :inode")})
public class SchemalessMetadata implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private SchemalessMetadataPK pk;

  @JoinColumns({
    @JoinColumn(name = "inode_parent_id",
            referencedColumnName = "parent_id",
            insertable = false,
            updatable = false),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name"),
    @JoinColumn(name = "inode_partition_id",
            referencedColumnName = "partition_id")})
  @OneToOne(optional = false)
  private Inode inode;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 12000)
  @Column(name = "data")
  private String data;

  public SchemalessMetadata() {
  }

  public SchemalessMetadata(Inode inode) {
    this.pk = new SchemalessMetadataPK(inode.getId(), inode.getInodePK().
            getParentId());
    this.inode = inode;
  }

  public SchemalessMetadata(Inode inode, String data) {
    this.pk = new SchemalessMetadataPK(inode.getId(), inode.getInodePK().
            getParentId());
    this.inode = inode;
    this.data = data;
  }

  public SchemalessMetadataPK getPK() {
    return pk;
  }

  public void setPK(SchemalessMetadataPK pk) {
    this.pk = pk;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 37 * hash + Objects.hashCode(this.pk);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SchemalessMetadata other = (SchemalessMetadata) obj;
    if (!Objects.equals(this.pk, other.pk)) {
      return false;
    }
    return true;
  }

}
