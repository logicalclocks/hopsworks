/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kth.hopsworks.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;


/**
 * Provides an interface to interact with Charon
 *
 * @author tiago
 */
public class CharonOperations {


  private static String CHARON_PATH = "/srv/Charon";
  private static String charonMountPointPath = "/srv/Charon/charon_fs";
  private static String addNewGranteePath = CHARON_PATH + File.separator + "NewSiteIds";
  private static String addNewSNSPath = CHARON_PATH + File.separator + "NewSNSs";
  private static String addedGrantees = CHARON_PATH + File.separator + "config/addedGrantees";
  private static String my_site_id = CHARON_PATH + File.separator + "config/site-id.charon";

  /**
   * Sets the charon mount point path. The default is /srv/Charon/charon_fs
   *
   * @param path - the new mount point path
   */
  public static void setCharonMountPointPath(String path){
    charonMountPointPath = path;
  }

  /**
   * Sets the charon install path. The default is /srv/Charon
   *
   * @param path - the new charon install path
   */
  public static void setCharonInstallPath(String path){
    CHARON_PATH = path;
    addNewGranteePath = CHARON_PATH + File.separator + "NewSiteIds";
    addNewSNSPath = CHARON_PATH + File.separator + "NewSNSs";
    addedGrantees = CHARON_PATH + File.separator + "config/addedGrantees";
    my_site_id = CHARON_PATH + File.separator + "config/site-id.charon";
  }

  /**
   * Add a new site id with information about user to be added
   *
   * @param site_id - the content of another user site_id
   * @throws Exception
   */
  public static void addSiteId(String site_id) throws Exception{
    String site_id_filename = "site-id.charon";
    File siteIdFile_temp = new File(CHARON_PATH + File.separator + site_id_filename);
    try {
      if(!siteIdFile_temp.exists())
        siteIdFile_temp.createNewFile();

      FileOutputStream out = new FileOutputStream(siteIdFile_temp);
      out.write(site_id.getBytes());
      out.close();

      siteIdFile_temp.renameTo(new File(addNewGranteePath + File.separator + site_id_filename));
      siteIdFile_temp.delete();
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    }
  }

  /**
   * Remove a site_id
   *
   * @param granteeId - the id of the site_id to be removed
   * @throws Exception
   */
  public static void removeSiteId(int granteeId) throws Exception{
    File grantees = new File(addedGrantees);
    File temp = new File(addedGrantees+"_");
    FileInputStream fis;
    FileOutputStream fos;
    try {
      fis = new FileInputStream(grantees);
      fos = new FileOutputStream(temp);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
      String line = null;
      while ((line = br.readLine()) != null){
        if(line.length() > 0){
          int lineId = Integer.parseInt(line.substring(line.indexOf("(")+1, line.indexOf(")")));
          if(lineId != granteeId){
            bw.write(line);
            bw.newLine();
          }
        }
      }
      temp.renameTo(new File(addedGrantees));
      br.close();
      bw.close();
      fis.close();
      fos.close();
    } catch (FileNotFoundException e) {
      throw new Exception("Something went wrong!");
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    }
  }

  /**
   * List all the site_ids already added
   *
   * @return a String in with each line represents a site_id: id, name, ip and port
   * @throws Exception
   */
  public static String listSiteIds() throws Exception{
    File grantees = new File(addedGrantees);
    FileInputStream fis;
    try {
      fis = new FileInputStream(grantees);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      String line = null;
      String toRet = "";
      while ((line = br.readLine()) != null){
        if(!line.startsWith("#"))
          toRet += line+"\n";
      }
      br.close();
      fis.close();
      return toRet;
    } catch (FileNotFoundException e) {
      throw new Exception("Something went wrong!");
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    }
  }

  /**
   * Get the content of the site_id of the current Charon user
   * @return the content of the site_id
   * @throws Exception
   */
  public static String getMySiteId() throws Exception{
    File site_id = new File(my_site_id);
    FileInputStream fis;
    try {
      fis = new FileInputStream(site_id);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      String line = null;
      String toRet = "";
      while ((line = br.readLine()) != null){
        if(!line.startsWith("#"))
          toRet += line+"\n";
      }
      br.close();
      return toRet;
    } catch (FileNotFoundException e) {
      throw new Exception("Something went wrong!");
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    }
  }

  /**
   * Creates a empty repository and shares it
   *
   * @param granteeId - the id of the site_id to share with
   * @param repPath - the path to the repository to be shared
   * @param permissions - the permissions ('r' or 'rw')
   * @return a share_token to share with the grantees site_ids
   * @throws Exception
   */
  public static String createSharedRepository(int granteeId, String repPath, String permissions) throws Exception{

    try {
      mkdir(repPath, null);
    } catch (Exception e) {
      throw new Exception("Something went wrong!");
    }
    if(!repPath.startsWith("/"))
      repPath = File.separator+repPath;
    File file = new File(charonMountPointPath + repPath);
    if(file.exists())
      if(file.list().length > 0)
        throw new Exception("The repository must be empty to be shared!");

    return share(repPath, permissions, granteeId);
  }

  /**
   * Creates a repository
   *
   * @param path - the new repository path
   * @param location - the location where the files inside that path will be placed (null represents the default location)
   * @throws Exception
   */
  public static void mkdir(String path, String location) throws Exception{

    if(!path.startsWith("/"))
      path = File.separator+path;
    File file = null;
    if(location == null)
      file = new File(charonMountPointPath + path);
    else{
      file = new File(charonMountPointPath+File.separator+".site="+location+path);
    }
    int count = 0;

    while(!file.exists() && count < 3) {
      boolean flag = file.mkdir();
      if(!flag)
        count ++;
    }
    if(count >= 3)
      throw new Exception("Something went wrong!");
  }

  /**
   * Share a repository with other site_ids
   *
   * @param path - the path to the repository to be shared (the repository should be empty)
   * @param permissions - the desired permissions ('r' or 'rw')
   * @param granteeId - the id of the grantee
   * @return - a share_token to share with the grantees site_ids
   */
  public static String share(String path, String permissions, int granteeId) throws Exception{

    if(!path.startsWith("/"))
      path = File.separator+path;

    try {
      String ACLcommand = "setfacl -m u:"+granteeId+":"+permissions+" "+charonMountPointPath+path;
      Process p = Runtime.getRuntime().exec(ACLcommand);
      p.waitFor();

      File file = new File(CHARON_PATH);
      byte[] data = null;
      for(File f : file.listFiles()){
        if(f.getName().startsWith("share")){
          RandomAccessFile rd = new RandomAccessFile(f, "rw");
          data = new byte[(int)f.length()];
          rd.read(data);

          f.delete();
          if(data!=null){
            break;
          }
          rd.close();
        }
      }
      return new String(data);
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    } catch (InterruptedException e) {
      throw new Exception("Something went wrong!");
    }
  }

  /**
   * Add a shared repository shared by a different site_id
   *
   * @param token - the token shared by the owner of the shared repository
   * @throws Exception
   */
  public static void addSharedRepository(String token) throws Exception{

    File tokenFile_temp = new File(CHARON_PATH + File.separator + "token.share");
    try {
      if(!tokenFile_temp.exists())
        tokenFile_temp.createNewFile();

      FileOutputStream out = new FileOutputStream(tokenFile_temp);
      out.write(token.getBytes());
      out.close();
      tokenFile_temp.renameTo(new File(addNewSNSPath + File.separator + "token.share"));
      tokenFile_temp.delete();
    } catch (IOException e) {
      throw new Exception("Something went wrong!");
    }
  }
}