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
package io.hops.hopsworks.alerting.config.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "href",
  "source",
  "alt"
  })
public class Imageconfig {
  @JsonProperty("href")
  private String href;
  @JsonProperty("source")
  private String source;
  @JsonProperty("alt")
  private String alt;
  
  public Imageconfig() {
  }
  
  public Imageconfig(String href, String source, String alt) {
    this.href = href;
    this.source = source;
    this.alt = alt;
  }
  
  @JsonProperty("href")
  public String getHref() {
    return href;
  }
  
  @JsonProperty("href")
  public void setHref(String href) {
    this.href = href;
  }
  
  @JsonProperty("source")
  public String getSource() {
    return source;
  }
  
  @JsonProperty("source")
  public void setSource(String source) {
    this.source = source;
  }
  
  @JsonProperty("alt")
  public String getAlt() {
    return alt;
  }
  
  @JsonProperty("alt")
  public void setAlt(String alt) {
    this.alt = alt;
  }
  
  @Override
  public String toString() {
    return "Imageconfig{" +
      "href='" + href + '\'' +
      ", source='" + source + '\'' +
      ", alt='" + alt + '\'' +
      '}';
  }
}
