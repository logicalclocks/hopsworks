/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.trainingdatasets.split;

import io.hops.hopsworks.common.featurestore.trainingdatasets.DateAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
//import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

/**
 * DTO containing the human-readable information of a training dataset split, can be converted to JSON or XML
 * representation using jaxb.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TrainingDatasetSplitDTO {
  @XmlElement
  private String name;
  @XmlElement
  private Float percentage;
  @XmlElement
  private SplitType splitType;
  @XmlJavaTypeAdapter(DateAdapter.class)
  private Date startTime;
  @XmlJavaTypeAdapter(DateAdapter.class)
  private Date endTime;
  
  public TrainingDatasetSplitDTO(){}
  
  public TrainingDatasetSplitDTO(String name, Float percentage) {
    this.name = name;
    this.percentage = percentage;
    this.splitType = SplitType.RANDOM_SPLIT;
  }

  public TrainingDatasetSplitDTO(String name, Date startTime, Date endTime) {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.splitType = SplitType.TIME_SERIES_SPLIT;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Float getPercentage() {
    return percentage;
  }
  
  public void setPercentage(Float percentage) {
    this.percentage = percentage;
  }

  public SplitType getSplitType() {
    return splitType;
  }

  public void setSplitType(SplitType splitType) {
    this.splitType = splitType;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }
}
