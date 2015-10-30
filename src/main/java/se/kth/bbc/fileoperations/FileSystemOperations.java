package se.kth.bbc.fileoperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import io.hops.metadata.hdfs.entity.EncodingPolicy;
import io.hops.metadata.hdfs.entity.EncodingStatus;
import javax.ejb.EJB;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import se.kth.hopsworks.util.Settings;

/**
 * Provides an interface for interaction with HDFS. Only interacts with HDFS.
 *
 * @author stig
 */
@Stateless
public class FileSystemOperations {

   @EJB
  private Settings settings;
  
  //TODO: use fs.copyFromLocalFile
  private static final Logger logger = Logger.getLogger(
          FileSystemOperations.class.getName());
  private DistributedFileSystem dfs;
  private Configuration conf;
  private String CORE_CONF_DIR;

  @PostConstruct
  public void init() {
    try {
      CORE_CONF_DIR = System.getenv("HADOOP_CONF_DIR");
      dfs = getDfs();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Unable to initialize FileSystem", ex);
    }
  }

  @PreDestroy
  public void closeFs() {
    if (dfs != null) {
      try {
        dfs.close();
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Error while closing file system.", ex);
      }
    }
  }

  /**
   * Get an input stream for the file at path <i>location</i>.
   *
   * @param location The location of the file.
   * @return An InputStream for the file.
   * @throws java.io.IOException When an error occurs upon HDFS opening.
   */
  public InputStream getInputStream(Path location) throws IOException {
    return dfs.open(location, 1048576); //TODO: undo hard coding of weird constant here...
  }

  /**
   * Create a new folder on the given path. Equivalent to mkdir -p.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdirs(Path location) throws IOException {

    return dfs.mkdirs(location, FsPermission.getDefault());
  }

  /**
   * Delete a file or directory from the file system.
   *
   * @param location The location of file or directory to be removed.
   * @param recursive If true, a directory will be removed with all its
   * children.
   * @return True if the operation is successful.
   * @throws IOException
   */
  public boolean rm(Path location, boolean recursive) throws IOException {
    if (dfs.exists(location)) {
      return dfs.delete(location, recursive);
    }
    return true;
  }

  /**
   * Get the HDFS file system with the Hadoop config files.
   * <p/>
   * @return
   * @throws IOException
   */
  private DistributedFileSystem getDfs() throws IOException {

    //If still not found: throw exception
    if (CORE_CONF_DIR == null) {
      logger.log(Level.WARNING, "No configuration path set, using default: "
              + settings.getHadoopConfDir());
      CORE_CONF_DIR = settings.getHadoopConfDir();
    }

    //Get the configuration file at found path
    File hadoopConfFile = new File(CORE_CONF_DIR, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }

    File yarnConfFile = new File(CORE_CONF_DIR, "yarn-site.xml");
    if (!yarnConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }

    File hdfsConfFile = new File(CORE_CONF_DIR, "hdfs-site.xml");
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
    //Need a different type of instantiation to get statistics object initialized
    //TODO: here we could use .get(Configuration conf, String user). FileSystem then will have to be instantiated, opened and closed on every method call. Now it's just done on EJB instance creation.
    FileSystem fs = FileSystem.get(conf);
    return (DistributedFileSystem) fs;
  }

  /**
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(Path file) throws IOException {
    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(dfs.
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

  /**
   * Copy a file from one filesystem to the other.
   * <p/>
   * @param deleteSource If true, the file at the source path will be deleted
   * after copying.
   * @param source
   * @param destination
   * @throws IOException
   */
  public void copyFromLocal(boolean deleteSource, Path source, Path destination)
          throws IOException {
    dfs.copyFromLocalFile(deleteSource, source, destination);
  }

  /**
   * Move a file in HDFS from one path to another.
   * <p/>
   * @param source
   * @param destination
   * @throws IOException
   */
  public void moveWithinHdfs(Path source, Path destination) throws IOException {
    dfs.rename(source, destination);
  }

  /**
   * Copy a file within HDFS. Largely taken from Hadoop code.
   * <p/>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyInHdfs(Path src, Path dst) throws IOException {
    Path[] srcs = FileUtil.stat2Paths(dfs.globStatus(src), src);
    if (srcs.length > 1 && !dfs.isDirectory(dst)) {
      throw new IOException("When copying multiple files, "
              + "destination should be a directory.");
    }
    for (Path src1 : srcs) {
      FileUtil.copy(dfs, src1, dfs, dst, false, conf);
    }
  }

  /**
   * Copy the file at the HDFS source path to the local destination.
   * <p/>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyToLocal(Path src, Path dst) throws IOException {
    dfs.copyToLocalFile(src, dst);
  }

  /**
   * Marks a folder/file in location as metaEnabled. This means that all file
   * operations from this path down the directory tree will be registered in
   * hdfs_metadata_log table
   * <p/>
   * @param location
   * @throws IOException
   */
  public void setMetaEnabled(Path location) throws IOException {
    dfs.setMetaEnabled(location, true);
  }

  /**
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param location The path to the new folder, its name included.
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdir(Path location) throws IOException {

    return dfs.mkdir(location, FsPermission.getDefault());
  }

  /**
   * Compress a directory in the given path
   * <p/>
   * @param location
   * @return
   * @throws IOException
   */
  public boolean compress(Path location) throws IOException,
          IllegalStateException {

    //add the erasure coding configuration file
    File erasureCodingConfFile
            = new File(CORE_CONF_DIR, "erasure-coding-site.xml");
    if (!erasureCodingConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              erasureCodingConfFile);
      throw new IllegalStateException(
              "No erasure coding conf file: erasure-coding-site.xml");
    }

    this.conf.addResource(new Path(erasureCodingConfFile.getAbsolutePath()));

    DistributedFileSystem localDfs = this.getDfs();
    localDfs.setConf(this.conf);

    EncodingPolicy policy = new EncodingPolicy("src", (short) 1);

    String path = location.toUri().getPath();
    localDfs.encodeFile(path, policy);

    EncodingStatus encodingStatus;
    while (!(encodingStatus = localDfs.getEncodingStatus(path)).isEncoded()) {
      try {
        Thread.sleep(1000);
        logger.log(Level.INFO, "ongoing file compression of {0} ", path);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Wait for encoding thread was interrupted.");
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates the number of blocks a file holds. The given path has to resolve
   * to a file
   * <p/>
   * @param path
   * @return
   * @throws IOException
   */
  public String getFileBlocks(Path path) throws IOException {

    if (this.dfs.isFile(path)) {
      FileStatus filestatus = this.dfs.getFileStatus(path);

      //get the size of the file in bytes
      long filesize = filestatus.getLen();
      long noOfBlocks = (long) Math.ceil(filesize / filestatus.getBlockSize());

      logger.log(Level.INFO, "File: {0}, No of blocks: {1}", new Object[]{path,
        noOfBlocks});

      return "" + noOfBlocks;
    }

    return "-1";
  }
}
