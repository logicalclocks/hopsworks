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

package io.hops.hopsworks.common.featurestore.datavalidationv2;

import com.logicalclocks.shaded.org.apache.commons.lang3.StringUtils;

import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_META;

public class TestValidationResultController {

  private ValidationResultController validationResultController = new ValidationResultController();
  private ValidationReport report;
  private ValidationResultDTO resultDTO;

  @Before
  public void setup() {
    resultDTO = makeValidValidationResultDTO();

    report = new ValidationReport();
    report.setValidationTime(new Date());
    report.setIngestionResult(IngestionResult.INGESTED);
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

  // ValidationResult input validation

  @Test
  public void testVerifyValidationResultMeta() {
    // Null
    resultDTO.setMeta(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_VALIDATION_RESULT_META + 10) + "\"}");
    resultDTO.setMeta(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setMeta(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultExpectationConfig() {
    // Null
    resultDTO.setExpectationConfig(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input are silently shortened.

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setExpectationConfig(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultExceptionInfo() {
    // Null is silently turned into empty json for exception info

    // Long input is silently caught for exception info

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setExceptionInfo(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyValidationResultResult() {
    // Null is silently caught and made into empty json.

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    resultDTO.setResult(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> validationResultController.convertResultDTOToPersistent(new ValidationReport(), resultDTO)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());

    // Long input is silently caught and result is truncated for result field.
  }
}
