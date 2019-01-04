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

package io.hops.hopsworks.common.dao.featurestore.stats;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the featurestore_statistic table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "featurestore_statistic", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreStatistic.findAll", query = "SELECT fss FROM FeaturestoreStatistic fss"),
    @NamedQuery(name = "FeaturestoreStatistic.findById",
        query = "SELECT fss FROM FeaturestoreStatistic fss WHERE fss.id = :id")})
public class FeaturestoreStatistic implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Null
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statistic_type")
  private FeaturestoreStatisticType statisticType = FeaturestoreStatisticType.DESCRIPTIVESTATISTICS;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @Null
  @Column(name = "value")
  @Basic(optional = false)
  @Convert(converter = FeaturestoreStatisticValueConverter.class)
  private FeaturestoreStatisticValue value;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FeaturestoreStatisticType getStatisticType() {
    return statisticType;
  }

  public void setStatisticType(FeaturestoreStatisticType statisticType) {
    this.statisticType = statisticType;
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public FeaturestoreStatisticValue getValue() {
    return value;
  }

  public void setValue(FeaturestoreStatisticValue value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeaturestoreStatistic)) return false;

    FeaturestoreStatistic that = (FeaturestoreStatistic) o;

    if (!id.equals(that.id)) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (statisticType != that.statisticType) return false;
    if (featuregroup != null ? !featuregroup.equals(that.featuregroup) : that.featuregroup != null) return false;
    return trainingDataset != null ? trainingDataset.equals(that.trainingDataset) : that.trainingDataset == null;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + statisticType.hashCode();
    result = 31 * result + (featuregroup != null ? featuregroup.hashCode() : 0);
    result = 31 * result + (trainingDataset != null ? trainingDataset.hashCode() : 0);
    return result;
  }
}
