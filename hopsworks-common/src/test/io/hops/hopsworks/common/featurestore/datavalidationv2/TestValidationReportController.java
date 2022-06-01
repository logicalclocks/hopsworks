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

import com.logicalclocks.shaded.org.apache.commons.lang3.StringUtils;
import io.hops.hopsworks.exceptions.FeaturestoreException;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_META;

public class TestValidationReportController {

  private ValidationReportController validationReportController = new ValidationReportController();

  private ValidationReportDTO makeValidValidationReportDTO() {
    ValidationReportDTO reportDTO = new ValidationReportDTO();
    reportDTO.setSuccess(true);
    reportDTO.setEvaluationParameters(
      "{\"verbosity\": \"Summary\"}"
    );
    reportDTO.setStatistics(
      "{\"succesful_expectations\": 12 }"
    );
    reportDTO.setMeta(
      "{\"whoAmI\": \"A random key-value pair\", \"validation_time\": \"20220311T130624.481059Z\"}"
    );
    // Not consistent to avoid unnecessary complication
    reportDTO.setValidationTime(
      new Date()
    );

    ArrayList<ValidationResultDTO> resultDTOs = new ArrayList<>();
    resultDTOs.add(makeValidValidationResultDTO());
    resultDTOs.add(makeValidValidationResultDTO());

    return reportDTO;
  }

  private ValidationResultDTO makeValidValidationResultDTO() {
    ValidationResultDTO resultDTO = new ValidationResultDTO();
    resultDTO.setExceptionInfo(
      "{\"raised_exception\": false,\"exception_message\": null,\"exception_traceback\": null}"
    );
    resultDTO.setMeta(
      "{\"whoAmI\":\"Result\"}"
    );
    resultDTO.setExpectationConfig(
      "{\"kwargs\":{\"column\":\"score_8\",\"max_value\":100,\"min_value\":4}," +
        "\"expectation_type\":\"expect_column_min_to_be_between\",\"meta\":{\"expectationId\":1028}}}"
    );
    resultDTO.setResult(
      "{\"observed_value\":4,\"element_count\":5,\"missing_count\":null,\"missing_percent\":null}"
    );
    resultDTO.setSuccess(true);

    return resultDTO;
  }

  // ValidationReport input validation

  @Test
  public void testVerifyValidationReportStatistics() {
    ValidationReportDTO reportDTO = makeValidValidationReportDTO();
    // Null
    reportDTO.setStatistics(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS + 10) + "\"}");
    reportDTO.setStatistics(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    reportDTO.setStatistics(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationReportMeta() {
    ValidationReportDTO reportDTO = makeValidValidationReportDTO();
    // Null
    reportDTO.setMeta(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_REPORT_META + 10) + "\"}");
    reportDTO.setMeta(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    reportDTO.setMeta(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationReportEvaluationParameters() {
    ValidationReportDTO reportDTO = makeValidValidationReportDTO();
    // Null
    reportDTO.setEvaluationParameters(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS + 10) + "\"}");
    reportDTO.setEvaluationParameters(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    reportDTO.setEvaluationParameters(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertReportDTOToPersistent(new Users(), new Featuregroup(), reportDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  // ValidationResult input validation

  @Test
  public void testVerifyValidationResultMeta() {
    ValidationResultDTO resultDTO = makeValidValidationResultDTO();
    // Null
    resultDTO.setMeta(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_RESULT_META + 10) + "\"}");
    resultDTO.setMeta(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setMeta(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultExpectationConfig() {
    ValidationResultDTO resultDTO = makeValidValidationResultDTO();
    // Null
    resultDTO.setExpectationConfig(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG + 10) + "\"}");
    resultDTO.setExpectationConfig(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setExpectationConfig(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultExceptionInfo() {
    ValidationResultDTO resultDTO = makeValidValidationResultDTO();
    // Null is silently turned into empty json for exception info

    // Long input is silently caught for exception info

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setExceptionInfo(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultResultAndObservedValue() {
    ValidationResultDTO resultDTO = makeValidValidationResultDTO();
    // Null is silently caught and made into empty json.

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setResult(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationReportController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());

    // Long input is silently caught and result is truncated for result field.
  }
}
