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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Embedding;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingDTO {

  @Getter
  private String indexName;
  @Getter
  private String colPrefix;
  @Getter
  private List<EmbeddingFeatureDTO> features;

  public EmbeddingDTO(Embedding embedding) {
    indexName = embedding.getVectorDbIndexName();
    features = embedding.getEmbeddingFeatures().stream().map(EmbeddingFeatureDTO::new).collect(Collectors.toList());
    colPrefix = embedding.getColPrefix();
  }

}
