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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.persistence.entity.featurestore.jobs.FeaturestoreJob;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract storage entity in the featurestore. Contains the common fields and functionality between feature groups
 * and training dataset entities.
 */
@XmlRootElement
@XmlSeeAlso({FeaturegroupDTO.class, TrainingDatasetDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = FeaturegroupDTO.class, name = "FeaturegroupDTO"),
  @JsonSubTypes.Type(value = TrainingDatasetDTO.class, name = "TrainingDatasetDTO")})
public abstract class FeaturestoreEntityDTO {

  private Integer featurestoreId;
  private String featurestoreName;
  private String description;
  private Date created;
  private String creator;
  private Integer version;
  private String name;
  private Integer id;
  private List<FeatureDTO> features;
  private String location = null;
  private List<FeaturestoreJobDTO> jobs;
  
  public FeaturestoreEntityDTO() {
  }
  
  public FeaturestoreEntityDTO(Integer featurestoreId, String name, Date created, Users creator,
                               Integer version, List<FeaturestoreJob> featurestoreJobs, Integer id) {
    this.featurestoreId = featurestoreId;
    this.created = created;
    this.creator = creator.getEmail();
    this.version = version;
    this.name = name;
    this.id = id;
    this.jobs = featurestoreJobs.stream().map(FeaturestoreJobDTO::new).collect(Collectors.toList());
  }
  
  @XmlElement
  public Date getCreated() {
    return created;
  }
  
  @XmlElement
  public String getCreator() {
    return creator;
  }
  
  @XmlElement
  public String getDescription() {
    return description;
  }
  
  @XmlElement
  public Integer getVersion() {
    return version;
  }
  
  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  @XmlElement
  public String getFeaturestoreName() {
    return featurestoreName;
  }
  
  @XmlElement
  public String getName() {
    return name;
  }
  
  @XmlElement
  public Integer getId() {
    return id;
  }
  
  @XmlElement
  public List<FeatureDTO> getFeatures() {
    return features;
  }
  
  @XmlElement
  public String getLocation() {
    return location;
  }
  
  @XmlElement(nillable = true)
  public List<FeaturestoreJobDTO> getJobs() {
    return jobs;
  }
  
  public void setLocation(String location) {
    this.location = location;
  }
  
  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
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
  
  public void setCreator(String creator) {
    this.creator = creator;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public void setJobs(List<FeaturestoreJobDTO> jobs) {
    this.jobs = jobs;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreEntityDTO{" +
      "featurestoreId=" + featurestoreId +
      ", featurestoreName='" + featurestoreName + '\'' +
      ", description='" + description + '\'' +
      ", created='" + created + '\'' +
      ", creator='" + creator + '\'' +
      ", version=" + version +
      ", name='" + name + '\'' +
      ", id=" + id +
      '}';
  }
}
