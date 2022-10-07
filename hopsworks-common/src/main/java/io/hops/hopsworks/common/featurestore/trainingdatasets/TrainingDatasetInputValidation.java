/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import com.google.common.base.Joiner;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogicDTO;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.split.TrainingDatasetSplitDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.restutils.RESTCodes;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName.TEST;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName.TRAIN;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName.VALIDATION;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitType.RANDOM_SPLIT;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitType.TIME_SERIES_SPLIT;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetInputValidation {

  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;
  @EJB
  private StatisticColumnController statisticColumnController;
  @EJB
  private FeaturestoreConnectorFacade connectorFacade;
  @EJB
  private QueryController queryController;
  /**
   * Verify entity names input by the user for creation of entities in the featurestore
   *
   * @param trainingDatasetDTO the user input data for the entity
   * @throws FeaturestoreException
   */
  public void verifyUserInput(TrainingDatasetDTO trainingDatasetDTO)
    throws FeaturestoreException {
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);

    // features
    if (trainingDatasetDTO.getQueryDTO() == null && trainingDatasetDTO.getFeatures() != null) {
      // during updates the features are null
      verifyTrainingDatasetFeatureList(trainingDatasetDTO.getFeatures());
    }
  }

  /**
   * Verifies the user input feature list for a training dataset entity with no query
   * @param trainingDatasetFeatureDTOS the feature list to verify
   */
  public void verifyTrainingDatasetFeatureList(List<TrainingDatasetFeatureDTO> trainingDatasetFeatureDTOS)
    throws FeaturestoreException {
    for (TrainingDatasetFeatureDTO trainingDatasetFeatureDTO : trainingDatasetFeatureDTOS) {
      featurestoreInputValidation.nameValidation(trainingDatasetFeatureDTO.getName());
    }
  }

  public void validate(TrainingDatasetDTO trainingDatasetDTO, Query query) throws FeaturestoreException {
    // Verify general entity related information
    verifyUserInput(trainingDatasetDTO);
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, query);
    validateType(trainingDatasetDTO.getTrainingDatasetType());
    validateVersion(trainingDatasetDTO.getVersion());
    validateDataFormat(trainingDatasetDTO.getDataFormat());
    String eventTimeFieldName = query != null ? query.getFeaturegroup().getEventTime() : null;
    validateSplits(trainingDatasetDTO.getSplits(), eventTimeFieldName);
    validateFeatures(query, trainingDatasetDTO.getFeatures());
    validateStorageConnector(trainingDatasetDTO.getStorageConnector());
    validateTrainSplit(trainingDatasetDTO.getTrainSplit(), trainingDatasetDTO.getSplits());
    validateExtraFilter(trainingDatasetDTO, query);
  }

  private void validateExtraFilter(TrainingDatasetDTO trainingDatasetDTO, Query query) throws FeaturestoreException {
    FilterLogicDTO filterLogicDTO = trainingDatasetDTO.getExtraFilter();
    if (filterLogicDTO != null) {
      validateExtraFilter(trainingDatasetDTO.getExtraFilter(),
          queryController.getFeatureGroups(query).stream().map(Featuregroup::getId).collect(Collectors.toSet()));
    }
  }

  private void validateExtraFilter(FilterLogicDTO filterLogicDTO, Set<Integer> allFgs) throws FeaturestoreException {
    if (
        (
            filterLogicDTO.getLeftFilter() != null
                && !allFgs.contains(filterLogicDTO.getLeftFilter().getFeature().getFeatureGroupId())
        ) ||
            (
                filterLogicDTO.getRightFilter() != null
                    && !allFgs.contains(filterLogicDTO.getRightFilter().getFeature().getFeatureGroupId())
            )
    ) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.FINE,
          String.format(
              "Feature '%s' is from feature group with id '%d' which is not available in the feature view's `Query`."
              + " All available feature group id in this query are [%s].",
              filterLogicDTO.getLeftFilter().getFeature().getName(),
              filterLogicDTO.getLeftFilter().getFeature().getFeatureGroupId(),
              Joiner.on(", ").join(allFgs)
          )
      );
    }
    if (filterLogicDTO.getLeftLogic() != null) {
      validateExtraFilter(filterLogicDTO.getLeftLogic(), allFgs);
    }
    if (filterLogicDTO.getRightLogic() != null) {
      validateExtraFilter(filterLogicDTO.getRightLogic(), allFgs);
    }
  }

  private void validateType(TrainingDatasetType trainingDatasetType) throws FeaturestoreException {
    if (trainingDatasetType != TrainingDatasetType.HOPSFS_TRAINING_DATASET &&
        trainingDatasetType != TrainingDatasetType.EXTERNAL_TRAINING_DATASET &&
        trainingDatasetType != TrainingDatasetType.IN_MEMORY_TRAINING_DATASET) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized Training Dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
              TrainingDatasetType.EXTERNAL_TRAINING_DATASET  + ", and: " +
              TrainingDatasetType.IN_MEMORY_TRAINING_DATASET +
              ". The provided training dataset type was not recognized: "
              + trainingDatasetType);
    }
  }

  private void validateVersion(Integer version) throws FeaturestoreException {
    if (version == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_VERSION_NOT_PROVIDED.getMessage());
    }
    if(version <= 0) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_VERSION, Level.FINE,
          " version cannot be negative or zero");
    }
  }

  private void validateDataFormat(String dataFormat) throws FeaturestoreException {
    if (!FeaturestoreConstants.TRAINING_DATASET_DATA_FORMATS.contains(dataFormat)) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_DATA_FORMAT, Level.FINE, ", the recognized " +
          "training dataset formats are: " +
          StringUtils.join(FeaturestoreConstants.TRAINING_DATASET_DATA_FORMATS) + ". The provided data " +
          "format:" + dataFormat + " was not recognized.");
    }
  }

  void validateSplits(List<TrainingDatasetSplitDTO> trainingDatasetSplitDTOs, String eventTimeFieldName)
      throws FeaturestoreException {
    if (trainingDatasetSplitDTOs != null && !trainingDatasetSplitDTOs.isEmpty()) {
      Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
      Set<String> splitNames = new HashSet<>();
      Boolean isTimeSplit = false;
      Date trainStart = null;
      Date trainEnd = null;
      Date validationStart = null;
      Date validationEnd = null;
      Date testStart = null;
      Date testEnd = null;
      for (TrainingDatasetSplitDTO trainingDatasetSplitDTO : trainingDatasetSplitDTOs) {
        if (!namePattern.matcher(trainingDatasetSplitDTO.getName()).matches()) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_NAME,
              Level.FINE, "The provided training dataset split name " + trainingDatasetSplitDTO.getName() + " is " +
              "invalid. Split names can only contain lower case characters, numbers and underscores and cannot be " +
              "longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
        }
        if (RANDOM_SPLIT.equals(trainingDatasetSplitDTO.getSplitType())
            && trainingDatasetSplitDTO.getPercentage() == null) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_PERCENTAGE,
              Level.FINE, "The provided training dataset split percentage is invalid. " +
              "Percentages can only be numeric. Weights will be normalized if they don’t sum up to 1.0.");
        }
        if (RANDOM_SPLIT.equals(trainingDatasetSplitDTO.getSplitType())
            && trainingDatasetSplitDTO.getPercentage() <= 0) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_PERCENTAGE,
              Level.FINE, "The provided training dataset split percentage is invalid. " +
              "Weights must be greater than 0.");
        }
        if (!splitNames.add(trainingDatasetSplitDTO.getName())) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_DUPLICATE_SPLIT_NAMES,
              Level.FINE, " The split names must be unique.");
        }
        if (TIME_SERIES_SPLIT.equals(trainingDatasetSplitDTO.getSplitType())) {
          isTimeSplit = true;
          if (Strings.isNullOrEmpty(eventTimeFieldName)) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.EVENT_TIME_FEATURE_NOT_FOUND, Level.FINE,
                "Failed to define time series split because event time column is not available in "
                    + "one or more feature groups.");
          }
        }
        if (TIME_SERIES_SPLIT.equals(trainingDatasetSplitDTO.getSplitType())) {
          if (TRAIN.getName().equals(trainingDatasetSplitDTO.getName())) {
            trainStart = trainingDatasetSplitDTO.getStartTime();
            trainEnd = trainingDatasetSplitDTO.getEndTime();
            if (trainStart == null || trainEnd == null) {
              throw new FeaturestoreException(
                  RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
                  Level.FINE, "Start/end time of train split is/are not provided.");
            }
          }
          if (VALIDATION.getName().equals(trainingDatasetSplitDTO.getName())) {
            validationStart = trainingDatasetSplitDTO.getStartTime();
            validationEnd = trainingDatasetSplitDTO.getEndTime();
            if (validationStart == null || validationEnd == null) {
              throw new FeaturestoreException(
                  RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
                  Level.FINE, "Start/end time of validation split is/are not provided.");
            }
          }
          if (TEST.getName().equals(trainingDatasetSplitDTO.getName())) {
            testStart = trainingDatasetSplitDTO.getStartTime();
            testEnd = trainingDatasetSplitDTO.getEndTime();
            if (testStart == null || testEnd == null) {
              throw new FeaturestoreException(
                  RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
                  Level.FINE, "Start/end time of test split is/are not provided.");
            }
          }
        }
      }
      if (isTimeSplit) {
        // Check if end time is >= start time
        if (trainStart != null && (trainStart.getTime() > trainEnd.getTime())) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
              Level.FINE,
              "End time of the train split should be greater than or equal to the start time."
          );
        }
        if (validationStart != null && (validationStart.getTime() > validationEnd.getTime())) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
              Level.FINE,
              "End time of the validation split should be greater than or equal to the start time."
          );
        }
        if (testStart != null && (testStart.getTime() > testEnd.getTime())) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
              Level.FINE,
              "End time of the test split should be greater than or equal to the start time."
          );
        }
        if (validationStart != null) {
          // Check if start time should be in order of train < validation
          if (validationStart.getTime() == trainStart.getTime() || validationStart.getTime() < trainEnd.getTime()) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
                Level.FINE,
                "Start time of the validation split should be greater than the start/end time of train split."
            );
          }
          // Check if start time should be in order of validation < test
          if (testStart.getTime() == validationStart.getTime() || testStart.getTime() < validationEnd.getTime()) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
                Level.FINE,
                "Start time of the test split should be greater than the start/end time of validation split."
            );
          }
        }
        // Check if start time should be in order of train < test
        if (testStart.getTime() == trainStart.getTime() || testStart.getTime() < trainEnd.getTime()) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT,
              Level.FINE,
              "Start time of the test split should be greater than the start/end time of train split."
          );
        }
      }
    }
  }

  public void validateFeatures(Query query, List<TrainingDatasetFeatureDTO> featuresDTOs)
      throws FeaturestoreException {
    if (query == null || featuresDTOs == null) {
      // If the query is null the features are taken from the featuresDTO, so we are guarantee that the label
      // features exists
      // if the featuresDTOs is null and the query is not, the the user didn't specify a label object, no validation
      // needed.
      return;
    }

    List<TrainingDatasetFeatureDTO> labels = featuresDTOs.stream()
        .filter(TrainingDatasetFeatureDTO::getLabel)
        .collect(Collectors.toList());
    List<TrainingDatasetFeatureDTO> featuresWithTransformation = featuresDTOs.stream()
        .filter(f -> f.getTransformationFunction() != null)
        .collect(Collectors.toList());
    List<Feature> features = collectFeatures(query);

    for (TrainingDatasetFeatureDTO label : labels) {
      if (features.stream().noneMatch(f -> f.getName().equals(label.getName()))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LABEL_NOT_FOUND, Level.FINE,
            "Label: " + label.getName() + " is missing");
      }
    }

    for (TrainingDatasetFeatureDTO featureWithTransformation : featuresWithTransformation) {
      if (features.stream().noneMatch(f ->
          f.getName().equals(featureWithTransformation.getFeatureGroupFeatureName()))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_WITH_TRANSFORMATION_NOT_FOUND,
            Level.FINE, "feature: " + featureWithTransformation.getName() +
            " is missing and transformation function can't be attached");
      }
    }

    //verify join prefix if any
    if (query != null && query.getJoins() != null) {
      for (Join join : query.getJoins()){
        if (join.getPrefix() != null){
          Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
          if (!namePattern.matcher(join.getPrefix()).matches()) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_PREFIX_NAME, Level.FINE,
                ", the provided prefix name " + join.getPrefix() + " is invalid. Prefix names can only contain lower" +
                    " case characters, numbers and underscores and cannot be longer than " +
                    FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
          }
        }
      }
    }
  }

  // Collect Features for verification
  private List<Feature> collectFeatures(Query query) {
    List<Feature> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        features.addAll(collectFeatures(join.getRightQuery()));
      }
    }

    return features;
  }


  private void validateStorageConnector(FeaturestoreStorageConnectorDTO connectorDTO)
      throws FeaturestoreException {
    if (connectorDTO == null || connectorDTO.getId() == null) {
      // The storage connector is null, the training dataset will use the default
      // HopsFS training dataset connector controller from the project
      return;
    }

    FeaturestoreConnector connector = connectorFacade.findById(connectorDTO.getId())
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
            Level.FINE, "Connector ID: " + connectorDTO.getId()));

    if (!(connector.getConnectorType() == FeaturestoreConnectorType.HOPSFS ||
      connector.getConnectorType() == FeaturestoreConnectorType.S3 ||
      connector.getConnectorType() == FeaturestoreConnectorType.ADLS ||
      connector.getConnectorType() == FeaturestoreConnectorType.GCS)) {
      // We only support creating training datasets using HopsFS, S3, ADLS or GCS connectors
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Only HopsFS, S3, ADLS and GCS storage connectors can be used to create training datasets");
    }
  }

  void validateTrainSplit(String trainSplit, List<TrainingDatasetSplitDTO> splits)
    throws FeaturestoreException {
    if ((splits == null || splits.isEmpty()) && !Strings.isNullOrEmpty(trainSplit)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_NAME, Level.FINE,
          "Training data split name provided without splitting the dataset.");
    }
    if (splits != null && !splits.isEmpty() &&
      !splits.stream().map(TrainingDatasetSplitDTO::getName).collect(Collectors.toList()).contains(trainSplit)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_NAME, Level.FINE,
        "The provided training data split name `" + trainSplit + "` could not be found among the provided splits.");
    }
  }
}
