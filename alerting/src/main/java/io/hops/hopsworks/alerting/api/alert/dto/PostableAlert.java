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
package io.hops.hopsworks.alerting.api.alert.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;
import java.util.Map;

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostableAlert {
  private Map<String, String> labels;
  private Map<String, String> annotations;
  private String startsAt;
  private String endsAt;
  private URL generatorURL;
  
  public PostableAlert() {
  }
  
  public PostableAlert(Map<String, String> labels, Map<String, String> annotations) {
    this.labels = labels;
    this.annotations = annotations;
  }
  
  public Map<String, String> getLabels() {
    return labels;
  }
  
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
  
  public Map<String, String> getAnnotations() {
    return annotations;
  }
  
  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }
  
  public String getStartsAt() {
    return startsAt;
  }
  
  public void setStartsAt(String startsAt) {
    this.startsAt = startsAt;
  }
  
  public String getEndsAt() {
    return endsAt;
  }
  
  public void setEndsAt(String endsAt) {
    this.endsAt = endsAt;
  }
  
  public URL getGeneratorURL() {
    return generatorURL;
  }
  
  public void setGeneratorURL(URL generatorURL) {
    this.generatorURL = generatorURL;
  }
  
  @Override
  public String toString() {
    return "PostableAlert{" +
      "labels=" + labels +
      ", annotations=" + annotations +
      ", startsAt=" + startsAt +
      ", endsAt=" + endsAt +
      ", generatorURL=" + generatorURL +
      '}';
  }
}
