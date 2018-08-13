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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.LogAggregationStatus;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat.ContainerLogsReader;

public class YarnLogUtil {

  private static final Logger LOGGER = Logger.getLogger(YarnLogUtil.class.
          getName());

  private enum Result {
    FAILED,
    SUCCESS,
    TIMEOUT
  }

  public static void writeLog(DistributedFileSystemOps dfs, String dst,
      String message, Exception exception) {
    PrintStream writer = null;
    try {
      writer = new PrintStream(dfs.create(dst));
      if (exception != null) {
        writer.print(message + "\n" + exception.getMessage());
      } else {
        writer.print(message);
      }
    } catch (IOException ex) {
      if (writer != null) {
        writer.print(YarnLogUtil.class.getName()
            + ": Failed to write logs.\n" + ex.getMessage());
      }
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }
   
  /**
   * Given aggregated yarn log path and destination path copies the desired log
   * type (stdout/stderr)
   *
   * @param dfs
   * @param src aggregated yarn log path
   * @param dst destination path to copy to
   * @param desiredLogTypes stderr or stdout or stdlog
   * @param monitor the monitor to check the log aggregation status
   */
  public static void copyAggregatedYarnLogs(DistributedFileSystemOps dfs, String src, String dst,
      String[] desiredLogTypes, YarnMonitor monitor) throws YarnException, IOException, InterruptedException {
  
    LogAggregationStatus logAggregationStatus = waitForLogAggregation(monitor);
    
    
    PrintStream writer = null;
    String[] srcs;
    try {   
      srcs = getAggregatedLogFilePaths(src, dfs);
      if (!logFilesReady(srcs, dfs)) {
        LOGGER.log(Level.SEVERE, "Error getting logs");
      }
      writer = new PrintStream(dfs.create(dst));
      switch (logAggregationStatus) {
        case FAILED:
          writer.print("The log aggregation failed");
          break;
        case TIME_OUT:
          writer.print("*** WARNING: Log aggregation has timed-out for some of the containers\n\n\n");
          for (String desiredLogType : desiredLogTypes) {
            writeLogs(dfs, srcs, writer, desiredLogType);
          }
          break;
        case SUCCEEDED:
          for (String desiredLogType : desiredLogTypes) {
            writeLogs(dfs, srcs, writer, desiredLogType);
          }
          break;
        default :
          writer.print("Something went wrong during log aggregation phase!");
      }
    } catch (Exception ex) {
      if (writer != null) {
        writer.print(YarnLogUtil.class.getName()
                + ": Failed to get aggregated logs.\n" + ex.getMessage());
      }
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }
  
  public static LogAggregationStatus waitForLogAggregation(YarnMonitor monitor) throws InterruptedException,
      YarnException, IOException {
    LogAggregationStatus logAggregationStatus = monitor.getLogAggregationStatus();

    while (!isFinal(logAggregationStatus)) {
      Thread.sleep(1000);
      logAggregationStatus = monitor.getLogAggregationStatus();
    }
    return logAggregationStatus;
  }

  public static boolean isFinal(LogAggregationStatus status){
    switch(status){
      case RUNNING:
      case RUNNING_WITH_FAILURE:
      case NOT_START:
        return false;
      default :
        return true;
    }
  }
  
  private static void writeLogs(DistributedFileSystemOps dfs, String[] srcs,
          PrintStream writer, String desiredLogType) {
    ArrayList<AggregatedLogFormat.LogKey> containerNames = new ArrayList<>();
    LogReader reader = null;
    DataInputStream valueStream;
    AggregatedLogFormat.LogKey key = new AggregatedLogFormat.LogKey();
    AggregatedLogFormat.ContainerLogsReader logReader = null;
    Path location;
    try {
      for (String src : srcs) {
        location = new Path(src);
        LOGGER.log(Level.INFO, "Copying log from {0}", src);
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
        } catch (FileNotFoundException e) {
          LOGGER.log(Level.SEVERE,
                  "Logs not available. Aggregation may have failed.");
          return;
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Error getting logs");
          return;
        }

        try {
          for (AggregatedLogFormat.LogKey containerKey : containerNames) {
            valueStream = reader.next(key);
            while (valueStream != null && !key.equals(containerKey)) {
              valueStream = reader.next(key);
            }
            if (valueStream != null) {
              logReader = new ContainerLogsReader(valueStream);
            }
            if (logReader != null) {
              readContainerLogs(logReader, writer, desiredLogType, containerKey,
                      location.getName());
            }
          }

        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Error getting logs");
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

  private static boolean logsReady(DistributedFileSystemOps dfs, String src) {
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
      } catch (FileNotFoundException e) {
        return false;
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
            logReader = new ContainerLogsReader(valueStream);
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

  private static boolean testLogs(
          AggregatedLogFormat.ContainerLogsReader logReader,
          String desiredLogType) throws IOException {
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
  private static boolean readContainerLogs(
          AggregatedLogFormat.ContainerLogsReader logReader, PrintStream writer,
          String desiredLogType, AggregatedLogFormat.LogKey containerKey,
          String nodename) throws
          IOException {
    int bufferSize = 65536;
    char[] cbuf = new char[bufferSize];
    boolean foundLog = false;
    String logType = logReader.nextLog();
    while (logType != null) {
      if (desiredLogType == null || desiredLogType.isEmpty()
              || logType.contains(desiredLogType)) {
        long logLength = logReader.getCurrentLogLength();
        if (!foundLog) {
          writer.append("Container: " + containerKey.toString() + " on "
                  + nodename + "\n"
                  + "==============================================="
                  + "=============================================== \n");
        }
        if (logLength == 0) {
          writer.append("Log Type: " + logType + "\n");
          writer.append("Log Length: " + 0 + "\n");
          logType = logReader.nextLog();
          continue;
        }
        writer.append("Log Type: " + logType + "\n");
        writer.append("Log Length: " + Long.toString(logLength) + "\n");
        writer.append("Log Contents: \n");
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

  private static Result waitForAggregatedLogFileCreation(String path,
          DistributedFileSystemOps dfs) throws
          IOException {

    boolean created = false;
    //If retain seconds not set deffault to 24hours.
    long maxWait = dfs.getConf().getLong(
            YarnConfiguration.LOG_AGGREGATION_RETAIN_SECONDS, 86400);
    maxWait = (maxWait > 0 ? maxWait : 86400);
    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis();
    long retries = 0l;
    long wait;
    long fileSize = 0l;
    long pFileSize = 0l;
    String[] paths;
    while (!created && (endTime - startTime) / 1000 < maxWait) {
      paths = getAggregatedLogFilePaths(path, dfs);
      created = logFilesReady(paths, dfs);
      if (created) {
        retries++;//wait any way to be sure there are no more logs 
      }
      for (String path1 : paths) {
        fileSize += getFileLen(path1, dfs);
      }
      wait = (long) Math.pow(2, retries);
      try {
        Thread.sleep(wait * 1000);
      } catch (InterruptedException ex) {
      }
      if (pFileSize == fileSize) {
        retries++;
      }
      retries++;
      pFileSize = fileSize;
      endTime = System.currentTimeMillis();
      paths = getAggregatedLogFilePaths(path, dfs);
      created = logFilesReady(paths, dfs);
    }
    if ((endTime - startTime) / 1000 >= maxWait) {
      return Result.TIMEOUT;
    } else if (created) {
      return Result.SUCCESS;
    } else {
      return Result.FAILED;
    }
  }

  /**
   * Given a path to an aggregated log returns the full path to the log file.
   */
  private static String[] getAggregatedLogFilePaths(String path,
          DistributedFileSystemOps dfs) throws IOException {
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

  private static long getFileLen(String path, DistributedFileSystemOps dfs) {
    Path location = new Path(path);
    FileStatus fileStatus;
    try {
      if (!dfs.exists(path)) {
        return 0l;
      }
      if (dfs.isDir(path)) {
        return 0l;
      }
      fileStatus = dfs.getFileStatus(location);
      if (fileStatus == null) {
        return 0l;
      }
    } catch (IOException ex) {
      return 0l;
    }
    return fileStatus.getLen();
  }

  private static boolean logFilesReady(String[] paths,
          DistributedFileSystemOps dfs) throws
          IOException {
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
