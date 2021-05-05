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
import java.util.Map;

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Labels {
  private String alertname;
  private Map<String, String> labels;

  public Labels() {
  }

  public Labels(String alertname) {
    this.alertname = alertname;
  }

  public String getAlertname() {
    return alertname;
  }

  public void setAlertname(String alertname) {
    this.alertname = alertname;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  @Override
  public String toString() {
    return "Labels{" +
      "alertname='" + alertname + '\'' +
      ", labels=" + labels +
      '}';
  }
}
