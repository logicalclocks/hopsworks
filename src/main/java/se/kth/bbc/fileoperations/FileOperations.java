package se.kth.bbc.fileoperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.Stateless;
import org.apache.hadoop.fs.Path;
import static se.kth.bbc.fileoperations.Operation.ADD;
import static se.kth.bbc.fileoperations.Operation.REMOVE;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeOpsFacade;
import se.kth.meta.exception.ApplicationException;

/**
 * Session bean for file operations. Translates high-level operations into
 * operations on both the file system (HDFS) and the backing DB model (table
 * Inodes).
 *
 * @author stig
 */
@Stateless
public class FileOperations {

  private static final Logger logger = Logger.getLogger(FileOperations.class.
          getName());

  @EJB
  private FileSystemOperations fsOps;

  @EJB
  private InodeFacade inodes;

  @EJB
  private InodeOpsFacade inodeOps;

  @EJB
  private StagingManager stagingManager;

  public FileOperations() {
  }

  /**
   * Get an InputStream for the file represented by Inode <i>inode</i>.
   *
   * @param inode The file to read.
   * @return InputStream from the file in the file system.
   */
  public InputStream getInputStream(Inode inode) throws IOException {
    Path location = new Path(inode.getPath());
    return fsOps.getInputStream(location);
  }

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
   *
   * @param path
   * @param templateId
   * @return
   * @throws IOException
   */
  public boolean mkDir(String path, int templateId) throws IOException {
    Path location = new Path(path);
    boolean success = fsOps.mkdir(location);
    String string = "CREATING A DIR " + location.getName();
    logger.log(Level.SEVERE, string);

    if (success) {
      try {
        inodes.createAndPersistDir(path, Inode.AVAILABLE, templateId);
      } catch (EJBTransactionRolledbackException e) {
        fsOps.rm(location, true);
        throw new EJBTransactionRolledbackException();
      }
    }

    return success;
  }

  /**
   * Copy a file from the local system to HDFS. The method first updates the
   * inode status to "copying to HDFS" and then copies the file to HDFS.
   * Afterwards updates the status to "available". Finally stores in the event
   * queue an event representing the file creation
   *
   * @param localFilename The name of the local file to be copied. Will be
   * sought for in the temp folder.
   * @param destination The path on HDFS on which the file should be created.
   * Includes the file name.
   * @param inode The Inode representing the file. Its status will be updated.
   * If null, a new inode is created for the file.
   */
  public void copyToHDFS(String localFilename, String destination, Inode inode)
          throws IOException {
    //Get the local file
    File localfile = getLocalFile(localFilename);

    String dirs = Utils.getDirectoryPart(destination);

    //We don't support associating templates to files yet. This should change
    //to set the actual template id upon file creation
    mkDir(dirs, -1);

    //Update the status of the Inode
    if (inode != null) {
      inode.setStatus(Inode.COPYING);
      inode.setSize((int) localfile.length());
      inodes.persist(inode);
    } else {
      inode = inodes.createAndPersistFile(destination, localfile.length(),
              Inode.COPYING, -1);
    }

    //Actually copy to HDFS
    boolean success = false;
    Path destp = new Path(destination);
    try (FileInputStream fis = new FileInputStream(localfile)) {
      fsOps.copyToHDFS(destp, fis);
      success = true;
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
      throw new IOException(ex);
    }

    //Update status
    //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
    if (success) {
      logger.log(Level.SEVERE, "CREATING A FILE");
      inode.setStatus(Inode.AVAILABLE);
      inodes.update(inode);
      //store the file creation event in the database
      this.inodeOps.createAndStoreOperation(inode, ADD);
    }
  }

  public void copyToHDFSFromPath(String path, String destination, Inode inode)
          throws IOException {
    //Get the local file
    File localfile = new File(path);

    String dirs = Utils.getDirectoryPart(destination);

    //We don't support associating templates to files yet. This should change
    //to set the actual template id upon file creation
    mkDir(dirs, -1);

    //Update the status of the Inode
    if (inode != null) {
      inode.setStatus(Inode.COPYING);
      inode.setSize((int) localfile.length());
      inodes.persist(inode);
    } else {
      inode = inodes.createAndPersistFile(destination, localfile.length(),
              Inode.COPYING, -1);
    }

    //Actually copy to HDFS
    boolean success = false;
    Path destp = new Path(destination);
    try (FileInputStream fis = new FileInputStream(localfile)) {
      fsOps.copyToHDFS(destp, fis);
      success = true;
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
      throw new IOException(ex);
    }

    //Update status
    //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
    if (success) {
      logger.log(Level.SEVERE, "Creating a file from path");
      inode.setStatus(Inode.AVAILABLE);
      inodes.update(inode);
      //store the file creation event in the queue
      this.inodeOps.createAndStoreOperation(inode, ADD);
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
  public void writeToHDFS(InputStream is, long size, String destination)
          throws IOException {

    Inode inode = inodes.createAndPersistFile(destination, size, Inode.COPYING,
            -1);
    //Actually copy to HDFS
    boolean success = false;
    Path destp = new Path(destination);
    try {
      fsOps.copyToHDFS(destp, is);
      success = true;
    } catch (IOException | URISyntaxException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new IOException(ex);
    }

    //Update status
    //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
    if (success) {
      logger.log(Level.SEVERE, "Writing file to hdfs");
      inode.setStatus(Inode.AVAILABLE);
      inodes.update(inode);
      //store the file creation event in the queue
      this.inodeOps.createAndStoreOperation(inode, ADD);
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
   * @param i The Inode to be removed.
   * @throws IOException
   */
  public boolean rm(Inode i) throws IOException {
    Path location = new Path(i.getPath());
    boolean success = fsOps.rm(location, false);
    if (success) {
      logger.log(Level.SEVERE, "REMOVING A DIR");
      inodes.remove(i);
      this.inodeOps.createAndStoreOperation(i, REMOVE);
    }
    return success;
  }

  /**
   * Delete the file in path.
   *
   * @param path
   * @return
   * @throws IOException
   */
  public boolean rm(String path) throws IOException {
    Path location = new Path(path);
    return fsOps.rm(location, false);
  }

  /**
   * Delete the file or folder at the given Inode recursively: if a folder,
   * all its children will be deleted.
   *
   * @param i Inode to be removed recursively.
   * @return True if successful, false otherwise.
   * @throws IOException
   */
  public boolean rmRecursive(Inode i) throws IOException {
    Path location = new Path(i.getPath());
    boolean success = fsOps.rm(location, true);

    if (success) {
      logger.log(Level.SEVERE, "Removing a node recursively");
      inodes.remove(i);
      this.inodeOps.createAndStoreOperation(i, REMOVE);
    }
    return success;
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
    boolean success = fsOps.rm(location, true);
    if (success) {
      logger.log(Level.SEVERE, "Removing a path recursively");
      inodes.removeRecursivePath(path);
    }
    return success;
  }

  /**
   * Deletes a file or folder at the given path recursively. In a folder it
   * visits all the subfolders until it reaches the leaves of the hierarchy.
   * For every leaf (file/folder) deleted, creates and stores an event to the
   * event queue. This way elastic will know which children in the flattened
   * hierarchy to remove
   *
   * @param i The path to file or folder to be removed recursively.
   * @return True if successful, false otherwise.
   *
   * @throws java.io.IOException in case of an error
   */
  public boolean rmR(Inode i) throws IOException {

    if (i.getChildren().size() > 0) {
      //remove recursively all the children
      for (Inode node : i.getChildren()) {
        this.rmR(node);
      }
      //empty the children list
      i.clearChildren();
    }

    //remove self
    Path location = new Path(i.getPath());
    boolean success = fsOps.rm(location, true);

    if (success) {
      inodes.remove(i);
      this.inodeOps.createAndStoreOperation(i, REMOVE);
    }

    return success;
  }

  /**
   * Signify the start of an upload of a file. Check if an Inode has been
   * created for this file and if not, create one with status "Uploading".
   *
   * @param destination The path to which the file should be uploaded.
   */
  public void startUpload(String destination) {
    if (!inodes.existsPath(destination)) {
      inodes.createAndPersistFile(destination, 0, Inode.UPLOADING, -1);
    }
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
    Inode node = inodes.getInodeAtPath(destination);
    copyToHDFS(localFilename, destination, node);
  }

  public String cat(String path) throws IOException {
    Path p = new Path(path);
    return fsOps.cat(p);
  }

  /**
   * Copy a file from local filesystem to HDFS. Do not create an Inode for the
   * file. (Used internally for prepping running jobs.)
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
    fsOps.moveWithinHdsf(src, dst);
  }

  /**
   * Checks if the path exists in HDFS and creates Inodes along it if they are
   * not yet present in the DB.
   * <p>
   * @param path
   * @return True if the path exists (and Inodes were created), false
   * otherwise.
   */
  public boolean createInodesIfNeeded(String path) throws IOException {
    Path p = new Path(path);
    return createInodesIfNeeded(p);
  }

  private boolean createInodesIfNeeded(Path p) throws IOException {
    if (fsOps.exists(p)) {
      if (fsOps.isDir(p)) {
        if (inodes.getInodeAtPath(p.toUri().getPath()) == null) {
          inodes.createAndPersistDir(p.toUri().getPath(), Inode.AVAILABLE, -1);
        }
        for (Path c : fsOps.getChildren(p)) {
          createInodesIfNeeded(c);
        }
      } else if (inodes.getInodeAtPath(p.toUri().getPath()) == null) {
        inodes.createAndPersistFile(p.toUri().getPath(), 0, Inode.AVAILABLE, -1);
      }
      return true;
    }
    return false;
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
    mkDir(dirPart, -1);
    //Actually copy
    fsOps.copyInHdfs(srcPath, dstPath);
    createInodesIfNeeded(dst);
    logger.log(Level.SEVERE, "Copy within hdfs");
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
