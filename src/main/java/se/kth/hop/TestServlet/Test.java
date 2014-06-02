/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hop.TestServlet;

//import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import org.apache.commons.codec.digest.DigestUtils;
import java.nio.charset.*;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author roshan
 */
public class Test {
 
 public static void main(String args[]){
      //String Str1 = "admin";

//      String username1 = "roshan@yahoo.com".split("@")[0];
//      String username2 = "roshan@yahoo.com".split("@")[0].trim();
//      String username3 = "roshan@yahoo.com".substring(0, "roshan@yahoo.com".indexOf('@'));
//      String buildpath1 = File.separator+username1+File.separator+"dataset";
//      String buildpath2 = File.separator+username2;
//      String buildpath3 = File.separator+username3;
//      
//      System.out.println(buildpath1);
//      System.out.println(buildpath2);
//      System.out.println(buildpath3);
//      //try{
//         byte[] Str2 = Str1.getBytes();
//         System.out.println("Returned  Value md5: " + DigestUtils.md5Hex(Str2));
//
//         Str2 = Str1.getBytes(StandardCharsets.UTF_8);
//         System.out.println("Returned  Value sha512: " + DigestUtils.sha512Hex(Str2));
//
//         Str2 = Str1.getBytes(Charset.defaultCharset());
//         System.out.println("Returned  Value " + DigestUtils.sha512Hex(Str2));
//         
//         Str2 = Str1.getBytes(StandardCharsets.UTF_16BE);
//         System.out.println("Returned  Value BE " + DigestUtils.sha512Hex(Str2));
//         
//         
//         System.out.println(System.getProperty("user.dir"));
         
         //Str2 = Str1.getBytes(Charset.defaultCharset());
         //String passwordInHex = String.format("%02x", new BigInteger(1, Str1.getBytes(Charset.defaultCharset())));
 //        System.out.println("Returned  Value DTYPE: " + DatatypeConverter.printHexBinary(Str1.getBytes()));
     // }catch( UnsupportedEncodingException e){
      //   System.out.println("Unsupported character set");
     // }
   
 
 //=========================================================================================
     
     
     
        try{
            
            File homedir = new File(System.getProperty("user.home"));
            System.out.println(homedir);
            File fileToCreate = new File(homedir, "/disk/samples");
            
            //URL uri = new URL("http://snurran.sics.se/hop");
            OutputStream os = new FileOutputStream(fileToCreate);
            
            byte[] buffer = new byte[65536]; 
            int readBytes = 0;
            InputStream is = null;            
            
            while((readBytes=is.read(buffer)) > 0){
                os.write(buffer,0,readBytes);
                os.flush();
            }
                os.flush();
                os.close();
                is.close();
                
            
            //addMessage("File Transfer time "+ time+ " ms");
        } catch(FileNotFoundException fnf){
            System.out.println("File not found! "+ fnf.toString());    
        } catch(IOException ioe){
            System.out.println("I/O Exception "+ ioe.toString());    
        }
 
 
 
 
 
 }
}   

