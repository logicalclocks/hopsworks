package se.kth.bbc.fileoperations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.fb.Inode;

//TODO: report errors to user!!! seems to be going wrong!
/**
 * Managed bean for accessing operations on the file system. Downloading,
 * uploading, creating files and directories. Methods do not care about the
 * specific implementation of the file system (i.e. separation between DB and FS
 * is not made here).
 *
 * @author stig
 */
@ManagedBean(name = "fileOperationsMB") //Not sure it makes sense to have it as an MB. Perhaps functionality should be split between an MB and EJB
@RequestScoped
public class FileOperationsManagedBean implements Serializable {

  @EJB
  private FileOperations fileOps;

  private String newFolderName;
  private static final Logger logger = Logger.getLogger(
          FileOperationsManagedBean.class.getName());

  /**
   * Download the file at the specified inode.
   *
   * @param inode The file to download.
   * @return StreamedContent of the file to be downloaded.
   */
  public StreamedContent downloadFile(String path) {

    StreamedContent sc = null;
    try {
      //TODO: should convert to try-with-resources? or does that break streamedcontent?
      InputStream is = fileOps.getInputStream(path);
      String extension = Utils.getExtension(path);
      String filename = Utils.getFileName(path);

      sc = new DefaultStreamedContent(is, extension, filename);
      logger.log(Level.FINE, "File was downloaded from HDFS path: {0}", path);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, null, ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Download failed.");
    }
    return sc;
  }

  /**
   * Create a new folder with the name newFolderName (class property) at the
   * specified path. The path must NOT contain the new folder name. Set this
   * using the <i>newFolderName</i> property.
   *
   * @param path Location at which to create the new folder, not including the
   * name of the new folder.
   */
  public void mkDir(String path) {
    String location;
    if (path.endsWith(File.separator)) {
      location = path + newFolderName;
    } else {
      location = path + File.separator + newFolderName;
    }
    try {
      boolean success = fileOps.mkDir(location, -1);
      if (success) {
        newFolderName = null;
      } else {
        MessagesController.addErrorMessage(MessagesController.ERROR,
                "Failed to create folder.");
      }
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, null, ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Failed to create folder.");
    }
  }

  public void setNewFolderName(String s) {
    newFolderName = s;
  }

  public String getNewFolderName() {
    return newFolderName;
  }

  public void deleteFile(Inode inode) {
    try {
      fileOps.rm(inode);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, "Failed to remove file.", ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Remove failed.");
    }
  }

  public void deleteFolderRecursive(String path) {
    try {
      fileOps.rmRecursive(path);
    } catch (IOException ex) {
      Logger.getLogger(FileOperationsManagedBean.class.getName()).log(
              Level.SEVERE, "Failed to remove file.", ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Remove failed.");
    }
  }
}
