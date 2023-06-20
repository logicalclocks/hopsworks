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
package io.hops.hopsworks.api.admin.alert.management;

import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.api.alert.receiver.PostableReceiverDTO;
import io.hops.hopsworks.api.alert.route.PostableRouteDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PostableAlertManagerConfig {
  private Global global;
  private List<String> templates;
  private PostableRouteDTO route;
  private List<InhibitRule> inhibitRules;
  private List<PostableReceiverDTO> receivers;
  
  public PostableAlertManagerConfig() {
  }
  
  public Global getGlobal() {
    return global;
  }
  
  public void setGlobal(Global global) {
    this.global = global;
  }
  
  public List<String> getTemplates() {
    return templates;
  }
  
  public void setTemplates(List<String> templates) {
    this.templates = templates;
  }
  
  public PostableRouteDTO getRoute() {
    return route;
  }
  
  public void setRoute(PostableRouteDTO route) {
    this.route = route;
  }
  
  public List<InhibitRule> getInhibitRules() {
    return inhibitRules;
  }
  
  public void setInhibitRules(List<InhibitRule> inhibitRules) {
    this.inhibitRules = inhibitRules;
  }
  
  public List<PostableReceiverDTO> getReceivers() {
    return receivers;
  }
  
  public void setReceivers(List<PostableReceiverDTO> receivers) {
    this.receivers = receivers;
  }
}
