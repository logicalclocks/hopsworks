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
  "text"
  })
public class LinkConfig {
  @JsonProperty("href")
  private String href;
  @JsonProperty("text")
  private String text;
  
  public LinkConfig() {
  }
  
  public LinkConfig(String href, String text) {
    this.href = href;
    this.text = text;
  }
  
  @JsonProperty("href")
  public String getHref() {
    return href;
  }
  
  @JsonProperty("href")
  public void setHref(String href) {
    this.href = href;
  }
  
  @JsonProperty("text")
  public String getText() {
    return text;
  }
  
  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }
  
  @Override
  public String toString() {
    return "LinkConfig{" +
      "href='" + href + '\'' +
      ", text='" + text + '\'' +
      '}';
  }
}
