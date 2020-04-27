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
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "CDS", catalog = "metastore")
public class HiveCds implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "CD_ID")
  private Long cdId;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "hiveCds")
  private Collection<HiveColumns> hiveColumnsCollection;

  public HiveCds() {
  }

  public HiveCds(Long cdId) {
    this.cdId = cdId;
  }

  public Long getCdId() {
    return cdId;
  }

  public void setCdId(Long cdId) {
    this.cdId = cdId;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HiveColumns> getHiveColumnsCollection() {
    return hiveColumnsCollection;
  }

  public void setHiveColumnsCollection(Collection<HiveColumns> hiveColumnsCollection) {
    this.hiveColumnsCollection = hiveColumnsCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (cdId != null ? cdId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveCds)) {
      return false;
    }
    HiveCds other = (HiveCds) object;
    if ((this.cdId == null && other.cdId != null) || (this.cdId != null && !this.cdId.equals(other.cdId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveCds[ cdId=" + cdId + " ]";
  }
  
}
