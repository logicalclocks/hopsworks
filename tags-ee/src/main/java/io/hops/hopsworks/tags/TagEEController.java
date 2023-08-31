/*
 *Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.tags;

import io.hops.hopsworks.common.dao.featurestore.metadata.TagSchemasFacade;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.common.tags.TagControllerIface;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.TagSchemas;
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
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TagEEController implements TagControllerIface {
  @EJB
  private TagSchemasFacade tagSchemasFacade;
  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  
  @Override
  public String get(Users user, DatasetPath path, String name)
    throws DatasetException, MetadataException, FeatureStoreMetadataException {
    String xAttrStr = xAttrsController.getXAttr(user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
    
    if (tags.containsKey(name)) {
      return tags.get(name);
    } else {
      throw new FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_FOUND, Level.FINE);
    }
  }
  
  @Override
  public Map<String, String> getAll(Users user, DatasetPath path)
    throws DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(user, path, FeaturestoreXAttrsConstants.TAGS);
    return FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  }
  
  @Override
  public Map<String, String> getAllAsSuperUser(DatasetPath path)
    throws DatasetException, MetadataException {
    String xAttrStr =  xAttrsController.getXAttrAsSuperUser(path.getFullPath().toString(),
      XAttrsController.XATTR_USER_NAMESPACE, FeaturestoreXAttrsConstants.TAGS);
    return FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  }
  
  @Override
  public AttachMetadataResult<String> upsert(Users user, DatasetPath path, String name, String value)
    throws MetadataException, FeatureStoreMetadataException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(path.getAccessProject(), user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path.getFullPath().toString(), FeaturestoreXAttrsConstants.TAGS,
        udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
      validateTag(name, value);
      boolean created = (tags.put(name, value) == null);
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path.getFullPath().toString(), FeaturestoreXAttrsConstants.TAGS,
        jsonTagsArr.toString(), udfso);
      return new AttachMetadataResult<>(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  @Override
  public AttachMetadataResult<String> upsertAll(Users user, DatasetPath path, Map<String, String> newTags)
    throws MetadataException, FeatureStoreMetadataException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(path.getAccessProject(), user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path.getFullPath().toString(), FeaturestoreXAttrsConstants.TAGS,
        udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
      
      boolean created = false;
      for(Map.Entry<String, String> tag : newTags.entrySet()) {
        validateTag(tag.getKey(), tag.getValue());
        created = created || (tags.put(tag.getKey(), tag.getValue()) == null);
      }
      
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path.getFullPath().toString(), FeaturestoreXAttrsConstants.TAGS,
        jsonTagsArr.toString(), udfso);
      return new AttachMetadataResult<>(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private void validateTag(String name, String value)
    throws FeatureStoreMetadataException {
    
    TagSchemas tagSchema = tagSchemasFacade.findByName(name);
    if (tagSchema == null) {
      throw new FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode.TAG_SCHEMA_NOT_FOUND,
        Level.FINE, name + " schema is not defined.");
    }
    SchematizedTagHelper.validateTag(tagSchema.getSchema(), value);
  }
  
  @Override
  public void delete(Users user, DatasetPath path, String name)
    throws DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
    tags.remove(name);
    
    JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
    xAttrsController.addXAttr(user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }
  
  @Override
  public void deleteAll(Users user, DatasetPath path) throws MetadataException, DatasetException {
    xAttrsController.removeXAttr(user, path, FeaturestoreXAttrsConstants.TAGS);
  }
}
