/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfs.http.client;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;


import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL.Token;
import org.apache.hadoop.security.authentication.client.AuthenticationException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author roshan
 */

public class WebHDFSHttpClient {
    
    private final Logger logger = LoggerFactory.getLogger(WebHDFSHttpClient.class.getName());
    
//    public static final String USER_NAME = "user.name";
//    private static final String USER_NAME_EQ = USER_NAME + "=";
    
    private final String httfsUrl = "http://snurran.sics.se:50070";
    private String principal = WebHDFSConnectionFactory.DEFAULT_USERNAME;
    
    private Token token;
    private AuthenticatedURL aUrl;
   
            
    public WebHDFSHttpClient(){
        token = new AuthenticatedURL.Token();
        aUrl = new AuthenticatedURL();
    }
    
    
    protected static long copy(InputStream input, OutputStream result) throws IOException {
	byte[] buffer = new byte[65536]; // 64K=65536
	long count = 0L;
	int n = 0;
	while ((n = input.read(buffer)) != -1) {
		result.write(buffer, 0, n);
		count += n;
		result.flush();
	}
                result.flush();
		
        return count;
    }

    
    private String result(HttpURLConnection conn, boolean input) throws IOException{
        StringBuffer sb = new StringBuffer();
        if(input){
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
            String line = null;
            
            while((line = br.readLine()) != null){
                sb.append(line);
            }
                br.close();
                is.close();
        }
        
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", conn.getResponseCode());
        result.put("mesg", conn.getResponseMessage());
        result.put("type", conn.getContentType());
        result.put("data", sb);

        Gson gson = new Gson();
        String json = gson.toJson(result);
        logger.info("json = " + json);

        return json;
    }
    
   public String getHomeDirectory()throws MalformedURLException, IOException, AuthenticationException{
        
        String spec = MessageFormat.format("/webhdfs/v1/?op=GETHOMEDIRECTORY&user.name={0}", principal);
        HttpURLConnection conn = aUrl.openConnection(new URL(new URL(httfsUrl),spec), token);
        conn.connect();
        String resp = result(conn, true);
        conn.disconnect();
        return resp;
    }
    
   public String open(String path, OutputStream os) throws MalformedURLException, IOException, AuthenticationException{
       
       String spec = MessageFormat.format("/webhdfs/v1/{0}?op=OPEN", path);
       HttpURLConnection conn = aUrl.openConnection(new URL(new URL(httfsUrl),spec), token);
       conn.setRequestMethod("GET");
       conn.setRequestProperty("Content-Type", "application/octet-stream");
       conn.connect();
       InputStream is = conn.getInputStream();
       copy(is, os);
       is.close();
       os.close();
       String resp = result(conn, false);
       conn.disconnect();
       
       return resp;
   
   }
   
   public String getContentSummary(String path) throws MalformedURLException, IOException, AuthenticationException {
		
      String spec = MessageFormat.format("/webhdfs/v1/{0}?op=GETCONTENTSUMMARY", path);
      HttpURLConnection conn = aUrl.openConnection(new URL(new URL(httfsUrl),spec), token);
      conn.setRequestMethod("GET");
      conn.connect();
      String resp = result(conn, true);
      conn.disconnect();

      return resp;
   }
   
   public String listStatus(String path) throws MalformedURLException, IOException, AuthenticationException {
	
        String spec = MessageFormat.format("/webhdfs/v1/{0}?op=LISTSTATUS", path);
        HttpURLConnection conn = aUrl.openConnection(new URL(new URL(httfsUrl),spec), token);
        conn.setRequestMethod("GET");
	conn.connect();
	String resp = result(conn, true);
	conn.disconnect();

	return resp;
	}
   
   public String mkdirs(String path) throws MalformedURLException, IOException, AuthenticationException {
		
        String spec = MessageFormat.format("/webhdfs/v1/{0}?op=MKDIRS&user.name={1}", path, principal);
        HttpURLConnection conn = aUrl.openConnection(new URL(new URL(httfsUrl),spec), token);   
        conn.setRequestMethod("PUT");
	conn.connect();
	String resp = result(conn, true);
	conn.disconnect();

	return resp;
	}
      
}   
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
