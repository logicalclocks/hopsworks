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
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.yarn_applicationstate")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnApplicationstate.findAll",
          query = "SELECT y FROM YarnApplicationstate y"),
  @NamedQuery(name = "YarnApplicationstate.findByApplicationid",
          query
          = "SELECT y FROM YarnApplicationstate y WHERE y.applicationid = :applicationid"),
  @NamedQuery(name = "YarnApplicationstate.findByAppuser",
          query
          = "SELECT y FROM YarnApplicationstate y WHERE y.appuser = :appuser"),
  @NamedQuery(name = "YarnApplicationstate.findByAppname",
          query
          = "SELECT y FROM YarnApplicationstate y WHERE y.appname = :appname ORDER BY y.applicationid DESC"),
  @NamedQuery(name = "YarnApplicationstate.findByAppuserAndAppsmstate",
          query
          = "SELECT y FROM YarnApplicationstate y WHERE y.appuser = :appuser AND y.appsmstate = :appsmstate"),
  @NamedQuery(name = "YarnApplicationstate.findByAppsmstate",
          query
          = "SELECT y FROM YarnApplicationstate y WHERE y.appsmstate = :appsmstate")})
public class YarnApplicationstate implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 45)
  @Column(name = "applicationid")
  private String applicationid;
  @Lob
  @Column(name = "appstate")
  private byte[] appstate;
  @Size(max = 45)
  @Column(name = "appuser")
  private String appuser;
  @Size(max = 200)
  @Column(name = "appname")
  private String appname;
  @Size(max = 45)
  @Column(name = "appsmstate")
  private String appsmstate;

  public YarnApplicationstate() {
  }

  public YarnApplicationstate(String applicationid) {
    this.applicationid = applicationid;
  }

  public String getApplicationid() {
    return applicationid;
  }

  public void setApplicationid(String applicationid) {
    this.applicationid = applicationid;
  }

  public byte[] getAppstate() {
    return appstate;
  }

  public void setAppstate(byte[] appstate) {
    this.appstate = appstate;
  }

  public String getAppuser() {
    return appuser;
  }

  public void setAppuser(String appuser) {
    this.appuser = appuser;
  }

  public String getAppname() {
    return appname;
  }

  public void setAppname(String appname) {
    this.appname = appname;
  }

  public String getAppsmstate() {
    return appsmstate;
  }

  public void setAppsmstate(String appsmstate) {
    this.appsmstate = appsmstate;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (applicationid != null ? applicationid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnApplicationstate)) {
      return false;
    }
    YarnApplicationstate other = (YarnApplicationstate) object;
    if ((this.applicationid == null && other.applicationid != null)
            || (this.applicationid != null && !this.applicationid.equals(
                    other.applicationid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.jobhistory.YarnApplicationstate[ applicationid="
            + applicationid + " ]";
  }

}
