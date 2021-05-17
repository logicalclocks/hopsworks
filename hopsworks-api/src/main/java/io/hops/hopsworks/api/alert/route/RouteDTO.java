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
package io.hops.hopsworks.api.alert.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class RouteDTO extends RestDTO<RouteDTO> {
  
  private List<String> groupBy;
  private String groupWait;
  private String groupInterval;
  private String repeatInterval;
  private String receiver;
  private List<Route> routes;
  @JsonProperty("continue")
  private Boolean _continue;
  private Map<String, String> match;
  private Map<String, String> matchRe;
  
  public RouteDTO() {
  }
  
  public List<String> getGroupBy() {
    return groupBy;
  }
  
  public void setGroupBy(List<String> groupBy) {
    this.groupBy = groupBy;
  }
  
  public String getGroupWait() {
    return groupWait;
  }
  
  public void setGroupWait(String groupWait) {
    this.groupWait = groupWait;
  }
  
  public String getGroupInterval() {
    return groupInterval;
  }
  
  public void setGroupInterval(String groupInterval) {
    this.groupInterval = groupInterval;
  }
  
  public String getRepeatInterval() {
    return repeatInterval;
  }
  
  public void setRepeatInterval(String repeatInterval) {
    this.repeatInterval = repeatInterval;
  }
  
  public String getReceiver() {
    return receiver;
  }
  
  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }
  
  public List<Route> getRoutes() {
    return routes;
  }
  
  public void setRoutes(List<Route> routes) {
    this.routes = routes;
  }
  
  public Boolean getContinue() {
    return _continue;
  }
  
  public void setContinue(Boolean _continue) {
    this._continue = _continue;
  }
  
  public Map<String, String> getMatch() {
    return match;
  }
  
  public void setMatch(Map<String, String> match) {
    this.match = match;
  }
  
  public Map<String, String> getMatchRe() {
    return matchRe;
  }
  
  public void setMatchRe(Map<String, String> matchRe) {
    this.matchRe = matchRe;
  }
}