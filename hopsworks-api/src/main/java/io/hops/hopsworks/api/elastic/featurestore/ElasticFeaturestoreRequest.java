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

import io.hops.hopsworks.common.elastic.FeaturestoreDocType;

public class ElasticFeaturestoreRequest {
  private String term;
  private FeaturestoreDocType docType;
  private Integer from;
  private Integer size;
  
  public ElasticFeaturestoreRequest() {
  }
  
  public ElasticFeaturestoreRequest(String term, FeaturestoreDocType docType, Integer from, Integer size) {
    this.term = term;
    this.docType = docType;
    this.from = from;
    this.size = size;
  }
  
  public String getTerm() {
    return term;
  }
  
  public void setTerm(String term) {
    this.term = term;
  }
  
  public FeaturestoreDocType getDocType() {
    return docType;
  }
  
  public void setDocType(FeaturestoreDocType docType) {
    this.docType = docType;
  }
  
  public Integer getFrom() {
    return from;
  }
  
  public void setFrom(Integer from) {
    this.from = from;
  }
  
  public Integer getSize() {
    return size;
  }
  
  public void setSize(Integer size) {
    this.size = size;
  }
}
