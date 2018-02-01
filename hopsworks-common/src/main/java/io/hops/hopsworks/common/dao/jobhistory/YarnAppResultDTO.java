/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class YarnAppResultDTO implements Serializable {

  private YarnAppResult yarnAppResult;
  private long totalExecutionDuration, runningTime;
  private String ownerFullName;

  public YarnAppResultDTO() {
  }

  public YarnAppResultDTO(YarnAppResult yarnAppResult) {
    this.yarnAppResult = yarnAppResult;
  }

  public YarnAppResultDTO(YarnAppResult yarnAppResult,
          long totalExecutionDuration, long runningTime) {
    this.yarnAppResult = yarnAppResult;
    this.totalExecutionDuration = totalExecutionDuration;
    this.runningTime = runningTime;
  }

  /**
   * @return the yarnAppResult
   */
  public YarnAppResult getYarnAppResult() {
    return yarnAppResult;
  }

  /**
   * @param yarnAppResult the yarnAppResult to set
   */
  public void setYarnAppResult(YarnAppResult yarnAppResult) {
    this.yarnAppResult = yarnAppResult;
  }

  /**
   * @return the totalExecutionDuration
   */
  public long getTotalExecutionDuration() {
    return totalExecutionDuration;
  }

  /**
   * @param totalExecutionDuration the totalExecutionDuration to set
   */
  public void setTotalExecutionDuration(long totalExecutionDuration) {
    this.totalExecutionDuration = totalExecutionDuration;
  }

  /**
   * @return the runningTime
   */
  public long getRunningTime() {
    return runningTime;
  }

  /**
   * @param runningTime the runningTime to set
   */
  public void setRunningTime(long runningTime) {
    this.runningTime = runningTime;
  }

  /**
   * @return the ownerFullName
   */
  public String getOwnerFullName() {
    return ownerFullName;
  }

  /**
   * @param ownerFullName the ownerFullName to set
   */
  public void setOwnerFullName(String ownerFullName) {
    this.ownerFullName = ownerFullName;
  }

}
