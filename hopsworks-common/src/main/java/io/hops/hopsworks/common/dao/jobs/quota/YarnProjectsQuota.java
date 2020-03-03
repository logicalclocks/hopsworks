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

package io.hops.hopsworks.common.dao.jobs.quota;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.yarn_projects_quota")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnProjectsQuota.findAll",
          query = "SELECT y FROM YarnProjectsQuota y"),
  @NamedQuery(name = "YarnProjectsQuota.findByProjectname",
          query
          = "SELECT y FROM YarnProjectsQuota y WHERE y.projectname = :projectname"),
  @NamedQuery(name = "YarnProjectsQuota.findByQuotaRemaining",
          query
          = "SELECT y FROM YarnProjectsQuota y WHERE y.quotaRemaining = :quotaRemaining"),
  @NamedQuery(name = "YarnProjectsQuota.findByTotal",
          query = "SELECT y FROM YarnProjectsQuota y WHERE y.total = :total")})
public class YarnProjectsQuota implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 100)
  private String projectname;
  @Column(name = "quota_remaining")
  private float quotaRemaining;
  private float total;

  public YarnProjectsQuota() {
  }

  public YarnProjectsQuota(String projectname) {
    this.projectname = projectname;
  }

  public YarnProjectsQuota(String projectname, int quotaRemaining, int total) {
    this.projectname = projectname;
    this.quotaRemaining = quotaRemaining;
    this.total = total;
  }

  public String getProjectname() {
    return projectname;
  }

  public void setProjectname(String projectname) {
    this.projectname = projectname;
  }

  public float getQuotaRemaining() {
    return quotaRemaining;
  }

  public void setQuotaRemaining(float quotaRemaining) {
    this.quotaRemaining = quotaRemaining;
  }

  public float getTotal() {
    return total;
  }

  public void setTotal(float total) {
    this.total = total;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectname != null ? projectname.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnProjectsQuota)) {
      return false;
    }
    YarnProjectsQuota other = (YarnProjectsQuota) object;
    if ((this.projectname == null && other.projectname != null)
            || (this.projectname != null && !this.projectname.equals(
                    other.projectname))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.YarnProjectsQuota[ projectname=" + projectname
            + " ]";
  }

}
