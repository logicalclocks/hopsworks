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

package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.query.FsQueryDTO;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.tags.TagsDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class FeatureViewDTO extends RestDTO<FeatureViewDTO> {
  // Cannot inherit `FeaturestoreEntityDTO` because the `Response.ok().entity()` use the toString() output from
  // `FeaturestoreEntityDTO` instead.

  private Integer featurestoreId;
  private String featurestoreName;
  private String description;
  private Date created;
  private UserDTO creator;
  private Integer version;
  private String name;
  private Integer id;
  private QueryDTO query;
  private List<TrainingDatasetFeatureDTO> features; // contains transformation info

  // Below fields do not need to provide
  private KeywordDTO keywords;
  private TagsDTO tags;
  private FsQueryDTO queryString;

  public FeatureViewDTO() {
  }

  public Integer getFeaturestoreId() {
    return featurestoreId;
  }

  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }

  public String getFeaturestoreName() {
    return featurestoreName;
  }

  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public UserDTO getCreator() {
    return creator;
  }

  public void setCreator(UserDTO creator) {
    this.creator = creator;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

}
