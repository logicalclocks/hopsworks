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

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup.OnDemandFeaturegroupDTO;

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
    @JsonSubTypes.Type(value = CachedFeaturegroupDTO.class, name = "CachedFeaturegroupDTO"),
    @JsonSubTypes.Type(value = OnDemandFeaturegroupDTO.class, name = "OnDemandFeaturegroupDTO")})
public class FeaturegroupDTO extends FeaturestoreEntityDTO {

  private FeaturegroupType featuregroupType;

  public FeaturegroupDTO() {
  }

  public FeaturegroupDTO(Featuregroup featuregroup) {
    super(featuregroup.getFeaturestore().getId(), featuregroup.getCreated(),
        featuregroup.getCreator(), featuregroup.getVersion(),
        (List) featuregroup.getStatistics(), featuregroup.getJob(),
        featuregroup.getId());
    this.featuregroupType = featuregroup.getFeaturegroupType();
  }

  @XmlElement
  public FeaturegroupType getFeaturegroupType() {
    return featuregroupType;
  }
  
  public void setFeaturegroupType(FeaturegroupType featuregroupType) {
    this.featuregroupType = featuregroupType;
  }

  @Override
  public String toString() {
    return "FeaturegroupDTO{" +
        "featuregroupType=" + featuregroupType +
        '}';
  }


}
