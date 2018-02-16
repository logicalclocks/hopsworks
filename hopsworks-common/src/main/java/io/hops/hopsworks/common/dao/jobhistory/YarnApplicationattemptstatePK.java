/*
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
 *
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
