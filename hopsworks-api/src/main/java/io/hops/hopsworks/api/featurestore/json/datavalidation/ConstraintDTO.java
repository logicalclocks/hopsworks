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
import io.hops.hopsworks.common.dao.featurestore.datavalidation.Constraint;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ConstraintDTO extends RestDTO<ConstraintDTO> {
  private String name;
  private String hint;
  private Double min;
  private Double max;
  private List<String> columns;
  
  public ConstraintDTO() {
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getHint() {
    return hint;
  }
  
  public void setHint(String hint) {
    this.hint = hint;
  }
  
  public Double getMin() {
    return min;
  }
  
  public void setMin(Double min) {
    this.min = min;
  }
  
  public Double getMax() {
    return max;
  }
  
  public void setMax(Double max) {
    this.max = max;
  }
  
  public List<String> getColumns() {
    return columns;
  }
  
  public void setColumns(List<String> columns) {
    this.columns = columns;
  }
  
  public List<Constraint> toConstraints() {
    List<Constraint> constraints = new ArrayList<>();
    if (items != null) {
      for (ConstraintDTO cdto : items) {
        Constraint constraint = new Constraint();
        constraint.setName(cdto.name);
        constraint.setHint(cdto.hint);
        constraint.setMin(cdto.min);
        constraint.setMax(cdto.max);
        constraint.setColumns(cdto.columns);
        
        constraints.add(constraint);
      }
    }
    return constraints;
  }
  
  public static ConstraintDTO fromConstraint(Constraint constraint) {
    ConstraintDTO cdto = new ConstraintDTO();
    cdto.setName(constraint.getName());
    cdto.setHint(constraint.getHint());
    cdto.setMin(constraint.getMin());
    cdto.setMax(constraint.getMax());
    cdto.setColumns(constraint.getColumns());
    return cdto;
  }
}
