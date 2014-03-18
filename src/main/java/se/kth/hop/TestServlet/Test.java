/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hop.TestServlet;

//import java.io.*;
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
      String Str1 = "admin";

      //try{
         byte[] Str2 = Str1.getBytes();
         System.out.println("Returned  Value md5: " + DigestUtils.md5Hex(Str2));

         Str2 = Str1.getBytes(StandardCharsets.UTF_8);
         System.out.println("Returned  Value sha512: " + DigestUtils.sha512Hex(Str2));

         Str2 = Str1.getBytes(Charset.defaultCharset());
         System.out.println("Returned  Value " + DigestUtils.sha512Hex(Str2));
         
         Str2 = Str1.getBytes(StandardCharsets.UTF_16BE);
         System.out.println("Returned  Value BE " + DigestUtils.sha512Hex(Str2));
         
         //Str2 = Str1.getBytes(Charset.defaultCharset());
         //String passwordInHex = String.format("%02x", new BigInteger(1, Str1.getBytes(Charset.defaultCharset())));
         System.out.println("Returned  Value DTYPE: " + DatatypeConverter.printHexBinary(Str1.getBytes()));
     // }catch( UnsupportedEncodingException e){
      //   System.out.println("Unsupported character set");
     // }
   }
}   

