package se.kth.bbc.upload;

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
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.study.StudyMB;

/**
 * Provides an interface for interaction with HDFS.
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

    //Copy file to HDFS 
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

    //Download file from HDFS
    public StreamedContent downloadFile(Path location) {
        //TODO: fix reporting of complete path!
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs;
        try {
            fs = FileSystem.get(conf);
        } catch (IOException ex) {
            Logger.getLogger(FileSystemOperations.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        StreamedContent file = null;

        try {

            if (!fs.exists(location)) {
                System.out.println("Error: File " + location + " does not exist.");
                logger.log(Level.SEVERE, "File does not exist on this path [{0}] of HDFS", location.toString());
                return null;
            }
            String extension = getExtension(location);
            String filename = location.getName();
            InputStream inStream = fs.open(location, 1048576);
            file = new DefaultStreamedContent(inStream, extension, filename);
            logger.log(Level.INFO, "File was downloaded from HDFS path: {1}", new Object[]{location.toString(), location.toString()});

        } catch (IOException ex) {
            Logger.getLogger(StudyMB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            //inStream.close();
        }
        return file;
    }

    /**
     * Create a new folder on the given path.
     *
     * @param location The path to the new folder, its name included.
     */
    public void mkdir(Path location) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        if (fs.exists(location)) {
            logger.log(Level.SEVERE, "Study directory exists : {0}", location);
            return;
        }
        fs.mkdirs(location, null);
        logger.log(Level.INFO, "Study directory was created on HDFS: {0}.", location);
    }
    
    private static String getExtension(Path file){
        String filename = file.getName();
        if(filename.length()==0) // path was a folder TODO: check if this is right!!
            return null;
        else if(filename.lastIndexOf('.')<0) //file does not have extension
            return "";
        else return filename.substring(filename.lastIndexOf('.')+1);
        
    }
    

}
