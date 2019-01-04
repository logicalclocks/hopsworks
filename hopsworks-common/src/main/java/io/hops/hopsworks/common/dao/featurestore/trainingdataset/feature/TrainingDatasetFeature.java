/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.feature;

import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the training_dataset_feature table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "training_dataset_feature", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TrainingDatasetFeature.findAll", query = "SELECT tdf FROM TrainingDatasetFeature tdf"),
    @NamedQuery(name = "TrainingDatasetFeature.findById",
        query = "SELECT tdf FROM TrainingDatasetFeature tdf WHERE tdf.id = :id")})
public class TrainingDatasetFeature implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @Basic(optional = false)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @Column(name = "type")
  private String type;
  @Basic(optional = false)
  @Column(name = "primary_column")
  private int primary = 0;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getPrimary() {
    return primary;
  }

  public void setPrimary(int primary) {
    this.primary = primary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrainingDatasetFeature)) return false;

    TrainingDatasetFeature that = (TrainingDatasetFeature) o;

    if (primary != that.primary) return false;
    if (!id.equals(that.id)) return false;
    if (!trainingDataset.equals(that.trainingDataset)) return false;
    if (!description.equals(that.description)) return false;
    if (!name.equals(that.name)) return false;
    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + trainingDataset.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + primary;
    return result;
  }
}
