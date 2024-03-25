/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.explicit;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;

public class ProvArtifact {
  
  private Integer id;
  private String project;
  private String name;
  private Integer version;
  
  public ProvArtifact() {
  }
  
  public ProvArtifact(Integer id, String projectName, String artifactName, Integer artifactVersion) {
    this.project = projectName;
    this.name = artifactName;
    this.version = artifactVersion;
    this.id = id;
  }
  
  public static ProvArtifact fromLinkAsParent(ProvExplicitNode link) {
    return new ProvArtifact(link.parentId(), link.parentProject(),
      link.parentName(), link.parentVersion());
  }
  
  public static ProvArtifact fromFeatureGroup(Featuregroup fg) {
    return new ProvArtifact(fg.getId(), fg.getFeaturestore().getProject().getName(),
      fg.getName(), fg.getVersion());
  }
  
  public static ProvArtifact fromFeatureView(FeatureView fv) {
    return new ProvArtifact(fv.getId(), fv.getFeaturestore().getProject().getName(),
      fv.getName(), fv.getVersion());
  }
  
  public static ProvArtifact fromTrainingDataset(TrainingDataset td) {
    return new ProvArtifact(td.getId(), td.getFeaturestore().getProject().getName(),
      td.getName(), td.getVersion());
  }

  public static ProvArtifact fromModel(ModelVersion model) {
    return new ProvArtifact(model.getId(), model.getModel().getProject().getName(),
        model.getModel().getName(), model.getVersion());
  }
  
  public String getProject() {
    return project;
  }
  
  public void setProject(String project) {
    this.project = project;
  }
  
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
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
}
