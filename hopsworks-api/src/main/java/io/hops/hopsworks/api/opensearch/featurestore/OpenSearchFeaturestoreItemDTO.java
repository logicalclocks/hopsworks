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

import io.hops.hopsworks.api.user.UserDTO;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OpenSearchFeaturestoreItemDTO {
  @XmlRootElement
  public static class Base {
    protected String elasticId;
    //base fields
    protected Integer featurestoreId;
    protected String name;
    protected Integer version;
  
    protected Long datasetIId;
    protected String description;
    protected Date created;
    protected UserDTO creator;
    
    protected Highlights highlights;
  
    //access fields
    protected Integer parentProjectId;
    protected String parentProjectName;
    protected Map<Integer, String> accessProjects = new HashMap<>();
  
    public Base() {
    }
  
    public String getElasticId() {
      return elasticId;
    }
  
    public void setElasticId(String elasticId) {
      this.elasticId = elasticId;
    }
  
    public Integer getFeaturestoreId() {
      return featurestoreId;
    }
  
    public void setFeaturestoreId(Integer featurestoreId) {
      this.featurestoreId = featurestoreId;
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
  
    public Long getDatasetIId() {
      return datasetIId;
    }
  
    public void setDatasetIId(Long datasetIId) {
      this.datasetIId = datasetIId;
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
  
    public Highlights getHighlights() {
      return highlights;
    }
  
    public void setHighlights(Highlights highlights) {
      this.highlights = highlights;
    }
    
    public Integer getParentProjectId() {
      return parentProjectId;
    }
  
    public void setParentProjectId(Integer parentProjectId) {
      this.parentProjectId = parentProjectId;
    }
  
    public String getParentProjectName() {
      return parentProjectName;
    }
  
    public void setParentProjectName(String parentProjectName) {
      this.parentProjectName = parentProjectName;
    }
  
    public Map<Integer, String> getAccessProjects() {
      return accessProjects;
    }
  
    public void setAccessProjects(Map<Integer, String> accessProjects) {
      this.accessProjects = accessProjects;
    }
  
    public void addAccessProject(Integer projectId, String projectName) {
      accessProjects.put(projectId, projectName);
    }
  }
  
  @XmlRootElement
  public static class Feature extends Base {
    protected String featuregroup;
    
    public Feature() {
    }
    
    public String getFeaturegroup() {
      return featuregroup;
    }
    
    public void setFeaturegroup(String featuregroup) {
      this.featuregroup = featuregroup;
    }
  }
  
  @XmlRootElement
  public static class Highlights {
    private String name;
    private String description;
    private List<FeatureHighlights> features = null;
    private List<Tag> tags = null;
    private Map<String, String> otherXattrs = null;
  
    public Highlights() {
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
  
    public List<FeatureHighlights> getFeatures() {
      return features;
    }
  
    public void setFeatures(List<FeatureHighlights> features) {
      this.features = features;
    }
  
    public List<Tag> getTags() {
      return tags;
    }
  
    public void setTags(List<Tag> tags) {
      this.tags = tags;
    }
  
    public Map<String, String> getOtherXattrs() {
      return otherXattrs;
    }
  
    public void setOtherXattrs(Map<String, String> otherXattrs) {
      this.otherXattrs = otherXattrs;
    }
  
    public void addFeature(String name) {
      if(features == null) {
        features = new LinkedList<>();
      }
      FeatureHighlights feature = new FeatureHighlights();
      features.add(feature);
      feature.setName(name);
    }
  
    public void addFeatureDescription(String description) {
      if(features == null) {
        features = new LinkedList<>();
      }
      FeatureHighlights feature = new FeatureHighlights();
      features.add(feature);
      feature.setDescription(description);
    }
  
    public void addTagKey(String key) {
      if(tags == null) {
        tags = new LinkedList<>();
      }
      Tag tag = new Tag();
      tag.key = key;
      tags.add(tag);
    }
  
    public void addTagValue(String value) {
      if(tags == null) {
        tags = new LinkedList<>();
      }
      Tag tag = new Tag();
      tag.value = value;
      tags.add(tag);
    }
  
    public void addOtherXAttr(String key, String val) {
      if(otherXattrs == null) {
        otherXattrs = new HashMap<>();
      }
      otherXattrs.put(key, val);
    }
  }
  
  @XmlRootElement
  public static class FeatureHighlights {
    private String name;
    private String description;
  
    public FeatureHighlights() {
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
  }
  
  @XmlRootElement
  public static class Tag {
    private String key;
    private String value;
  
    public Tag() {
    }
  
    public String getKey() {
      return key;
    }
  
    public void setKey(String key) {
      this.key = key;
    }
  
    public String getValue() {
      return value;
    }
  
    public void setValue(String value) {
      this.value = value;
    }
  }
}
