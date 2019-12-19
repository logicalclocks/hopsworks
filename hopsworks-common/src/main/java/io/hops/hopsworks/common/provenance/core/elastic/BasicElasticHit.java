/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.core.elastic;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class BasicElasticHit {
  private String id;
  private Float score;
  private Map<String, Object> source;
  
  public BasicElasticHit() {}
  public BasicElasticHit(String id, Float score, Map<String, Object> source) {
    this.id = id;
    this.score = score;
    this.source = source;
  }
  
  public static BasicElasticHit instance(GetResponse response) {
    return new BasicElasticHit(response.getId(), null, response.getSourceAsMap());
  }
  
  public static BasicElasticHit instance(SearchHit hit) {
    return new BasicElasticHit(hit.getId(), hit.getScore(), hit.getSourceAsMap());
  }
  
  public Float getScore() {
    return score;
  }
  
  public void setScore(Float score) {
    this.score = score;
  }
  
  public Map<String, Object> getSource() {
    return source;
  }
  
  public void setSource(Map<String, Object> source) {
    this.source = source;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
}
