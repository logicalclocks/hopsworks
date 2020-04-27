/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class HiveKeyConstraintsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "POSITION")
  private long position;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 400)
  @Column(name = "CONSTRAINT_NAME")
  private String constraintName;

  public HiveKeyConstraintsPK() {
  }

  public HiveKeyConstraintsPK(long position, String constraintName) {
    this.position = position;
    this.constraintName = constraintName;
  }

  public long getPosition() {
    return position;
  }

  public void setPosition(long position) {
    this.position = position;
  }

  public String getConstraintName() {
    return constraintName;
  }

  public void setConstraintName(String constraintName) {
    this.constraintName = constraintName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) position;
    hash += (constraintName != null ? constraintName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveKeyConstraintsPK)) {
      return false;
    }
    HiveKeyConstraintsPK other = (HiveKeyConstraintsPK) object;
    if (this.position != other.position) {
      return false;
    }
    if ((this.constraintName == null && other.constraintName != null) ||
        (this.constraintName != null && !this.constraintName.equals(other.constraintName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveKeyConstraintsPK[ position=" + position +
        ", constraintName=" + constraintName + " ]";
  }
  
}
