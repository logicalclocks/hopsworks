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
  "type",
  "url",
  "name",
  "value",
  "confirm",
  "style"
  })
public class ActionConfig {
  @JsonProperty("text")
  private String text;
  @JsonProperty("type")
  private String type;
  @JsonProperty("url")
  private String url;
  @JsonProperty("name")
  private String name;
  @JsonProperty("value")
  private String value;
  @JsonProperty("confirm")
  private ActionConfirmFieldConfig confirm;
  @JsonProperty("style")
  private String style;

  public ActionConfig() {
  }

  public ActionConfig(String text, String type) {
    this.text = text;
    this.type = type;
  }

  @JsonProperty("text")
  public String getText() {
    return text;
  }

  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  public ActionConfig withUrl(String url) {
    this.url = url;
    return this;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ActionConfig withName(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(String value) {
    this.value = value;
  }

  public ActionConfig withValue(String value) {
    this.value = value;
    return this;
  }

  @JsonProperty("confirm")
  public ActionConfirmFieldConfig getConfirm() {
    return confirm;
  }

  @JsonProperty("confirm")
  public void setConfirm(ActionConfirmFieldConfig confirm) {
    this.confirm = confirm;
  }

  public ActionConfig withConfirm(ActionConfirmFieldConfig confirm) {
    this.confirm = confirm;
    return this;
  }

  @JsonProperty("style")
  public String getStyle() {
    return style;
  }

  @JsonProperty("style")
  public void setStyle(String style) {
    this.style = style;
  }

  public ActionConfig withStyle(String style) {
    this.style = style;
    return this;
  }

  @Override
  public String toString() {
    return "ActionConfig{" +
      "text='" + text + '\'' +
      ", type='" + type + '\'' +
      ", url='" + url + '\'' +
      ", name='" + name + '\'' +
      ", value='" + value + '\'' +
      ", confirm=" + confirm +
      ", style='" + style + '\'' +
      '}';
  }
}
