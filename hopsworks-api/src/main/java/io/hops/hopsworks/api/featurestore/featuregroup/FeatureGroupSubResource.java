/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;

public abstract class FeatureGroupSubResource extends FeaturestoreSubResource {
  private Integer featureGroupId;

  public Integer getFeatureGroupId() {
    return featureGroupId;
  }

  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }

  public Featuregroup getFeaturegroup(Project project) throws FeaturestoreException, ProjectException {
    if (featureGroupId != null) {
      return getFeaturegroupController().getFeaturegroupById(getFeaturestore(project), featureGroupId);
    }
    return null;
  }

  public Featuregroup getFeaturegroup(Featurestore featurestore) throws FeaturestoreException {
    if (featureGroupId != null) {
      return getFeaturegroupController().getFeaturegroupById(featurestore, featureGroupId);
    }
    return null;
  }

  public Featuregroup getFeaturegroup() throws ProjectException, FeaturestoreException {
    if (featureGroupId != null) {
      return getFeaturegroupController().getFeaturegroupById(getFeaturestore(), featureGroupId);
    }
    return null;
  }

  protected abstract FeaturegroupController getFeaturegroupController();
}
