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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "KEY_CONSTRAINTS", catalog = "metastore")
@XmlRootElement
public class HiveKeyConstraints implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HiveKeyConstraintsPK hiveKeyConstraintsPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "PARENT_INTEGER_IDX")
  private int parentIntegerIdx;
  @Basic(optional = false)
  @NotNull
  @Column(name = "CONSTRAINT_TYPE")
  private short constraintType;
  @JoinColumn(name = "PARENT_CD_ID", referencedColumnName = "CD_ID")
  @ManyToOne
  private HiveCds parentCdId;
  @JoinColumn(name = "PARENT_TBL_ID", referencedColumnName = "TBL_ID")
  @ManyToOne(optional = false)
  private HiveTbls parentTblId;
  @Column(name = "DEFAULT_VALUE")
  private String defaultValue;

  public HiveKeyConstraints() {
  }

  public HiveKeyConstraints(HiveKeyConstraintsPK hiveKeyConstraintsPK) {
    this.hiveKeyConstraintsPK = hiveKeyConstraintsPK;
  }

  public HiveKeyConstraints(long position, String constraintName) {
    this.hiveKeyConstraintsPK = new HiveKeyConstraintsPK(position, constraintName);
  }

  public HiveKeyConstraintsPK getHiveKeyConstraintsPK() {
    return hiveKeyConstraintsPK;
  }

  public void setHiveKeyConstraintsPK(HiveKeyConstraintsPK hiveKeyConstraintsPK) {
    this.hiveKeyConstraintsPK = hiveKeyConstraintsPK;
  }

  public int getParentIntegerIdx() {
    return parentIntegerIdx;
  }

  public void setParentIntegerIdx(int parentIntegerIdx) {
    this.parentIntegerIdx = parentIntegerIdx;
  }

  public short getConstraintType() {
    return constraintType;
  }

  public void setConstraintType(short constraintType) {
    this.constraintType = constraintType;
  }

  public HiveCds getParentCdId() {
    return parentCdId;
  }

  public void setParentCdId(HiveCds parentCdId) {
    this.parentCdId = parentCdId;
  }

  public HiveTbls getParentTblId() {
    return parentTblId;
  }

  public void setParentTblId(HiveTbls parentTblId) {
    this.parentTblId = parentTblId;
  }
  
  public String getDefaultValue() {
    return defaultValue;
  }
  
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (hiveKeyConstraintsPK != null ? hiveKeyConstraintsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveKeyConstraints)) {
      return false;
    }
    HiveKeyConstraints other = (HiveKeyConstraints) object;
    if ((this.hiveKeyConstraintsPK == null && other.hiveKeyConstraintsPK != null) ||
        (this.hiveKeyConstraintsPK != null && !this.hiveKeyConstraintsPK.equals(other.hiveKeyConstraintsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveKeyConstraints[ hiveKeyConstraintsPK=" +
        hiveKeyConstraintsPK + " ]";
  }
  
}
