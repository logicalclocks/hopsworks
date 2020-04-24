/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
import org.apache.hadoop.yarn.client.api.YarnClient;
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
  
  public YarnClient getYarnClient() {
    return yarnClientWrapper.getYarnClient();
  }
}
