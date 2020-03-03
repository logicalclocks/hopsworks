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
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.meta_data")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Metadata.findAll",
          query
          = "SELECT m FROM Metadata m"),
  @NamedQuery(name = "Metadata.findByPrimaryKey",
          query
          = "SELECT m FROM Metadata m WHERE m.metadataPK = :metadataPK"),
  @NamedQuery(name = "Metadata.findById",
          query = "SELECT m FROM Metadata m WHERE m.metadataPK.id = :id")})
public class Metadata implements EntityIntf, Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private MetadataPK metadataPK;

  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
          max = 12000)
  @Column(name = "data")
  private String data;

  @ManyToOne(optional = false,
          fetch = FetchType.EAGER)
  @JoinColumns(value = {
    @JoinColumn(name = "fieldid",
            referencedColumnName = "fieldid",
            insertable = false,
            updatable = false),
    @JoinColumn(name = "tupleid",
            referencedColumnName = "tupleid",
            insertable = false,
            updatable = false)})
  private RawData rawdata;

  public Metadata() {
    this.metadataPK = new MetadataPK(-1, -1, -1);
  }

  public Metadata(MetadataPK metadataPK) {
    this.metadataPK = metadataPK;
  }

  public Metadata(MetadataPK metadataPK, String data) {
    this.metadataPK = metadataPK;
    this.data = data;
  }

  public void setMetadataPK(MetadataPK metadataPK) {
    this.metadataPK = metadataPK;
  }

  public MetadataPK getMetadataPK() {
    return this.metadataPK;
  }

  public void setRawData(RawData rawdata) {
    this.rawdata = rawdata;
  }

  public RawData getRawData() {
    return this.rawdata;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getData() {
    return this.data;
  }

  @Override
  public void copy(EntityIntf entity) {
    Metadata metadata = (Metadata) entity;

    this.metadataPK.copy(metadata.getMetadataPK());
    this.data = metadata.getData();
  }

  @Override
  public Integer getId() {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void setId(Integer id) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (this.metadataPK != null ? this.metadataPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Metadata)) {
      return false;
    }
    Metadata other = (Metadata) object;

    return !((this.metadataPK == null && other.getMetadataPK() != null)
            || (this.metadataPK != null
            && !this.metadataPK.equals(other.getMetadataPK())));
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.MetaData[ metadataPK= " + metadataPK + " ]";
  }
}
