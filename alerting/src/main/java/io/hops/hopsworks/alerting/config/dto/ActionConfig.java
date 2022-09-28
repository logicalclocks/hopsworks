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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionConfig {
  private String text;
  private String type;
  private String url;
  private String name;
  private String value;
  private ActionConfirmFieldConfig confirm;
  private String style;
  
  public ActionConfig() {
  }

  public ActionConfig(String text, String type) {
    this.text = text;
    this.type = type;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ActionConfig withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ActionConfig withName(String name) {
    this.name = name;
    return this;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ActionConfig withValue(String value) {
    this.value = value;
    return this;
  }

  public ActionConfirmFieldConfig getConfirm() {
    return confirm;
  }

  public void setConfirm(ActionConfirmFieldConfig confirm) {
    this.confirm = confirm;
  }

  public ActionConfig withConfirm(ActionConfirmFieldConfig confirm) {
    this.confirm = confirm;
    return this;
  }

  public String getStyle() {
    return style;
  }

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
