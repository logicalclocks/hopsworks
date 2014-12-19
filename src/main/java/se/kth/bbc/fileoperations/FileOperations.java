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
import se.kth.bbc.study.fb.Inode;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.upload.UploadServlet;

/**
 * Session bean for file operations. Translates high-level operations into
 * operations on both the file system (HDFS) and the backing DB model (table
 * Inodes).
 *
 * @author stig
 */
@Stateless
public class FileOperations {

    private static final Logger logger = Logger.getLogger(FileOperationsManagedBean.class.getName());

    @EJB
    private FileSystemOperations fsOps;
    @EJB
    private InodeFacade inodes;

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

    public boolean mkDir(String path) throws IOException {
        Path location = new Path(path);
        boolean success = fsOps.mkdir(location);
        if (success) {
            inodes.createAndPersistDir(path, Inode.AVAILABLE);
        }
        return success;
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
     * @param inode The Inode representing the file. Its status will be updated.
     * If null, a new inode is created for the file.
     */
    public void copyToHDFS(String localFilename, String destination, Inode inode) throws IOException {
        //Get the local file
        File localfile = getLocalFile(localFilename);
        
        String dirs = getDirPart(destination);
        mkDir(dirs);

        //Update the status of the Inode
        if (inode != null) {
            inode.setStatus(Inode.COPYING);
            inode.setSize((int) localfile.length());
            inodes.persist(inode);
        } else {
            inode = inodes.createAndPersistFile(destination, localfile.length(), Inode.COPYING);
        }

        //Actually copy to HDFS
        boolean success = false;
        Path destp = new Path(destination);
        try {
            fsOps.copyToHDFS(destp, new FileInputStream(localfile));
            success = true;
        } catch (IOException|URISyntaxException ex) {
            logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
            throw new IOException(ex);
        }

        //Update status
        //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
        if (success) {
            inode.setStatus(Inode.AVAILABLE);
            inodes.update(inode);
        }
    }
    
    public void copyToHDFSFromPath(String path, String destination, Inode inode) throws IOException {
      //Get the local file
        File localfile = new File(path);
        
        String dirs = getDirPart(destination);
        mkDir(dirs);

        //Update the status of the Inode
        if (inode != null) {
            inode.setStatus(Inode.COPYING);
            inode.setSize((int) localfile.length());
            inodes.persist(inode);
        } else {
            inode = inodes.createAndPersistFile(destination, localfile.length(), Inode.COPYING);
        }

        //Actually copy to HDFS
        boolean success = false;
        Path destp = new Path(destination);
        try {
            fsOps.copyToHDFS(destp, new FileInputStream(localfile));
            success = true;
        } catch (IOException|URISyntaxException ex) {
            logger.log(Level.SEVERE, "Error while copying to HDFS", ex);
            throw new IOException(ex);
        }

        //Update status
        //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
        if (success) {
            inode.setStatus(Inode.AVAILABLE);
            inodes.update(inode);
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
    public void writeToHDFS(InputStream is, long size, String destination) throws IOException{
        Inode inode = inodes.createAndPersistFile(destination, size, Inode.COPYING);
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
            inode.setStatus(Inode.AVAILABLE);
            inodes.update(inode);
        }
    }

    private static File getLocalFile(String localFilename) {
        return new File(getLocalFilePath(localFilename));
    }

    private static String getLocalFilePath(String localFilename) {
        return UploadServlet.UPLOAD_DIR + File.separator + localFilename;
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
            inodes.remove(i);
        }
        return success;
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
            inodes.remove(i);
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
            inodes.removeRecursivePath(path);
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
            inodes.createAndPersistFile(destination, 0, Inode.UPLOADING);
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
    public void copyAfterUploading(String localFilename, String destination) throws IOException{
        Inode node = inodes.getInodeAtPath(destination);
        copyToHDFS(localFilename, destination, node);
    }
    
    private static String getDirPart(String path){
      int lastSlash = path.lastIndexOf("/");
      return path.substring(0,lastSlash);
    }

}
