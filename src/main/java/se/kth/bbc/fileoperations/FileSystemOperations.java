package se.kth.bbc.fileoperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import se.kth.bbc.lims.Constants;

/**
 * Provides an interface for interaction with HDFS. Only interacts with HDFS.
 *
 * @author stig
 */
@Stateless
public class FileSystemOperations {

  //TODO: use fs.copyFromLocalFile
  private static final Logger logger = Logger.getLogger(
          FileSystemOperations.class.getName());
  private FileSystem fs;
  private Configuration conf;

  @PostConstruct
  public void init() {
    try {
      fs = getFs();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Unable to initialize FileSystem", ex);
    }
  }

  /**
   * Copy a file to HDFS. The file will end up at <i>location</i>. The
   * InputStream represents the file.
   *
   * @param location Location to which the file should be copied. Includes the
   * filename.
   * @param is The inputstream representing the file.
   * @throws IOException
   * @throws URISyntaxException
   */
  public void copyToHDFS(Path location, InputStream is) throws IOException,
          URISyntaxException {
    // Write file
    if (!fs.exists(location)) {
      try (FSDataOutputStream os = fs.create(location, false)) {
        IOUtils.copyBytes(is, os, 131072, true); //TODO: check what this 131072 means...
      }
    }
  }

  /**
   * Get an input stream for the file at path <i>location</i>.
   *
   * @param location The location of the file.
   * @return An InputStream for the file.
   */
  public InputStream getInputStream(Path location) throws IOException {
    return fs.open(location, 1048576); //TODO: undo hard coding of weird constant here...
  }

  /**
   * Create a new folder on the given path. Equivalent to mkdir -p.
   *
   * @param location The path to the new folder, its name included.
   * @return True if successful.
   */
  public boolean mkdir(Path location) throws IOException {
    return fs.mkdirs(location, null);
  }

  /**
   * Delete a file or directory form the file system.
   *
   * @param location The location of file or directory to be removed.
   * @param recursive If true, a directory will be removed with all its
   * children.
   * @return True if the operation is successful.
   * @throws IOException
   */
  public boolean rm(Path location, boolean recursive) throws IOException {
    if (fs.exists(location)) {
      return fs.delete(location, recursive);
    } else {
      return true;
    }
  }

  private FileSystem getFs() throws IOException {

    String coreConfDir = System.getenv("HADOOP_CONF_DIR");
    //If still not found: throw exception
    if (coreConfDir == null) {
      logger.log(Level.WARNING, "No configuration path set, using default: "
              + Constants.DEFAULT_HADOOP_CONF_DIR);
      coreConfDir = Constants.DEFAULT_HADOOP_CONF_DIR;
    }

    //Get the configuration file at found path
    File hadoopConfFile = new File(coreConfDir, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }
    File yarnConfFile = new File(coreConfDir, "yarn-site.xml");
    if (!yarnConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }
    File hdfsConfFile = new File(coreConfDir, "hdfs-site.xml");
    if (!hdfsConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hdfsConfFile);
      throw new IllegalStateException("No hdfs conf file: hdfs-site.xml");
    }

    //Set the Configuration object for the hdfs client
    Path yarnPath = new Path(yarnConfFile.getAbsolutePath());
    Path hdfsPath = new Path(hdfsConfFile.getAbsolutePath());
    Path hadoopPath = new Path(hadoopConfFile.getAbsolutePath());
    conf = new Configuration();
    conf.addResource(hadoopPath);
    conf.addResource(yarnPath);
    conf.addResource(hdfsPath);
    FileSystem fs = FileSystem.get(conf);
    return fs;
  }

  public String cat(Path file) throws IOException {
    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.
            open(file)));) {
      String line;
      line = br.readLine();
      while (line != null) {
        out.append(line).append("\n");
        line = br.readLine();
      }
      return out.toString();
    }
  }

  public void copyFromLocal(Path source, Path destination) throws IOException {
    fs.copyFromLocalFile(false, source, destination);
  }

  public void moveWithinHdfs(Path source, Path destination) throws IOException {
    fs.rename(source, destination);
  }

  /**
   * Copy a file within HDFS. Largely taken from Hadoop code.
   * <p>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyInHdfs(Path src, Path dst) throws IOException {
    Path[] srcs = FileUtil.stat2Paths(fs.globStatus(src), src);
    if (srcs.length > 1 && !fs.isDirectory(dst)) {
      throw new IOException("When copying multiple files, "
              + "destination should be a directory.");
    }
    for (Path src1 : srcs) {
      FileUtil.copy(fs, src1, fs, dst, false, conf);
    }
  }

  public void copyToLocal(Path src, Path dst) throws IOException {
    fs.copyToLocalFile(src, dst);
  }

}