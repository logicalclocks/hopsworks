/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfs.http.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author roshan
 */
public class WebHDFSConnectionFactory {
    
    private final Logger logger = LoggerFactory.getLogger(WebHDFSConnectionFactory.class);
    
    public static final String HOST_HDFS = "http://localhost";
    public static final int HOST_HTTP_PORT = 50070;
    public static final String DEFAULT_USERNAME = "roshan";
    
    
    private String host = HOST_HDFS;
    private int port = HOST_HTTP_PORT;
    private String username = DEFAULT_USERNAME;
    
    
    public WebHDFSConnectionFactory(){}
    
    public WebHDFSConnectionFactory(String host, int port, String username){
    
            this.host = host;
            this.port = port;
            this.username = username;
    
    }
    
    public String getHost(){
        return host;
    }
    
    public void setHost(String host){
        this.host = host;
    }
    
    public int getPort(){
        return port;
    }
    
    public void setPort(int port){
        this.port = port;
    }
    
    public String getUsername(){
        return username;
    }
    
    public void setUsername(String username){
        this.username = username;
    }
}

