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
