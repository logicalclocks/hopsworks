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

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertManagerConfig {
  private Global global;
  private List<String> templates = null;
  private Route route;
  @JsonAlias({"inhibit_rules"})
  private List<InhibitRule> inhibitRules = null;
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

  public Global getGlobal() {
    return global;
  }

  public void setGlobal(Global global) {
    this.global = global;
  }

  public AlertManagerConfig withGlobal(Global global) {
    this.global = global;
    return this;
  }

  public List<String> getTemplates() {
    return templates;
  }

  public void setTemplates(List<String> templates) {
    this.templates = templates;
  }

  public AlertManagerConfig withTemplates(List<String> templates) {
    this.templates = templates;
    return this;
  }

  public Route getRoute() {
    return route;
  }

  public void setRoute(Route route) {
    this.route = route;
  }

  public AlertManagerConfig withRoute(Route route) {
    this.route = route;
    return this;
  }

  public List<InhibitRule> getInhibitRules() {
    return inhibitRules;
  }

  public void setInhibitRules(List<InhibitRule> inhibitRules) {
    this.inhibitRules = inhibitRules;
  }

  public AlertManagerConfig withInhibitRules(List<InhibitRule> inhibitRules) {
    this.inhibitRules = inhibitRules;
    return this;
  }

  public List<Receiver> getReceivers() {
    return receivers;
  }

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