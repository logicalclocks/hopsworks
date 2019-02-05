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
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class RawDataPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  public RawDataPK() {
  }

  public RawDataPK(int fieldid, int tupleid) {
    this.fieldid = fieldid;
    this.tupleid = tupleid;
  }

  public void copy(RawDataPK rawdataPK) {
    this.fieldid = rawdataPK.getFieldid();
    this.tupleid = rawdataPK.getTupleid();
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
  }

  public void setTupleid(int tupleid) {
    this.tupleid = tupleid;
  }

  public int getFieldid() {
    return this.fieldid;
  }

  public int getTupleid() {
    return this.tupleid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) this.fieldid;
    hash += (int) this.tupleid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawDataPK)) {
      return false;
    }
    RawDataPK other = (RawDataPK) object;

    if (this.fieldid != other.fieldid) {
      return false;
    }

    return this.tupleid == other.tupleid;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.RawDataPK[ fieldid="
            + this.fieldid + ", tupleid= " + this.tupleid + " ]";
  }
}
