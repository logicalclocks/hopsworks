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

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "global",
  "templates",
  "route",
  "inhibit_rules",
  "receivers"
  })
public class AlertManagerConfig {
  
  @JsonProperty("global")
  private Global global;
  @JsonProperty("templates")
  private List<String> templates = null;
  @JsonProperty("route")
  private Route route;
  @JsonProperty("inhibit_rules")
  private List<InhibitRule> inhibitRules = null;
  @JsonProperty("receivers")
  private List<Receiver> receivers = null;
  
  public AlertManagerConfig() {
  }
  
  /**
   * @param route
   * @param receivers
   * @param templates
   * @param inhibitRules
   * @param global
   */
  public AlertManagerConfig(Global global, List<String> templates, Route route, List<InhibitRule> inhibitRules,
    List<Receiver> receivers) {
    this.global = global;
    this.templates = templates;
    this.route = route;
    this.inhibitRules = inhibitRules;
    this.receivers = receivers;
  }
  
  @JsonProperty("global")
  public Global getGlobal() {
    return global;
  }
  
  @JsonProperty("global")
  public void setGlobal(Global global) {
    this.global = global;
  }
  
  public AlertManagerConfig withGlobal(Global global) {
    this.global = global;
    return this;
  }
  
  @JsonProperty("templates")
  public List<String> getTemplates() {
    return templates;
  }
  
  @JsonProperty("templates")
  public void setTemplates(List<String> templates) {
    this.templates = templates;
  }
  
  public AlertManagerConfig withTemplates(List<String> templates) {
    this.templates = templates;
    return this;
  }
  
  @JsonProperty("route")
  public Route getRoute() {
    return route;
  }
  
  @JsonProperty("route")
  public void setRoute(Route route) {
    this.route = route;
  }
  
  public AlertManagerConfig withRoute(Route route) {
    this.route = route;
    return this;
  }
  
  @JsonProperty("inhibit_rules")
  public List<InhibitRule> getInhibitRules() {
    return inhibitRules;
  }
  
  @JsonProperty("inhibit_rules")
  public void setInhibitRules(List<InhibitRule> inhibitRules) {
    this.inhibitRules = inhibitRules;
  }
  
  public AlertManagerConfig withInhibitRules(List<InhibitRule> inhibitRules) {
    this.inhibitRules = inhibitRules;
    return this;
  }
  
  @JsonProperty("receivers")
  public List<Receiver> getReceivers() {
    return receivers;
  }
  
  @JsonProperty("receivers")
  public void setReceivers(List<Receiver> receivers) {
    this.receivers = receivers;
  }
  
  public AlertManagerConfig withReceivers(List<Receiver> receivers) {
    this.receivers = receivers;
    return this;
  }
  
  @Override
  public String toString() {
    return "AlertManagerConfig{" +
      "global=" + global +
      ", templates=" + templates +
      ", route=" + route +
      ", inhibitRules=" + inhibitRules +
      ", receivers=" + receivers +
      '}';
  }
}