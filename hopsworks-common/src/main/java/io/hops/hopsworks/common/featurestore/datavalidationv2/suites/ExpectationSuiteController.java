/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidationv2.suites;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.greatexpectations.GreatExpectationFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.GreatExpectation;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_DATA_ASSET_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_SUITE_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_GE_CLOUD_ID;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExpectationSuiteController {
  private static final Logger LOGGER = Logger.getLogger(ExpectationSuiteController.class.getName());

  @EJB
  ExpectationSuiteFacade expectationSuiteFacade;
  @EJB
  GreatExpectationFacade greatExpectationFacade;
  @EJB
  ExpectationController expectationController;
  @EJB
  FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  FeaturegroupController featuregroupController;

  ///////////////////////////////////////////////////
  ////// Great Expectations
  //////////////////////////////////////////////////

  public CollectionInfo<GreatExpectation> getAllGreatExpectations() {
    return greatExpectationFacade.findAllGreatExpectation();
  }

  //////////////////////////////////////////////////////
  ////// Expectation Suite CRUD
  //////////////////////////////////////////////////////

  public ExpectationSuite getExpectationSuite(Featuregroup featureGroup) throws FeaturestoreException {
    Optional<ExpectationSuite> e = expectationSuiteFacade.findByFeaturegroup(featureGroup);

    return e.orElse(null);
  }

  public ExpectationSuite getExpectationSuiteById(Integer expectationSuiteId) throws FeaturestoreException {
    Optional<ExpectationSuite> e = expectationSuiteFacade.findById(expectationSuiteId);

    return e.orElse(null);
  }

  public ExpectationSuite createExpectationSuite(Users user, Featuregroup featureGroup, 
    ExpectationSuiteDTO expectationSuiteDTO)
    throws FeaturestoreException {
    verifyExpectationSuite(
      expectationSuiteDTO, 
      featuregroupController.getFeatureNames(
        featureGroup, featureGroup.getFeaturestore().getProject(), user));

    ExpectationSuite expectationSuite = convertExpectationSuiteDTOToPersistent(featureGroup, expectationSuiteDTO);
    expectationSuiteFacade.persist(expectationSuite);

    fsActivityFacade.logExpectationSuiteActivity(
      user, featureGroup, expectationSuite, FeaturestoreActivityMeta.EXPECTATION_SUITE_ATTACHED, "");

    return expectationSuite;
  }

  public void deleteExpectationSuite(Users user, Featuregroup featureGroup) throws FeaturestoreException {
    ExpectationSuite expectationSuite = getExpectationSuite(featureGroup);
    if (expectationSuite != null) {
      expectationSuiteFacade.remove(expectationSuite);
      fsActivityFacade.logExpectationSuiteActivity(
        user, featureGroup, null, FeaturestoreActivityMeta.EXPECTATION_SUITE_DELETED, "");
    }
  }


  /**
   * Convert an expectation suite DTO to the persistent representation
   *
   * @param featuregroup
   *   the feature group to which the expectation suite belongs
   * @param expectationSuiteDTO
   *   the entity to convert
   * @return a persitent representation of the entity
   */
  public ExpectationSuite convertExpectationSuiteDTOToPersistent(Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO) {
    ExpectationSuite expectationSuite =
      convertExpectationSuiteMetadataDTOToPersistent(featuregroup, expectationSuiteDTO);

    List<Expectation> persistentExpectations = new ArrayList<Expectation>();
    for (ExpectationDTO dto : expectationSuiteDTO.getExpectations()) {
      persistentExpectations.add(expectationController.convertExpectationDTOToPersistent(expectationSuite, dto));
    }

    expectationSuite.setExpectations(persistentExpectations);

    return expectationSuite;
  }

  public ExpectationSuite convertExpectationSuiteMetadataDTOToPersistent(Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO) {

    ExpectationSuite expectationSuite = new ExpectationSuite();
    expectationSuite.setFeaturegroup(featuregroup);
    expectationSuite.setMeta(expectationSuiteDTO.getMeta());
    expectationSuite.setName(expectationSuiteDTO.getExpectationSuiteName());
    expectationSuite.setValidationIngestionPolicy(expectationSuiteDTO.getValidationIngestionPolicy());
    expectationSuite.setRunValidation(expectationSuiteDTO.getRunValidation());
    expectationSuite.setDataAssetType(expectationSuiteDTO.getDataAssetType());
    expectationSuite.setGeCloudId(expectationSuiteDTO.getGeCloudId());

    return expectationSuite;
  }

  ////////////////////////////////////////
  //// Input Verification
  ///////////////////////////////////////

  public void verifyExpectationSuite(ExpectationSuiteDTO dto, List<String> featureNames) throws FeaturestoreException {
    if (dto == null) {
      return;
    }
    verifyExpectationSuiteFields(dto);
    for (ExpectationDTO expectationDTO : dto.getExpectations()) {
      expectationController.verifyExpectationFields(expectationDTO, featureNames);
    }
  }

  private void verifyExpectationSuiteFields(ExpectationSuiteDTO dto) throws FeaturestoreException {
    verifyExpectationSuiteGeCloudId(dto);
    verifyExpectationSuiteDataAssetType(dto);
    verifyExpectationSuiteMeta(dto);
    verifyExpectationSuiteName(dto);
  }

  private void verifyExpectationSuiteName(ExpectationSuiteDTO dto) throws FeaturestoreException {
    String expectationSuiteName = dto.getExpectationSuiteName();
    if (expectationSuiteName == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Expectation Suite Name cannot be null. Pass an empty string."
      );
    }

    if (expectationSuiteName.length() > MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Suite Name %s exceeds the max allowed character length %d.",
          expectationSuiteName, MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME)
      );
    }
  }

  private void verifyExpectationSuiteMeta(ExpectationSuiteDTO dto) throws FeaturestoreException {
    String meta = dto.getMeta();
    if (meta == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Expectation Suite Meta cannot be null. Pass a stringified Json."
      );
    }

    if (meta.length() > MAX_CHARACTERS_IN_EXPECTATION_SUITE_META) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Suite Meta field %s exceeds the max allowed character length %d.",
          meta, MAX_CHARACTERS_IN_EXPECTATION_SUITE_META)
      );
    }

    try {
      new JSONObject(meta);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation Suite Meta field %s is not a valid json.", meta),
        e.getMessage()
      );
    }
  }

  private void verifyExpectationSuiteGeCloudId(ExpectationSuiteDTO dto) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(dto.getGeCloudId()) && dto.getGeCloudId().length() > MAX_CHARACTERS_IN_GE_CLOUD_ID) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Suite Ge Cloud Id %s exceeds the max allowed character length %d.",
          dto.getGeCloudId(), MAX_CHARACTERS_IN_GE_CLOUD_ID)
      );
    }
  }

  private void verifyExpectationSuiteDataAssetType(ExpectationSuiteDTO dto) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(dto.getDataAssetType()) &&
      dto.getDataAssetType().length() > MAX_CHARACTERS_IN_DATA_ASSET_TYPE) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Suite Data Asset Type %s exceeds the max allowed character length %d.",
          dto.getDataAssetType(), MAX_CHARACTERS_IN_DATA_ASSET_TYPE)
      );
    }
  }

  ////////////////////////////////////////////////////////
  //// Smart update to preserve expectations and associated result
  ////////////////////////////////////////////////////////

  public ExpectationSuite updateExpectationSuite(Users user, Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {
    verifyExpectationSuite(
      expectationSuiteDTO, 
      featuregroupController.getFeatureNames(
        featuregroup, featuregroup.getFeaturestore().getProject(), user));

    Optional<ExpectationSuite> optionalOldExpectationSuite = expectationSuiteFacade.findByFeaturegroup(featuregroup);

    if (!optionalOldExpectationSuite.isPresent()) {
      return createExpectationSuite(user, featuregroup, expectationSuiteDTO);
    } else {
      // Smart update to preserve reports/results history
      return smartUpdateExpectationSuite(user, featuregroup, expectationSuiteDTO, optionalOldExpectationSuite.get());
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = {FeaturestoreException.class})
  public ExpectationSuite smartUpdateExpectationSuite(Users user, Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO, ExpectationSuite oldExpectationSuite) throws FeaturestoreException {
    boolean logActivity = false;
    boolean verifyInput = true;
    ExpectationSuite newExpectationSuite = updateMetadataExpectationSuite(
      user, featuregroup, expectationSuiteDTO, logActivity, verifyInput);

    ArrayList<Expectation> newExpectationList = new ArrayList<>();
    
    verifyInput = false;
    for (ExpectationDTO expectationDTO : expectationSuiteDTO.getExpectations()) {
      newExpectationList.add(
        expectationController.createOrUpdateExpectation(
          user, newExpectationSuite, expectationDTO, logActivity, verifyInput));
    }
    newExpectationSuite.setExpectations(newExpectationList);

    deleteMissingExpectations(user, newExpectationList, oldExpectationSuite.getExpectations());

    // Activity message could be reworked to add more detail about the update
    fsActivityFacade.logExpectationSuiteActivity(
      user, featuregroup, newExpectationSuite, FeaturestoreActivityMeta.EXPECTATION_SUITE_UPDATED, "");

    return newExpectationSuite;
  }

  ////////////////////////////////////////////////////////
  //// Smart update helper functions to preserve expectations and associated result
  ////////////////////////////////////////////////////////

  // Update only metadata of the ExpectationSuite (everything but Expectation list)
  public ExpectationSuite updateMetadataExpectationSuite(Users user, Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO, boolean logActivity, boolean verifyInput) throws FeaturestoreException {
    if (verifyInput) {
      verifyExpectationSuite(
        expectationSuiteDTO, 
        featuregroupController.getFeatureNames(
          featuregroup, featuregroup.getFeaturestore().getProject(), user));
    }

    ExpectationSuite oldExpectationSuite = getExpectationSuite(featuregroup);

    ExpectationSuite expectationSuite =
      convertExpectationSuiteMetadataDTOToPersistent(featuregroup, expectationSuiteDTO);

    expectationSuite.setId(oldExpectationSuite.getId());

    expectationSuite.setExpectations(oldExpectationSuite.getExpectations());

    expectationSuiteFacade.updateExpectationSuite(expectationSuite);

    if (logActivity) {
      fsActivityFacade.logExpectationSuiteActivity(user, featuregroup, expectationSuite,
        FeaturestoreActivityMeta.EXPECTATION_SUITE_METADATA_UPDATED, "");
    }

    return expectationSuite;
  }

  private void deleteMissingExpectations(Users user, ArrayList<Expectation> newExpectations,
    Collection<Expectation> oldExpectations) throws FeaturestoreException {
    List<Integer> newAndPreservedExpectationIds =
      newExpectations.stream().map(Expectation::getId).collect(Collectors.toList());
    ArrayList<Integer> missingExpectationIds = new ArrayList<>();

    for (Expectation expectation : oldExpectations) {
      if (!newAndPreservedExpectationIds.contains(expectation.getId())) {
        missingExpectationIds.add(expectation.getId());
      }
    }

    for (Integer expectationId : missingExpectationIds) {
      // Don't log the activity for every single deletion
      expectationController.deleteExpectation(user, expectationId, false);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //// Post-processing to populate id in meta fields, use to be in the builder
  ////////////////////////////////////////////////////////////////////////////

  public ExpectationSuite addAllExpectationIdToMetaField(ExpectationSuite expectationSuite)
    throws FeaturestoreException {
    ArrayList<Expectation> listOfExpectations = new ArrayList<>();
    for (Expectation expectation: expectationSuite.getExpectations()) {
      listOfExpectations.add(expectationController.addExpectationIdToMetaField(expectation));
    }
    expectationSuite.setExpectations(listOfExpectations);

    return expectationSuite;
  }
}
