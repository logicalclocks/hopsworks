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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "TBLS", catalog = "metastore", schema = "")
@NamedQueries({
    @NamedQuery(name = "HiveTable.findById", query = "SELECT ht FROM HiveTbls ht WHERE ht.tblId = :id"),
    @NamedQuery(name = "HiveTable.findByNameAndDbId", query = "SELECT ht FROM HiveTbls ht " +
        "WHERE ht.tblName = :name AND ht.dbId = :dbId")})
public class HiveTbls implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "TBL_ID")
  private Long tblId;
  @Column(name = "DB_ID")
  private Long dbId;
  @Size(max = 256)
  @Column(name = "TBL_NAME")
  private String tblName;
  @Size(max = 128)
  @Column(name = "TBL_TYPE")
  private String tblType;
  @JoinColumn(name = "SD_ID", referencedColumnName = "SD_ID")
  @ManyToOne
  private HiveSds sdId;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "hiveTbls")
  private Collection<HivePartitionKeys> hivePartitionKeysCollection;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "hiveTbls")
  private Collection<HiveTableParams> hiveTableParamsCollection;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentTblId")
  private Collection<HiveKeyConstraints> hiveKeyConstraintsCollection;

  public HiveTbls() {
  }

  public HiveTbls(Long tblId) {
    this.tblId = tblId;
  }

  public Long getTblId() {
    return tblId;
  }

  public void setTblId(Long tblId) {
    this.tblId = tblId;
  }

  public Long getDbId() {
    return dbId;
  }

  public void setDbId(Long dbId) {
    this.dbId = dbId;
  }

  public String getTblName() {
    return tblName;
  }

  public void setTblName(String tblName) {
    this.tblName = tblName;
  }

  public String getTblType() {
    return tblType;
  }

  public void setTblType(String tblType) {
    this.tblType = tblType;
  }

  public HiveSds getSdId() {
    return sdId;
  }

  public void setSdId(HiveSds sdId) {
    this.sdId = sdId;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HivePartitionKeys> getHivePartitionKeysCollection() {
    return hivePartitionKeysCollection;
  }

  public void setHivePartitionKeysCollection(Collection<HivePartitionKeys> hivePartitionKeysCollection) {
    this.hivePartitionKeysCollection = hivePartitionKeysCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HiveTableParams> getHiveTableParamsCollection() {
    return hiveTableParamsCollection;
  }

  public void setHiveTableParamsCollection(Collection<HiveTableParams> hiveTableParamsCollection) {
    this.hiveTableParamsCollection = hiveTableParamsCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HiveKeyConstraints> getHiveKeyConstraintsCollection() {
    return hiveKeyConstraintsCollection;
  }

  public void setHiveKeyConstraintsCollection(Collection<HiveKeyConstraints> hiveKeyConstraintsCollection) {
    this.hiveKeyConstraintsCollection = hiveKeyConstraintsCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (tblId != null ? tblId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveTbls)) {
      return false;
    }
    HiveTbls other = (HiveTbls) object;
    if ((this.tblId == null && other.tblId != null) || (this.tblId != null && !this.tblId.equals(other.tblId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveTbls[ tblId=" + tblId + " ]";
  }
  
}
