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

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "yarn_applicationattemptstate",
        catalog = "hops",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnApplicationattemptstate.findAll",
          query = "SELECT y FROM YarnApplicationattemptstate y"),
  @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationid",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.yarnApplicationattemptstatePK.applicationid = :applicationid"),
  @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattemptid",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.yarnApplicationattemptstatePK.applicationattemptid = :applicationattemptid"),
  @NamedQuery(name
          = "YarnApplicationattemptstate.findByApplicationattempttrakingurl",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.applicationattempttrakingurl = :applicationattempttrakingurl")})
public class YarnApplicationattemptstate implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected YarnApplicationattemptstatePK yarnApplicationattemptstatePK;
  @Lob
  @Column(name = "applicationattemptstate")
  private byte[] applicationattemptstate;
  @Size(max = 120)
  @Column(name = "applicationattempttrakingurl")
  private String applicationattempttrakingurl;

  @JoinColumn(name = "applicationid",
          referencedColumnName = "applicationid",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private YarnApplicationstate yarnApplicationstate;

  public YarnApplicationattemptstate() {
  }

  public YarnApplicationattemptstate(
          YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
    this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
  }

  public YarnApplicationattemptstate(String applicationid,
          String applicationattemptid) {
    this.yarnApplicationattemptstatePK = new YarnApplicationattemptstatePK(
            applicationid, applicationattemptid);
  }

  public YarnApplicationattemptstatePK getYarnApplicationattemptstatePK() {
    return yarnApplicationattemptstatePK;
  }

  public void setYarnApplicationattemptstatePK(
          YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
    this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
  }

  public byte[] getApplicationattemptstate() {
    return applicationattemptstate;
  }

  public void setApplicationattemptstate(byte[] applicationattemptstate) {
    this.applicationattemptstate = applicationattemptstate;
  }

  public String getApplicationattempttrakingurl() {
    return applicationattempttrakingurl;
  }

  public void setApplicationattempttrakingurl(
          String applicationattempttrakingurl) {
    this.applicationattempttrakingurl = applicationattempttrakingurl;
  }

  public YarnApplicationstate getYarnApplicationstate() {
    return yarnApplicationstate;
  }

  public void setYarnApplicationstate(YarnApplicationstate yarnApplicationstate) {
    this.yarnApplicationstate = yarnApplicationstate;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (yarnApplicationattemptstatePK != null
            ? yarnApplicationattemptstatePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnApplicationattemptstate)) {
      return false;
    }
    YarnApplicationattemptstate other = (YarnApplicationattemptstate) object;
    if ((this.yarnApplicationattemptstatePK == null
            && other.yarnApplicationattemptstatePK != null)
            || (this.yarnApplicationattemptstatePK != null
            && !this.yarnApplicationattemptstatePK.equals(
                    other.yarnApplicationattemptstatePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.yarn.YarnApplicationattemptstate[ yarnApplicationattemptstatePK="
            + yarnApplicationattemptstatePK + " ]";
  }

}
