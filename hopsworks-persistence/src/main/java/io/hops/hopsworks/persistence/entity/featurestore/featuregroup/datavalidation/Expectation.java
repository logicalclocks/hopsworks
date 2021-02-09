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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

public class Expectation {
  
  private String name;
  @XmlTransient
  private String description;
  @XmlElement(name = "features")
  private List<String> features;
  @XmlElement(name = "rules")
  private List<Rule> rules;

  public Expectation() {
  }
  
  public Expectation(String name, String description, List<String> features, List<Rule> rules) {
    this.name = name;
    this.description = description;
    this.features = features;
    this.rules = rules;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public List<String> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<String> features) {
    this.features = features;
  }
  
  @XmlTransient
  public String getDescription() {
    return description;
  }
  
  @XmlTransient
  public void setDescription(String description) {
    this.description = description;
  }
  
  public List<Rule> getRules() {
    return rules;
  }
  
  public void setRules(List<Rule> rules) {
    this.rules = rules;
  }
  
  @Override
  public String toString() {
    return "Expectation{" +
      "name='" + name + '\'' +
      ", features=" + features +
      ", description='" + description + '\'' +
      ", rule=" + rules +
      '}';
  }
}
