package io.hops.hopsworks.common.featurestore.utils;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreInputValidation {
  
  public FeaturestoreInputValidation() {
  }
  
  /**
   * Verify entity names input by the user for creation of entities in the featurestore
   *
   * @param featurestoreEntityDTO the user input data for the entity
   * @throws FeaturestoreException
   */
  public void verifyUserInput(FeaturestoreEntityDTO featurestoreEntityDTO, Boolean sync)
    throws FeaturestoreException {
    
    Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
    
    // name
    if (!namePattern.matcher(featurestoreEntityDTO.getName()).matches()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ENTITY_NAME, Level.FINE,
        ", the provided name " + featurestoreEntityDTO.getName() + " is invalid. Entity names can only contain lower " +
          "case characters, numbers and underscores and cannot be longer than " +
          FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
    }
    
    // description - can be empty
    if (!Strings.isNullOrEmpty(featurestoreEntityDTO.getDescription()) &&
      featurestoreEntityDTO.getDescription().length() >
        FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ENTITY_DESCRIPTION, Level.FINE,
        ", the provided description for the entity " + featurestoreEntityDTO.getName() + " is too long. Entity " +
          "descriptions cannot be longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH +
          " characters.");
    }
    
    // features
    // Only need to verify the DDL when creating table from Scratch and not Syncing
    if (!sync) {
      verifyFeatureList(featurestoreEntityDTO.getFeatures());
    }
  }
  
  /**
   * Verifies the user input feature list for a feature store entity
   *
   * @param featureDTOs the feature list to verify
   */
  private void verifyFeatureList(List<FeatureDTO> featureDTOs) throws FeaturestoreException {
    if(featureDTOs != null && !featureDTOs.isEmpty()) {
      Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
      if (featureDTOs.stream().anyMatch(f -> !namePattern.matcher(f.getName()).matches())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME, Level.FINE,
          ", one or more of the provided feature names is invalid. Feature names can only contain lower " +
            "case characters, numbers and underscores and cannot be longer than " +
            FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
      }
  
      if (featureDTOs.stream().anyMatch(f -> !Strings.isNullOrEmpty(f.getDescription()) &&
        f.getDescription().length() > FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION, Level.FINE,
          ", one or more of the provided feature descriptions is too long. Feature descriptions cannot be longer than "
            + FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH + " characters.");
      }
    }
  }
}
