package se.kth.bbc.fileoperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.study.fb.Inode;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.upload.UploadServlet;

/**
 * Session bean for file operations. Translates high-level operations in
 * operations on both the file system (HDFS) and the backing DB model (table
 * Inodes).
 *
 * @author stig
 */
@Singleton
public class FileOperations {

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
    public void copyToHDFS(String localFilename, String destination, Inode inode) {
        //Get the local file
        File localfile = getLocalFile(localFilename);

        //Update the status of the Inode
        if (inode != null) {
            inode.setStatus(Inode.COPYING);
            inode.setSize((int)localfile.length());
            inodes.update(inode);
        } else {
            inode = inodes.createAndPersistFile(destination, localfile.length(), Inode.COPYING);
        }

        //Actually copy to HDFS
        boolean success = false;
        Path destp = new Path(destination);
        try {
            fsOps.copyToHDFS(destp, new FileInputStream(localfile));
            success = true;
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(FileOperationsManagedBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Update status
        //TODO: if fails, shoud local file be removed and Inode as well? Or retry? How to handle this?
        if (success) {
            inode.setStatus(Inode.AVAILABLE);
            inodes.update(inode);
        }
    }

    //TODO: add method for starting an upload of a file, which will create an Inode in status "uploading"
    private static File getLocalFile(String localFilename) {
        return new File(getLocalFilePath(localFilename));
    }
    
    private static String getLocalFilePath(String localFilename){
        return UploadServlet.UPLOAD_DIR + File.separator + localFilename;
    }
    
     /**
     * Delete the file at given path.
     *
     * @param location: Path to file to be removed
     * @throws IOException
     */
    public boolean rm(Inode i) throws IOException {
        Path location = new Path(i.getPath());
        boolean success = fsOps.rm(location, false);
        if(success){
           inodes.remove(i);
        }
        return success;
    }

    /**
     * Delete the file or folder at the given path recursively: if a folder, all
     * its children will be deleted.
     *
     * @param location
     * @return True if successful, false otherwise.
     * @throws IOException
     */
    public boolean rmRecursive(Inode i) throws IOException {
        Path location = new Path(i.getPath());
        boolean success = fsOps.rm(location, true);
        if(success){
            inodes.remove(i);
        }
        return success;
    }
    
    public boolean rmRecursive(String path) throws IOException{
        Path location = new Path(path);
        boolean success = fsOps.rm(location, true);
        if(success){
            inodes.removeRecursivePath(path);
        }
        return success;
    }
    
    /**
     * Upload a file to HDFS. First, check if an Inode has been created. If not:
     * create one with status "Uploading". If file is already available locally 
     * (i.e. has been staged), copy to HDFS. Else, return.
     * @param localFilename Name of the local file, assumed to be in the tmp directory.
     * @param destination Destination path in HDFS, includes the filename.
     */
    public synchronized void uploadFile(String localFilename, String destination){
        //Make sure that an entry exists in the DB and get it.
        Inode upload;
        if(!inodes.existsPath(destination)){
            upload = inodes.createAndPersistFile(destination, 0, Inode.UPLOADING);
        }else{
            upload = inodes.getInodeAtPath(destination);
        }
        
        //check if uploading is finished: file exists locally
        if(Files.exists(Paths.get(getLocalFilePath(localFilename))) && upload.getStatus().equals(Inode.UPLOADING)){
            copyToHDFS(localFilename, destination, upload);
        }
    }

}
