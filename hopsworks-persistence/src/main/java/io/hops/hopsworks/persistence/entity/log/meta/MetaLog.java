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

package io.hops.hopsworks.persistence.entity.log.meta;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import io.hops.hopsworks.persistence.entity.log.operation.OperationType;
import io.hops.hopsworks.persistence.entity.metadata.Metadata;
import io.hops.hopsworks.persistence.entity.metadata.MetadataPK;

@Entity
@Table(name = "meta_log", catalog = "hopsworks")
public class MetaLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_id")
  private Integer metaId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_field_id")
  private Integer metaFieldId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_tuple_id")
  private Integer metaTupleId;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_op_type")
  private OperationType metaOpType;

  public MetaLog() {

  }

  public MetaLog(Metadata metaData, OperationType opType) {
    MetadataPK pk = metaData.getMetadataPK();
    this.metaId = pk.getId();
    this.metaFieldId = pk.getFieldid();
    this.metaTupleId = pk.getTupleid();
    this.metaOpType = opType;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getMetaId() {
    return metaId;
  }

  public void setMetaId(Integer metaId) {
    this.metaId = metaId;
  }

  public Integer getMetaFieldId() {
    return metaFieldId;
  }

  public void setMetaFieldId(Integer metaFieldId) {
    this.metaFieldId = metaFieldId;
  }

  public Integer getMetaTupleId() {
    return metaTupleId;
  }

  public void setMetaTupleId(Integer metaTupleId) {
    this.metaTupleId = metaTupleId;
  }
  
  public OperationType getMetaOpType() {
    return metaOpType;
  }

  public void setMetaOpType(OperationType metaOpType) {
    this.metaOpType = metaOpType;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 73 * hash + Objects.hashCode(this.id);
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
    final MetaLog other = (MetaLog) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

}
