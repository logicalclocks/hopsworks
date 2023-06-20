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
import io.hops.hopsworks.api.alert.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
public class PostableRouteDTO {
  private List<String> groupBy;
  private String groupWait;
  private String groupInterval;
  private String repeatInterval;
  private String receiver;
  private List<Route> routes;
  @JsonProperty("continue")
  private Boolean _continue;
  private List<Entry> match;
  private List<Entry> matchRe;

  public PostableRouteDTO() {
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

  public List<Entry> getMatch() {
    return match;
  }

  public void setMatch(List<Entry> match) {
    this.match = match;
  }

  public List<Entry> getMatchRe() {
    return matchRe;
  }

  public void setMatchRe(List<Entry> matchRe) {
    this.matchRe = matchRe;
  }

  public Route toRoute() {
    return new Route(this.getReceiver())
      .withGroupBy(this.getGroupBy())
      .withGroupWait(this.getGroupWait())
      .withGroupInterval(this.getGroupInterval())
      .withRepeatInterval(this.getRepeatInterval())
      .withRoutes(this.getRoutes())
      .withContinue(this.getContinue())
      .withMatch(this.getMatch() != null? this.getMatch().stream()
        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)) : null)
      .withMatchRe(this.getMatchRe() != null? this.getMatchRe().stream()
        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)) : null);
  }
}
