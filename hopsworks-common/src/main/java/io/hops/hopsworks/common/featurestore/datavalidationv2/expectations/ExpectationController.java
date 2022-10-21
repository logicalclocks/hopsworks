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
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.Optional;
import java.util.logging.Level;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_KWARGS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_META;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExpectationController {
  @EJB
  ExpectationFacade expectationFacade;

  public Expectation getExpectationById(Integer expectationId) throws FeaturestoreException {
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

  public Expectation createOrUpdateExpectation(ExpectationSuite expectationSuite, ExpectationDTO expectationDTO)
    throws FeaturestoreException {
    verifyExpectationFields(expectationDTO);
    // Existing expectation can be sent with id in id field or as key-value in meta field.
    Integer expectationId = null;
    if (expectationDTO.getId() == null) {
      expectationId = parseExpectationIdIfExist(expectationDTO);
    } else {
      expectationId = expectationDTO.getId();
    }
    expectationDTO.setId(expectationId);

    Expectation expectation = convertExpectationDTOToPersistent(expectationSuite, expectationDTO);

    // If it has an id try to update it, otherwise create it
    if (expectation.getId() == null) {
      createExpectation(expectation);
    } else {
      updateExpectation(expectation);
    }

    return expectation;
  }

  public void deleteExpectation(Integer expectationId) throws FeaturestoreException {
    Expectation expectation = expectationFacade.findById(expectationId).orElse(null);
    expectationFacade.remove(expectation);
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
      e.printStackTrace();
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

  public void verifyExpectationFields(ExpectationDTO dto) throws FeaturestoreException {
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
    if (oldKwargs.has("column") && newKwargs.has("column")) {
      if (!oldKwargs.getString("column").equals(newKwargs.getString("column"))) {
        throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_EXPECTATION_UPDATE,
          Level.SEVERE,
          String.format("The stored expectation has kwarg column %s but updated expectation has kwarg column %s.",
            oldKwargs.getString("column"), newKwargs.getString("column")));
      }
    }

    // Preserve dual column
    if (oldKwargs.has("columnA") && newKwargs.has("columnA") && oldKwargs.has("columnB") && newKwargs.has("columnB")) {
      if (!oldKwargs.getString("columnA").equals(newKwargs.getString("columnA")) &&
        !oldKwargs.getString("columnB").equals(newKwargs.getString("columnB"))) {
        throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_EXPECTATION_UPDATE,
          Level.SEVERE,
          String.format("The stored expectation has kwarg column %s but updated expectation has kwarg column %s.",
            oldKwargs.getString("column"), newKwargs.getString("column")));
      }
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
