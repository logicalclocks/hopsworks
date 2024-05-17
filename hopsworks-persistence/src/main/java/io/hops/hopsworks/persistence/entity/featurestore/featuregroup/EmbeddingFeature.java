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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup;

import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "embedding_feature", catalog = "hopsworks")
@XmlRootElement
public class EmbeddingFeature implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "embedding_id", referencedColumnName = "id")
  private Embedding embedding;
  @Column
  private String name;
  @Column
  private Integer dimension;
  @Column(name = "similarity_function_type")
  @Enumerated(EnumType.STRING)
  private SimilarityFunctionType similarityFunctionType;
  @JoinColumn(name = "model_version_id", referencedColumnName = "id")
  @OneToOne
  private ModelVersion modelVersion;

  public EmbeddingFeature() {
  }

  public EmbeddingFeature(Embedding embedding, String name, Integer dimension,
      SimilarityFunctionType similarityFunctionType) {
    this.embedding = embedding;
    this.name = name;
    this.dimension = dimension;
    this.similarityFunctionType = similarityFunctionType;
  }

  public EmbeddingFeature(Embedding embedding, String name, Integer dimension,
      SimilarityFunctionType similarityFunctionType, ModelVersion modelVersion) {
    this.embedding = embedding;
    this.name = name;
    this.dimension = dimension;
    this.similarityFunctionType = similarityFunctionType;
    this.modelVersion = modelVersion;
  }

  public EmbeddingFeature(Integer id, Embedding embedding, String name, Integer dimension,
      SimilarityFunctionType similarityFunctionType) {
    this.id = id;
    this.embedding = embedding;
    this.name = name;
    this.dimension = dimension;
    this.similarityFunctionType = similarityFunctionType;
  }

  public Integer getId() {
    return id;
  }

  public Embedding getEmbedding() {
    return embedding;
  }

  public String getName() {
    return name;
  }

  public Integer getDimension() {
    return dimension;
  }

  public SimilarityFunctionType getSimilarityFunctionType() {
    return similarityFunctionType;
  }

  public ModelVersion getModelVersion() {
    return modelVersion;
  }

  @JsonIgnore
  public String getFieldName() {
    return embedding.getColPrefix() == null
        ? name
        : embedding.getColPrefix() + name;
  }

}
