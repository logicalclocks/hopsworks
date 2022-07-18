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
package io.hops.hopsworks.api.opensearch.featurestore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.javatuples.Triplet;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class OpenSearchFeaturestoreDTO {
  private List<OpenSearchFeaturestoreItemDTO.Base> featuregroups = new LinkedList<>();
  private List<OpenSearchFeaturestoreItemDTO.Base> featureViews = new LinkedList<>();
  private List<OpenSearchFeaturestoreItemDTO.Base> trainingdatasets = new LinkedList<>();
  private List<OpenSearchFeaturestoreItemDTO.Feature> features = new LinkedList<>();
  @JsonIgnore
  private Map<Triplet<String, String, String>, OpenSearchFeaturestoreItemDTO.Feature> featuresAux = new HashMap<>();
  private Integer featuregroupsFrom = 0;
  private Long featuregroupsTotal = 0l;
  private Integer featureViewsFrom = 0;
  private Long featureViewsTotal = 0l;
  private Integer trainingdatasetsFrom = 0;
  private Long trainingdatasetsTotal = 0l;
  private Integer featuresFrom = 0;
  private Long featuresTotal = 0l;
  
  public OpenSearchFeaturestoreDTO() {
  }
  
  public List<OpenSearchFeaturestoreItemDTO.Base> getFeaturegroups() {
    return featuregroups;
  }
  
  public void setFeaturegroups(List<OpenSearchFeaturestoreItemDTO.Base> featuregroups) {
    this.featuregroups = featuregroups;
  }
  
  public List<OpenSearchFeaturestoreItemDTO.Base> getFeatureViews() {
    return featureViews;
  }
  
  public void setFeatureViews(
    List<OpenSearchFeaturestoreItemDTO.Base> featureViews) {
    this.featureViews = featureViews;
  }
  
  public List<OpenSearchFeaturestoreItemDTO.Base> getTrainingdatasets() {
    return trainingdatasets;
  }
  
  public void setTrainingdatasets(List<OpenSearchFeaturestoreItemDTO.Base> trainingdatasets) {
    this.trainingdatasets = trainingdatasets;
  }
  
  public List<OpenSearchFeaturestoreItemDTO.Feature> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<OpenSearchFeaturestoreItemDTO.Feature> features) {
    this.features = features;
    for(OpenSearchFeaturestoreItemDTO.Feature feature: features) {
      featuresAux.put(
        Triplet.with(feature.getParentProjectName(), feature.getFeaturegroup(), feature.getName()), feature);
    }
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
  
  public Integer getFeatureViewsFrom() {
    return featureViewsFrom;
  }
  
  public void setFeatureViewsFrom(Integer featureViewsFrom) {
    this.featureViewsFrom = featureViewsFrom;
  }
  
  public Long getFeatureViewsTotal() {
    return featureViewsTotal;
  }
  
  public void setFeatureViewsTotal(Long featureViewsTotal) {
    this.featureViewsTotal = featureViewsTotal;
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
  
  public void addTrainingdataset(OpenSearchFeaturestoreItemDTO.Base trainingdataset) {
    trainingdatasets.add(trainingdataset);
  }
  
  public void addFeatureView(OpenSearchFeaturestoreItemDTO.Base featureView) {
    featureViews.add(featureView);
  }
  
  public void addFeaturegroup(OpenSearchFeaturestoreItemDTO.Base featuregroup) {
    featuregroups.add(featuregroup);
  }
  
  public void addFeature(OpenSearchFeaturestoreItemDTO.Feature feature) {
    features.add(feature);
    featuresAux.put(
      Triplet.with(feature.getParentProjectName(), feature.getFeaturegroup(), feature.getName()), feature);
  }
  
  public OpenSearchFeaturestoreItemDTO.Feature getFeature(String parentProjectName,
                                                          String featuregroupName,
                                                          String name) {
    return featuresAux.get(Triplet.with(parentProjectName, featuregroupName, name));
  }
}
