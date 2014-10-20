package se.kth.bbc.upload;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import javax.ejb.Stateless;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * Provides an interface for interaction with HDFS.
 * @author stig
 */
@Stateless
public class FileSystemOperations {
    
    
    public final String nameNodeURI = "hdfs://snurran.sics.se:9999";

    //Copy file to HDFS 
    public void copyToHDFS(Path location, InputStream is) throws IOException, URISyntaxException {

        // Get the filesystem
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        // Write file
        if (!fs.exists(location)) {
            FSDataOutputStream os = fs.create(location, false);
            IOUtils.copyBytes(is, os, 131072, true); //TODO: check what this 131072 means...
        }
    }

}
