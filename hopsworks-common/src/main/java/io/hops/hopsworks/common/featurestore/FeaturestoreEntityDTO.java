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

package io.hops.hopsworks.common.featurestore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.Date;

/**
 * Abstract storage entity in the featurestore. Contains the common fields and functionality between feature groups
 * and training dataset entities.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FeaturegroupDTO.class, name = "featuregroupDTO"),
  @JsonSubTypes.Type(value = FeatureViewDTO.class, name = "featureViewDTO"),
  @JsonSubTypes.Type(value = TrainingDatasetDTO.class, name = "trainingDatasetDTO")})
public abstract class FeaturestoreEntityDTO<T> extends RestDTO<T> {

  private Integer featurestoreId;
  private String featurestoreName;
  private String description;
  private Date created;
  private UserDTO creator;
  private Integer version;
  private String name;
  private Integer id;
  private String location = null;
  @JsonSetter(nulls = Nulls.SKIP)
  private StatisticsConfigDTO statisticsConfig = new StatisticsConfigDTO();
  
  public FeaturestoreEntityDTO() {
  }
  
  public FeaturestoreEntityDTO(Integer featurestoreId, String name, Date created, Users creator,
                               Integer version, Integer id, StatisticsConfigDTO statisticsConfig) {
    this.featurestoreId = featurestoreId;
    this.created = created;
    this.creator = new UserDTO();
    this.creator.setFirstName(creator.getFname());
    this.creator.setLastName(creator.getLname());
    this.creator.setEmail(creator.getEmail());
    this.version = version;
    this.name = name;
    this.id = id;
    this.statisticsConfig = statisticsConfig;
  }

  public FeaturestoreEntityDTO(Integer featurestoreId, String featurestoreName, Integer id,
                               String name, Integer version) {
    this.featurestoreId = featurestoreId;
    this.featurestoreName = featurestoreName;
    this.id = id;
    this.name = name;
    this.version = version;
  }
  
  public Date getCreated() {
    return created;
  }
  
  public UserDTO getCreator() {
    return creator;
  }
  
  public String getDescription() {
    return description;
  }
  
  public Integer getVersion() {
    return version;
  }
  
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  public String getFeaturestoreName() {
    return featurestoreName;
  }
  
  public String getName() {
    return name;
  }
  
  public Integer getId() {
    return id;
  }

  public String getLocation() {
    return location;
  }

  public StatisticsConfigDTO getStatisticsConfig() {
    return statisticsConfig;
  }
  
  public void setLocation(String location) {
    this.location = location;
  }

  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
  
  public void setCreated(Date created) {
    this.created = created;
  }
  
  public void setCreator(UserDTO creator) {
    this.creator = creator;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public void setStatisticsConfig(StatisticsConfigDTO statisticsConfig) {
    this.statisticsConfig = statisticsConfig;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreEntityDTO{" +
      "featurestoreId=" + featurestoreId +
      ", featurestoreName='" + featurestoreName + '\'' +
      ", description='" + description + '\'' +
      ", created=" + created +
      ", creator='" + creator + '\'' +
      ", version=" + version +
      ", name='" + name + '\'' +
      ", id=" + id +
      ", location='" + location + '\'' +
      ", statisticsConfig=" + statisticsConfig +
      '}';
  }
}
