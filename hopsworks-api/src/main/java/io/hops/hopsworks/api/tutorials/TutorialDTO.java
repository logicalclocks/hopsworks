/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.tutorials;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.tutorials.Tutorial;

public class TutorialDTO extends RestDTO<TutorialDTO> {
  private Integer id;
  private Integer index;
  private String name;
  private String githubUrl;
  private String colabUrl = null;
  private String description;

  public TutorialDTO() {}

  public TutorialDTO(Tutorial tutorial) {
    this.id = tutorial.getId();
    this.index = tutorial.getIdx();
    this.name = tutorial.getName();
    this.githubUrl = "https://github.com/" + tutorial.getGithubPath();
    this.colabUrl = "https://colab.research.google.com/github/" + tutorial.getGithubPath();
    this.description = tutorial.getDescription();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGithubUrl() {
    return githubUrl;
  }

  public void setGithubUrl(String githubUrl) {
    this.githubUrl = githubUrl;
  }

  public String getColabUrl() {
    return colabUrl;
  }

  public void setColabUrl(String colabUrl) {
    this.colabUrl = colabUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}
