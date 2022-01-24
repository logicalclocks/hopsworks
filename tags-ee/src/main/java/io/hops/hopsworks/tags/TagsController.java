/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.tags;

import io.hops.hopsworks.common.dao.featurestore.tag.TagSchemasFacade;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagSchemas;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TagsController {
  @EJB
  private TagSchemasFacade tagSchemasFacade;
  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  
  public Map<String, String> getAll(Project accessProject, Users user, String path)
    throws DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(accessProject, user, path, FeaturestoreXAttrsConstants.TAGS);
    return FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  }
  
  public String get(Project accessProject, Users user, String path, String name)
    throws DatasetException, MetadataException, SchematizedTagException {
    String xAttrStr = xAttrsController.getXAttr(accessProject, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  
    if (tags.containsKey(name)) {
      return tags.get(name);
    } else {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_FOUND, Level.FINE);
    }
  }
  
  public AttachTagResult upsert(Project accessProject, Users user, String path, String name, String value)
    throws MetadataException, SchematizedTagException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(accessProject, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.TAGS, udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
      validateTag(name, value);
      boolean created = (tags.put(name, value) == null);
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString(), udfso);
      return new AttachTagResult(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public AttachTagResult upsert(Project accessProject, Users user, String path, Map<String, String> newTags)
    throws MetadataException, SchematizedTagException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(accessProject, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.TAGS, udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
      
      boolean created = false;
      for(Map.Entry<String, String> tag : newTags.entrySet()) {
        validateTag(tag.getKey(), tag.getValue());
        created = created || (tags.put(tag.getKey(), tag.getValue()) == null);
      }
      
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString(), udfso);
      return new AttachTagResult(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private void validateTag(String name, String value)
    throws SchematizedTagException {
    
    TagSchemas tagSchema = tagSchemasFacade.findByName(name);
    if (tagSchema == null) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_SCHEMA_NOT_FOUND,
        Level.FINE, name + " schema is not defined.");
    }
    SchematizedTagHelper.validateTag(tagSchema.getSchema(), value);
  }
  
  public void deleteAll(Project accessProject, Users user, String path) throws MetadataException, DatasetException {
    xAttrsController.removeXAttr(accessProject, user, path, FeaturestoreXAttrsConstants.TAGS);
  }
  
  public void delete(Project accessProject, Users user, String path, String name)
    throws DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(accessProject, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
    tags.remove(name);
    
    JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
    xAttrsController.addXAttr(accessProject, user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }
}
