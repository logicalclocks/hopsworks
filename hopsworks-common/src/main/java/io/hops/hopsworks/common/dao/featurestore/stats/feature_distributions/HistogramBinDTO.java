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

package io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * XML/JSON DTO representing a Bin in a feature histogram
 */
@XmlRootElement
@XmlType(propOrder = {"bin", "frequency"})
public class HistogramBinDTO {

  private String bin;
  private Integer frequency;

  public HistogramBinDTO() {
    //Default empty constructor required for JAXB
  }

  @XmlElement
  public String getBin() {
    return bin;
  }

  @XmlElement
  public Integer getFrequency() {
    return frequency;
  }

  public void setBin(String bin) {
    this.bin = bin;
  }

  public void setFrequency(Integer frequency) {
    this.frequency = frequency;
  }

  @Override
  public String toString() {
    return "HistogramBinDTO{" +
        "bin='" + bin + '\'' +
        ", frequency=" + frequency +
        '}';
  }
}
