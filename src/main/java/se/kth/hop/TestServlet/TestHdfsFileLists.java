/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hop.TestServlet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 *
 * @author roshan
 */
public class TestHdfsFileLists {
     
   
    private final static String PROP_NAME = "fs.defaultFS";
    
    public static void main(String[] args) throws Exception{
    
        Configuration conf = new Configuration();
        //System.out.println(conf.get(PROP_NAME));
        
        conf.addResource(new Path("/home/glassfish/roshan/hadoop-2.2.0/etc/hadoop/core-site.xml"));
        conf.addResource(new Path("/home/glassfish/roshan/hadoop-2.2.0/etc/hadoop/hdfs-site.xml"));
//        
       
        //System.out.println("After add resource "+ conf.get(PROP_NAME));
         conf.set(PROP_NAME, "hdfs://localhost:9999/roshan/dataSets");
        
       //System.out.println("Manually set "+ conf.get(PROP_NAME));
        //System.out.println(conf.getRaw(PROP_NAME));
        
        Path path = new Path(conf.get(PROP_NAME));
        
        FileSystem hdfs = FileSystem.get(conf);
      
        FileStatus[] files = hdfs.listStatus(path);
        for(FileStatus file: files){
            System.out.println(file.getPath().getName());
        }
    
    
    }
    
}
