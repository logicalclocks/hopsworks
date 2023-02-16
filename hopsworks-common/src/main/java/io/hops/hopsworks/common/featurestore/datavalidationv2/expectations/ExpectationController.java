/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidationv2.expectations;


import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_KWARGS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_META;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExpectationController {

  private static final Logger LOGGER = Logger.getLogger(ExpectationController.class.getName());

  @EJB
  ExpectationFacade expectationFacade;
  @EJB
  FeaturestoreActivityFacade fsActivityFacade;
  @EJB  
  FeaturegroupController featuregroupController;

  public Expectation getExpectationById(Integer expectationId) {
    Optional<Expectation> expectation = expectationFacade.findById(expectationId);

    return expectation.orElse(null);
  }

  public void createExpectation(Expectation expectation) {

    expectationFacade.createExpectation(expectation);
  }

  public void updateExpectation(Expectation expectation) throws FeaturestoreException {

    // Check existence, throw exception if does not exist
    Expectation oldExpectation = getExpectationById(expectation.getId());

    // Only allow edit which preserves expectation_typ and column
    preserveTypeAndColumn(oldExpectation, expectation);

    expectationFacade.updateExpectation(expectation);
  }

  public Expectation createOrUpdateExpectation(Users user, ExpectationSuite expectationSuite, 
    ExpectationDTO expectationDTO, boolean logActivity, boolean verifyInput) throws FeaturestoreException {
    if (verifyInput) {
      verifyExpectationFields(
        expectationDTO, 
        featuregroupController.getFeatureNames(
          expectationSuite.getFeaturegroup(),
          expectationSuite.getFeaturegroup().getFeaturestore().getProject(),
          user
        )
      );
    }
    
    // Existing expectation can be sent with id in id field or as key-value in meta field.
    Integer expectationId = null;
    if (expectationDTO.getId() == null) {
      expectationId = parseExpectationIdIfExist(expectationDTO);
    } else {
      expectationId = expectationDTO.getId();
    }
    expectationDTO.setId(expectationId);

    Expectation expectation = convertExpectationDTOToPersistent(expectationSuite, expectationDTO);

    String activityMessage = null;    
    // If it has an id try to update it, otherwise create it
    if (expectation.getId() == null) {
      createExpectation(expectation);
      activityMessage = String.format("Created expectation with id: %d.", expectation.getId());
    } else {
      updateExpectation(expectation);
      activityMessage = String.format("Updated expectation with id: %d.", expectation.getId());
    }

    if (logActivity) {
      FeaturestoreActivityMeta activityMeta = FeaturestoreActivityMeta.EXPECTATION_SUITE_UPDATED;
      fsActivityFacade.logExpectationSuiteActivity(
        user, expectationSuite.getFeaturegroup(), expectationSuite, activityMeta, activityMessage);
    }

    return expectation;
  }

  public void deleteExpectation(Users user, Integer expectationId, boolean logActivity) {
    Expectation expectation = expectationFacade.findById(expectationId).orElse(null);
    ExpectationSuite expectationSuite = null;
    String activityMessage = null;
    if (expectation != null) {
      expectationSuite = expectation.getExpectationSuite(); 
      activityMessage = String.format("Deleted expectation with id: %d.", expectation.getId());
    }
    expectationFacade.remove(expectation);
    if (expectation != null && logActivity) {
      FeaturestoreActivityMeta activityMeta = FeaturestoreActivityMeta.EXPECTATION_SUITE_UPDATED;
      // expectationSuite cannot be null pointer because fetched in if above.
      fsActivityFacade.logExpectationSuiteActivity(
        user, expectationSuite.getFeaturegroup(), expectationSuite, activityMeta, activityMessage);
    }
  }

  public CollectionInfo<Expectation> getExpectationsByExpectationSuite(ExpectationSuite expectationSuite) {
    return expectationFacade.findByExpectationSuite(expectationSuite);
  }

  public Integer parseExpectationIdIfExist(ExpectationDTO expectationDTO) {
    Integer expectationId = null;
    JSONObject meta;

    try {
      meta = new JSONObject(expectationDTO.getMeta());
      if (meta.has("expectationId")) {
        expectationId = meta.getInt("expectationId");
      }
    } catch (JSONException e) {
      // This should never happen as verify methods should have thrown exception earlier
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return expectationId;
  }

  public Expectation convertExpectationDTOToPersistent(ExpectationSuite expectationSuite, ExpectationDTO dto) {
    Expectation persistentExpectation = new Expectation();
    persistentExpectation.setExpectationSuite(expectationSuite);
    persistentExpectation.setKwargs(dto.getKwargs());
    persistentExpectation.setMeta(dto.getMeta());
    persistentExpectation.setExpectationType(dto.getExpectationType());
    persistentExpectation.setId(dto.getId());

    return persistentExpectation;
  }

  //////////////////////////////////////////
  //// Input Verification
  //////////////////////////////////////////

  public void verifyExpectationFields(ExpectationDTO dto, List<String> featureNames) throws FeaturestoreException {
    verifyExpectationExpectationType(dto);
    verifyExpectationKwargs(dto, featureNames);
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

  private void verifyExpectationKwargs(ExpectationDTO dto, List<String> featureNames) throws FeaturestoreException {
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
    JSONObject jsonKwargs;
    try {
      jsonKwargs = new JSONObject(kwargs);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation Kwargs field %s is not a valid json.", kwargs),
        e.getMessage()
      );
    }

    verifyKwargsColumnExistence(featureNames, jsonKwargs);
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

  private void verifyKwargsColumnExistence(List<String> featureNames, JSONObject jsonKwargs)
    throws FeaturestoreException {
    // Check if kwargs has column type key, if so check that value corresponds to a feature name
    // which exist for this featuregroup version
    ArrayList<String> possibleFeatureNames = new ArrayList<>();

    if (jsonKwargs.has("column")) {
      possibleFeatureNames.add(jsonKwargs.getString("column"));
    } else if (jsonKwargs.has("columnA") && jsonKwargs.has("columnB")) {
      possibleFeatureNames.add(jsonKwargs.getString("columnA"));
      possibleFeatureNames.add(jsonKwargs.getString("columnB"));
    } else if (jsonKwargs.has("column_list")) {
      JSONArray columns = jsonKwargs.getJSONArray("column_list");
      // It is ugly to use for loop with indexing but seems like anything else is throwing error
      for(int index = 0; index < columns.length(); index++) {
        possibleFeatureNames.add(columns.getString(index));
      }
    } else if (jsonKwargs.has("column_set")) {
      JSONArray columns = jsonKwargs.getJSONArray("column_set");
      // It is ugly to use for loop with indexing but seems like anything else is throwing error
      for(int index = 0; index < columns.length(); index++) {
        possibleFeatureNames.add(columns.getString(index));
      }
    } else {
      return;
    }

    for (String name : possibleFeatureNames) {
      if (!featureNames.contains(name)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NAME_NOT_FOUND, Level.SEVERE,
          String.format("Expectation Kwargs contains name %s which has not been found in this group's feature:%n%s.",
            name, featureNames));
      }
    }
  }


  // Additional check to ensure compatibility to preserve an expectation
  private void preserveTypeAndColumn(Expectation oldExpectation, Expectation newExpectation)
    throws FeaturestoreException {
    // It must preserve expectation type
    if (!oldExpectation.getExpectationType().equals(newExpectation.getExpectationType())) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_EXPECTATION_UPDATE,
        Level.SEVERE,
        String.format("The stored expectation has type %s but updated expectation has type %s.",
          oldExpectation.getExpectationType(), newExpectation.getExpectationType()));
    }

    // Parse kwargs to get column type arguments
    JSONObject oldKwargs;
    JSONObject newKwargs;
    try {
      oldKwargs = new JSONObject(oldExpectation.getKwargs());
    } catch (JSONException e) {
      // This should not happen.
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation % Kwargs field %s is not a valid json.",
          oldExpectation.getId(),
          oldExpectation.getKwargs()),
        e.getMessage()
      );
    }

    try {
      newKwargs = new JSONObject(newExpectation.getKwargs());
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Expectation %d Kwargs field %s is not a valid json.", newExpectation.getId(),
          newExpectation.getKwargs()),
        e.getMessage()
      );
    }

    // Preserve column
    if (oldKwargs.has("column") && newKwargs.has("column") && 
      !oldKwargs.getString("column").equals(newKwargs.getString("column"))) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_EXPECTATION_UPDATE,
        Level.SEVERE,
        String.format("The stored expectation has kwarg column %s but updated expectation has kwarg column %s.",
          oldKwargs.getString("column"), newKwargs.getString("column")));
    }

    // Preserve dual column
    if (oldKwargs.has("columnA") && newKwargs.has("columnA") && oldKwargs.has("columnB") && 
      newKwargs.has("columnB") && !oldKwargs.getString("columnA").equals(newKwargs.getString("columnA")) 
      && !oldKwargs.getString("columnB").equals(newKwargs.getString("columnB"))) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_EXPECTATION_UPDATE,
        Level.SEVERE,
        String.format("The stored expectation has kwarg column %s but updated expectation has kwarg column %s.",
          oldKwargs.getString("column"), newKwargs.getString("column")));
    }

  }

  ////////////////////////////////////////////////////////////////////////////
  //// Post-processing to populate id in meta fields, use to be in the builder
  ////////////////////////////////////////////////////////////////////////////

  public Expectation addExpectationIdToMetaField(Expectation expectation) throws FeaturestoreException {
    // Set expectationId in the meta field
    try {
      JSONObject meta = new JSONObject(expectation.getMeta());
      meta.put("expectationId", expectation.getId());
      expectation.setMeta(meta.toString());
    } catch (JSONException e) {
      // Argument can be made that we simply return it, rather than throwing an exception
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.FAILED_TO_PARSE_EXPECTATION_META_FIELD, Level.SEVERE,
        String.format("Expectation meta field is not valid json : %s", expectation.getMeta())
      );
    }

    return expectation;
  }
}
