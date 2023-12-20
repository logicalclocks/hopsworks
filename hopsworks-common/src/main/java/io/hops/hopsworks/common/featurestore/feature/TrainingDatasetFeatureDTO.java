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

package io.hops.hopsworks.common.featurestore.feature;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TrainingDatasetFeatureDTO {

  private String name;
  private String type;
  private FeaturegroupDTO featuregroup;
  private String featureGroupFeatureName;
  private Integer index;
  private Boolean label = false;
  private Boolean inferenceHelperColumn = false;
  private Boolean trainingHelperColumn = false;
  private TransformationFunctionDTO transformationFunction;

  public TrainingDatasetFeatureDTO() {
  }

  public TrainingDatasetFeatureDTO(String name, String type, FeaturegroupDTO featuregroupDTO,
      String featureGroupFeatureName,Integer index, Boolean label, Boolean inferenceHelperColumn ,
      Boolean trainingHelperColumn) {
    this.name = name;
    this.type = type;
    this.featuregroup = featuregroupDTO;
    this.index = index;
    this.label = label;
    this.featureGroupFeatureName = featureGroupFeatureName;
    this.inferenceHelperColumn = inferenceHelperColumn;
    this.trainingHelperColumn = trainingHelperColumn;
  }

  public TrainingDatasetFeatureDTO(String name, String type,
      FeaturegroupDTO featuregroup, String featureGroupFeatureName, Integer index, Boolean label,
      Boolean inferenceHelperColumn , Boolean trainingHelperColumn, TransformationFunctionDTO transformationFunction) {
    this.name = name;
    this.type = type;
    this.featuregroup = featuregroup;
    this.featureGroupFeatureName = featureGroupFeatureName;
    this.index = index;
    this.label = label;
    this.inferenceHelperColumn = inferenceHelperColumn;
    this.trainingHelperColumn = trainingHelperColumn;
    this.transformationFunction = transformationFunction;
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

  public FeaturegroupDTO getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(FeaturegroupDTO featuregroupDTO) {
    this.featuregroup = featuregroupDTO;
  }

  public String getFeatureGroupFeatureName() {
    return featureGroupFeatureName;
  }

  public void setFeatureGroupFeatureName(String featureGroupFeatureName) {
    this.featureGroupFeatureName = featureGroupFeatureName;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Boolean getLabel() {
    return label;
  }

  public void setLabel(Boolean label) {
    this.label = label;
  }
  
  public Boolean getInferenceHelperColumn() {
    return inferenceHelperColumn;
  }
  
  public void setInferenceHelperColumn(Boolean inferenceHelperColumn) {
    this.inferenceHelperColumn = inferenceHelperColumn;
  }
  
  public Boolean getTrainingHelperColumn() {
    return trainingHelperColumn;
  }
  
  public void setTrainingHelperColumn(Boolean trainingHelperColumn) {
    this.trainingHelperColumn = trainingHelperColumn;
  }
  
  public TransformationFunctionDTO getTransformationFunction() {
    return transformationFunction;
  }

  public void setTransformationFunction(TransformationFunctionDTO transformationFunction) {
    this.transformationFunction = transformationFunction;
  }
}
