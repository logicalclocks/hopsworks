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
  "text",
  "dismiss_text",
  "ok_text",
  "title"
  })
public class ActionConfirmFieldConfig {
  @JsonProperty("text")
  private String text;
  @JsonProperty("dismiss_text")
  private String dismissText;
  @JsonProperty("ok_text")
  private String okText;
  @JsonProperty("title")
  private String title;
  
  public ActionConfirmFieldConfig() {
  }
  
  public ActionConfirmFieldConfig(String text) {
    this.text = text;
  }
  
  @JsonProperty("text")
  public String getText() {
    return text;
  }
  
  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }
  
  @JsonProperty("dismiss_text")
  public String getDismissText() {
    return dismissText;
  }
  
  @JsonProperty("dismiss_text")
  public void setDismissText(String dismissText) {
    this.dismissText = dismissText;
  }
  
  public ActionConfirmFieldConfig withDismissText(String dismissText) {
    this.dismissText = dismissText;
    return this;
  }
  
  @JsonProperty("ok_text")
  public String getOkText() {
    return okText;
  }
  
  @JsonProperty("ok_text")
  public void setOkText(String okText) {
    this.okText = okText;
  }
  
  public ActionConfirmFieldConfig withOkText(String okText) {
    this.okText = okText;
    return this;
  }
  
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }
  
  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }
  
  public ActionConfirmFieldConfig withTitle(String title) {
    this.title = title;
    return this;
  }
  
  @Override
  public String toString() {
    return "ActionConfirmFieldConfig{" +
      "text='" + text + '\'' +
      ", dismissText='" + dismissText + '\'' +
      ", okText='" + okText + '\'' +
      ", title='" + title + '\'' +
      '}';
  }
}
