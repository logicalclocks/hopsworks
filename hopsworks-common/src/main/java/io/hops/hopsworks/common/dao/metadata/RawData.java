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
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.meta_raw_data")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RawData.findAll",
          query = "SELECT r FROM RawData r"),
  @NamedQuery(name = "RawData.findByPrimaryKey",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK = :rawdataPK"),
  @NamedQuery(name = "RawData.findByFieldid",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK.fieldid = :fieldid"),
  @NamedQuery(name = "RawData.findByTupleid",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK.tupleid = :tupleid"),
  @NamedQuery(name = "RawData.lastInsertedTupleId",
          query
          = "SELECT r FROM RawData r GROUP BY r.rawdataPK.tupleid ORDER BY r.rawdataPK.tupleid desc")})
public class RawData implements EntityIntf, Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private RawDataPK rawdataPK;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldid",
          referencedColumnName = "fieldid")
  private Field fields;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "tupleid",
          referencedColumnName = "tupleid")
  private TupleToFile tupleToFile;

  @OneToMany(mappedBy = "rawdata",
          targetEntity = Metadata.class,
          fetch = FetchType.LAZY,
          cascade = {CascadeType.ALL})
  private List<Metadata> metadata;

  public RawData() {
    this.rawdataPK = new RawDataPK(-1, -1);
  }

  public RawData(RawDataPK rawdataPK) {
    this.rawdataPK = rawdataPK;
  }

  public RawData(int fieldid, int tupleid) {
    this.rawdataPK = new RawDataPK(fieldid, tupleid);
  }

  @Override
  public void copy(EntityIntf raw) {
    RawData r = (RawData) raw;

    this.rawdataPK.copy(r.getRawdataPK());
  }

  public void setRawdataPK(RawDataPK rawdataPK) {
    this.rawdataPK = rawdataPK;
  }

  public RawDataPK getRawdataPK() {
    return this.rawdataPK;
  }

  /*
   * get and set the parent entity
   */
  public void setField(Field fields) {
    this.fields = fields;
  }

  public Field getField() {
    return this.fields;
  }

  /*
   * get and set the parent entity
   */
  public void setTupleToFile(TupleToFile ttf) {
    this.tupleToFile = ttf;
  }

  public TupleToFile getTupleToFile() {
    return this.tupleToFile;
  }

  /*
   * get and set the metadata
   */
  public void setMetadata(List<Metadata> metadata) {
    this.metadata = metadata;
  }

  public List<Metadata> getMetadata() {
    return this.metadata;
  }

  public void resetMetadata() {
    this.metadata.clear();
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
    hash += (this.rawdataPK != null ? this.rawdataPK.hashCode() : 0);

    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawData)) {
      return false;
    }
    RawData other = (RawData) object;

    return !((this.rawdataPK == null && other.getRawdataPK() != null)
            || (this.rawdataPK != null
            && !this.rawdataPK.equals(other.getRawdataPK())));
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.RawData[ rawdataPK= " + this.rawdataPK + " ]";
  }

}
