/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.metadata;

import io.hops.hopsworks.common.commands.featurestore.search.SearchFSCommandLogger;
import io.hops.hopsworks.common.dao.featurestore.metadata.FeatureStoreTagFacade;
import io.hops.hopsworks.common.dao.featurestore.metadata.TagSchemasFacade;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.TagSchemas;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.tags.SchematizedTagHelper;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeatureStoreTagEEController implements FeatureStoreTagControllerIface {
  @EJB
  private TagSchemasFacade tagSchemasFacade;
  @EJB
  private FeatureStoreTagFacade tagFacade;
  @EJB
  private SearchFSCommandLogger searchCommandLogger;
  
  /**
   * Get all tags associated with a featuregroup
   * @param featureGroup
   * @return {@link Map} containing tags and their associated value
   */
  @Override
  public Map<String, FeatureStoreTag> getTags(Featuregroup featureGroup) {
    return tagFacade.findAll(featureGroup);
  }
  
  /**
   * Get all tags associated with a featureView
   * @param featureView
   * @return {@link Map} containing tags and their associated value
   */
  @Override
  public Map<String, FeatureStoreTag> getTags(FeatureView featureView) {
    return tagFacade.findAll(featureView);
  }
  
  /**
   * Get all tags associated with a trainingDataset
   * @param trainingDataset
   * @return {@link Map} containing tags and their associated value
   */
  @Override
  public Map<String, FeatureStoreTag> getTags(TrainingDataset trainingDataset){
    return tagFacade.findAll(trainingDataset);
  }
  
  /**
   * Get single tag by name attached to featuregroup
   * @param featureGroup
   * @param name
   * @return tag if present
   */
  @Override
  public Optional<FeatureStoreTag> getTag(Featuregroup featureGroup, String name) throws FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    return tagFacade.findBySchema(featureGroup, schema);
  }
  
  /**
   * Get single tag by name attached to trainingDataset
   * @param trainingDataset
   * @param name
   * @return tag if present
   */
  @Override
  public Optional<FeatureStoreTag> getTag(TrainingDataset trainingDataset, String name)
    throws FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    return tagFacade.findBySchema(trainingDataset, schema);
  }

  /**
   * Get single tag by name attached to featureView
   * @param featureView
   * @param name
   * @return tag if present
   */
  @Override
  public Optional<FeatureStoreTag> getTag(FeatureView featureView, String name) throws FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    return tagFacade.findBySchema(featureView, schema);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param featureGroup
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTag(Featuregroup featureGroup, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(featureGroup);
    boolean created = upsertTagInt(tags, name, value, updateArtifactInTag(featureGroup));
    searchCommandLogger.updateTags(featureGroup);
    return new AttachMetadataResult<>(tags, created);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param trainingDataset
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTag(TrainingDataset trainingDataset, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(trainingDataset);
    boolean created = upsertTagInt(tags, name, value, updateArtifactInTag(trainingDataset));
    searchCommandLogger.updateTags(trainingDataset);
    return new AttachMetadataResult<>(tags, created);
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param featureView
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTag(FeatureView featureView, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(featureView);
    boolean created = upsertTagInt(tags, name, value, updateArtifactInTag(featureView));
    searchCommandLogger.updateTags(featureView);
    return new AttachMetadataResult<>(tags, created);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param featureGroup
   * @param newTags
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTags(Featuregroup featureGroup, Map<String, String> newTags)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(featureGroup);
    boolean created = upsertTagsInt(tags, newTags, updateArtifactInTag(featureGroup));
    searchCommandLogger.updateTags(featureGroup);
    return new AttachMetadataResult<>(tags, created);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param trainingDataset
   * @param newTags
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTags(TrainingDataset trainingDataset, Map<String, String> newTags)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(trainingDataset);
    boolean created = upsertTagsInt(tags, newTags, updateArtifactInTag(trainingDataset));
    searchCommandLogger.updateTags(trainingDataset);
    return new AttachMetadataResult<>(tags, created);
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param featureView
   * @param newTags
   * @return attached all attached tags and created or updated
   */
  @Override
  public AttachMetadataResult<FeatureStoreTag> upsertTags(FeatureView featureView, Map<String, String> newTags)
    throws FeatureStoreMetadataException, FeaturestoreException {
    Map<String, FeatureStoreTag> tags = tagFacade.findAll(featureView);
    boolean created = upsertTagsInt(tags, newTags, updateArtifactInTag(featureView));
    searchCommandLogger.updateTags(featureView);
    return new AttachMetadataResult<>(tags, created);
  }
  
  /**
   * Delete a single tag attached to a featuregroup
   * @param featureGroup
   * @param name
   */
  public void deleteTag(Featuregroup featureGroup, String name)
    throws FeaturestoreException, FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    tagFacade.deleteBySchema(featureGroup, schema);
    searchCommandLogger.updateTags(featureGroup);
  }
  
  /**
   * Delete a single tag attached to a trainingDataset
   * @param trainingDataset
   * @param name
   */
  public void deleteTag(TrainingDataset trainingDataset, String name)
    throws FeaturestoreException, FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    tagFacade.deleteBySchema(trainingDataset, schema);
    searchCommandLogger.updateTags(trainingDataset);
  }
  
  /**
   * Delete a single tag attached to a featureView
   * @param featureView
   * @param name
   */
  public void deleteTag(FeatureView featureView, String name)
    throws FeaturestoreException, FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    tagFacade.deleteBySchema(featureView, schema);
    searchCommandLogger.updateTags(featureView);
  }
  
  /**
   * Delete all tags attached to a featuregroup
   * @param featureGroup
   */
  @Override
  public void deleteTags(Featuregroup featureGroup) throws FeaturestoreException {
    tagFacade.deleteAll(featureGroup);
    searchCommandLogger.updateTags(featureGroup);
  }
  
  /**
   * Delete all tags attached to a trainingDataset
   * @param trainingDataset
   */
  @Override
  public void deleteTags(TrainingDataset trainingDataset) throws FeaturestoreException {
    tagFacade.deleteAll(trainingDataset);
    searchCommandLogger.updateTags(trainingDataset);
  }

  /**
   * Delete all tags attached to a featureView
   * @param featureView
   */
  @Override
  public void deleteTags(FeatureView featureView) throws FeaturestoreException {
    tagFacade.deleteAll(featureView);
    searchCommandLogger.updateTags(featureView);
  }
  
  private UnaryOperator<FeatureStoreTag> updateArtifactInTag(Featuregroup featuregroup) {
    return tag -> {
      tag.setFeatureGroup(featuregroup);
      return tag;
    };
  }
  
  private UnaryOperator<FeatureStoreTag> updateArtifactInTag(FeatureView featureView) {
    return tag -> {
      tag.setFeatureView(featureView);
      return tag;
    };
  }
  
  private UnaryOperator<FeatureStoreTag> updateArtifactInTag(TrainingDataset trainingDataset) {
    return tag -> {
      tag.setTrainingDataset(trainingDataset);
      return tag;
    };
  }
  
  private TagSchemas getSchemaAndValidate(String name, String value) throws FeatureStoreMetadataException {
    TagSchemas schema = findSchema(name);
    SchematizedTagHelper.validateTag(schema.getSchema(), value);
    if(value.length() > 29000) {
      throw new FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_VALUE,
        Level.FINE, "tag value is too long - max allowed value:29000");
    }
    return schema;
  }
  
  private TagSchemas findSchema(String name) throws FeatureStoreMetadataException {
    TagSchemas schema = tagSchemasFacade.findByName(name);
    if(schema == null) {
      throw FeatureStoreMetadataException.schemaNotDefined(name, Level.FINE);
    }
    return schema;
  }
  
  private boolean upsertTagInt(Map<String, FeatureStoreTag> tags, String name, String value,
                               UnaryOperator<FeatureStoreTag> updateTag)
    throws FeatureStoreMetadataException {
    boolean created = false;
    TagSchemas schema = getSchemaAndValidate(name, value);
    FeatureStoreTag tag = tags.get(name);
    if(tag == null) {
      tag = new FeatureStoreTag(schema, value);
      tag = updateTag.apply(tag);
      tags.put(name, tag);
      created = true;
    } else {
      tag.setValue(value);
    }
    tagFacade.insertOrUpdate(tag);
    return created;
  }
  
  private boolean upsertTagsInt(Map<String, FeatureStoreTag> tags, Map<String, String> newTags,
                                UnaryOperator<FeatureStoreTag> updateTag)
    throws FeatureStoreMetadataException {
    boolean created = false;
    for(Map.Entry<String, String> newTag : newTags.entrySet()) {
      boolean c = upsertTagInt(tags, newTag.getKey(), newTag.getValue(), updateTag);
      created = (created || c);
    }
    return created;
  }
}
