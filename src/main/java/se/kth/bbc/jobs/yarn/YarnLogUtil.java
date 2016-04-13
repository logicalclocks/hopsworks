package se.kth.bbc.jobs.yarn;

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
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat.ContainerLogsReader;
import se.kth.bbc.project.fb.Inode;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;

public class YarnLogUtil {

  private static final Logger LOGGER = Logger.getLogger(YarnLogUtil.class.
          getName());

  private enum Result {
    FAILED,
    SUCCESS,
    TIMEOUT
  }

  /**
   * Given aggregated yarn log path and destination path copies the desired log 
   * type (stdout/stderr) 
   * 
   * @param fsService
   * @param dfs
   * @param src aggregated yarn log path
   * @param dst destination path to copy to
   * @param desiredLogType stderr or stdout
   * @throws IOException 
   */
  public static void copyAggregatedYarnLogs(DistributedFsService fsService,
          DistributedFileSystemOps dfs, String src, String dst,
          String desiredLogType) throws
          IOException {
    long wait = dfs.getConf().getLong(
            YarnConfiguration.LOG_AGGREGATION_RETAIN_SECONDS, 86400);
    PrintStream writer = null;
    try {
      Result result = waitForAggregatedLogFileCreation(src, dfs, fsService);
      src = getAggregatedLogFilePath(src, dfs);
      writer = new PrintStream(dfs.create(dst));
      switch (result) {
        case FAILED:
          writer.print("Failed to get the aggregated logs.");
          break;
        case TIMEOUT:
          writer.print("Failed to get the aggregated logs after waitting for "
                  + wait + " seconds.");
          break;
        case SUCCESS:
          writeLogs(dfs, src, writer, desiredLogType);
          break;
      }
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }

  private static void writeLogs(DistributedFileSystemOps dfs, String src,
          PrintStream writer, String desiredLogType) {
    ArrayList<AggregatedLogFormat.LogKey> containerNames = new ArrayList<>();
    AggregatedLogFormat.LogReader reader = null;
    DataInputStream valueStream;
    AggregatedLogFormat.LogKey key = new AggregatedLogFormat.LogKey();
    AggregatedLogFormat.ContainerLogsReader logReader = null;
    try {
      try {
        reader = new AggregatedLogFormat.LogReader(dfs.getConf(), new Path(src));
        valueStream = reader.next(key);
        while (valueStream != null) {
          containerNames.add(key);
          valueStream = reader.next(key);
        }
        reader = new AggregatedLogFormat.LogReader(dfs.getConf(), new Path(src));
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
            readContainerLogs(logReader, writer, desiredLogType, containerKey);
          }
        }

      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error getting logs");
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  //Mostly taken from org.apache.hadoop.yarn.webapp.log.AggregatedLogsBlock
  private static boolean readContainerLogs(
          AggregatedLogFormat.ContainerLogsReader logReader, PrintStream writer,
          String desiredLogType, AggregatedLogFormat.LogKey containerKey) throws
          IOException {
    int bufferSize = 65536;
    char[] cbuf = new char[bufferSize];
    boolean foundLog = false;
    String logType = logReader.nextLog();
    while (logType != null) {
      if (desiredLogType == null || desiredLogType.isEmpty()
              || desiredLogType.equals(logType)) {
        long logLength = logReader.getCurrentLogLength();
        if (logLength == 0) {
          logType = logReader.nextLog();
          continue;
        }
        if (!foundLog) {
          writer.append("Container: " + containerKey.toString() + "\n"
                  + "==============================================="
                  + "============================================== \n");
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
          DistributedFileSystemOps dfs, DistributedFsService fsService) throws
          IOException {

    boolean created = false;
    //If retain seconds not set deffault to 24hours.
    long maxWait = dfs.getConf().getLong(
            YarnConfiguration.LOG_AGGREGATION_RETAIN_SECONDS, 86400);

    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis();
    long retries = 0l;
    long wait;
    long fileSize = 0l;
    path = getAggregatedLogFilePath(path, dfs);
    created = logFileReady(path, dfs, fsService);
    while (!created && (endTime - startTime) / 1000 < maxWait) {
      wait = (long) Math.pow(2, retries);
      try {
        Thread.sleep(wait * 1000);
      } catch (InterruptedException ex) {
      }
      if (dfs.isDir(path)) {
        path = getAggregatedLogFilePath(path, dfs);
      }
      //if file is still the same size increment twice 
      if (fileSize == getFileLen(path, dfs)) {
        retries++;
      }
      retries++;
      fileSize = getFileLen(path, dfs);
      created = logFileReady(path, dfs, fsService);
      endTime = System.currentTimeMillis();
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
  private static String getAggregatedLogFilePath(String path,
          DistributedFileSystemOps dfs) throws IOException {
    Path location = new Path(path);
    FileStatus[] fileStatus;
    if (!dfs.exists(path)) {
      return path;
    }
    if (!dfs.isDir(path)) {
      return path;
    }
    fileStatus = dfs.listStatus(location);
    if (fileStatus == null || fileStatus.length == 0) {
      return path;
    }
    if (fileStatus.length > 1) {
      throw new IOException("Found more than one file under path.");
    }
    return path + File.separator + fileStatus[0].getPath().getName();
  }

  private static long getFileLen(String path, DistributedFileSystemOps dfs) {
    Path location = new Path(path);
    FileStatus[] fileStatus;
    try {
      if (!dfs.exists(path)) {
        return 0l;
      }
      if (dfs.isDir(path)) {
        return 0l;
      }
      fileStatus = dfs.listStatus(location);
      if (fileStatus == null || fileStatus.length == 0) {
        return 0l;
      }
    } catch (IOException ex) {
      return 0l;
    }
    return fileStatus[0].getLen();
  }

  private static boolean logFileReady(String path, DistributedFileSystemOps dfs,
          DistributedFsService fsService) throws
          IOException {
    Path location = new Path(path);
    FileStatus[] fileStatus;
    if (!dfs.exists(path)) {
      return false;
    }
    if (dfs.isDir(path)) {
      return false;
    }
    fileStatus = dfs.listStatus(location);
    if (fileStatus == null || fileStatus.length == 0) {
      return false;
    }
    if (fileStatus.length > 1) {
      throw new IOException("Found more than one file under path.");
    }
    if (fileStatus[0].getLen() == 0l) {
      return false;
    }
    Inode i = fsService.getInode(path);
    if (i == null) {
      throw new IOException("File inode does not exist.");
    }
    if (i.getUnderConstruction() == 1) {
      return false;
    }
    return true;
  }

}
