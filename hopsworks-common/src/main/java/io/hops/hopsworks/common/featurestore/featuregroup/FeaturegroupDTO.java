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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.Nulls;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;

/**
 * DTO containing the human-readable information of a featuregroup, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlSeeAlso({CachedFeaturegroupDTO.class, StreamFeatureGroupDTO.class, OnDemandFeaturegroupDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonTypeName("featuregroupDTO")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CachedFeaturegroupDTO.class, name = "cachedFeaturegroupDTO"),
    @JsonSubTypes.Type(value = StreamFeatureGroupDTO.class, name = "streamFeatureGroupDTO"),
    @JsonSubTypes.Type(value = OnDemandFeaturegroupDTO.class, name = "onDemandFeaturegroupDTO")})
public class FeaturegroupDTO extends FeaturestoreEntityDTO<FeaturegroupDTO> {

  private List<FeatureGroupFeatureDTO> features;
  private ExpectationSuiteDTO expectationSuite = null;
  private String onlineTopicName = null;
  private String eventTime = null;
  @JsonSetter(nulls = Nulls.SKIP)
  private Boolean onlineEnabled = false;
  @JsonSetter(nulls = Nulls.SKIP)
  private Boolean deprecated = false;
  
  public FeaturegroupDTO() {
  }

  public FeaturegroupDTO(Integer featurestoreId, String featurestoreName, Integer id, String name, Integer version,
    String onlineTopicName, Boolean deprecated) {
    super(featurestoreId, featurestoreName, id, name, version);
    this.onlineTopicName = onlineTopicName;
    this.deprecated = deprecated;
  }

  public FeaturegroupDTO(Featuregroup featuregroup) {
    super(featuregroup.getFeaturestore().getId(), featuregroup.getName(), featuregroup.getCreated(),
        featuregroup.getCreator(), featuregroup.getVersion(),
        featuregroup.getId(), new StatisticsConfigDTO(featuregroup.getStatisticsConfig()));
    this.eventTime = featuregroup.getEventTime();
    this.deprecated = featuregroup.isDeprecated();
  }
  
  // for testing
  public FeaturegroupDTO(String eventTime) {
    this.eventTime = eventTime;
  }

  public List<FeatureGroupFeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureGroupFeatureDTO> features) {
    this.features = features;
  }

  public ExpectationSuiteDTO getExpectationSuite() {
    return expectationSuite;
  }

  public void setExpectationSuite(ExpectationSuiteDTO expectationSuite) {
    this.expectationSuite = expectationSuite;
  }

  public String getOnlineTopicName() {
    return onlineTopicName;
  }

  public void setOnlineTopicName(String onlineTopicName) {
    this.onlineTopicName = onlineTopicName;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }

  public Boolean getOnlineEnabled() {
    return onlineEnabled;
  }

  public void setOnlineEnabled(Boolean onlineEnabled) {
    this.onlineEnabled = onlineEnabled;
  }

  public Boolean getDeprecated() {
    return deprecated;
  }

  public void setDeprecated(Boolean deprecated) {
    this.deprecated = deprecated;
  }
  
  @Override
  public String toString() {
    return "FeaturegroupDTO{" +
      "features=" + features +
      '}';
  }
}
