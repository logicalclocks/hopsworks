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

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class YarnApplicationattemptstatePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 45)
  @Column(name = "applicationid")
  private String applicationid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 45)
  @Column(name = "applicationattemptid")
  private String applicationattemptid;

  public YarnApplicationattemptstatePK() {
  }

  public YarnApplicationattemptstatePK(String applicationid,
          String applicationattemptid) {
    this.applicationid = applicationid;
    this.applicationattemptid = applicationattemptid;
  }

  public String getApplicationid() {
    return applicationid;
  }

  public void setApplicationid(String applicationid) {
    this.applicationid = applicationid;
  }

  public String getApplicationattemptid() {
    return applicationattemptid;
  }

  public void setApplicationattemptid(String applicationattemptid) {
    this.applicationattemptid = applicationattemptid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (applicationid != null ? applicationid.hashCode() : 0);
    hash += (applicationattemptid != null ? applicationattemptid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnApplicationattemptstatePK)) {
      return false;
    }
    YarnApplicationattemptstatePK other = (YarnApplicationattemptstatePK) object;
    if ((this.applicationid == null && other.applicationid != null)
            || (this.applicationid != null && !this.applicationid.equals(
                    other.applicationid))) {
      return false;
    }
    if ((this.applicationattemptid == null && other.applicationattemptid != null)
            || (this.applicationattemptid != null && !this.applicationattemptid.
            equals(other.applicationattemptid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.yarn.YarnApplicationattemptstatePK[ applicationid="
            + applicationid + ", applicationattemptid=" + applicationattemptid
            + " ]";
  }

}
