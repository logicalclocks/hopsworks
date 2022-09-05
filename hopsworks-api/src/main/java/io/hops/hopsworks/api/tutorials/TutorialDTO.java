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
import io.hops.hopsworks.persistence.entity.tutorials.TutorialCategory;

public class TutorialDTO extends RestDTO<TutorialDTO> {
  private Integer id;
  private Integer index;
  private String name;
  private String githubUrl;
  private String colabUrl = null;
  private String imageUrl;
  private String description;
  private String duration;
  private String tags;
  private TutorialCategory category;
  private String style;

  public TutorialDTO() {}

  public TutorialDTO(Tutorial tutorial) {
    this.id = tutorial.getId();
    this.index = tutorial.getIdx();
    this.name = tutorial.getName();
    this.githubUrl = "https://github.com/" + tutorial.getGithubPath();
    if (tutorial.isSingleNotebook()) {
      this.colabUrl = "https://colab.research.google.com/github/" + tutorial.getGithubPath();
    }
    this.imageUrl = tutorial.getImageUrl();
    this.description = tutorial.getDescription();
    this.duration = tutorial.getDuration();
    this.tags = tutorial.getTags();
    this.category = tutorial.getCategory();
    this.style = tutorial.getStyle();
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

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public TutorialCategory getCategory() {
    return category;
  }

  public void setCategory(TutorialCategory category) {
    this.category = category;
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }
}
