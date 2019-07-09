/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.json.datavalidation;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.featurestore.datavalidation.ConstraintGroup;
import io.hops.hopsworks.common.dao.featurestore.datavalidation.ConstraintGroupLevel;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ConstraintGroupDTO extends RestDTO<ConstraintGroupDTO> {
  
  @XmlElement(required = true)
  private String name;
  @XmlElement(required = true)
  private String description;
  @XmlElement(required = true)
  private ConstraintGroupLevel level;
  private ConstraintDTO constraints;
  
  public ConstraintGroupDTO() {
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
  
  public ConstraintGroupLevel getLevel() {
    return level;
  }
  
  public void setLevel(ConstraintGroupLevel level) {
    this.level = level;
  }
  
  public ConstraintDTO getConstraints() {
    return constraints;
  }
  
  public void setConstraints(ConstraintDTO constraints) {
    this.constraints = constraints;
  }
  
  public List<ConstraintGroup> toConstraintGroups() {
    List<ConstraintGroup> constraintGroups = new ArrayList<>();
    if (items != null) {
      for (ConstraintGroupDTO cgdto : items) {
        ConstraintGroup constraintGroup = new ConstraintGroup();
        constraintGroup.setName(cgdto.name);
        constraintGroup.setDescription(cgdto.description);
        constraintGroup.setLevel(cgdto.level);
        constraintGroup.setConstraints(cgdto.constraints.toConstraints());
        constraintGroups.add(constraintGroup);
      }
    }
    return constraintGroups;
  }
  
  public static ConstraintGroupDTO fromConstraintGroups(List<ConstraintGroup> constraintGroups) {
    ConstraintGroupDTO outterConstraintGroupDTO = new ConstraintGroupDTO();
    constraintGroups.forEach(cg -> {
      ConstraintGroupDTO cgdto = new ConstraintGroupDTO();
      cgdto.setName(cg.getName());
      cgdto.setDescription(cg.getDescription());
      cgdto.setLevel(cg.getLevel());
      
      ConstraintDTO outterConstraintDTO = new ConstraintDTO();
      cg.getConstraints().forEach(c -> outterConstraintDTO.addItem(ConstraintDTO.fromConstraint(c)));
      cgdto.setConstraints(outterConstraintDTO);
      outterConstraintGroupDTO.addItem(cgdto);
    });
    return outterConstraintGroupDTO;
  }
}
