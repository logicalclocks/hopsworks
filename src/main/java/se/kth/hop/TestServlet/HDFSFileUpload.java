/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hop.TestServlet;

/**
 *
 * @author roshan
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSClient;

public class HDFSFileUpload {
    
    private final String hdfsUrl = "hdfs://localhost:9000";
 
    private String sourceFilename;
    private String destinationFilename;
 
    public String getSourceFilename() {
        return sourceFilename;
    }
    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }
    public String getDestinationFilename() {
        return destinationFilename;
    }
 
    public void setDestinationFilename(String destinationFilename) {
        this.destinationFilename = destinationFilename;
    }
 
    public void uploadFile() throws IOException, URISyntaxException {
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.hdfsUrl);
        DFSClient client = new DFSClient(new URI(this.hdfsUrl), conf);
        OutputStream out = null;
        InputStream in = null;
        try {
            if (client.exists(destinationFilename)) {
                System.out.println("File already exists in hdfs: " + destinationFilename);
                return;
            }
            out = new BufferedOutputStream(client.create(destinationFilename, false));
            in = new BufferedInputStream(new FileInputStream(sourceFilename));
            byte[] buffer = new byte[1024];
 
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } finally {
            if (client != null) {
                client.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
    
    
    
