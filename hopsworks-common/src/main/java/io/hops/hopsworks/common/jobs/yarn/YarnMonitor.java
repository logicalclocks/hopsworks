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

package io.hops.hopsworks.common.jobs.yarn;

import java.io.Closeable;
import java.io.IOException;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LogAggregationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;

public final class YarnMonitor implements Closeable {

  private final YarnClientWrapper yarnClientWrapper;
  private final ApplicationId appId;
  private final YarnClientService ycs;

  public YarnMonitor(ApplicationId id, YarnClientWrapper yarnClientWrapper,
      YarnClientService ycs) {
    if (id == null) {
      throw new IllegalArgumentException(
              "ApplicationId cannot be null for Yarn monitor!");
    }
    this.appId = id;
    this.yarnClientWrapper = yarnClientWrapper;
    this.ycs = ycs;
  }

  // Deprecated: YarnClient is initialized and started from the YarnClientService
  public YarnMonitor start() {
    yarnClientWrapper.getYarnClient().start();
    return this;
  }

  public void stop() {
    if (null != yarnClientWrapper) {
      ycs.closeYarnClient(yarnClientWrapper);
    }
  }

  public boolean isStarted() {
    return yarnClientWrapper.getYarnClient().isInState(Service.STATE.STARTED);
  }

  public boolean isStopped() {
    return yarnClientWrapper.getYarnClient().isInState(Service.STATE.STOPPED);
  }

  //---------------------------------------------------------------------------        
  //--------------------------- STATUS QUERIES --------------------------------
  //---------------------------------------------------------------------------
  public YarnApplicationState getApplicationState() throws YarnException,
          IOException {
    return yarnClientWrapper.getYarnClient().getApplicationReport(appId)
        .getYarnApplicationState();
  }

  public LogAggregationStatus getLogAggregationStatus() throws YarnException, IOException {
    return yarnClientWrapper.getYarnClient().getApplicationReport(appId)
        .getLogAggregationStatus();
  }
  
  public FinalApplicationStatus getFinalApplicationStatus() throws YarnException,
          IOException {
    return yarnClientWrapper.getYarnClient().getApplicationReport(appId)
        .getFinalApplicationStatus();
  }

  public float getProgress() throws YarnException,
          IOException {
    return yarnClientWrapper.getYarnClient().getApplicationReport(appId).getProgress();
  }

  public ApplicationId getApplicationId() {
    return appId;
  }

  //---------------------------------------------------------------------------        
  //------------------------- YARNCLIENT UTILS --------------------------------
  //---------------------------------------------------------------------------
  @Override
  public void close() {
    stop();
  }

  public void cancelJob(String appid) throws YarnException, IOException {
    ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
    yarnClientWrapper.getYarnClient().killApplication(applicationId);
  }
}
