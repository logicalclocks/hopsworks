/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
