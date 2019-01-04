/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * XML/JSON DTO representing a 2-dimensional datapoint in cluster analysi
 */
@XmlRootElement
@XmlType(propOrder = {"datapointName", "firstDimension", "secondDimension"})
public class DatapointDTO {

  private String datapointName;
  private Float firstDimension;
  private Float secondDimension;

  public DatapointDTO() {
  }

  public DatapointDTO(String datapointName, Float firstDimension, Float secondDimension) {
    this.datapointName = datapointName;
    this.firstDimension = firstDimension;
    this.secondDimension = secondDimension;
  }

  @XmlElement
  public String getDatapointName() {
    return datapointName;
  }

  @XmlElement
  public Float getFirstDimension() {
    return firstDimension;
  }

  @XmlElement
  public Float getSecondDimension() {
    return secondDimension;
  }

  public void setFirstDimension(Float firstDimension) {
    this.firstDimension = firstDimension;
  }

  public void setSecondDimension(Float secondDimension) {
    this.secondDimension = secondDimension;
  }

  public void setDatapointName(String datapointName) {
    this.datapointName = datapointName;
  }

  @Override
  public String toString() {
    return "DatapointDTO{" +
        "datapointName='" + datapointName + '\'' +
        ", firstDimension=" + firstDimension +
        ", secondDimension=" + secondDimension +
        '}';
  }
}
