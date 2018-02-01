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
