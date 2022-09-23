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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionConfirmFieldConfig {
  private String text;
  @JsonAlias({"dismiss_text"})
  private String dismissText;
  @JsonAlias({"ok_text"})
  private String okText;
  private String title;

  public ActionConfirmFieldConfig() {
  }

  public ActionConfirmFieldConfig(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getDismissText() {
    return dismissText;
  }

  public void setDismissText(String dismissText) {
    this.dismissText = dismissText;
  }

  public ActionConfirmFieldConfig withDismissText(String dismissText) {
    this.dismissText = dismissText;
    return this;
  }

  public String getOkText() {
    return okText;
  }

  public void setOkText(String okText) {
    this.okText = okText;
  }

  public ActionConfirmFieldConfig withOkText(String okText) {
    this.okText = okText;
    return this;
  }

  public String getTitle() {
    return title;
  }

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
