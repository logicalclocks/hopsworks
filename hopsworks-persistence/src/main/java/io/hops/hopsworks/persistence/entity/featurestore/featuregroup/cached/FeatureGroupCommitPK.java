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

import javax.persistence.Embeddable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class FeatureGroupCommitPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "feature_group_id")
  private Integer featureGroupId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "commit_id")
  private Long commitId;

  public FeatureGroupCommitPK() {
  }

  public FeatureGroupCommitPK(Integer featureGroupId, Long commitId) {
    this.featureGroupId = featureGroupId;
    this.commitId = commitId;
  }

  public Integer getFeatureGroupId() {
    return featureGroupId;
  }

  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }

  public Long getCommitId() {
    return commitId;
  }

  public void setCommitId(Long commitId) {
    this.commitId = commitId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += featureGroupId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FeatureGroupCommitPK)) {
      return false;
    }
    FeatureGroupCommitPK other = (FeatureGroupCommitPK) object;
    if (!this.featureGroupId.equals(other.featureGroupId)) {
      return false;
    }
    if (!this.commitId.equals(other.commitId)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.featurestore.featuregroup,cached.FeatureGroupCommitPK[ " +
            "featureGroupId=" + featureGroupId + ", commitId=" + commitId + "]";
  }
}
