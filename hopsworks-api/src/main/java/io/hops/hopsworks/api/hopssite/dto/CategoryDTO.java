/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.hopssite.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CategoryDTO {

  private String categoryName;
  private String displayName;
  private boolean parentCategory;

  public CategoryDTO() {
  }

  public CategoryDTO(String categoryName, String displayName, boolean parentCategory) {
    this.categoryName = categoryName;
    this.displayName = displayName;
    this.parentCategory = parentCategory;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public boolean isParentCategory() {
    return parentCategory;
  }

  public void setParentCategory(boolean parentCategory) {
    this.parentCategory = parentCategory;
  }


  @Override
  public String toString() {
    return "CategoryDTO{" + "categoryName=" + categoryName + ", displayName=" + displayName + '}';
  }

}
