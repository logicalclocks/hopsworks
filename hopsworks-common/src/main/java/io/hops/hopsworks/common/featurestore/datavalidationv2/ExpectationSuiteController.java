/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidationv2;

import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.GreatExpectation;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_DATA_ASSET_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_KWARGS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_META;
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

  ///////////////////////////////////////////////////
  ////// Great Expectations
  //////////////////////////////////////////////////

  public CollectionInfo<GreatExpectation> getAllGreatExpectations() {
    return greatExpectationFacade.findAllGreatExpectation();
  }

  //////////////////////////////////////////////////////
  ////// Expectation Suite CRUD
  //////////////////////////////////////////////////////

  public ExpectationSuite getExpectationSuite(Featuregroup featureGroup) {
    Optional<ExpectationSuite> e = expectationSuiteFacade.findByFeaturegroup(featureGroup);
    return e.orElse(null);
  }

  public ExpectationSuite createExpectationSuite(Featuregroup featureGroup, ExpectationSuiteDTO expectationSuiteDTO)
    throws FeaturestoreException {
    Optional<ExpectationSuite> e = expectationSuiteFacade.findByFeaturegroup(featureGroup);
    verifyExpectationSuiteFields(expectationSuiteDTO);
    
    if (e.isPresent()) {
      deleteExpectationSuite(featureGroup);
    }

    ExpectationSuite expectationSuite = convertExpectationSuiteDTOToPersistent(featureGroup, expectationSuiteDTO);
    expectationSuiteFacade.persist(expectationSuite);

    return expectationSuite;
  }

  public void deleteExpectationSuite(Featuregroup featureGroup) {
    expectationSuiteFacade.remove(getExpectationSuite(featureGroup));
  }


  /**
   * Convert an expectation suite entity to a DTO representation
   *
   * @param featuregroup
   *   the entity to convert
   * @return a DTO representation of the entity
   */
  public ExpectationSuite convertExpectationSuiteDTOToPersistent(Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {
    ExpectationSuite expectationSuite = new ExpectationSuite();
    expectationSuite.setFeaturegroup(featuregroup);
    expectationSuite.setMeta(expectationSuiteDTO.getMeta());
    expectationSuite.setName(expectationSuiteDTO.getExpectationSuiteName());
    expectationSuite.setValidationIngestionPolicy(expectationSuiteDTO.getValidationIngestionPolicy());
    expectationSuite.setRunValidation(expectationSuiteDTO.getRunValidation());
    expectationSuite.setDataAssetType(expectationSuiteDTO.getDataAssetType());
    expectationSuite.setGeCloudId(expectationSuiteDTO.getGeCloudId());

    List<Expectation> persistentExpectations = new ArrayList<Expectation>();
    for (ExpectationDTO dto : expectationSuiteDTO.getExpectations()) {
      persistentExpectations.add(convertExpectationDTOToPersistent(expectationSuite, dto));
    }

    expectationSuite.setExpectations(persistentExpectations);

    return expectationSuite;
  }
  
  public ExpectationSuite convertExpectationSuiteDTOToPersistentValidated(Featuregroup featuregroup,
    ExpectationSuiteDTO expectationSuiteDTO) throws FeaturestoreException {
    // validating also here cause this method is used by FeatureGroupController directly
    verifyExpectationSuiteFields(expectationSuiteDTO);
    return convertExpectationSuiteDTOToPersistent(featuregroup, expectationSuiteDTO);
  }

  public Expectation convertExpectationDTOToPersistent(ExpectationSuite expectationSuite, ExpectationDTO dto)
    throws FeaturestoreException {
    verifyExpectationFields(dto);
    Expectation persistentExpectation = new Expectation();
    persistentExpectation.setExpectationSuite(expectationSuite);
    persistentExpectation.setKwargs(dto.getKwargs());
    persistentExpectation.setMeta(dto.getMeta());
    persistentExpectation.setExpectationType(dto.getExpectationType());

    return persistentExpectation;
  }

  ////////////////////////////////////////
  //// Input Verification
  ///////////////////////////////////////

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

  private void verifyExpectationFields(ExpectationDTO dto) throws FeaturestoreException {
    verifyExpectationExpectationType(dto);
    verifyExpectationKwargs(dto);
    verifyExpectationMeta(dto);
  }

  private void verifyExpectationMeta(ExpectationDTO dto) throws FeaturestoreException {
    String meta = dto.getMeta();
    if (meta == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Expectation meta cannot be null. Pass a stringified JSON."
      );
    }

    if (meta.length() > MAX_CHARACTERS_IN_EXPECTATION_META) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Meta field %s exceeds the max allowed character length %d.",
          meta, MAX_CHARACTERS_IN_EXPECTATION_META)
      );
    }

    try {
      new JSONObject(meta);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation Meta field %s is not a valid json.", meta),
        e.getMessage()
      );
    }
  }

  private void verifyExpectationKwargs(ExpectationDTO dto) throws FeaturestoreException {
    String kwargs = dto.getKwargs();
    if (kwargs == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Expectation Kwargs cannot be null. Pass a stringified JSON."
      );
    }

    if (kwargs.length() > MAX_CHARACTERS_IN_EXPECTATION_KWARGS) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Kwargs field %s exceeds the max allowed character length %d.",
          kwargs, MAX_CHARACTERS_IN_EXPECTATION_KWARGS)
      );
    }

    try {
      new JSONObject(kwargs);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation Kwargs field %s is not a valid json.", kwargs),
        e.getMessage()
      );
    }
  }

  private void verifyExpectationExpectationType(ExpectationDTO dto) throws FeaturestoreException {
    String expectationType = dto.getExpectationType();
    if (expectationType == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Expectation Expectation Type cannot be null. Pass a supported expectation type."
      );
    }

    if (expectationType.length() > MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Expectation Expectation Type field %s exceeds the max allowed character length %d.",
          expectationType, MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE)
      );
    }
  }
}
