package se.kth.bbc.fileoperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.lims.StagingManager;
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
  @EJB
  private StagingManager stagingManager;

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
   * Copy a file from the local system to HDFS. The method first updates the
   * inode status to "copying to HDFS" and then copies the file to HDFS.
   * Afterwards updates the status to "available".
   *
   * @param localFilename The name of the local file to be copied. Will be
   * sought for in the temp folder.
   * @param destination The path on HDFS on which the file should be created.
   * Includes the file name.
   */
  public void copyToHDFS(String localFilename, String destination)
          throws IOException {
    //Get the local file
    File localfile = getLocalFile(localFilename);

    String dirs = Utils.getDirectoryPart(destination);
    mkDir(dirs);

    //Actually copy to HDFS
    Path destp = new Path(destination);
    try (FileInputStream fis = new FileInputStream(localfile)) {
      fsOps.copyToHDFS(destp, fis);
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
      throw new IOException(ex);
    }
  }

  public void copyToHDFSFromPath(String path, String destination)
          throws IOException {
    //Get the local file
    File localfile = new File(path);

    String dirs = Utils.getDirectoryPart(destination);
    mkDir(dirs);

    //Actually copy to HDFS
    Path destp = new Path(destination);
    try (FileInputStream fis = new FileInputStream(localfile)) {
      fsOps.copyToHDFS(destp, fis);
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
      throw new IOException(ex);
    }
  }

  /**
   * Write an input stream to a HDFS file. An Inode is also created.
   *
   * @param is The InputStream to be written.
   * @param size The length of the file.
   * @param destination The path on HDFS at which the file should be created.
   * Includes the filename.
   */
  public void writeToHDFS(InputStream is, long size, String destination) throws
          IOException {
    //Actually copy to HDFS
    Path destp = new Path(destination);
    try {
      fsOps.copyToHDFS(destp, is);
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new IOException(ex);
    }
  }

  private File getLocalFile(String localFilename) {
    return new File(getLocalFilePath(localFilename));
  }

  private String getLocalFilePath(String localFilename) {
    return stagingManager.getStagingPath() + File.separator + localFilename;
  }

  /**
   * Delete the file represented by Inode i.
   *
   * @param path The Inode to be removed.
   * @return 
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
   * Copy a file from the local file system to HDFS after its upload. Finds
   * the corresponding Inode for the file and copies the file, updating the
   * Inode.
   *
   * @param localFilename The local name of the uploaded file.
   * @param destination The path in HDFS where the file should end up.
   * Includes the file name.
   */
  public void copyAfterUploading(String localFilename, String destination)
          throws IOException {
    copyToHDFS(localFilename, destination);
  }

  public String cat(String path) throws IOException {
    Path p = new Path(path);
    return fsOps.cat(p);
  }

  /**
   * Copy a file from local filesystem to HDFS. Do not create an Inode for the
   * file.
   * (Used internally for prepping running jobs.)
   * <p>
   * @param localPath
   * @param hdfsPath
   */
  public void copyFromLocalNoInode(String localPath, String hdfsPath) throws
          IOException {
    Path source = new Path(localPath);
    Path destination = new Path(hdfsPath);
    fsOps.copyFromLocal(source, destination);
  }

  public void renameInHdfs(String source, String destination) throws IOException {
    Path src = new Path(source);
    Path dst = new Path(destination);
    fsOps.moveWithinHdfs(src, dst);
  }

  public boolean isDir(String path) {
    Inode i = inodes.getInodeAtPath(path);
    if (i != null) {
      return i.isDir();
    } else {
      return false;
    }
  }

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

  public void copyToLocal(String hdfsPath, String localPath) throws IOException {
    if (!hdfsPath.startsWith("hdfs:")) {
      hdfsPath = "hdfs://" + hdfsPath;
    }
    if (!localPath.startsWith("file:")) {
      localPath = "file://" + localPath;
    }
    fsOps.copyToLocal(new Path(hdfsPath), new Path(localPath));
  }

  public boolean exists(String path) throws IOException {
    return inodes.existsPath(path);
  }
}