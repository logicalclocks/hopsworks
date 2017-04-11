package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.io.file.tfile.TFile;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat.ContainerLogsReader;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat.LogKey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat;

/**
 * Re-implementation of
 * org.apache.hadoop.yarn.logaggregation.AggregatedLogFormat.LogReader
 * <p>
 */
public class LogReader {

  private static final Log LOG = LogFactory.getLog(LogReader.class);
  private final FSDataInputStream fsDataIStream;
  private final TFile.Reader.Scanner scanner;
  private final TFile.Reader reader;
  private static final Map<String, LogKey> RESERVED_KEYS;
  private static final LogKey APPLICATION_ACL_KEY
          = new LogKey("APPLICATION_ACL");
  private static final LogKey APPLICATION_OWNER_KEY = new LogKey(
          "APPLICATION_OWNER");
  private static final LogKey VERSION_KEY = new LogKey("VERSION");

  static {
    RESERVED_KEYS = new HashMap<String, AggregatedLogFormat.LogKey>();
    RESERVED_KEYS.put(APPLICATION_ACL_KEY.toString(), APPLICATION_ACL_KEY);
    RESERVED_KEYS.put(APPLICATION_OWNER_KEY.toString(), APPLICATION_OWNER_KEY);
    RESERVED_KEYS.put(VERSION_KEY.toString(), VERSION_KEY);
  }

  public LogReader(Configuration conf, DistributedFileSystemOps dfs,
          Path remoteAppLogFile)
          throws IOException {
    this.fsDataIStream = dfs.open(remoteAppLogFile);
    reader = new TFile.Reader(this.fsDataIStream, dfs.getFileStatus(
            remoteAppLogFile).getLen(), conf);
    this.scanner = reader.createScanner();
  }
  private boolean atBeginning = true;

  /**
   * Read the next key and return the value-stream.
   *
   * @param key
   * @return the valueStream if there are more keys or null otherwise.
   * @throws IOException
   */
  public DataInputStream next(LogKey key) throws IOException {
    if (!this.atBeginning) {
      this.scanner.advance();
    } else {
      this.atBeginning = false;
    }
    if (this.scanner.atEnd()) {
      return null;
    }
    TFile.Reader.Scanner.Entry entry = this.scanner.entry();
    key.readFields(entry.getKeyStream());
//     Skip META keys
    if (RESERVED_KEYS.containsKey(key.toString())) {
      return next(key);
    }
    DataInputStream valueStream = entry.getValueStream();
    return valueStream;
  }

  /**
   * Get a ContainerLogsReader to read the logs for
   * the specified container.
   *
   * @param containerId
   * @return object to read the container's logs or null if the
   * logs could not be found
   * @throws IOException
   */
  public ContainerLogsReader getContainerLogsReader(
          ContainerId containerId) throws IOException {
    ContainerLogsReader logReader = null;

    final LogKey containerKey = new LogKey(containerId);
    LogKey key = new LogKey();
    DataInputStream valueStream = next(key);
    while (valueStream != null && !key.equals(containerKey)) {
      valueStream = next(key);
    }

    if (valueStream != null) {
      logReader = new ContainerLogsReader(valueStream);
    }

    return logReader;
  }

  /**
   * Writes all logs for a single container to the provided writer.
   *
   * @param valueStream
   * @param writer
   * @throws IOException
   */
  public static void readAcontainerLogs(DataInputStream valueStream,
          Writer writer) throws IOException {
    int bufferSize = 65536;
    char[] cbuf = new char[bufferSize];
    String fileType;
    String fileLengthStr;
    long fileLength;

    while (true) {
      try {
        fileType = valueStream.readUTF();
      } catch (EOFException e) {
        // EndOfFile
        return;
      }
      fileLengthStr = valueStream.readUTF();
      fileLength = Long.parseLong(fileLengthStr);
      writer.write("\n\nLogType:");
      writer.write(fileType);
      writer.write("\nLogLength:");
      writer.write(fileLengthStr);
      writer.write("\nLog Contents:\n");
      // ByteLevel
      BoundedInputStream bis = new BoundedInputStream(valueStream, fileLength);
      InputStreamReader reader = new InputStreamReader(bis);
      int currentRead = 0;
      int totalRead = 0;
      while ((currentRead = reader.read(cbuf, 0, bufferSize)) != -1) {
        writer.write(cbuf, 0, currentRead);
        totalRead += currentRead;
      }
    }
  }

  /**
   * Keep calling this till you get a {@link EOFException} for getting logs of
   * all types for a single container.
   *
   * @param valueStream
   * @param out
   * @throws IOException
   */
  public static void readAContainerLogsForALogType(
          DataInputStream valueStream, PrintStream out)
          throws IOException {

    byte[] buf = new byte[65535];

    String fileType = valueStream.readUTF();
    String fileLengthStr = valueStream.readUTF();
    long fileLength = Long.parseLong(fileLengthStr);
    out.print("LogType: ");
    out.println(fileType);
    out.print("LogLength: ");
    out.println(fileLengthStr);
    out.println("Log Contents:");

    long curRead = 0;
    long pendingRead = fileLength - curRead;
    int toRead = pendingRead > buf.length ? buf.length : (int) pendingRead;
    int len = valueStream.read(buf, 0, toRead);
    while (len != -1 && curRead < fileLength) {
      out.write(buf, 0, len);
      curRead += len;

      pendingRead = fileLength - curRead;
      toRead = pendingRead > buf.length ? buf.length : (int) pendingRead;
      len = valueStream.read(buf, 0, toRead);
    }
    out.println("");
  }

  public void close() {
    IOUtils.cleanup(LOG, scanner, reader, fsDataIStream);
  }

}
