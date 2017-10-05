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
