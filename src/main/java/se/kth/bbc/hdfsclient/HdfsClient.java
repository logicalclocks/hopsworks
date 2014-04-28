/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

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

/**
 *
 * @author roshan
 */
public class HdfsClient {
    
    public final String nameNodeURI = "";
    
    private String srcFile;
    private String dstFile;
 
    public String getSourceFilename() {
        return srcFile;
    }
    public void setSourceFilename(String srcFile) {
        this.srcFile = srcFile;
    }
    public String getDestinationFilename() {
        return dstFile;
    }
 
    public void setDestinationFilename(String dstFile) {
        this.dstFile = dstFile;
    }
    
    public void uploadFile() throws IOException, URISyntaxException{
    
        Configuration conf = new Configuration();
        conf.set("fs.defaultfs.name", this.nameNodeURI);
        DFSClient client = new DFSClient(new URI(this.nameNodeURI), conf);
        OutputStream out = null;
        InputStream in = null;
        
        try {
            if (client.exists(dstFile)) {
                System.out.println("File already exists in the storage: " + dstFile);
                return;
            }
            
            out = new BufferedOutputStream(client.create(dstFile, false));
            in = new BufferedInputStream(new FileInputStream(srcFile));
            byte[] buffer = new byte[10240];
 
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch(IOException ioe){
            System.err.println("IOException during operation"+ ioe.toString());
            System.exit(1);
        } finally {
            
            client.close();
        
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        
        
    }
    
    
}
