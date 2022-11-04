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

import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;

import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_KWARGS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_META;

public class TestExpectationController {
  // Expectation Input Validation
  private ExpectationController expectationController = new ExpectationController();
  private ExpectationDTO expectationDTO;
  private List<String> featureNames;

  @Before
  public void setup() {
    expectationDTO = new ExpectationDTO();
    expectationDTO.setExpectationType("expect_column_max_to_be_between");
    expectationDTO.setMeta("{\"expectationId\": 12}");
    expectationDTO.setKwargs("{\"min_value\": 0, \"max_value\": 1}");

    featureNames = new ArrayList<String>();
    featureNames.add("featureA");
    featureNames.add("featureB");
  }
  
  @Test
  public void testVerifyExpectationMeta() {
    // Null
    expectationDTO.setMeta(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
      StringUtils.repeat("A", MAX_CHARACTERS_IN_EXPECTATION_META + 10) + "\"}");
    expectationDTO.setMeta(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    expectationDTO.setMeta(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyExpectationKwargs() {
    // Null
    expectationDTO.setKwargs(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \"" +
        StringUtils.repeat("A", MAX_CHARACTERS_IN_EXPECTATION_KWARGS + 10) + "\"}");
    expectationDTO.setKwargs(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    expectationDTO.setKwargs(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyExpectationExpectationType() {
    // Null
    expectationDTO.setExpectationType(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      (int) nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = StringUtils.repeat("A", MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE + 10);
    expectationDTO.setExpectationType(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to exceed max character error: ", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());
  }

  @Test
  public void testVerifyExpectationKwargsColumn() {
    expectationDTO.setKwargs("{\"column\":\"feature\"}");
    FeaturestoreException featureNameNotFoundException = assertThrows(
      FeaturestoreException.class, 
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to feature not found error: ", 210,
    (int) featureNameNotFoundException.getErrorCode().getCode() - featureNameNotFoundException.getErrorCode().getRange());

    expectationDTO.setKwargs("{\"columnA\":\"feature\", \"columnB\":\"featureB\"}");
    FeaturestoreException featureNameNotFoundExceptionBiColumn = assertThrows(
      FeaturestoreException.class, 
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to feature not found error: ", 210,
    (int) featureNameNotFoundExceptionBiColumn.getErrorCode().getCode() - featureNameNotFoundExceptionBiColumn.getErrorCode().getRange());

    expectationDTO.setKwargs("{\"column_list\":[\"feature\"]}");
    FeaturestoreException featureNameNotFoundExceptionListColumn = assertThrows(
      FeaturestoreException.class, 
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to feature not found error: ", 210,
    (int) featureNameNotFoundExceptionListColumn.getErrorCode().getCode() - featureNameNotFoundExceptionListColumn.getErrorCode().getRange());
  
    expectationDTO.setKwargs("{\"column_set\":[\"feature\"]}");
    FeaturestoreException featureNameNotFoundExceptionSetColumn = assertThrows(
      FeaturestoreException.class, 
      () -> expectationController.verifyExpectationFields(expectationDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to feature not found error: ", 210,
    (int) featureNameNotFoundExceptionSetColumn.getErrorCode().getCode() - featureNameNotFoundExceptionSetColumn.getErrorCode().getRange());
  }

}
