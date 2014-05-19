/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfs.http.test;

import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.junit.Before;
import org.junit.Test;
import se.kth.bbc.hdfs.http.client.WebHDFSHttpClient;

/**
 *
 * @author roshan
 */
public class WebHDFSHttpClientTest {
    
    WebHDFSHttpClient conn;
    
    @Before
    public void setUP(){
        conn = new WebHDFSHttpClient(); 
    }
    
    @Test
    public void getHomeDirectory()throws MalformedURLException, IOException, AuthenticationException{
        String json = conn.getHomeDirectory();
        System.out.println(json);
    }
}
