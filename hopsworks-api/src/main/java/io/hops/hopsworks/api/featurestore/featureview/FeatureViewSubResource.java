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
package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.project.Project;

public abstract class FeatureViewSubResource extends FeaturestoreSubResource {
  private String name;
  private Integer version;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setFeatureView(String name, Integer version) {
    this.name = name;
    this.version = version;
  }

  public FeatureView getFeatureView(Project project) throws FeaturestoreException, ProjectException {
    return getFeatureViewController().getByNameVersionAndFeatureStore(name, version, getFeaturestore(project));
  }

  public FeatureView getFeatureView(Featurestore featurestore) throws FeaturestoreException {
    return getFeatureViewController().getByNameVersionAndFeatureStore(name, version, featurestore);
  }

  public FeatureView getFeatureView() throws ProjectException, FeaturestoreException {
    return getFeatureViewController().getByNameVersionAndFeatureStore(name, version, getFeaturestore());
  }

  protected abstract FeatureViewController getFeatureViewController();
}
