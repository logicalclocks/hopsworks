/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.keyword.KeywordControllerIface;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KeywordController implements KeywordControllerIface {

  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private KeywordsUsedCache keywordsUsedCache;

  private ObjectMapper objectMapper = new ObjectMapper();

  public List<String> getAll(Project project, Users user,
                             Featuregroup featureGroup, TrainingDataset trainingDataset)
      throws FeaturestoreException, MetadataException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      return getAll(featureGroup, trainingDataset, udfso);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error reading keywords", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public List<String> getAll(Featuregroup featureGroup, TrainingDataset trainingDataset, DistributedFileSystemOps udfso)
      throws IOException, MetadataException, FeaturestoreException {
    if (featureGroup != null) {
      return getAll(featureGroup, udfso);
    } else {
      return getAll(trainingDataset, udfso);
    }
  }

  public List<String> getAll(Project project, Users user, FeatureView featureView)
      throws IOException, MetadataException, FeaturestoreException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    String path = getFeatureViewLocation(featureView);
    String keywords = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, udfso);
    if (!Strings.isNullOrEmpty(keywords)) {
      return objectMapper.readValue(keywords, List.class);
    } else {
      return new ArrayList<>();
    }
  }

  private String getFeatureViewLocation(FeatureView featureView) {
    // TODO feature view:
    return "";
  }

  private List<String> getAll(Featuregroup featuregroup, DistributedFileSystemOps udfso)
      throws IOException, MetadataException {
    String path = featuregroupController.getFeatureGroupLocation(featuregroup);
    String keywords = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, udfso);
    if (!Strings.isNullOrEmpty(keywords)) {
      return objectMapper.readValue(keywords, List.class);
    } else {
      return new ArrayList<>();
    }
  }

  private List<String> getAll(TrainingDataset trainingDataset, DistributedFileSystemOps udfso)
      throws IOException, MetadataException {
    String path = trainingDatasetController.getTrainingDatasetInodePath(trainingDataset);
    String keywords = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, udfso);
    if (!Strings.isNullOrEmpty(keywords)) {
      return objectMapper.readValue(keywords, List.class);
    } else {
      return new ArrayList<>();
    }
  }

  public List<String> replaceKeywords(Project project, Users user, Featuregroup featureGroup,
                                      TrainingDataset trainingDataset, List<String> keywords)
      throws FeaturestoreException, MetadataException {
    validateKeywords(keywords);
    Set<String> currentKeywords;
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      currentKeywords = new HashSet<>(keywords);

      if (featureGroup != null) {
        addFeatureGroupKeywords(featureGroup, currentKeywords, udfso);
      } else {
        addTrainingDatasetKeywords(trainingDataset, currentKeywords, udfso);
      }
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error adding keywords", e.getMessage(), e);
    } finally {
      // Invalidate used keywords cache
      keywordsUsedCache.invalidateCache();

      dfs.closeDfsClient(udfso);
    }

    return new ArrayList<>(currentKeywords);
  }

  public List<String> deleteKeywords(Project project, Users user, Featuregroup featureGroup,
                                     TrainingDataset trainingDataset, List<String> keywords)
      throws FeaturestoreException, MetadataException {
    Set<String> currentKeywords;
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      currentKeywords = new HashSet<>(getAll(featureGroup, trainingDataset, udfso));
      currentKeywords.removeAll(keywords);

      if (featureGroup != null) {
        addFeatureGroupKeywords(featureGroup, currentKeywords, udfso);
      } else {
        addTrainingDatasetKeywords(trainingDataset, currentKeywords, udfso);
      }
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error deleting keywords", e.getMessage(), e);
    } finally {
      // Invalidate used keywords cache
      keywordsUsedCache.invalidateCache();

      dfs.closeDfsClient(udfso);
    }

    return new ArrayList<>(currentKeywords);
  }

  private void addFeatureGroupKeywords(Featuregroup featureGroup, Set<String> keywords,
                                       DistributedFileSystemOps udfso) throws IOException, MetadataException {
    String keywordsStr = objectMapper.writeValueAsString(keywords);
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, keywordsStr, udfso);
  }

  private void addTrainingDatasetKeywords(TrainingDataset trainingDataset, Set<String> keywords,
                                          DistributedFileSystemOps udfso)
      throws IOException, MetadataException {
    String keywordsStr = objectMapper.writeValueAsString(keywords);
    String path = trainingDatasetController.getTrainingDatasetInodePath(trainingDataset);
    xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, keywordsStr, udfso);
  }

  public List<String> getUsedKeywords() throws FeaturestoreException {
    try {
      return keywordsUsedCache.getUsedKeywords();
    } catch (ExecutionException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error fetching used keywords");
    }
  }

  private void validateKeywords(List<String> keywords) throws FeaturestoreException {
    for (String keyword : keywords) {
      if (!FeaturestoreConstants.KEYWORDS_REGEX.matcher(keyword).matches()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_FORMAT_ERROR, Level.FINE,
            "Keywords can only contain characters, numbers and underscores and cannot be " +
                "longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
      }
    }
  }
}
