/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author ermiasg
 */
@XmlRootElement
public class DataSetDTO {

  private String name;
  private String description;
  private String searchable;

  public DataSetDTO() {
  }

  public DataSetDTO(String name, String description, String searchable) {
    this.name = name;
    this.description = description;
    this.searchable = searchable;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSearchable() {
    return searchable;
  }

  public void setSearchable(String searchable) {
    this.searchable = searchable;
  }

  @Override
  public String toString() {
    return "DataSetDTO{" + "name=" + name + ", description=" + description
            + ", searchable=" + searchable + '}';
  }

}
