/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.metadata;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.Charsets;
import org.apache.hadoop.fs.Path;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Stateless(name = "XAttrsController")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class XAttrsController {
  
  private static String XATTR_USER_NAMESPACE = "user.";
  
  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  
  
  public boolean addXAttr(Project project, Users user, String inodePath,
      String name, String metaObj)
      throws DatasetException, MetadataException {
    if(name == null || name.isEmpty()) {
      throw new MetadataException(
          RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }
    JSONObject metaJSON = new JSONObject(metaObj);
    if(!metaJSON.has(name)){
      throw new MetadataException(
          RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }
    String metadata = metaJSON.getString(name);
    if (metadata.length() > 13500 || name.length() > 255) {
      throw new MetadataException(
          RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED, Level.FINE);
    }
  
    boolean created = getXAttr(project, user, inodePath, name) == null;
    
    addXAttrInt(project, user, inodePath, name, metadata);
    
    return created;
  }
  
  public Map<String, String> getXAttrs(Project project, Users user, String inodePath,
      String name) throws DatasetException, MetadataException{
    Map<String, String> result = new HashMap<>();;
    if(name == null || name.isEmpty()){
      result = getXAttrs(project, user, inodePath);
    }else{
      String metadata = getXAttr(project, user, inodePath,
          name);
      if(metadata != null){
        result.put(name, metadata);
      }
    }
    return result;
  }
  
  public boolean removeXAttr(Project project, Users user, String inodePath,
      String name) throws MetadataException, DatasetException {
    
    if(name == null || name.isEmpty())
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    
    boolean found = getXAttr(project, user, inodePath,
        name) != null;
    if(found){
      removeXAttrInt(project, user, inodePath, name);
    }
    return found;
  }
  
  private void addXAttrInt(Project project, Users user, String inodePath,
      String name, String metadataJson)
      throws DatasetException, MetadataException {
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      udfso.setXAttr(new Path(path), getXAttrName(name),
              metadataJson.getBytes(Charsets.UTF_8));
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
          Level.SEVERE, path, e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private void removeXAttrInt(Project project, Users user, String inodePath,
      String name) throws MetadataException, DatasetException {
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      udfso.removeXAttr(new Path(path), getXAttrName(name));
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
          Level.SEVERE, path, e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private String getXAttr(Project project, Users user, String inodePath,
      String name) throws DatasetException, MetadataException{
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      byte[] value = udfso.getXAttr(new Path(path), getXAttrName(name));
      if(value != null) {
        return new String(value, Charsets.UTF_8);
      }
      return null;
    } catch (IOException e) {
      if(e.getMessage().contains("At least one of the attributes provided was" +
          " not found.")){
        return null;
      }
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
          Level.SEVERE, path, e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private Map<String, String> getXAttrs(Project project, Users user,
      String inodePath)
      throws DatasetException, MetadataException {
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      Map<String, String> result = new HashMap<>();
      Map<String, byte[]> attrs = udfso.getXAttrs(new Path(path));
      for(Map.Entry<String, byte[]> e : attrs.entrySet()){
        if(e.getValue() != null) {
          if(e.getKey().startsWith(XATTR_USER_NAMESPACE)){
            result.put(e.getKey().split(XATTR_USER_NAMESPACE)[1], new String(e.getValue(), Charsets.UTF_8));
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
          Level.SEVERE, path, e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private String validatePath(String path)
      throws MetadataException, DatasetException {
    String inodePath = path;
    
    try {
      inodePath = Utils.prepPath(path);
    } catch (UnsupportedEncodingException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
          Level.SEVERE, path, e.getMessage(), e);
    }
  
    if (!inodeController.existsPath(inodePath)) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND,
          Level.FINE,
          "file " + inodePath + "doesn't exist");
    }
    return inodePath;
  }
  
  private DistributedFileSystemOps getDFS(Project project, Users user){
    String hdfsUserName = hdfsUsersController.getHdfsUserName(project, user);
    return dfs.getDfsOps(hdfsUserName);
  }
  
  private String getXAttrName(String name) {
    return XATTR_USER_NAMESPACE + name;
  }
  
}
