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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "embedding", catalog = "hopsworks")
public class Embedding implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;
  @Column(name = "vector_db_index_name")
  private String vectorDbIndexName;
  @Column(name = "col_prefix")
  private String colPrefix;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "embedding")
  private Collection<EmbeddingFeature> embeddingFeatures;

  public Embedding() {
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public Collection<EmbeddingFeature> getEmbeddingFeatures() {
    return embeddingFeatures;
  }

  public void setEmbeddingFeatures(
      Collection<EmbeddingFeature> embeddingFeatures) {
    this.embeddingFeatures = embeddingFeatures;
  }

  public String getVectorDbIndexName() {
    return vectorDbIndexName;
  }

  public void setVectorDbIndexName(String vectorDbIndexName) {
    this.vectorDbIndexName = vectorDbIndexName;
  }

  public String getColPrefix() {
    return colPrefix;
  }

  public void setColPrefix(String colPrefix) {
    this.colPrefix = colPrefix;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Embedding embedding = (Embedding) o;
    return Objects.equals(id, embedding.id)
        && Objects.equals(vectorDbIndexName, embedding.vectorDbIndexName) && Objects.equals(colPrefix,
        embedding.colPrefix) && Objects.equals(embeddingFeatures, embedding.embeddingFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, vectorDbIndexName, colPrefix, embeddingFeatures);
  }
}
