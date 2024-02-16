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

package io.hops.hopsworks.common.featurestore.datavalidationv2.results;

import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;
import io.hops.hopsworks.persistence.entity.util.AbstractFacade;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.UNEXPECTED_COUNT_KEY;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.UNEXPECTED_PERCENT_KEY;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.PARTIAL_UNEXPECTED_LIST_KEY;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.UNEXPECTED_PERCENT_NONMISSING_KEY;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.OBSERVED_VALUE_KEY;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.EXCEPTION_MESSAGE_KEY;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ValidationResultController {
  private static final Logger LOGGER = Logger.getLogger(ValidationResultController.class.getName());

  @EJB
  private ValidationResultFacade validationResultFacade;
  @EJB
  private ExpectationFacade expectationFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;

  public AbstractFacade.CollectionInfo<ValidationResult> getAllValidationResultByExpectationId (
      Integer offset, Integer limit, Set<? extends AbstractFacade.SortBy> sorts,
      Set<? extends AbstractFacade.FilterBy> filters, Integer expectationId) {
    Optional<Expectation> optExpectation = expectationFacade.findById(expectationId);
    Expectation expectation;

    if (optExpectation.isPresent()) {
      expectation = optExpectation.get();
    } else {
      // fake it, we just need the id
      expectation = new Expectation();
      expectation.setId(expectationId);
    }

    return validationResultFacade.findByExpectation(offset, limit, sorts, filters, expectation);
  }

  public ValidationResult convertResultDTOToPersistent(ValidationReport report, ValidationResultDTO dto)
    throws FeaturestoreException {
    verifyValidationResultDTOFields(dto);
    ValidationResult result = new ValidationResult();
    result.setMeta(dto.getMeta());
    result.setSuccess(dto.getSuccess());
    result.setValidationReport(report);
    result.setIngestionResult(report.getIngestionResult());
    result.setValidationTime(report.getValidationTime());

    // We need:
    // - Get the expectation id from the meta field in the expectation_config field.
    // - Shorten result field if too long
    // - Shorten exceptionInfo field if too long
    // - Shorten expectationConfig field if too long
    result.setExpectation(parseExpectationIdFromResultDTO(dto.getExpectationConfig()));

    if (dto.getResult().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
      result.setResult(validationResultShortenResultField(dto.getResult()));
    } else {
      result.setResult(dto.getResult());
    }

    if (dto.getExceptionInfo().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO) {
      result.setExceptionInfo(validationResultShortenExceptionInfoField(dto.getExceptionInfo()));
    } else {
      result.setExceptionInfo(dto.getExceptionInfo());
    }
    if (dto.getExpectationConfig().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG) {
      result.setExpectationConfig(
        validationResultShortenExpectationConfigField(
          dto.getExpectationConfig(), 
          result.getExpectation().getId()
        )
      );
    } else {
      result.setExpectationConfig(dto.getExpectationConfig());
    }

    
    return result;
  }

  private Expectation parseExpectationIdFromResultDTO(String dtoExpectationConfig) throws FeaturestoreException {
    // 1. Parse config, 2. Look for expectation_id, 3. findExpectationById, 4. setExpectation, 5. Celebrate!
    JSONObject expectationConfig;
    Integer expectationId;
    try {
      expectationConfig = new JSONObject(dtoExpectationConfig);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config field %s is not a valid json.",
          dtoExpectationConfig),
        e.getMessage()
      );
    }

    JSONObject meta;
    try {
      meta = expectationConfig.getJSONObject("meta");
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config meta field %s is not a valid json.",
          dtoExpectationConfig),
        e.getMessage()
      );
    }

    try {
      expectationId = meta.getInt("expectationId");
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.KEY_NOT_FOUND_OR_INVALID_VALUE_TYPE_IN_JSON_OBJECT,
        Level.SEVERE,
        String.format("Validation result expectation config meta %s does not contain expectationId key or the " +
          "associated value does not convert to an integer", meta),
        e.getMessage());
    }

    Optional<Expectation> expectation = expectationFacade.findById(expectationId);

    if (!expectation.isPresent()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.EXPECTATION_NOT_FOUND, Level.WARNING);
    }

    return expectation.get();
  }

  public String validationResultShortenResultField(String result) {
    JSONObject resultJson;
    try {
      resultJson = new JSONObject(result);
    } catch (JSONException e) {
      LOGGER.warning(String.format(
        "Parsing result field threw JSONException that should have been handled when verifying input.%n%s%n%s",
        e.getMessage(), Arrays.toString(e.getStackTrace())));
      resultJson = new JSONObject();
    }

     
    JSONObject shortResultJson = new JSONObject();
    String userMessage =
      "Result field exceeded max available space in SQL table, " +
        "download validation report file to access the complete result.";
    shortResultJson.put("user_message", userMessage);

    if (resultJson.has(OBSERVED_VALUE_KEY)) {
      shortResultJson.put(OBSERVED_VALUE_KEY, resultJson.getString(OBSERVED_VALUE_KEY));

      if (shortResultJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
        shortResultJson.remove(OBSERVED_VALUE_KEY);
        return shortResultJson.toString();
      }
    }

    if (resultJson.has(UNEXPECTED_COUNT_KEY) && resultJson.has(PARTIAL_UNEXPECTED_LIST_KEY)
      && resultJson.has(UNEXPECTED_PERCENT_KEY) && resultJson.has(UNEXPECTED_PERCENT_NONMISSING_KEY)) {

      shortResultJson.put(UNEXPECTED_COUNT_KEY, resultJson.getInt(UNEXPECTED_COUNT_KEY));
      shortResultJson.put(UNEXPECTED_PERCENT_KEY, resultJson.getFloat(UNEXPECTED_PERCENT_KEY));
      shortResultJson.put(UNEXPECTED_PERCENT_NONMISSING_KEY, resultJson.getFloat(UNEXPECTED_PERCENT_NONMISSING_KEY));
      shortResultJson.put(PARTIAL_UNEXPECTED_LIST_KEY, resultJson.getJSONArray(PARTIAL_UNEXPECTED_LIST_KEY));

      if (shortResultJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
        shortResultJson.remove(PARTIAL_UNEXPECTED_LIST_KEY);
        return shortResultJson.toString();
      }
    }

    return shortResultJson.toString();
  }

  public String validationResultShortenExceptionInfoField(String exceptionInfo) {
    JSONObject exceptionInfoJson;
    try {
      exceptionInfoJson = new JSONObject(exceptionInfo);
    } catch (JSONException e) {
      LOGGER.warning(String.format(
        "Parsing exceptionInfo field threw JSONException that should have been handled when verifying input.%n%s%n%s",
        e.getMessage(), Arrays.toString(e.getStackTrace())));
      exceptionInfoJson = new JSONObject();
    }

    JSONObject shortExceptionInfoJson = new JSONObject();
    String userMessage =
      "exception_info field exceeded max available space in SQL table, " +
        "download validation report file to access the complete info.";
    shortExceptionInfoJson.put("user_message", userMessage);
    shortExceptionInfoJson.put("raised_exception", exceptionInfoJson.getBoolean("raised_exception"));

    

    shortExceptionInfoJson.put(EXCEPTION_MESSAGE_KEY, exceptionInfoJson.getString(EXCEPTION_MESSAGE_KEY));
    if (shortExceptionInfoJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO) {
      shortExceptionInfoJson.remove(EXCEPTION_MESSAGE_KEY);
      return shortExceptionInfoJson.toString();
    }

    // exception_traceback cannot fit otherwise we would not be in this function

    return shortExceptionInfoJson.toString();
  }

  public String validationResultShortenExpectationConfigField(String expectationConfig, Integer expectationId) {
    JSONObject expectationConfigJson;
    try {
      expectationConfigJson = new JSONObject(expectationConfig);
    } catch (JSONException e) {
      LOGGER.warning(String.format(
        "Parsing expectationConfig field threw JSONException that should have " +
        " been handled when verifying input.\n%s\n%s",
        e.getMessage(), Arrays.toString(e.getStackTrace())));
      expectationConfigJson = new JSONObject();
    }

    // Create a shorten version with three same fields: expectation_type, kwargs, meta
    JSONObject shortexpectationConfigJson = new JSONObject();
    JSONObject configMeta = new JSONObject();
    // Store user message and expectationId in meta field.
    String userMessage = "expectation_config field exceeded max available space in SQL table, " +
        "download validation report file to access the complete info. ";
    configMeta.put("userMessage", userMessage);    
    configMeta.put("expectationId", expectationId);
    shortexpectationConfigJson.put("meta", configMeta);
    // Expectation type takes limited space
    shortexpectationConfigJson.put("expectation_type", expectationConfigJson.getString("expectation_type"));
    shortexpectationConfigJson.put("kwargs", new JSONObject());

    return shortexpectationConfigJson.toString();
  }

  public JSONObject convertValidationResultDTOToJson(ValidationResultDTO resultDTO) throws FeaturestoreException {
    JSONObject resultJSON = new JSONObject();

    try {
      resultJSON.put("success", resultDTO.getSuccess());
      resultJSON.put("exception_info", new JSONObject(resultDTO.getExceptionInfo()));
      resultJSON.put("result", new JSONObject(resultDTO.getResult()));
      resultJSON.put("meta", new JSONObject(resultDTO.getMeta()));
      resultJSON.put("expectation_config", new JSONObject(resultDTO.getExpectationConfig()));
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.VALIDATION_RESULT_IS_NOT_VALID_JSON, Level.WARNING, e.getMessage());
    }

    return resultJSON;
  }

  ////////////////////////////////////////
  //// Input Verification for Validation Result
  ///////////////////////////////////////

  public void verifyValidationResultDTOFields(ValidationResultDTO dto) throws FeaturestoreException {
    verifyValidationResultMeta(dto.getMeta());
    verifyValidationResultExpectationConfig(dto.getExpectationConfig());
    // override with default value if null
    dto.setResult(verifyValidationResultResult(dto.getResult()));
    dto.setExceptionInfo(verifyValidationResultExceptionInfo(dto.getExceptionInfo()));
  }

  public void verifyValidationResultMeta(String meta) throws FeaturestoreException {
    if (meta == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation result meta field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (meta.length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_META) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation result meta field %s exceeds the max allowed character length %d.",
          meta, MAX_CHARACTERS_IN_VALIDATION_RESULT_META)
      );
    }

    try {
      new JSONObject(meta);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result meta field %s is not a valid json.", meta),
        e.getMessage()
      );
    }
  }

  public void verifyValidationResultExpectationConfig(String expectationConfig) throws FeaturestoreException {
    if (expectationConfig == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation result expectation config field cannot be null. Pass an empty stringified JSON."
      );
    }

    // Long expectationConfig are shortened and need not throw an error

    try {
      new JSONObject(expectationConfig);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config field %s is not a valid json.", expectationConfig),
        e.getMessage()
      );
    }
  }

  public String verifyValidationResultExceptionInfo(String exceptionInfo) throws FeaturestoreException {
    if (exceptionInfo == null) {
      return "{}";
    }

    try {
      new JSONObject(exceptionInfo);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result exception info field %s is not a valid json.", exceptionInfo),
        e.getMessage()
      );
    }

    return exceptionInfo;
  }

  public String verifyValidationResultResult(String result) throws FeaturestoreException {
    // For result_format = {"result_format": "BOOLEAN_ONLY"}, result field is null. Turned into empty JSON.
    if (result == null) {
      return "{}";
    }

    // If not null it must be valid json object
    try {
      new JSONObject(result);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result result field %s is not a valid json.", result),
        e.getMessage()
      );
    }

    return result;
  }
}