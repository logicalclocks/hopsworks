/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.featurestore.xattr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * document attached as an xattr to a featuregroup directory
 */
public class TrainingDatasetXAttrDTO implements FeatureStoreItem {
  @JsonProperty(FeaturestoreXAttrsConstants.FEATURESTORE_ID)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer featurestoreId;
  @JsonProperty(FeaturestoreXAttrsConstants.DESCRIPTION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String description;
  @JsonProperty(FeaturestoreXAttrsConstants.CREATE_DATE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long createDate;
  @JsonProperty(FeaturestoreXAttrsConstants.CREATOR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String creator;
  @JsonProperty(FeaturestoreXAttrsConstants.TD_FEATURES)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<FeaturegroupXAttr.SimplifiedDTO> features = new LinkedList<>();
  
  public TrainingDatasetXAttrDTO() {
  }
  
  public TrainingDatasetXAttrDTO(Integer featurestoreId, String description,
    Date createDate, String creator) {
    this(featurestoreId, description, createDate, creator, new LinkedList<>());
  }
  
  public TrainingDatasetXAttrDTO(Integer featurestoreId, String description,
    Date createDate, String creator, List<FeaturegroupXAttr.SimplifiedDTO> features) {
    this.featurestoreId = featurestoreId;
    this.description = description;
    this.createDate = createDate.getTime();
    this.creator = creator;
    this.features = features;
  }
  
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Long getCreateDate() {
    return createDate;
  }
  
  public void setCreateDate(Long createDate) {
    this.createDate = createDate;
  }
  
  public String getCreator() {
    return creator;
  }
  
  public void setCreator(String creator) {
    this.creator = creator;
  }
  
  public List<FeaturegroupXAttr.SimplifiedDTO> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<FeaturegroupXAttr.SimplifiedDTO> features) {
    this.features = features;
  }
}
