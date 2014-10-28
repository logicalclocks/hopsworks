package se.kth.bbc.fileoperations;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * Provides an interface for interaction with HDFS. Only interacts with HDFS.
 *
 * @author stig
 */
@Stateless
public class FileSystemOperations {

    public static final String nameNodeURI = "hdfs://snurran.sics.se:9999";
    private static final Logger logger = Logger.getLogger(FileSystemOperations.class.getName());
    public static final String DIR_ROOT = "Projects";
    public static final String DIR_SAMPLES = "Samples";
    public static final String DIR_CUNEIFORM = "Cuneiform";
    public static final String DIR_RESULTS = "Results";
    public static final String DIR_BAM = "bam";
    public static final String DIR_SAM = "sam";
    public static final String DIR_FASTQ = "fastq";
    public static final String DIR_FASTA = "fasta";
    public static final String DIR_VCF = "vcf";

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
    public void copyToHDFS(Path location, InputStream is) throws IOException, URISyntaxException {

        // Get the filesystem
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        // Write file
        if (!fs.exists(location)) {
            FSDataOutputStream os = fs.create(location, false);
            IOUtils.copyBytes(is, os, 131072, true); //TODO: check what this 131072 means...
        }
    }

    /**
     * Get an input stream for the file at path <i>location</i>.
     *
     * @param location The location of the file.
     * @return An InputStream for the file.
     */
    public InputStream getInputStream(Path location) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        return fs.open(location, 1048576); //TODO: undo hard coding of weird constant here...
    }

    /**
     * Create a new folder on the given path.
     *
     * @param location The path to the new folder, its name included.
     * @return True if successful.
     */
    public boolean mkdir(Path location) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        return fs.mkdirs(location, null);
    }

    /**
     * Delete the file at given path.
     *
     * @param location: Path to file to be removed
     * @throws IOException
     */
    public boolean rm(Path location) throws IOException {
        return rm(location, false);
    }

    /**
     * Delete the file or folder at the given path recursively: if a folder, all
     * its children will be deleted.
     *
     * @param location
     * @return True if successful, false otherwise.
     * @throws IOException
     */
    public boolean rmRecursive(Path location) throws IOException {
        return rm(location, true);
    }

    private boolean rm(Path location, boolean recursive) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        boolean retVal = false;

        if (fs.exists(location)) {
            retVal = fs.delete(location, recursive);
        } else {
            logger.log(Level.SEVERE, "File does not exist on path: {0}", location.toString());
        }

        logger.log(Level.INFO, "File {0} was deleted", location);
        return retVal;
    }

}
