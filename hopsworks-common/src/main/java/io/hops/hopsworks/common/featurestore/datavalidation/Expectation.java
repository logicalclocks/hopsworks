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

package io.hops.hopsworks.common.featurestore.datavalidation;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Rule;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class Expectation {
  
  private String name;
  private String description;
  @XmlElement(name = "features")
  private List<String> features;
  @XmlElement(name = "rules")
  private List<Rule> rules;
  
  public Expectation() {
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

  public List<String> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<String> features) {
    this.features = features;
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
      ", rules=" + rules +
      '}';
  }
}
