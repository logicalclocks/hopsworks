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

package io.hops.hopsworks.common.featurestore.featuregroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.ValidationType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;

/**
 * DTO containing the human-readable information of a featuregroup, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlSeeAlso({CachedFeaturegroupDTO.class, OnDemandFeaturegroupDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CachedFeaturegroupDTO.class, name = "OnlineFeaturegroupDTO"),
    @JsonSubTypes.Type(value = OnDemandFeaturegroupDTO.class, name = "OnDemandFeaturegroupDTO")})
public class FeaturegroupDTO extends FeaturestoreEntityDTO {

  @XmlElement
  private List<FeatureGroupFeatureDTO> features;
  @XmlElement
  private List<String> expectationsNames; // List of expectation names
  @XmlElement
  private ValidationType validationType;
  @XmlElement
  private String onlineTopicName = null;
  
  public FeaturegroupDTO() {
  }

  public FeaturegroupDTO(Integer featurestoreId, String featurestoreName, Integer id, String name, Integer version,
    String onlineTopicName) {
    super(featurestoreId, featurestoreName, id, name, version);
    this.onlineTopicName = onlineTopicName;
  }

  public FeaturegroupDTO(Featuregroup featuregroup) {
    super(featuregroup.getFeaturestore().getId(), featuregroup.getName(), featuregroup.getCreated(),
        featuregroup.getCreator(), featuregroup.getVersion(),
        featuregroup.getId(), new StatisticsConfigDTO(featuregroup.getStatisticsConfig()));
  }

  public List<FeatureGroupFeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureGroupFeatureDTO> features) {
    this.features = features;
  }

  public List<String> getExpectationsNames() {
    return expectationsNames;
  }

  public void setExpectationsNames(List<String> expectationsNames) {
    this.expectationsNames = expectationsNames;
  }

  public ValidationType getValidationType() {
    return validationType;
  }

  public void setValidationType(ValidationType validationType) {
    this.validationType = validationType;
  }

  public String getOnlineTopicName() {
    return onlineTopicName;
  }

  public void setOnlineTopicName(String onlineTopicName) {
    this.onlineTopicName = onlineTopicName;
  }

  @Override
  public String toString() {
    return "FeaturegroupDTO{" +
      "features=" + features +
      '}';
  }
}
