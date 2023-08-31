/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.commands.featurestore.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;

import java.util.List;

public class SearchDoc {
  @JsonProperty(FeaturestoreXAttrsConstants.DOC_TYPE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private OpenSearchDocType docType;
  @JsonProperty(FeaturestoreXAttrsConstants.NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;
  @JsonProperty(FeaturestoreXAttrsConstants.VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer version;
  @JsonProperty(FeaturestoreXAttrsConstants.PROJECT_ID)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer projectId;
  @JsonProperty(FeaturestoreXAttrsConstants.PROJECT_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String projectName;
  @JsonProperty(FeaturestoreXAttrsConstants.DATASET_INODE_ID)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long datasetIId;
  @JsonProperty(FeaturestoreXAttrsConstants.OPENSEARCH_XATTR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private XAttr xattr;
  
  public OpenSearchDocType getDocType() {
    return docType;
  }
  
  public void setDocType(OpenSearchDocType docType) {
    this.docType = docType;
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
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public Long getDatasetIId() {
    return datasetIId;
  }
  
  public void setDatasetIId(Long datasetIId) {
    this.datasetIId = datasetIId;
  }
  
  public XAttr getXattr() {
    return xattr;
  }
  
  public void setXattr(XAttr xattr) {
    this.xattr = xattr;
  }
  
  public static class XAttr {
    @JsonProperty("featurestore")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object featurestore;
    @JsonProperty("tags")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Tag> tags;
    @JsonProperty("keywords")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> keywords;
  
    public Object getFeaturestore() {
      return featurestore;
    }
  
    public void setFeaturestore(Object featurestore) {
      this.featurestore = featurestore;
    }
  
    public List<Tag> getTags() {
      return tags;
    }
  
    public void setTags(List<Tag> tags) {
      this.tags = tags;
    }
  
    public List<String> getKeywords() {
      return keywords;
    }
  
    public void setKeywords(List<String> keywords) {
      this.keywords = keywords;
    }
  }
  
  public static class Tag {
    @JsonProperty("key")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String key;
    @JsonProperty("value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String value;
  
    public Tag(String key, String value) {
      this.key = key;
      this.value = value;
    }
  
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
