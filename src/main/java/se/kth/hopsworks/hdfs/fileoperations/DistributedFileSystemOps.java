package se.kth.hopsworks.hdfs.fileoperations;

import io.hops.metadata.hdfs.entity.EncodingPolicy;
import io.hops.metadata.hdfs.entity.EncodingStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.util.Settings;

public class DistributedFileSystemOps {

  private static final Logger logger = Logger.getLogger(
          DistributedFileSystemOps.class.getName());
  private final DistributedFileSystem dfs;
  private final String username;
  private Configuration conf;
  private String hadoopConfDir;

  public DistributedFileSystemOps(Settings settings) {
    this.username = null;
    this.dfs = getDfs(null, settings);
  }

  public DistributedFileSystemOps(String username, Settings settings) {
    this.username = username;
    this.dfs = getDfs(username, settings);
  }

  private DistributedFileSystem getDfs(String username, Settings settings) {
    hadoopConfDir = settings.getHadoopConfDir();
    //Get the configuration file at found path
    File hadoopConfFile = new File(hadoopConfDir, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }

    File yarnConfFile = new File(hadoopConfDir, "yarn-site.xml");
    if (!yarnConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }

    File hdfsConfFile = new File(hadoopConfDir, "hdfs-site.xml");
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
    FileSystem fs = null;
    try {
      if (username != null) {
        conf.set("fs.permissions.umask-mode", "000");
        fs = FileSystem.get(FileSystem.getDefaultUri(conf), conf,
                username);
      } else {
        fs = FileSystem.get(conf);
      }

    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Unable to initialize FileSystem", ex);
    }
    return (DistributedFileSystem) fs;
  }

  public DistributedFileSystem getDfs() {
    return dfs;
  }

  public String getUsername() {
    return username;
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
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(String file) throws IOException {
    Path path = new Path(file);
    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(dfs.
            open(path)));) {
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
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdir(Path location, FsPermission filePermission) throws IOException {
    return dfs.mkdir(location, filePermission);
    }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdirs(Path location, FsPermission filePermission) throws IOException {
    return dfs.mkdirs(location, filePermission);
  }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @throws java.io.IOException
   */
  public void touchz(Path location) throws IOException {
    dfs.create(location);
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
   * Copy a file from one file system to the other.
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
   * Copy a file from the local path to the HDFS destination.
   * <p/>
   * @param deleteSource If true, deletes the source file after copying.
   * @param src
   * @param destination
   * @throws IOException
   * @throws IllegalArgumentException If the destination path contains an
   * invalid folder name.
   */
  public void copyToHDFSFromLocal(boolean deleteSource, String src,
          String destination)
          throws IOException {
    //Make sure the directories exist
    Path dirs = new Path(Utils.getDirectoryPart(destination));
    mkdirs(dirs, getParentPermission(dirs));
    //Actually copy to HDFS
    Path destp = new Path(destination);
    Path srcp = new Path(src);
    copyFromLocal(deleteSource, srcp, destp);
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
   * Move the file from the source path to the destination path.
   * <p/>
   * @param source
   * @param destination
   * @throws IOException
   * @thows IllegalArgumentException If the destination path contains an invalid
   * folder name.
   */
  public void renameInHdfs(String source, String destination) throws IOException {
    //Check if source and destination are the same
    if (source.equals(destination)) {
      return;
    }
    //If source does not start with hdfs, prepend.
    if (!source.startsWith("hdfs")) {
      source = "hdfs://" + source;
    }

    //Check destination place, create directory.
    String destDir;
    if (!destination.startsWith("hdfs")) {
      destDir = Utils.getDirectoryPart(destination);
      destination = "hdfs://" + destination;
    } else {
      String tmp = destination.substring("hdfs://".length());
      destDir = Utils.getDirectoryPart(tmp);
    }
    Path dest = new Path(destDir);
    if (!dfs.exists(dest)) {
      dfs.mkdirs(dest);
    }
    Path src = new Path(source);
    Path dst = new Path(destination);
    moveWithinHdfs(src, dst);
  }

  /**
   * Check if the path exists in HDFS.
   * <p/>
   * @param path
   * @return
   * @throws IOException
   */
  public boolean exists(String path) throws IOException {
    Path location = new Path(path);
    return dfs.exists(location);
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
   * Set permission for path.
   * <p>
   * @param path
   * @param permission
   * @throws IOException
   */
  public void setPermission(Path path, FsPermission permission) throws
          IOException {
    dfs.setPermission(path, permission);
  }

  /**
   * Set owner for path.
   * <p>
   * @param path
   * @param username
   * @param groupname
   * @throws IOException
   */
  public void setOwner(Path path, String username, String groupname) throws
          IOException {
    dfs.setOwner(path, username, groupname);
  }

  //Set quota in GB
  public void setQuota(Path src, long diskspaceQuota) throws
          IOException {
    dfs.setQuota(src, HdfsConstants.QUOTA_DONT_SET, 1073741824 * diskspaceQuota);
  }

  //Get quota in GB
  public long getQuota(Path path) throws IOException {
    return dfs.getContentSummary(path).getSpaceQuota() / 1073741824;
  }

  //Get used disk space in GB
  public long getUsedQuota(Path path) throws IOException {
    return dfs.getContentSummary(path).getSpaceConsumed() / 1073741824;
  }

  public FSDataInputStream open(Path location) throws IOException {
    return this.dfs.open(location);
  }

  public FSDataInputStream open(String location) throws IOException {
    Path path = new Path(location);
    return this.dfs.open(path);
  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Compress a file from the given location
   * <p/>
   * @param p
   * @return
   * @throws IOException
   */
  public boolean compress(String p) throws IOException,
          IllegalStateException {
    Path location = new Path(p);
    //add the erasure coding configuration file
    File erasureCodingConfFile
            = new File(hadoopConfDir, "erasure-coding-site.xml");
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
   * Returns the parents permission
   * @param path
   * @return
   * @throws IOException 
   */
  public FsPermission getParentPermission(Path path) throws IOException {
    Path location = new Path(path.toUri());
    while(!dfs.exists(location)){
      location = location.getParent();
    }
    return dfs.getFileStatus(location).getPermission();
  }
  /**
   * Closes the distributed file system.
   */
  public void close() {
    try {
      dfs.close();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error while closing file system.", ex);
    }
  }

}
