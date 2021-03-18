/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity class representing the cached_feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_group_commit", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeatureGroupCommit.findByLatestCommittedOn",
      query = "SELECT fgc FROM FeatureGroupCommit fgc " +
          "WHERE fgc.committedOn IN " +
              "(SELECT MAX(fgc.committedOn) FROM FeatureGroupCommit fgc " +
                "WHERE fgc.committedOn <= :requestedPointInTime " +
                "AND fgc.featureGroupCommitPK.featureGroupId = :featureGroupId) " +
          " AND fgc.featureGroupCommitPK.featureGroupId = :featureGroupId"),
    @NamedQuery(name = "FeatureGroupCommit.findLatestCommit",
        query = "SELECT fgc FROM FeatureGroupCommit fgc WHERE fgc.committedOn IN (SELECT MAX(fgc.committedOn) FROM " +
            "FeatureGroupCommit fgc WHERE fgc.featureGroupCommitPK.featureGroupId = :featureGroupId) " +
            "AND fgc.featureGroupCommitPK.featureGroupId = :featureGroupId"),
    @NamedQuery(name = "FeatureGroupCommit.findByValidation",
            query = "SELECT fgc FROM FeatureGroupCommit fgc WHERE fgc.validation = :validation")
     })

public class FeatureGroupCommit implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected FeatureGroupCommitPK featureGroupCommitPK;
  @JoinColumns({
      @JoinColumn(name = "feature_group_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
  @NotNull
  @Column(name = "committed_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date committedOn;
  @Column(name = "num_rows_updated")
  private Long numRowsUpdated;
  @Column(name = "num_rows_inserted")
  private Long numRowsInserted;
  @Column(name = "num_rows_deleted")
  private Long numRowsDeleted;
  @NotNull
  @JoinColumns({
      @JoinColumn(name = "inode_pid",
          referencedColumnName = "parent_id"),
      @JoinColumn(name = "inode_name",
          referencedColumnName = "name"),
      @JoinColumn(name = "partition_id",
          referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;
  @JoinColumn(name = "validation_id", referencedColumnName = "id")
  @OneToOne
  private FeatureGroupValidation validation;

  public FeatureGroupCommit() {}

  public FeatureGroupCommit(FeatureGroupCommitPK featureGroupCommitPK) {
    this.featureGroupCommitPK = featureGroupCommitPK;
  }
  public FeatureGroupCommit(Integer featureGroupId, Long commitId) {
    this.featureGroupCommitPK = new FeatureGroupCommitPK(featureGroupId, commitId);
  }

  public FeatureGroupCommitPK getFeatureGroupCommitPK() {
    return featureGroupCommitPK;
  }

  public void setFeatureGroupCommitPK(FeatureGroupCommitPK featureGroupCommitPK) {
    this.featureGroupCommitPK = featureGroupCommitPK;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public Long getCommittedOn() {
    return committedOn.getTime();
  }

  public void setCommittedOn(Date committedOn) {
    this.committedOn = committedOn;
  }

  public Long getNumRowsUpdated() {
    return numRowsUpdated;
  }

  public void setNumRowsUpdated(Long numRowsUpdated) {
    this.numRowsUpdated = numRowsUpdated;
  }

  public Long getNumRowsInserted() {
    return numRowsInserted;
  }

  public void setNumRowsInserted(Long numRowsInserted) {
    this.numRowsInserted = numRowsInserted;
  }

  public Long getNumRowsDeleted() {
    return numRowsDeleted;
  }

  public void setNumRowsDeleted(Long numRowsDeleted) {
    this.numRowsDeleted = numRowsDeleted;
  }

  public FeatureGroupValidation getValidation() {
    return validation;
  }

  public void setValidation(FeatureGroupValidation validation) {
    this.validation = validation;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (featureGroupCommitPK != null ? featureGroupCommitPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FeatureGroupCommitPK)) {
      return false;
    }
    FeatureGroupCommit other = (FeatureGroupCommit) object;
    if ((this.featureGroupCommitPK == null && other.featureGroupCommitPK != null) ||
            (this.featureGroupCommitPK != null && !this.featureGroupCommitPK.equals(other.featureGroupCommitPK))) {
      return false;
    }
    return true;
  }
}
