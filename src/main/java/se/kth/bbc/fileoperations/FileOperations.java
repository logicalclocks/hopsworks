package se.kth.bbc.fileoperations;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;

/**
 * Session bean for file operations. Translates high-level operations into
 * operations on both the file system (HDFS) and the backing DB model (table
 * Inodes).
 *
 * @author stig
 */
@Stateless
public class FileOperations {

  private static final Logger logger = Logger.getLogger(
          FileOperationsManagedBean.class.getName());

  @EJB
  private FileSystemOperations fsOps;
  @EJB
  private InodeFacade inodes;

  /**
   * Get an InputStream for the file on the given path.
   * <p>
   * @param path The file to read.
   * @return Inputstream from the file in the file system.
   * @throws IOException
   */
  public InputStream getInputStream(String path) throws IOException {
    Path location = new Path(path);
    return fsOps.getInputStream(location);
  }

  /**
   * Create the folders on the given path. Equivalent to mkdir -p.
   * <p>
   * @param path
   * @return
   * @throws IOException
   */
  public boolean mkDir(String path) throws IOException {
    Path location = new Path(path);
    return fsOps.mkdir(location);
  }
  
  /**
   * Copy a file from the local path to the HDFS destination.
   * @param deleteSource If true, deletes the source file after copying.
   * @param src
   * @param destination
   * @throws IOException 
   */
  public void copyToHDFSFromLocal(boolean deleteSource, String src, String destination)
          throws IOException {
    //Make sure the directories exist
    String dirs = Utils.getDirectoryPart(destination);
    mkDir(dirs);
    //Actually copy to HDFS
    Path destp = new Path(destination);
    Path srcp = new Path(src);
    fsOps.copyFromLocal(deleteSource, srcp, destp);
  }

  /**
   * Delete the file at the given path.
   *
   * @param path The path to the file to be removed.
   * @return true if the file has been deleted, false otherwise.
   * @throws IOException
   */
  public boolean rm(String path) throws IOException {
    Path location = new Path(path);
    return fsOps.rm(location, false);
  }

  /**
   * Delete the file or folder at the given path recursively: if a folder, all
   * its children will be deleted.
   *
   * @param path The path to file or folder to be removed recursively.
   * @return True if successful, false otherwise.
   * @throws IOException
   */
  public boolean rmRecursive(String path) throws IOException {
    Path location = new Path(path);
    return fsOps.rm(location, true);
  }

  /**
   * Get the contents of the file at the given path.
   * @param path
   * @return
   * @throws IOException 
   */
  public String cat(String path) throws IOException {
    Path p = new Path(path);
    return fsOps.cat(p);
  }

  /**
   * Move the file from the source path to the destination path.
   * @param source
   * @param destination
   * @throws IOException 
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
    if (!exists(destDir)) {
      mkDir("hdfs://" + destDir);
    }
    Path src = new Path(source);
    Path dst = new Path(destination);
    fsOps.moveWithinHdfs(src, dst);
  }

  /**
   * Check if the inode at the given path is a directory.
   * @param path
   * @return 
   */
  public boolean isDir(String path) {
    Inode i = inodes.getInodeAtPath(path);
    if (i != null) {
      return i.isDir();
    } else {
      return false;
    }
  }

  /**
   * Copy a file from one location (src) in HDFS to another (dst).
   * <p>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyWithinHdfs(String src, String dst) throws IOException {
    //Convert into Paths
    Path srcPath = new Path(src);
    Path dstPath = new Path(dst);
    //Make the necessary output directories
    String dirPart = Utils.getDirectoryPart(dst);
    mkDir(dirPart);
    //Actually copy
    fsOps.copyInHdfs(srcPath, dstPath);
  }

  /**
   * Copy from HDFS to the local file system.
   * @param hdfsPath
   * @param localPath
   * @throws IOException 
   */
  public void copyToLocal(String hdfsPath, String localPath) throws IOException {
    if (!hdfsPath.startsWith("hdfs:")) {
      hdfsPath = "hdfs://" + hdfsPath;
    }
    if (!localPath.startsWith("file:")) {
      localPath = "file://" + localPath;
    }
    fsOps.copyToLocal(new Path(hdfsPath), new Path(localPath));
  }

  /**
   * Check if the path exists in HDFS.
   * @param path
   * @return
   * @throws IOException 
   */
  public boolean exists(String path) throws IOException {
    if(path.startsWith("hdfs:")){
      path = path.substring(5);
      while(path.charAt(1) == '/'){
        path = path.substring(1);
      }
    }
    return inodes.existsPath(path);
  }

  /**
   * Get the absolute HDFS path of the form
   * <i>hdfs:///projects/projectname/relativepath</i>
   * <p>
   * @param projectname
   * @param relativePath
   * @return
   */
  public String getAbsoluteHDFSPath(String projectname, String relativePath) {
    //Strip relativePath from all leading slashes
    while (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    return "hdfs:///" + Constants.DIR_ROOT + "/" + projectname + "/"
            + relativePath;
  }

}
