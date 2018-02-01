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

import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.validation.constraints.NotNull;

@Embeddable
public class SchemalessMetadataPK {

  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private int inodeId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_parent_id")
  private int inodeParentId;

  public SchemalessMetadataPK() {
  }

  public SchemalessMetadataPK(Integer inodeId, Integer inodeParentId) {
    this.inodeId = inodeId;
    this.inodeParentId = inodeParentId;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getInodeId() {
    return inodeId;
  }

  public void setInodeId(int inodeId) {
    this.inodeId = inodeId;
  }

  public int getInodeParentId() {
    return inodeParentId;
  }

  public void setInodeParentId(int inodeParentId) {
    this.inodeParentId = inodeParentId;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 47 * hash + Objects.hashCode(this.id);
    hash = 47 * hash + this.inodeId;
    hash = 47 * hash + this.inodeParentId;
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
    final SchemalessMetadataPK other = (SchemalessMetadataPK) obj;
    if (this.inodeId != other.inodeId) {
      return false;
    }
    if (this.inodeParentId != other.inodeParentId) {
      return false;
    }
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SchemalessMetadataPK{" + "id=" + id + ", inodeId=" + inodeId
            + ", inodeParentId=" + inodeParentId + '}';
  }

}
