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
package io.hops.hopsworks.common.hdfs.xattrs;

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
import org.apache.hadoop.ipc.RemoteException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

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

  private final static String XATTR_USER_NAMESPACE = "user.";
  private final static String XATTR_PROV_NAMESPACE = "provenance.";

  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;


  public boolean addXAttr(Project project, Users user, String inodePath,
                          String name, String metaObj)
      throws DatasetException, MetadataException {
    if (name == null || name.isEmpty()) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }

    JSONObject metaJSON = null;
    Object json = new JSONTokener(metaObj).nextValue();
    if (json instanceof JSONObject) {
      metaJSON = (JSONObject) json;
    } else if (json instanceof JSONArray) {
      metaJSON = new JSONObject();
      metaJSON.put(name, json.toString());
    }

    if(!metaJSON.has(name)){
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }

    String metadata = metaJSON.getString(name);
    boolean created = getXAttr(project, user, inodePath, XATTR_USER_NAMESPACE, name) == null;
    addXAttrInt(project, user, inodePath, name, metadata);
    return created;
  }

  public void addStrXAttr(String path, String name, String value, DistributedFileSystemOps udfso)
      throws MetadataException {
    if (name == null || name.isEmpty()) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }

    addXAttrInt(udfso, path, XATTR_USER_NAMESPACE, name, value.getBytes(Charsets.UTF_8));
  }

  public String getXAttr(Project project, Users user, String inodePath, String name)
      throws DatasetException, MetadataException {
    return getXAttr(project, user, inodePath, XATTR_USER_NAMESPACE, name);
  }

  public String getXAttr(String path, String name, DistributedFileSystemOps udfso) throws MetadataException {
    return getXAttr(path, XATTR_USER_NAMESPACE, name, udfso);
  }

  public Map<String, String> getXAttrs(String path, DistributedFileSystemOps udfso) throws MetadataException {
    return getXAttrsInt(path, udfso);
  }

  public boolean removeXAttr(Project project, Users user, String inodePath,
                             String name) throws MetadataException, DatasetException {

    if(name == null || name.isEmpty())
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);

    boolean found = getXAttr(project, user, inodePath, XATTR_USER_NAMESPACE, name) != null;
    if(found){
      removeXAttrInt(project, user, inodePath, name);
    }
    return found;
  }

  private void addXAttrInt(Project project, Users user, String inodePath, String name, String metadataJson)
    throws DatasetException, MetadataException {
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      addXAttrInt(udfso, path, XATTR_USER_NAMESPACE, name, metadataJson.getBytes(Charsets.UTF_8));
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private void removeXAttrInt(Project project, Users user, String inodePath, String name)
    throws MetadataException, DatasetException {
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      removeXAttrInt(udfso, path, XATTR_USER_NAMESPACE, name);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  private String getXAttr(String path, String namespace, String name, DistributedFileSystemOps udfso)
      throws MetadataException{
    byte[] value = getXAttrInt(udfso, path, namespace, name);
    if(value != null) {
      return new String(value, Charsets.UTF_8);
    }
    return null;
  }

  private String getXAttr(Project project, Users user, String inodePath, String namespace, String name)
    throws DatasetException, MetadataException{
    String path = validatePath(inodePath);
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      return getXAttr(path, namespace, name, udfso);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private byte[] getXAttrInt(DistributedFileSystemOps udfso, String path, String namespace, String name)
    throws MetadataException {
    try {
      return udfso.getXAttr(new Path(path), getXAttrName(namespace, name));
    } catch (RemoteException e) {
      if(e.getClassName().equals("io.hops.exception.StorageException")
        && e.getMessage().startsWith("com.mysql.clusterj.ClusterJUserException: Data length")) {
        throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED, Level.FINE, e);
      }
      if(e.getClassName().equals("java.io.IOException")
        && e.getMessage().startsWith("At least one of the attributes provided was not found.")) {
        return null;
      }
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR, Level.SEVERE, path, e.getMessage(), e);
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR, Level.SEVERE, path, e.getMessage(), e);
    }
  }

  private Map<String, String> getXAttrsInt(String path, DistributedFileSystemOps udfso) throws MetadataException {
    try {
      Map<String, String> result = new HashMap<>();
      Map<String, byte[]> attrs = udfso.getXAttrs(new Path(path));
      for (Map.Entry<String, byte[]> e : attrs.entrySet()) {
        if (e.getValue() != null) {
          if (e.getKey().startsWith(XATTR_USER_NAMESPACE)) {
            result.put(e.getKey().split(XATTR_USER_NAMESPACE)[1], new String(e.getValue(), Charsets.UTF_8));
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR, Level.SEVERE, path, e.getMessage(), e);
    }
  }

  private void addXAttrInt(DistributedFileSystemOps udfso, String path, String namespace, String name, byte[] value)
    throws MetadataException {
    try {
      udfso.setXAttr(new Path(path), getXAttrName(namespace, name), value);
    } catch(RemoteException e) {
      if(e.getClassName().equals("org.apache.hadoop.HadoopIllegalArgumentException")
        && e.getMessage().startsWith("The XAttr value is too big.")) {
        throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED, Level.FINE, e);
      } else if(e.getClassName().equals("java.io.IOException")
        && e.getMessage().startsWith("Cannot add additional XAttr to inode, would exceed limit")) {
        throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED, Level.FINE, e);
      } else {
        throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR, Level.SEVERE, path, e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR, Level.SEVERE, path, e.getMessage(), e);
    }
  }
  
  private void removeXAttrInt(DistributedFileSystemOps udfso, String path, String namespace, String name)
    throws MetadataException {
    try {
      udfso.removeXAttr(new Path(path), getXAttrName(namespace, name));
    } catch (IOException e) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_ERROR,
        Level.SEVERE, path, e.getMessage(), e);
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

  private String getXAttrName(String namespace, String name) {
    return namespace + name;
  }
  
  // provenance
  public byte[] getProvXAttr(DistributedFileSystemOps udfso, String inodePath, String name)
    throws DatasetException, MetadataException {
    String path = validatePath(inodePath);
    return getXAttrInt(udfso, path, XATTR_PROV_NAMESPACE, name);
  }
  
  public boolean upsertProvXAttr(Project project, Users user, String inodePath, String name, byte[] value)
    throws MetadataException, DatasetException {
    DistributedFileSystemOps udfso = getDFS(project, user);
    try {
      return upsertProvXAttr(udfso, inodePath, name, value);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public boolean upsertProvXAttr(DistributedFileSystemOps udfso, String inodePath, String name, byte[] value)
    throws MetadataException, DatasetException {
    if (name == null || name.isEmpty()) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MISSING_FIELD, Level.FINE);
    }
    String path = validatePath(inodePath);
    boolean hasPrevious = (getXAttrInt(udfso, path, XATTR_PROV_NAMESPACE, name) != null);
    addXAttrInt(udfso, path, XATTR_PROV_NAMESPACE, name, value);
    return hasPrevious;
  }
  
  public void removeProvXAttr(DistributedFileSystemOps udfso, String inodePath, String name)
    throws DatasetException, MetadataException {
    String path = validatePath(inodePath);
    removeXAttrInt(udfso, path, XATTR_PROV_NAMESPACE, name);
  }
}
