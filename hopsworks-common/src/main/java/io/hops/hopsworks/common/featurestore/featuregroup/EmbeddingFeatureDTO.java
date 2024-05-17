/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.SimilarityFunctionType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.EmbeddingFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingFeatureDTO {

  @Getter
  private String name;
  @Getter
  private SimilarityFunctionType similarityFunctionType;
  @Getter
  private Integer dimension;
  @Getter
  private ModelDto model;

  public EmbeddingFeatureDTO(String name, SimilarityFunctionType similarityFunctionType, Integer dimension) {
    this.name = name;
    this.similarityFunctionType = similarityFunctionType;
    this.dimension = dimension;
  }

  public EmbeddingFeatureDTO(EmbeddingFeature feature) {
    name = feature.getName();
    similarityFunctionType = feature.getSimilarityFunctionType();
    dimension = feature.getDimension();
    if (feature.getModelVersion() != null) {
      model = new ModelDto(
          // model registry id is same as project id
          feature.getModelVersion().getModel().getProject().getId(),
          feature.getModelVersion().getModel().getName(),
          feature.getModelVersion().getVersion());
    }
  }
}
