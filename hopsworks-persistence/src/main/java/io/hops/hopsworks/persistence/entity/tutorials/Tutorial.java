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

package io.hops.hopsworks.persistence.entity.tutorials;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tutorial", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(
    name = "Tutorial.getAll",
    query = "SELECT tut FROM Tutorial tut"
  ),
  @NamedQuery(
    name = "Tutorial.countAll",
    query = "SELECT COUNT(tut.id) from Tutorial tut"
  )}
)
public class Tutorial implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "idx")
  private Integer idx;

  @Basic(optional = false)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @Column(name = "github_path")
  private String githubPath;

  @Basic(optional = false)
  @Column(name = "image_url")
  private String imageUrl;

  @Basic(optional = false)
  @Column(name = "single_notebook")
  private boolean singleNotebook;

  @Basic(optional = false)
  @Column(name = "description")
  private String description;

  @Basic(optional = false)
  @Column(name = "duration")
  private String duration;

  @Basic(optional = false)
  @Column(name = "tags")
  private String tags;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "category")
  private TutorialCategory category;

  @Basic
  @Column(name = "style")
  private String style;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getIdx() {
    return idx;
  }

  public void setIdx(Integer idx) {
    this.idx = idx;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGithubPath() {
    return githubPath;
  }

  public void setGithubPath(String githubPath) {
    this.githubPath = githubPath;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public boolean isSingleNotebook() {
    return singleNotebook;
  }

  public void setSingleNotebook(boolean singleNotebook) {
    this.singleNotebook = singleNotebook;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tutorial tutorial = (Tutorial) o;
    return singleNotebook == tutorial.singleNotebook && Objects.equals(id, tutorial.id) &&
      Objects.equals(idx, tutorial.idx) && Objects.equals(name, tutorial.name) &&
      Objects.equals(githubPath, tutorial.githubPath) &&
      Objects.equals(imageUrl, tutorial.imageUrl) &&
      Objects.equals(description, tutorial.description) &&
      Objects.equals(duration, tutorial.duration) && Objects.equals(tags, tutorial.tags) &&
      category == tutorial.category && Objects.equals(style, tutorial.style);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, idx, name, githubPath, imageUrl, singleNotebook, description, duration, tags, category,
      style);
  }
}
