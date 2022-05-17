/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.hops.hopsworks.common.hdfs.inode.InodeController;
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
  @EJB
  private InodeController inodeController;

  private ObjectMapper objectMapper = new ObjectMapper();

  public List<String> getAll(Project project, Users user, Featuregroup featureGroup, TrainingDataset trainingDataset,
                             FeatureView featureView)
      throws FeaturestoreException, MetadataException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      String path = getPath(featureGroup, trainingDataset, featureView);
      return getAll(path, udfso);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error reading keywords", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  private List<String> getAll(String path, DistributedFileSystemOps udfso)
      throws MetadataException, JsonProcessingException {
    String keywords = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, udfso);
    if (!Strings.isNullOrEmpty(keywords)) {
      return objectMapper.readValue(keywords, List.class);
    } else {
      return new ArrayList<>();
    }
  }

  public List<String> replaceKeywords(Project project, Users user, Featuregroup featureGroup,
                                      TrainingDataset trainingDataset, FeatureView featureView, List<String> keywords)
      throws FeaturestoreException, MetadataException {
    validateKeywords(keywords);
    Set<String> currentKeywords = new HashSet<>(keywords);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      String keywordsStr = objectMapper.writeValueAsString(currentKeywords);
      String path = getPath(featureGroup, trainingDataset, featureView);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, keywordsStr, udfso);
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
                                     TrainingDataset trainingDataset, FeatureView featureView, List<String> keywords)
      throws FeaturestoreException, MetadataException {
    Set<String> currentKeywords;
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      String path = getPath(featureGroup, trainingDataset, featureView);
      currentKeywords = new HashSet<>(getAll(path, udfso));
      currentKeywords.removeAll(keywords);

      String keywordsStr = objectMapper.writeValueAsString(currentKeywords);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.KEYWORDS, keywordsStr, udfso);
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

  public List<String> getUsedKeywords() throws FeaturestoreException {
    try {
      return keywordsUsedCache.getUsedKeywords();
    } catch (ExecutionException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error fetching used keywords");
    }
  }

  private String getPath(Featuregroup featureGroup, TrainingDataset trainingDataset, FeatureView featureView)
      throws FeaturestoreException {
    String path;
    if (featureGroup != null) {
      path = featuregroupController.getFeatureGroupLocation(featureGroup);
    } else if (trainingDataset != null) {
      path = trainingDatasetController.getTrainingDatasetInodePath(trainingDataset);
    } else if (featureView != null) {
      path = getFeatureViewLocation(featureView);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KEYWORD_ERROR, Level.FINE,
          "Error fetching keyword path");
    }
    return path;
  }

  private String getFeatureViewLocation(FeatureView featureView) throws FeaturestoreException {
    return inodeController.getPath(featureView.getInode());
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
