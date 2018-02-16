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

package io.hops.hopsworks.common.dao.user.sshkey;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class AuthorizedSshkeysPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 64)
  @Column(name = "project")
  private String project;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 48)
  @Column(name = "user")
  private String user;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 64)
  @Column(name = "sshkey_name")
  private String sshkeyName;

  public AuthorizedSshkeysPK() {
  }

  public AuthorizedSshkeysPK(String project, String user, String sshkeyName) {
    this.project = project;
    this.user = user;
    this.sshkeyName = sshkeyName;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getSshkeyName() {
    return sshkeyName;
  }

  public void setSshkeyName(String sshkeyName) {
    this.sshkeyName = sshkeyName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (project != null ? project.hashCode() : 0);
    hash += (user != null ? user.hashCode() : 0);
    hash += (sshkeyName != null ? sshkeyName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof AuthorizedSshkeysPK)) {
      return false;
    }
    AuthorizedSshkeysPK other = (AuthorizedSshkeysPK) object;
    if ((this.project == null && other.project != null) || (this.project != null
            && !this.project.equals(other.project))) {
      return false;
    }
    if ((this.user == null && other.user != null) || (this.user != null
            && !this.user.equals(other.user))) {
      return false;
    }
    if ((this.sshkeyName == null && other.sshkeyName != null)
            || (this.sshkeyName != null && !this.sshkeyName.equals(
                    other.sshkeyName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.users.AuthorizedSshkeysPK[ project=" + project
            + ", user=" + user + ", sshkeyName=" + sshkeyName + " ]";
  }

}
