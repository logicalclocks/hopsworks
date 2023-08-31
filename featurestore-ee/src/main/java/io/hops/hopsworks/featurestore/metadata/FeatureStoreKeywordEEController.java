/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.metadata;

import com.google.common.collect.Sets;
import io.hops.hopsworks.common.commands.featurestore.search.SearchFSCommandLogger;
import io.hops.hopsworks.common.dao.featurestore.metadata.FeatureStoreKeywordFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreKeyword;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeatureStoreKeywordEEController implements FeatureStoreKeywordControllerIface {
  @EJB
  private FeatureStoreKeywordFacade keywordFacade;
  @EJB
  private SearchFSCommandLogger searchCommandLogger;
  
  /**
   * @return {@link List} containing all cluster keywords
   */
  @Override
  public List<String> getAllKeywords() {
    return keywordFacade.getAll();
  }
  /**
   * Get all keywords associated with a featuregroup
   * @param featureGroup
   * @return {@link List} containing keywords
   */
  @Override
  public List<String> getKeywords(Featuregroup featureGroup) {
    return keywordFacade.findAllAsNames(featureGroup);
  }
  
  /**
   * Get all keywords associated with a featureView
   * @param featureView
   * @return {@link List} containing attached keywords
   */
  @Override
  public List<String> getKeywords(FeatureView featureView) {
    return keywordFacade.findAllAsNames(featureView);
  }
  
  /**
   * Get all keywords associated with a trainingDataset
   * @param trainingDataset
   * @return {@link List} containing attached keywords
   */
  @Override
  public List<String> getKeywords(TrainingDataset trainingDataset){
    return keywordFacade.findAllAsNames(trainingDataset);
  }
  
  /**
   * Attaches a new keyword if it does not already exists
   * @param featureGroup
   * @param keyword
   * @return all attached keywords
   */
  @Override
  public AttachMetadataResult<FeatureStoreKeyword> insertKeyword(Featuregroup featureGroup, String keyword)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> keywords = keywordFacade.findAll(featureGroup);
    boolean created = insertKeywordInt(keywords, keyword, updateArtifactInKeyword(featureGroup));
    searchCommandLogger.updateKeywords(featureGroup);
    return new AttachMetadataResult<>(keywords, created);
  }
  
  /**
   * Attaches a new keyword if it does not already exists
   * @param trainingDataset
   * @param keyword
   * @return all attached keywords
   */
  @Override
  public AttachMetadataResult<FeatureStoreKeyword> insertKeyword(TrainingDataset trainingDataset, String keyword)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> keywords = keywordFacade.findAll(trainingDataset);
    boolean created = insertKeywordInt(keywords, keyword, updateArtifactInKeyword(trainingDataset));
    searchCommandLogger.updateKeywords(trainingDataset);
    return new AttachMetadataResult<>(keywords, created);
  }
  
  /**
   * Attaches a new keyword if it does not already exists
   * @param featureView
   * @param keyword
   * @return all attached keywords
   */
  @Override
  public AttachMetadataResult<FeatureStoreKeyword> insertKeyword(FeatureView featureView, String keyword)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> keywords = keywordFacade.findAll(featureView);
    boolean created = insertKeywordInt(keywords, keyword, updateArtifactInKeyword(featureView));
    searchCommandLogger.updateKeywords(featureView);
    return new AttachMetadataResult<>(keywords, created);
  }
  
  /**
   * Attaches new keywords if they do not already exists
   * @param featureGroup
   * @param keywords
   * @return all attached keywords
   */
  @Override
  public List<String> insertKeywords(Featuregroup featureGroup, Set<String> keywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(featureGroup);
    insertKeywordsInt(existingKeywords, keywords, updateArtifactInKeyword(featureGroup));
    searchCommandLogger.updateKeywords(featureGroup);
    return new LinkedList<>(existingKeywords.keySet());
  }
  
  /**
   * Attaches new keywords if they do not already exists
   * @param trainingDataset
   * @param keywords
   * @return all attached keywords
   */
  @Override
  public List<String> insertKeywords(TrainingDataset trainingDataset, Set<String> keywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(trainingDataset);
    insertKeywordsInt(existingKeywords, keywords, updateArtifactInKeyword(trainingDataset));
    searchCommandLogger.updateKeywords(trainingDataset);
    return new LinkedList<>(existingKeywords.keySet());
  }
  
  /**
   * Attaches new keywords if they do not already exists
   * @param featureView
   * @param keywords
   * @return all attached keywords
   */
  @Override
  public List<String> insertKeywords(FeatureView featureView, Set<String> keywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(featureView);
    insertKeywordsInt(existingKeywords, keywords, updateArtifactInKeyword(featureView));
    searchCommandLogger.updateKeywords(featureView);
    return new LinkedList<>(existingKeywords.keySet());
  }
  
  /**
   * @param featureGroup
   * @param newKeywords
   * @return all attached keywords
   */
  public List<String> replaceKeywords(Featuregroup featureGroup, Set<String> newKeywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(featureGroup);
    replaceKeywordsInt(existingKeywords, newKeywords,
      updateArtifactInKeyword(featureGroup),
      keywords -> keywordFacade.removeByName(featureGroup, keywords));
    searchCommandLogger.updateKeywords(featureGroup);
    return new ArrayList<>(existingKeywords.keySet());
  }
  
  /**
   * @param featureView
   * @param newKeywords
   * @return all attached keywords
   */
  public List<String> replaceKeywords(FeatureView featureView, Set<String> newKeywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(featureView);
    replaceKeywordsInt(existingKeywords, newKeywords,
      updateArtifactInKeyword(featureView),
      keywords -> keywordFacade.removeByName(featureView, keywords));
    searchCommandLogger.updateKeywords(featureView);
    return new ArrayList<>(existingKeywords.keySet());
  }
  
  /**
   * @param trainingDataset
   * @param newKeywords
   * @return all attached keywords
   */
  public List<String> replaceKeywords(TrainingDataset trainingDataset, Set<String> newKeywords)
    throws FeaturestoreException {
    Map<String, FeatureStoreKeyword> existingKeywords = keywordFacade.findAll(trainingDataset);
    replaceKeywordsInt(existingKeywords, newKeywords,
      updateArtifactInKeyword(trainingDataset),
      keywords -> keywordFacade.removeByName(trainingDataset, keywords));
    searchCommandLogger.updateKeywords(trainingDataset);
    return new ArrayList<>(existingKeywords.keySet());
  }
  
  /**
   * Delete a single keyword attached to a featuregroup
   * @param featureGroup
   * @param name
   */
  public void deleteKeyword(Featuregroup featureGroup, String name) throws FeaturestoreException {
    keywordFacade.removeByName(featureGroup, name);
    searchCommandLogger.updateKeywords(featureGroup);
  }
  
  /**
   * Delete a single keyword attached to a trainingDataset
   * @param trainingDataset
   * @param name
   */
  public void deleteKeyword(TrainingDataset trainingDataset, String name) throws FeaturestoreException {
    keywordFacade.removeByName(trainingDataset, name);
    searchCommandLogger.updateKeywords(trainingDataset);
  }
  
  /**
   * Delete a single keyword attached to a featureView
   * @param featureView
   * @param name
   */
  public void deleteKeyword(FeatureView featureView, String name) throws FeaturestoreException {
    keywordFacade.removeByName(featureView, name);
    searchCommandLogger.updateKeywords(featureView);
  }
  
  /**
   * Delete all keywords attached to a featuregroup
   * @param featureGroup
   */
  @Override
  public void deleteKeywords(Featuregroup featureGroup) throws FeaturestoreException {
    keywordFacade.deleteAll(featureGroup);
    searchCommandLogger.updateKeywords(featureGroup);
  }
  
  /**
   * Delete all keywords attached to a trainingDataset
   * @param trainingDataset
   */
  @Override
  public void deleteKeywords(TrainingDataset trainingDataset) throws FeaturestoreException {
    keywordFacade.deleteAll(trainingDataset);
    searchCommandLogger.updateKeywords(trainingDataset);
  }
  
  /**
   * Delete all keywords attached to a featureView
   * @param featureView
   */
  @Override
  public void deleteKeywords(FeatureView featureView) throws FeaturestoreException {
    keywordFacade.deleteAll(featureView);
    searchCommandLogger.updateKeywords(featureView);
  }
  
  /**
   * Delete from keywords attached to a featureView
   * @param featureGroup
   * @return all attached keywords
   */
  @Override
  public List<String> deleteKeywords(Featuregroup featureGroup, Set<String> keywords) throws FeaturestoreException {
    if(!keywords.isEmpty()) {
      keywordFacade.removeByName(featureGroup, keywords);
      searchCommandLogger.updateKeywords(featureGroup);
    }
    return getKeywords(featureGroup);
  }
  
  /**
   * Delete from keywords attached to a featureView
   * @param featureView
   * @return all attached keywords
   */
  @Override
  public List<String> deleteKeywords(FeatureView featureView, Set<String> keywords) throws FeaturestoreException {
    if(!keywords.isEmpty()) {
      keywordFacade.removeByName(featureView, keywords);
      searchCommandLogger.updateKeywords(featureView);
    }
    return getKeywords(featureView);
  }
  
  /**
   * Delete from keywords attached to a featureView
   * @param trainingDataset
   * @return all attached keywords
   */
  @Override
  public List<String> deleteKeywords(TrainingDataset trainingDataset, Set<String> keywords)
    throws FeaturestoreException {
    if(!keywords.isEmpty()) {
      keywordFacade.removeByName(trainingDataset, keywords);
      searchCommandLogger.updateKeywords(trainingDataset);
    }
    return getKeywords(trainingDataset);
  }
  
  private void validateKeyword(String keyword) throws FeaturestoreException {
    if (!FeaturestoreConstants.KEYWORDS_REGEX.matcher(keyword).matches()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_FORMAT_ERROR, Level.FINE,
        "Keywords can only contain characters, numbers and underscores and cannot be " +
          "longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
    }
  }
  
  private boolean insertKeywordInt(Map<String, FeatureStoreKeyword> keywords, String name,
                                  UnaryOperator<FeatureStoreKeyword> updateKeyword)
    throws FeaturestoreException {
    validateKeyword(name);
    boolean created = false;
    FeatureStoreKeyword keyword = keywords.get(name);
    if(keyword == null) {
      keyword = new FeatureStoreKeyword(name);
      keyword = updateKeyword.apply(keyword);
      keywords.put(name, keyword);
      created = true;
    }
    keywordFacade.insertIfNotPresent(keyword);
    return created;
  }
  
  private boolean insertKeywordsInt(Map<String, FeatureStoreKeyword> keywords, Set<String> newKeywords,
                                    UnaryOperator<FeatureStoreKeyword> updateKeyword)
    throws FeaturestoreException {
    boolean created = false;
    for(String newKeyword : newKeywords) {
      boolean c = insertKeywordInt(keywords, newKeyword, updateKeyword);
      created = created || c;
    }
    return created;
  }
  
  private void replaceKeywordsInt(Map<String, FeatureStoreKeyword> existingKeywords, Set<String> newKeywords,
                                  UnaryOperator<FeatureStoreKeyword> updateKeyword,
                                  Consumer<Set<String>> removeKeywords)
    throws FeaturestoreException {
    //remove extra ones
    Set<String> toDelete = Sets.difference(existingKeywords.keySet(), newKeywords);
    if(!toDelete.isEmpty()) {
      removeKeywords.accept(toDelete);
      toDelete.forEach(existingKeywords::remove);
    }
    //add new ones
    Set<String> toAdd = Sets.difference(newKeywords, existingKeywords.keySet());
    if(!toAdd.isEmpty()) {
      insertKeywordsInt(existingKeywords, toAdd, updateKeyword);
    }
  }
  
  private UnaryOperator<FeatureStoreKeyword> updateArtifactInKeyword(Featuregroup featuregroup) {
    return keyword -> {
      keyword.setFeatureGroup(featuregroup);
      return keyword;
    };
  }
  
  private UnaryOperator<FeatureStoreKeyword> updateArtifactInKeyword(FeatureView featureView) {
    return keyword -> {
      keyword.setFeatureView(featureView);
      return keyword;
    };
  }
  
  private UnaryOperator<FeatureStoreKeyword> updateArtifactInKeyword(TrainingDataset trainingDataset) {
    return keyword -> {
      keyword.setTrainingDataset(trainingDataset);
      return keyword;
    };
  }
}
