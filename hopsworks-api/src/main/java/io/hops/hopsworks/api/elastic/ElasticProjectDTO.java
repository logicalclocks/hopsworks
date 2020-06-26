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
import java.util.List;
import java.util.Map;

@XmlRootElement
public class ElasticProjectDTO extends RestDTO<ElasticProjectDTO> {
  private String elasticId;
  private Float score;
  private Integer projectId;
  private String name;
  private Long projectIId;
  private String description;
  private Date created;
  private String creator;
  private Boolean member;
  private Map<String, HighlightField> highlights;
  private List<String> members;
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
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Long getProjectIId() {
    return projectIId;
  }
  
  public void setProjectIId(Long projectIId) {
    this.projectIId = projectIId;
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
  
  public String getCreator() {
    return creator;
  }
  
  public void setCreator(String creator) {
    this.creator = creator;
  }
  
  public Boolean getMember() {
    return member;
  }
  
  public void setMember(Boolean member) {
    this.member = member;
  }
  
  public Map<String, HighlightField> getHighlights() {
    return highlights;
  }
  
  public void setHighlights(
    Map<String, HighlightField> highlights) {
    this.highlights = highlights;
  }
  
  public List<String> getMembers() {
    return members;
  }
  
  public void setMembers(List<String> members) {
    this.members = members;
  }
  
  public Map<String, Object> getMap() {
    return map;
  }
  
  public void setMap(Map<String, Object> map) {
    this.map = map;
  }
  
  @Override
  public String toString() {
    return "ElasticProjectDTO{" +
      "elasticId='" + elasticId + '\'' +
      ", score=" + score +
      ", projectId=" + projectId +
      ", name='" + name + '\'' +
      ", projectIId=" + projectIId +
      ", description='" + description + '\'' +
      ", created=" + created +
      ", creator='" + creator + '\'' +
      ", highlights=" + highlights +
      ", members=" + members +
      ", map=" + map +
      ", items=" + items +
      '}';
  }
}
