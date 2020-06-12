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
package io.hops.hopsworks.api.elastic;

import io.hops.hopsworks.common.api.RestDTO;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Map;

@XmlRootElement
public class ElasticDatasetDTO extends RestDTO<ElasticDatasetDTO> {
  private String elasticId;
  private Float score;
  private Integer datasetId;
  private String name;
  private Long datasetIId;
  private String description;
  private Date modificationTime;
  private String creator;
  private Long size;
  private Boolean localDataset;
  private Boolean publicDataset;
  private String publicDatasetId;
  private Map<String, HighlightField> highlights;
  private Integer parentProjectId;
  private String parentProjectName;
  private Map<Integer, String> accessProjects;
  private Map<String, Object> map;
  
  public String getElasticId() {
    return elasticId;
  }
  
  public void setElasticId(String elasticId) {
    this.elasticId = elasticId;
  }
  
  public Float getScore() {
    return score;
  }
  
  public void setScore(Float score) {
    this.score = score;
  }
  
  public Integer getDatasetId() {
    return datasetId;
  }
  
  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
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
  
  public Date getModificationTime() {
    return modificationTime;
  }
  
  public void setModificationTime(Date modificationTime) {
    this.modificationTime = modificationTime;
  }
  
  public String getCreator() {
    return creator;
  }
  
  public void setCreator(String creator) {
    this.creator = creator;
  }
  
  public Long getSize() {
    return size;
  }
  
  public void setSize(Long size) {
    this.size = size;
  }
  
  public Boolean getLocalDataset() {
    return localDataset;
  }
  
  public void setLocalDataset(Boolean localDataset) {
    this.localDataset = localDataset;
  }
  
  public Boolean getPublicDataset() {
    return publicDataset;
  }
  
  public void setPublicDataset(Boolean publicDataset) {
    this.publicDataset = publicDataset;
  }
  
  public String getPublicDatasetId() {
    return publicDatasetId;
  }
  
  public void setPublicDatasetId(String publicDatasetId) {
    this.publicDatasetId = publicDatasetId;
  }
  
  public Map<String, HighlightField> getHighlights() {
    return highlights;
  }
  
  public void setHighlights(
    Map<String, HighlightField> highlights) {
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
  
  public Map<String, Object> getMap() {
    return map;
  }
  
  public void setMap(Map<String, Object> map) {
    this.map = map;
  }
  
  @Override
  public String toString() {
    return "ElasticDatasetDTO{" +
      "elasticId='" + elasticId + '\'' +
      ", score=" + score +
      ", datasetId=" + datasetId +
      ", name='" + name + '\'' +
      ", datasetIId=" + datasetIId +
      ", description='" + description + '\'' +
      ", modificationTime=" + modificationTime +
      ", creator='" + creator + '\'' +
      ", size=" + size +
      ", localDataset=" + localDataset +
      ", publicDataset=" + publicDataset +
      ", publicDatasetId='" + publicDatasetId + '\'' +
      ", highlights=" + highlights +
      ", parentProjectId=" + parentProjectId +
      ", parentProjectName='" + parentProjectName + '\'' +
      ", accessProjects=" + accessProjects +
      ", map=" + map +
      ", count=" + count +
      '}';
  }
}
