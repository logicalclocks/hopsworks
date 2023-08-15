/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featureview;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.query.FsQueryDTO;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.tags.TagsDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@JsonTypeName("featureViewDTO")
public class FeatureViewDTO extends FeaturestoreEntityDTO<FeatureViewDTO> {

  private QueryDTO query;
  private List<TrainingDatasetFeatureDTO> features; // contains transformation info

  // Below fields do not need to provide
  private KeywordDTO keywords;
  private TagsDTO tags;
  private FsQueryDTO queryString;
  private List<ServingKeyDTO> servingKeys;

  public FeatureViewDTO() {
  }

  public QueryDTO getQuery() {
    return query;
  }

  public void setQuery(QueryDTO query) {
    this.query = query;
  }

  public FsQueryDTO getQueryString() {
    return queryString;
  }

  public void setQueryString(FsQueryDTO queryString) {
    this.queryString = queryString;
  }

  public List<TrainingDatasetFeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<TrainingDatasetFeatureDTO> features) {
    this.features = features;
  }

  public KeywordDTO getKeywords() {
    return keywords;
  }

  public void setKeywords(KeywordDTO keywords) {
    this.keywords = keywords;
  }

  public TagsDTO getTags() {
    return tags;
  }

  public void setTags(TagsDTO tags) {
    this.tags = tags;
  }

  public List<ServingKeyDTO> getServingKeys() {
    return servingKeys;
  }

  public void setServingKeys(List<ServingKeyDTO> servingKeys) {
    this.servingKeys = servingKeys;
  }
}
