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
package io.hops.hopsworks.api.elastic.featurestore;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement
public class ElasticFeaturestoreDTO {
  private List<ElasticFeaturestoreItemDTO.Base> featuregroups = new LinkedList<>();
  private List<ElasticFeaturestoreItemDTO.Base> trainingdatasets = new LinkedList<>();
  private List<ElasticFeaturestoreItemDTO.Feature> features = new LinkedList<>();
  private Integer featuregroupsFrom = 0;
  private Long featuregroupsTotal = 0l;
  private Integer trainingdatasetsFrom = 0;
  private Long trainingdatasetsTotal = 0l;
  private Integer featuresFrom = 0;
  private Long featuresTotal = 0l;
  
  public ElasticFeaturestoreDTO() {
  }
  
  public List<ElasticFeaturestoreItemDTO.Base> getFeaturegroups() {
    return featuregroups;
  }
  
  public void setFeaturegroups(List<ElasticFeaturestoreItemDTO.Base> featuregroups) {
    this.featuregroups = featuregroups;
  }
  
  public List<ElasticFeaturestoreItemDTO.Base> getTrainingdatasets() {
    return trainingdatasets;
  }
  
  public void setTrainingdatasets(List<ElasticFeaturestoreItemDTO.Base> trainingdatasets) {
    this.trainingdatasets = trainingdatasets;
  }
  
  public List<ElasticFeaturestoreItemDTO.Feature> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<ElasticFeaturestoreItemDTO.Feature> features) {
    this.features = features;
  }
  
  public Integer getFeaturegroupsFrom() {
    return featuregroupsFrom;
  }
  
  public void setFeaturegroupsFrom(Integer featuregroupsFrom) {
    this.featuregroupsFrom = featuregroupsFrom;
  }
  
  public Long getFeaturegroupsTotal() {
    return featuregroupsTotal;
  }
  
  public void setFeaturegroupsTotal(Long featuregroupsTotal) {
    this.featuregroupsTotal = featuregroupsTotal;
  }
  
  public Integer getTrainingdatasetsFrom() {
    return trainingdatasetsFrom;
  }
  
  public void setTrainingdatasetsFrom(Integer trainingdatasetsFrom) {
    this.trainingdatasetsFrom = trainingdatasetsFrom;
  }
  
  public Long getTrainingdatasetsTotal() {
    return trainingdatasetsTotal;
  }
  
  public void setTrainingdatasetsTotal(Long trainingdatasetsTotal) {
    this.trainingdatasetsTotal = trainingdatasetsTotal;
  }
  
  public Integer getFeaturesFrom() {
    return featuresFrom;
  }
  
  public void setFeaturesFrom(Integer featuresFrom) {
    this.featuresFrom = featuresFrom;
  }
  
  public Long getFeaturesTotal() {
    return featuresTotal;
  }
  
  public void setFeaturesTotal(Long featuresTotal) {
    this.featuresTotal = featuresTotal;
  }
  
  public void addTrainingdataset(ElasticFeaturestoreItemDTO.Base trainingdataset) {
    trainingdatasets.add(trainingdataset);
  }
  
  public void addFeaturegroup(ElasticFeaturestoreItemDTO.Base featuregroup) {
    featuregroups.add(featuregroup);
  }
  
  public void addFeature(ElasticFeaturestoreItemDTO.Feature feature) {
    features.add(feature);
  }
}
