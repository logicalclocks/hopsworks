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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "group_by",
  "group_wait",
  "group_interval",
  "repeat_interval",
  "receiver",
  "routes",
  "continue",
  "match",
  "match_re"
  })
public class Route {

  @JsonAlias({"group_by"})
  private List<String> groupBy = null;
  @JsonAlias({"group_wait"})
  private String groupWait;
  @JsonAlias({"group_interval"})
  private String groupInterval;
  @JsonAlias({"repeat_interval"})
  private String repeatInterval;
  private String receiver;
  private List<Route> routes = null;
  @JsonProperty("continue")
  private Boolean _continue;
  private Map<String, String> match;
  @JsonAlias({"match_re"})
  private Map<String, String> matchRe;

  public Route() {
  }

  public Route(String receiver) {
    this.receiver = receiver;
  }

  public List<String> getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(List<String> groupBy) {
    this.groupBy = groupBy;
  }

  public Route withGroupBy(List<String> groupBy) {
    this.groupBy = groupBy;
    return this;
  }

  public String getGroupWait() {
    return groupWait;
  }

  public void setGroupWait(String groupWait) {
    this.groupWait = groupWait;
  }

  public Route withGroupWait(String groupWait) {
    this.groupWait = groupWait;
    return this;
  }

  public String getGroupInterval() {
    return groupInterval;
  }

  public void setGroupInterval(String groupInterval) {
    this.groupInterval = groupInterval;
  }

  public Route withGroupInterval(String groupInterval) {
    this.groupInterval = groupInterval;
    return this;
  }

  public String getRepeatInterval() {
    return repeatInterval;
  }

  public void setRepeatInterval(String repeatInterval) {
    this.repeatInterval = repeatInterval;
  }

  public Route withRepeatInterval(String repeatInterval) {
    this.repeatInterval = repeatInterval;
    return this;
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

  public Route withRoutes(List<Route> routes) {
    this.routes = routes;
    return this;
  }

  public Boolean getContinue() {
    return _continue;
  }

  public void setContinue(Boolean _continue) {
    this._continue = _continue;
  }

  public Route withContinue(Boolean _continue) {
    this._continue = _continue;
    return this;
  }

  public Map<String, String> getMatch() {
    return match;
  }

  public void setMatch(Map<String, String> match) {
    this.match = match;
  }

  public Route withMatch(Map<String, String> match) {
    this.match = match;
    return this;
  }

  public Map<String, String> getMatchRe() {
    return matchRe;
  }

  public void setMatchRe(Map<String, String> matchRe) {
    this.matchRe = matchRe;
  }

  public Route withMatchRe(Map<String, String> matchRe) {
    this.matchRe = matchRe;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Route route = (Route) o;
    return Objects.equals(receiver, route.receiver) && Objects.equals(match, route.match) &&
        Objects.equals(matchRe, route.matchRe);
  }

  @Override
  public int hashCode() {
    return Objects.hash(receiver, match, matchRe);
  }

  @Override
  public String toString() {
    return "Route{" +
      "groupBy=" + groupBy +
      ", groupWait='" + groupWait + '\'' +
      ", groupInterval='" + groupInterval + '\'' +
      ", repeatInterval='" + repeatInterval + '\'' +
      ", receiver='" + receiver + '\'' +
      ", routes=" + routes +
      ", continue=" + _continue +
      ", match=" + match +
      ", matchRe=" + matchRe +
      '}';
  }
}
