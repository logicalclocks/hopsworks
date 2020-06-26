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
public class ElasticInodeDTO extends RestDTO<ElasticInodeDTO> {
  private String elasticId;
  private Float score;
  private String name;
  private Long inodeId;
  private String description;
  private Date modificationTime;
  private String creator;
  private Long size;
  private String path;
  private Map<String, HighlightField> highlights;
  private Integer parentProjectId;
  private Long parentDatasetIId;
  private Integer parentDatasetId;
  private String parentDatasetName;
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
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Long getSize() {
    return size;
  }
  
  public void setSize(Long size) {
    this.size = size;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
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
  
  public Integer getParentDatasetId() {
    return parentDatasetId;
  }
  
  public void setParentDatasetId(Integer parentDatasetId) {
    this.parentDatasetId = parentDatasetId;
  }
  
  public String getParentDatasetName() {
    return parentDatasetName;
  }
  
  public void setParentDatasetName(String parentDatasetName) {
    this.parentDatasetName = parentDatasetName;
  }
  
  public Long getParentDatasetIId() {
    return parentDatasetIId;
  }
  
  public void setParentDatasetIId(Long parentDatasetIId) {
    this.parentDatasetIId = parentDatasetIId;
  }
  
  public Map<String, Object> getMap() {
    return map;
  }
  
  public void setMap(Map<String, Object> map) {
    this.map = map;
  }
  
  @Override
  public String toString() {
    return "ElasticInodeDTO{" +
      "elasticId='" + elasticId + '\'' +
      ", score=" + score +
      ", name='" + name + '\'' +
      ", inodeId=" + inodeId +
      ", description='" + description + '\'' +
      ", modificationTime=" + modificationTime +
      ", creator='" + creator + '\'' +
      ", size=" + size +
      ", path='" + path + '\'' +
      ", highlights=" + highlights +
      ", parentProjectId=" + parentProjectId +
      ", parentDatasetIId=" + parentDatasetIId +
      ", parentDatasetId=" + parentDatasetId +
      ", parentDatasetName='" + parentDatasetName + '\'' +
      ", map=" + map +
      ", items=" + items +
      ", count=" + count +
      '}';
  }
}
