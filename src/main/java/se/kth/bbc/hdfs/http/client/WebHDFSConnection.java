    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfs.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;


import org.apache.hadoop.security.authentication.client.AuthenticationException;

/**
 *
 * @author roshan
 */
public interface WebHDFSConnection {
 
    /*
    * ==================================================================================
    *           GET 
    * ==================================================================================
    */

    /**
    * <b>GETHOMEDIRECTORY</b>
    * curl -i "http://<HOST>:<PORT>/webhdfs/v1/?op=GETHOMEDIRECTORY"
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    public String getHomeDirectory() throws MalformedURLException, IOException;
    
    
    /**
    * OPEN and Read a file
    * curl -i -L "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=OPEN
                    [&offset=<LONG>][&length=<LONG>][&buffersize=<INT>]"
    * @param path
    * @param os
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    public String open(String path, OutputStream os) throws MalformedURLException, IOException, AuthenticationException;
    
    /**
    * <b>GETCONTENTSUMMARY</b>
    * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETCONTENTSUMMARY"
    *
    * @path path
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */

    //public String getContentSummary(String path) throws MalformedURLException, IOException;
    
    /**
    * List a directory
    * 
    * curl -i  "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=LISTSTATUS"
    * @param path
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    public String listStatus(String path) throws MalformedURLException, IOException, AuthenticationException;
    
    /**
    * Status of a File/Directory
    * <b>GETFILESTATUS</b>
    * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETFILESTATUS"
    * @param path
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    //public String getFileStatus(String path) throws MalformedURLException, IOException;
    
    /**
    * <b>GETFILECHECKSUM</b>
    * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETFILECHECKSUM"
    * @param path
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    //public String getFileCheckSum(String path) throws MalformedURLException, IOException;
    
    
    /*
     * ======================================================================================================
     *                  PUT
     * ======================================================================================================
     */
    
    /**
     * Create and write to a file
     * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=CREATE
     * [&overwrite=<true|false>][&blocksize=<LONG>][&replication=<SHORT>]
     * [&permission=<OCTAL>][&buffersize=<INT>]"
     * 
     * @param path
     * @param is
     * @return
     * @throws IOException
     * @throws MalformedURLException
     */
    
    public String create(String path, InputStream is) throws MalformedURLException, IOException, AuthenticationException;
    
    /**
    * <b>MKDIRS</b>
    * curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=MKDIRS[&permission=<OCTAL>]"
    *
    * Make a directory
    * @param path
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    public String mkdirs(String path) throws MalformedURLException, IOException, AuthenticationException;
    
    /**
    * <b>CREATESYMLINK</b>
    *
    * curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=CREATESYMLINK&destination=<PATH>[&createParent=<true|false>]"
    * 
    * Create a symbolic link
    * @param srcPath
    * @path destPath
    * @return
    * @throws IOException
    * @throws MalformedURLException
    */
    
    //public String createSymLink(String srcPath, String destPath) throws MalformedURLException, IOException;
 
    /**
	 * <b>RENAME</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=RENAME
	 * &destination=<PATH>[&createParent=<true|false>]"
	 * 
         * Rename a file/Directory
	 * @param path
         * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    
    //public String rename(String srcPath, String destPath) throws MalformedURLException, IOException;
    
        /*
	 * ==========================================================================================================
	 *                POST
	 * ==========================================================================================================
	 */
	/**
	 * curl -i -X POST
	 * "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=APPEND[&buffersize=<INT>]"
	 * 
         * Append content to a file
	 * @param path
	 * @param is
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    //public String append(String path, InputStream is) throws MalformedURLException, IOException;

    
    /* ========================================================================
     *                   DELETE
     * ========================================================================
     */
	/**
	 * <b>DELETE</b>
	 * 
	 * curl -i -X DELETE "http://<host>:<port>/webhdfs/v1/<path>?op=DELETE
	 * [&recursive=<true|false>]"
	 * 
         * Delete a file/directory
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
   public String delete(String path, boolean recursive) throws MalformedURLException, IOException, AuthenticationException;

    
   
}