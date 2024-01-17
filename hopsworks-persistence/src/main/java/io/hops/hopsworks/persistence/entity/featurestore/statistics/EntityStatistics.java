/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.statistics;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public abstract class EntityStatistics implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @Column(name = "computation_time")
  @NotNull
  private Date computationTime;
  
  @Basic
  @Column(name = "row_percentage")
  @NotNull
  private Float rowPercentage;
  
  public EntityStatistics() {  }
  
  public EntityStatistics(Date computationTime, Float rowPercentage) {
    this.computationTime = computationTime;
    this.rowPercentage = rowPercentage;
  }
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Date getComputationTime() {
    return this.computationTime;
  }
  
  public void setComputationTime(Date computationTime) {
    this.computationTime = computationTime;
  }
  
  public Float getRowPercentage() {
    return rowPercentage;
  }
  
  public void setRowPercentage(Float rowPercentage) {
    this.rowPercentage = rowPercentage;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    EntityStatistics that = (EntityStatistics) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    if (!computationTime.equals(that.computationTime)) {
      return false;
    }
    if (!rowPercentage.equals(that.rowPercentage)) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + computationTime.hashCode();
    result = 31 * result + rowPercentage.hashCode();
    return result;
  }
}