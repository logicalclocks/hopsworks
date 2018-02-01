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
