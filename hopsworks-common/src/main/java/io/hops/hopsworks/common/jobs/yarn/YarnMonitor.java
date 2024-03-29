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

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LogAggregationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class YarnMonitor {
  private static final Logger LOGGER = Logger.getLogger(YarnMonitor.class.getName());


  //---------------------------------------------------------------------------        
  //--------------------------- STATUS QUERIES --------------------------------
  //---------------------------------------------------------------------------
  public YarnApplicationState getApplicationState(YarnClient yarnClient, ApplicationId appId)
      throws YarnException, IOException {
    return yarnClient.getApplicationReport(appId).getYarnApplicationState();
  }
  
  public LogAggregationStatus getLogAggregationStatus(YarnClient yarnClient, ApplicationId appId)
      throws YarnException, IOException {
    return yarnClient.getApplicationReport(appId).getLogAggregationStatus();
  }
  
  public FinalApplicationStatus getFinalApplicationStatus(YarnClient yarnClient, ApplicationId appId)
      throws YarnException, IOException {
    return yarnClient.getApplicationReport(appId).getFinalApplicationStatus();
  }
  
  public float getProgress(YarnClient yarnClient, ApplicationId appId) throws YarnException, IOException {
    return yarnClient.getApplicationReport(appId).getProgress();
  }
  
  //---------------------------------------------------------------------------        
  //------------------------- YARNCLIENT UTILS --------------------------------
  //---------------------------------------------------------------------------
  
  public void cancelJob(YarnClient yarnClient, ApplicationId appid) throws YarnException, IOException {
    yarnClient.killApplication(appid);
  }
  
  //---------------------------------------------------------------------------
  //------------------------- Yarn log util --------------------------------
  //---------------------------------------------------------------------------
  
  /**
   * Given aggregated yarn log path and destination path copies the desired log
   * type (stdout/stderr)
   *
   * @param dfs
   * @param src
   *   aggregated yarn log path
   * @param dst
   *   destination path to copy to
   * @param desiredLogTypes
   *   stderr or stdout or stdlog
   */
  public void copyAggregatedYarnLogs(ApplicationId applicationId, DistributedFileSystemOps dfs,
                                     YarnClient yarnClient, String src, String dst, String[] desiredLogTypes)
      throws YarnException, IOException, InterruptedException {
    
    LogAggregationStatus logAggregationStatus = waitForLogAggregation(yarnClient, applicationId);
    if (logAggregationStatus == null) {
      // ServiceStatus might be null if there were issues starting the application
      // most likely on the yarn side.
      return;
    }
    
    PrintStream writer = null;
    String[] srcs;
    try {
      srcs = getAggregatedLogFilePaths(src, dfs);
      if (!logFilesReady(srcs, dfs)) {
        LOGGER.log(Level.INFO, "Log is not ready for AppId: {0}. Will retry. ", applicationId);
      }
      writer = new PrintStream(dfs.create(dst));
      switch (logAggregationStatus) {
        case FAILED:
          writer.print("The log aggregation failed");
          break;
        case TIME_OUT:
          writer.print("*** WARNING: Log aggregation has timed-out for some of the containers\n\n\n");
          for (String desiredLogType : desiredLogTypes) {
            writeLogs(applicationId, dfs, srcs, writer, desiredLogType);
          }
          break;
        case SUCCEEDED:
          for (String desiredLogType : desiredLogTypes) {
            writeLogs(applicationId, dfs, srcs, writer, desiredLogType);
          }
          break;
        default:
          writer.print("Something went wrong during log aggregation phase! Log aggregation status is: "
            + logAggregationStatus.name());
      }
    } catch (Exception ex) {
      if (writer != null) {
        writer.print(YarnMonitor.class.getName() + ": Failed to get aggregated logs.\n" + ex.getMessage());
      }
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }
  
  public LogAggregationStatus waitForLogAggregation(YarnClient yarnClient, ApplicationId appId)
      throws InterruptedException, YarnException, IOException {
    LogAggregationStatus logAggregationStatus = getLogAggregationStatus(yarnClient, appId);
    
    int not_startRetries = 0;
    while (!isFinal(logAggregationStatus)) {
      TimeUnit.SECONDS.sleep(2);
      logAggregationStatus = getLogAggregationStatus(yarnClient, appId);
      // NOT_START LogAggregation status might happen in two cases:
      // (a) Application has failed very early and status didn't change to FAILED
      // (b) Application has succeeded but the moment we probe for status,
      // log aggregation hasn't started yet.
      if (logAggregationStatus.equals(LogAggregationStatus.NOT_START)) {
        if (++not_startRetries > 30) {
          break;
        }
      }
    }
    return logAggregationStatus;
  }
  
  private boolean isFinal(LogAggregationStatus status) {
    if (status == null) {
      // ServiceStatus might be null if there were issues starting the application
      // most likely on the yarn side.
      return true;
    }
    
    switch (status) {
      case RUNNING:
      case RUNNING_WITH_FAILURE:
      case NOT_START:
        return false;
      default:
        return true;
    }
  }
  
  private void writeLogs(ApplicationId appId, DistributedFileSystemOps dfs, String[] srcs, PrintStream writer,
      String desiredLogType) {
    ArrayList<AggregatedLogFormat.LogKey> containerNames = new ArrayList<>();
    LogReader reader = null;
    DataInputStream valueStream;
    AggregatedLogFormat.LogKey key = new AggregatedLogFormat.LogKey();
    AggregatedLogFormat.ContainerLogsReader logReader = null;
    Path location;
    try {
      for (String src : srcs) {
        location = new Path(src);
        LOGGER.log(Level.FINE, "Copying log from {0}", src);
        try {
          reader = new LogReader(dfs.getConf(), dfs, location);
          valueStream = reader.next(key);
          while (valueStream != null) {
            containerNames.add(key);
            valueStream = reader.next(key);
          }
          reader.close();
          reader = new LogReader(dfs.getConf(), dfs, location);
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Logs are not available. Aggregation might not be done. AppId: {0}", appId);
          return;
        }
        
        try {
          for (AggregatedLogFormat.LogKey containerKey : containerNames) {
            valueStream = reader.next(key);
            while (valueStream != null && !key.equals(containerKey)) {
              valueStream = reader.next(key);
            }
            if (valueStream != null) {
              logReader = new AggregatedLogFormat.ContainerLogsReader(valueStream);
            }
            if (logReader != null) {
              readContainerLogs(logReader, writer, desiredLogType, containerKey,
                location.getName());
            }
          }
          
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Failed to get log. Aggregation might not be done. AppId: {0}", appId);
        }
        containerNames.clear();
        key = new AggregatedLogFormat.LogKey();
        logReader = null;
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
  
  private boolean logsReady(DistributedFileSystemOps dfs, String src) {
    ArrayList<AggregatedLogFormat.LogKey> containerNames = new ArrayList<>();
    LogReader reader = null;
    DataInputStream valueStream;
    AggregatedLogFormat.LogKey key = new AggregatedLogFormat.LogKey();
    AggregatedLogFormat.ContainerLogsReader logReader = null;
    try {
      try {
        reader = new LogReader(dfs.getConf(), dfs,
          new Path(src));
        valueStream = reader.next(key);
        while (valueStream != null) {
          containerNames.add(key);
          valueStream = reader.next(key);
        }
        reader.close();
        reader = new LogReader(dfs.getConf(), dfs,
          new Path(src));
      } catch (IOException e) {
        return false;
      }
      
      try {
        for (AggregatedLogFormat.LogKey containerKey : containerNames) {
          valueStream = reader.next(key);
          while (valueStream != null && !key.equals(containerKey)) {
            valueStream = reader.next(key);
          }
          if (valueStream != null) {
            logReader = new AggregatedLogFormat.ContainerLogsReader(valueStream);
          }
          if (logReader != null) {
            if (!testLogs(logReader, "out")) {
              return false;
            }
          }
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error testing logs");
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return true;
  }
  
  private boolean testLogs(AggregatedLogFormat.ContainerLogsReader logReader, String desiredLogType)
      throws IOException {
    boolean foundLog = true;
    String logType = logReader.nextLog();
    while (logType != null) {
      foundLog = true;
      if (!logType.contains(desiredLogType)) {
        foundLog = false;
      }
      logType = logReader.nextLog();
    }
    return foundLog;
  }
  
  //Mostly taken from org.apache.hadoop.yarn.webapp.log.AggregatedLogsBlock
  private boolean readContainerLogs(AggregatedLogFormat.ContainerLogsReader logReader, PrintStream writer,
      String desiredLogType, AggregatedLogFormat.LogKey containerKey, String nodename) throws IOException {
    int bufferSize = 65536;
    char[] cbuf = new char[bufferSize];
    boolean foundLog = false;
    String logType = logReader.nextLog();
    while (logType != null) {
      if (desiredLogType == null || desiredLogType.isEmpty()
        || logType.contains(desiredLogType)) {
        long logLength = logReader.getCurrentLogLength();
        if (!foundLog) {
          writer.append("Container: ")
            .append(containerKey.toString())
            .append(" on ")
            .append(nodename)
            .append("\n")
            .append("===============================================")
            .append("=============================================== \n");
        }
        if (logLength == 0) {
          writer.append("Log Type: ")
            .append(logType)
            .append("\n")
            .append("Log Length: " + 0 + "\n");
          logType = logReader.nextLog();
          continue;
        }
        writer.append("Log Type: ")
          .append(logType).append("\n")
          .append("Log Length: ")
          .append(String.valueOf(logLength))
          .append("\n")
          .append("Log Contents: \n");
        int len = 0;
        int currentToRead = logLength > bufferSize ? bufferSize
          : (int) logLength;
        while (logLength > 0 && (len = logReader.read(cbuf, 0, currentToRead))
          > 0) {
          writer.append(new String(cbuf, 0, len));
          logLength = logLength - len;
          currentToRead = logLength > bufferSize ? bufferSize : (int) logLength;
        }
        writer.append("\n");
        foundLog = true;
      }
      logType = logReader.nextLog();
    }
    return foundLog;
  }
  
  /**
   * Given a path to an aggregated log returns the full path to the log file.
   */
  private String[] getAggregatedLogFilePaths(String path, DistributedFileSystemOps dfs) throws IOException {
    Path location = new Path(path);
    String[] paths;
    FileStatus[] fileStatus;
    if (!dfs.exists(path)) {
      paths = new String[1];
      paths[0] = path;
      return paths;
    }
    if (!dfs.isDir(path)) {
      paths = new String[1];
      paths[0] = path;
      return paths;
    }
    fileStatus = dfs.listStatus(location);
    if (fileStatus == null || fileStatus.length == 0) {
      paths = new String[1];
      paths[0] = path;
      return paths;
    }
    paths = new String[fileStatus.length];
    for (int i = 0; i < fileStatus.length; i++) {
      paths[i] = path + File.separator + fileStatus[i].getPath().getName();
    }
    return paths;
  }
  
  private boolean logFilesReady(String[] paths, DistributedFileSystemOps dfs) throws IOException {
    boolean ready = false;
    for (String path : paths) {
      Path location = new Path(path);
      FileStatus fileStatus;
      if (!dfs.exists(path)) {
        return false;
      }
      if (dfs.isDir(path)) {
        return false;
      }
      fileStatus = dfs.getFileStatus(location);
      if (fileStatus == null) {
        return false;
      }
      if (fileStatus.getLen() == 0l) {
        return false;
      }
      if (!logsReady(dfs, path)) {
        return false;
      }
      ready = true;
    }
    return ready;
  }
}
