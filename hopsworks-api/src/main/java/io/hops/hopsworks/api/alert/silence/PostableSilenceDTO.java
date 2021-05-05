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

package io.hops.hopsworks.api.alert.silence;

import io.hops.hopsworks.alerting.api.alert.dto.Matcher;

import java.util.List;

public class PostableSilenceDTO {

  private String id;
  private String comment;
  private String endsAt;
  private String startsAt;
  private List<Matcher> matchers;

  public PostableSilenceDTO() {
  }

  public PostableSilenceDTO(String comment, String endsAt, String startsAt, List<Matcher> matchers) {
    this.comment = comment;
    this.endsAt = endsAt;
    this.startsAt = startsAt;
    this.matchers = matchers;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(String endsAt) {
    this.endsAt = endsAt;
  }

  public String getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(String startsAt) {
    this.startsAt = startsAt;
  }

  public List<Matcher> getMatchers() {
    return matchers;
  }

  public void setMatchers(List<Matcher> matchers) {
    this.matchers = matchers;
  }

  @Override
  public String toString() {
    return "PostableSilenceDTO{" +
        "id='" + id + '\'' +
        ", comment='" + comment + '\'' +
        ", endsAt='" + endsAt + '\'' +
        ", startsAt='" + startsAt + '\'' +
        ", matchers=" + matchers +
        '}';
  }
}
