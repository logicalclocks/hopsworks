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

public class FeaturegroupXAttr {
  /**
   * common fields
   */
  public abstract static class Base {
    @JsonProperty(FeaturestoreXAttrsConstants.FEATURESTORE_ID)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer featurestoreId;
  
    protected Base() {}
  
    protected Base(Integer featurestoreId) {
      this.featurestoreId = featurestoreId;
    }
  
    public Integer getFeaturestoreId() {
      return featurestoreId;
    }
    
    public void setFeaturestoreId(Integer featurestoreId) {
      this.featurestoreId = featurestoreId;
    }
  
    @Override
    public String toString() {
      return "Base{" +
        "featurestoreId=" + featurestoreId +
        '}';
    }
  }
  
  /**
   * document attached as an xattr to a featuregroup directory
   */
  public static class FullDTO extends Base implements FeatureStoreItem {
    @JsonProperty(FeaturestoreXAttrsConstants.DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    @JsonProperty(FeaturestoreXAttrsConstants.CREATE_DATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long createDate;
    @JsonProperty(FeaturestoreXAttrsConstants.CREATOR)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String creator;
    @JsonProperty(FeaturestoreXAttrsConstants.FG_TYPE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FGType fgType;
    @JsonProperty(FeaturestoreXAttrsConstants.FG_FEATURES)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SimpleFeatureDTO> features = new LinkedList<>();
  
    public FullDTO() {
      super();
    }
  
    public FullDTO(Integer featurestoreId, String description, Date createDate, String creator) {
      this(featurestoreId, description, createDate, creator, new LinkedList<>());
    }
  
    public FullDTO(Integer featurestoreId, String description,
      Date createDate, String creator, List<SimpleFeatureDTO> features) {
      super(featurestoreId);
      this.features = features;
      this.description = description;
      this.createDate = createDate.getTime();
      this.creator = creator;
    }
  
    @Override
    public String getDescription() {
      return description;
    }
  
    public void setDescription(String description) {
      this.description = description;
    }
  
    @Override
    public Long getCreateDate() {
      return createDate;
    }
  
    public void setCreateDate(Long createDate) {
      this.createDate = createDate;
    }
  
    @Override
    public String getCreator() {
      return creator;
    }
  
    public void setCreator(String creator) {
      this.creator = creator;
    }
    public FGType getFgType() {
      return fgType;
    }

    public void setFgType(FGType fgType) {
      this.fgType = fgType;
    }

    public List<SimpleFeatureDTO> getFeatures() {
      return features;
    }

    public void setFeatures(List<SimpleFeatureDTO> features) {
      this.features = features;
    }

    public void addFeature(SimpleFeatureDTO feature) {
      features.add(feature);
    }

    public void addFeatures(List<SimpleFeatureDTO> features) {
      this.features.addAll(features);
    }

    @Override
    public String toString() {
      return super.toString() + "Extended{" +
        "description='" + description + '\'' +
        ", createDate=" + createDate +
        ", creator='" + creator + '\'' +
        ", fgType='" + fgType + '\'' +
        ", features=" + features +
        '}';
    }
  }
  
  /**
   * simplified version of FullDTO that is part of the
   * @link io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO
   */
  public static class SimplifiedDTO extends Base {
    @JsonProperty(FeaturestoreXAttrsConstants.NAME)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;
    @JsonProperty(FeaturestoreXAttrsConstants.VERSION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer version;
    @JsonProperty(FeaturestoreXAttrsConstants.FG_FEATURES)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> features = new LinkedList<>();
  
    public SimplifiedDTO() {
      super();
    }
    
    public SimplifiedDTO(Integer featurestoreId, String name, Integer version) {
      super(featurestoreId);
      this.name = name;
      this.version = version;
    }
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public Integer getVersion() {
      return version;
    }
  
    public void setVersion(Integer version) {
      this.version = version;
    }

    public List<String> getFeatures() {
      return features;
    }
  
    public void setFeatures(List<String> features) {
      this.features = features;
    }
  
    public void addFeature(String feature) {
      features.add(feature);
    }
  
    public void addFeatures(List<String> features) {
      this.features.addAll(features);
    }
  }

  public static class SimpleFeatureDTO {
    @JsonProperty(FeaturestoreXAttrsConstants.NAME)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;
    @JsonProperty(FeaturestoreXAttrsConstants.DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    
    public SimpleFeatureDTO() {}
    
    public SimpleFeatureDTO(String name, String description) {
      this.name = name;
      this.description = description;
    }
  
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public String getDescription() {
      return description;
    }
  
    public void setDescription(String description) {
      this.description = description;
    }
  
    @Override
    public String toString() {
      return "SimpleFeatureDTO{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        '}';
    }
  }

  public enum FGType {
    ON_DEMAND,
    CACHED,
    STREAM
  }
}
